/**
 * Copyright © 2023-2030 The ruanrongman Authors
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package top.rslly.iot.utility.ai.voice.concentus;

class FindPredCoefs {

  static void silk_find_pred_coefs(
      SilkChannelEncoder psEnc, /* I/O encoder state */
      SilkEncoderControl psEncCtrl, /* I/O encoder control */
      short[] res_pitch, /* I Residual from pitch analysis */
      short[] x, /* I Speech signal */
      int x_ptr,
      int condCoding /* I The type of conditional coding to use */
  ) {
    int i;
    int[] invGains_Q16 = new int[SilkConstants.MAX_NB_SUBFR];
    int[] local_gains = new int[SilkConstants.MAX_NB_SUBFR];
    int[] Wght_Q15 = new int[SilkConstants.MAX_NB_SUBFR];
    short[] NLSF_Q15 = new short[SilkConstants.MAX_LPC_ORDER];
    int x_ptr2;
    int x_pre_ptr;
    short[] LPC_in_pre;
    int tmp, min_gain_Q16, minInvGain_Q30;
    int[] LTP_corrs_rshift = new int[SilkConstants.MAX_NB_SUBFR];

    /* weighting for weighted least squares */
    min_gain_Q16 = Integer.MAX_VALUE >> 6;
    for (i = 0; i < psEnc.nb_subfr; i++) {
      min_gain_Q16 = Inlines.silk_min(min_gain_Q16, psEncCtrl.Gains_Q16[i]);
    }
    for (i = 0; i < psEnc.nb_subfr; i++) {
      /* Divide to Q16 */
      Inlines.OpusAssert(psEncCtrl.Gains_Q16[i] > 0);
      /*
       * Invert and normalize gains, and ensure that maximum invGains_Q16 is within range of a 16
       * bit int
       */
      invGains_Q16[i] = Inlines.silk_DIV32_varQ(min_gain_Q16, psEncCtrl.Gains_Q16[i], 16 - 2);

      /* Ensure Wght_Q15 a minimum value 1 */
      invGains_Q16[i] = Inlines.silk_max(invGains_Q16[i], 363);

      /* Square the inverted gains */
      Inlines.OpusAssert(invGains_Q16[i] == Inlines.silk_SAT16(invGains_Q16[i]));
      tmp = Inlines.silk_SMULWB(invGains_Q16[i], invGains_Q16[i]);
      Wght_Q15[i] = Inlines.silk_RSHIFT(tmp, 1);

      /* Invert the inverted and normalized gains */
      local_gains[i] = Inlines.silk_DIV32(((int) 1 << 16), invGains_Q16[i]);
    }

    LPC_in_pre = new short[psEnc.nb_subfr * psEnc.predictLPCOrder + psEnc.frame_length];
    if (psEnc.indices.signalType == SilkConstants.TYPE_VOICED) {
      int[] WLTP;

      /**
       * *******
       */
      /* VOICED */
      /**
       * *******
       */
      Inlines.OpusAssert(psEnc.ltp_mem_length - psEnc.predictLPCOrder >= psEncCtrl.pitchL[0]
          + SilkConstants.LTP_ORDER / 2);

      WLTP = new int[psEnc.nb_subfr * SilkConstants.LTP_ORDER * SilkConstants.LTP_ORDER];

      /* LTP analysis */
      BoxedValueInt boxed_codgain = new BoxedValueInt(psEncCtrl.LTPredCodGain_Q7);
      FindLTP.silk_find_LTP(psEncCtrl.LTPCoef_Q14, WLTP, boxed_codgain,
          res_pitch, psEncCtrl.pitchL, Wght_Q15, psEnc.subfr_length,
          psEnc.nb_subfr, psEnc.ltp_mem_length, LTP_corrs_rshift);
      psEncCtrl.LTPredCodGain_Q7 = boxed_codgain.Val;

      /* Quantize LTP gain parameters */
      BoxedValueByte boxed_periodicity = new BoxedValueByte(psEnc.indices.PERIndex);
      BoxedValueInt boxed_gain = new BoxedValueInt(psEnc.sum_log_gain_Q7);
      QuantizeLTPGains.silk_quant_LTP_gains(psEncCtrl.LTPCoef_Q14, psEnc.indices.LTPIndex,
          boxed_periodicity,
          boxed_gain, WLTP, psEnc.mu_LTP_Q9, psEnc.LTPQuantLowComplexity, psEnc.nb_subfr);
      psEnc.indices.PERIndex = boxed_periodicity.Val;
      psEnc.sum_log_gain_Q7 = boxed_gain.Val;

      /* Control LTP scaling */
      LTPScaleControl.silk_LTP_scale_ctrl(psEnc, psEncCtrl, condCoding);

      /* Create LTP residual */
      LTPAnalysisFilter.silk_LTP_analysis_filter(LPC_in_pre, x, x_ptr - psEnc.predictLPCOrder,
          psEncCtrl.LTPCoef_Q14,
          psEncCtrl.pitchL, invGains_Q16, psEnc.subfr_length, psEnc.nb_subfr,
          psEnc.predictLPCOrder);

    } else {
      /**
       * *********
       */
      /* UNVOICED */
      /**
       * *********
       */
      /* Create signal with prepended subframes, scaled by inverse gains */
      x_ptr2 = x_ptr - psEnc.predictLPCOrder;
      x_pre_ptr = 0;
      for (i = 0; i < psEnc.nb_subfr; i++) {
        Inlines.silk_scale_copy_vector16(LPC_in_pre, x_pre_ptr, x, x_ptr2, invGains_Q16[i],
            psEnc.subfr_length + psEnc.predictLPCOrder);
        x_pre_ptr += psEnc.subfr_length + psEnc.predictLPCOrder;
        x_ptr2 += psEnc.subfr_length;
      }

      Arrays.MemSet(psEncCtrl.LTPCoef_Q14, (short) 0, psEnc.nb_subfr * SilkConstants.LTP_ORDER);
      psEncCtrl.LTPredCodGain_Q7 = 0;
      psEnc.sum_log_gain_Q7 = 0;
    }

    /* Limit on total predictive coding gain */
    if (psEnc.first_frame_after_reset != 0) {
      minInvGain_Q30 =
          ((int) ((1.0f / SilkConstants.MAX_PREDICTION_POWER_GAIN_AFTER_RESET) * ((long) 1 << (30))
              + 0.5))/*
                      * Inlines.SILK_CONST(1.0f /
                      * SilkConstants.MAX_PREDICTION_POWER_GAIN_AFTER_RESET, 30)
                      */;
    } else {
      minInvGain_Q30 = Inlines.silk_log2lin(Inlines.silk_SMLAWB(
          16 << 7, (int) psEncCtrl.LTPredCodGain_Q7,
          ((int) ((1.0f / 3f) * ((long) 1 << (16)) + 0.5))/* Inlines.SILK_CONST(1.0f / 3f, 16) */));
      /* Q16 */
      minInvGain_Q30 = Inlines.silk_DIV32_varQ(minInvGain_Q30,
          Inlines.silk_SMULWW(((int) ((SilkConstants.MAX_PREDICTION_POWER_GAIN) * ((long) 1 << (0))
              + 0.5))/* Inlines.SILK_CONST(SilkConstants.MAX_PREDICTION_POWER_GAIN, 0) */,
              Inlines.silk_SMLAWB(
                  ((int) ((0.25f) * ((long) 1 << (18)) + 0.5))/* Inlines.SILK_CONST(0.25f, 18) */,
                  ((int) ((0.75f) * ((long) 1 << (18)) + 0.5))/* Inlines.SILK_CONST(0.75f, 18) */,
                  psEncCtrl.coding_quality_Q14)),
          14);
    }

    /*
     * LPC_in_pre contains the LTP-filtered input for voiced, and the unfiltered input for unvoiced
     */
    FindLPC.silk_find_LPC(psEnc, NLSF_Q15, LPC_in_pre, minInvGain_Q30);

    /* Quantize LSFs */
    NLSF.silk_process_NLSFs(psEnc, psEncCtrl.PredCoef_Q12, NLSF_Q15, psEnc.prev_NLSFq_Q15);

    /* Calculate residual energy using quantized LPC coefficients */
    ResidualEnergy.silk_residual_energy(psEncCtrl.ResNrg, psEncCtrl.ResNrgQ, LPC_in_pre,
        psEncCtrl.PredCoef_Q12, local_gains,
        psEnc.subfr_length, psEnc.nb_subfr, psEnc.predictLPCOrder);

    /* Copy to prediction struct for use in next frame for interpolation */
    System.arraycopy(NLSF_Q15, 0, psEnc.prev_NLSFq_Q15, 0, SilkConstants.MAX_LPC_ORDER);
  }
}
