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

class FindPitchLags {

  /* Find pitch lags */
  static void silk_find_pitch_lags(
      SilkChannelEncoder psEnc, /* I/O encoder state */
      SilkEncoderControl psEncCtrl, /* I/O encoder control */
      short[] res, /* O residual */
      short[] x, /* I Speech signal */
      int x_ptr) {
    int buf_len, i, scale;
    int thrhld_Q13, res_nrg;
    int x_buf, x_buf_ptr;
    short[] Wsig;
    int Wsig_ptr;
    int[] auto_corr = new int[SilkConstants.MAX_FIND_PITCH_LPC_ORDER + 1];
    short[] rc_Q15 = new short[SilkConstants.MAX_FIND_PITCH_LPC_ORDER];
    int[] A_Q24 = new int[SilkConstants.MAX_FIND_PITCH_LPC_ORDER];
    short[] A_Q12 = new short[SilkConstants.MAX_FIND_PITCH_LPC_ORDER];

    /**
     * ***************************************
     */
    /* Set up buffer lengths etc based on Fs */
    /**
     * ***************************************
     */
    buf_len = psEnc.la_pitch + psEnc.frame_length + psEnc.ltp_mem_length;

    /* Safety check */
    Inlines.OpusAssert(buf_len >= psEnc.pitch_LPC_win_length);

    x_buf = x_ptr - psEnc.ltp_mem_length;

    /**
     * **********************************
     */
    /* Estimate LPC AR coefficients */
    /**
     * **********************************
     */

    /* Calculate windowed signal */
    Wsig = new short[psEnc.pitch_LPC_win_length];

    /* First LA_LTP samples */
    x_buf_ptr = x_buf + buf_len - psEnc.pitch_LPC_win_length;
    Wsig_ptr = 0;
    ApplySineWindow.silk_apply_sine_window(Wsig, Wsig_ptr, x, x_buf_ptr, 1, psEnc.la_pitch);

    /* Middle un - windowed samples */
    Wsig_ptr += psEnc.la_pitch;
    x_buf_ptr += psEnc.la_pitch;
    System.arraycopy(x, x_buf_ptr, Wsig, Wsig_ptr,
        (psEnc.pitch_LPC_win_length - Inlines.silk_LSHIFT(psEnc.la_pitch, 1)));

    /* Last LA_LTP samples */
    Wsig_ptr += psEnc.pitch_LPC_win_length - Inlines.silk_LSHIFT(psEnc.la_pitch, 1);
    x_buf_ptr += psEnc.pitch_LPC_win_length - Inlines.silk_LSHIFT(psEnc.la_pitch, 1);
    ApplySineWindow.silk_apply_sine_window(Wsig, Wsig_ptr, x, x_buf_ptr, 2, psEnc.la_pitch);

    /* Calculate autocorrelation sequence */
    BoxedValueInt boxed_scale = new BoxedValueInt(0);
    Autocorrelation.silk_autocorr(auto_corr, boxed_scale, Wsig, psEnc.pitch_LPC_win_length,
        psEnc.pitchEstimationLPCOrder + 1);
    scale = boxed_scale.Val;

    /* Add white noise, as fraction of energy */
    auto_corr[0] =
        Inlines.silk_SMLAWB(auto_corr[0], auto_corr[0],
            ((int) ((TuningParameters.FIND_PITCH_WHITE_NOISE_FRACTION) * ((long) 1 << (16))
                + 0.5))/*
                        * Inlines.SILK_CONST(TuningParameters.FIND_PITCH_WHITE_NOISE_FRACTION, 16)
                        */)
            + 1;

    /* Calculate the reflection coefficients using schur */
    res_nrg = Schur.silk_schur(rc_Q15, auto_corr, psEnc.pitchEstimationLPCOrder);

    /* Prediction gain */
    psEncCtrl.predGain_Q16 =
        Inlines.silk_DIV32_varQ(auto_corr[0], Inlines.silk_max_int(res_nrg, 1), 16);

    /* Convert reflection coefficients to prediction coefficients */
    K2A.silk_k2a(A_Q24, rc_Q15, psEnc.pitchEstimationLPCOrder);

    /* Convert From 32 bit Q24 to 16 bit Q12 coefs */
    for (i = 0; i < psEnc.pitchEstimationLPCOrder; i++) {
      A_Q12[i] = (short) Inlines.silk_SAT16(Inlines.silk_RSHIFT(A_Q24[i], 12));
    }

    /* Do BWE */
    BWExpander
        .silk_bwexpander(A_Q12, psEnc.pitchEstimationLPCOrder,
            ((int) ((TuningParameters.FIND_PITCH_BANDWIDTH_EXPANSION) * ((long) 1 << (16))
                + 0.5))/* Inlines.SILK_CONST(TuningParameters.FIND_PITCH_BANDWIDTH_EXPANSION, 16) */);

    /**
     * **************************************
     */
    /* LPC analysis filtering */
    /**
     * **************************************
     */
    Filters.silk_LPC_analysis_filter(res, 0, x, x_buf, A_Q12, 0, buf_len,
        psEnc.pitchEstimationLPCOrder);

    if (psEnc.indices.signalType != SilkConstants.TYPE_NO_VOICE_ACTIVITY
        && psEnc.first_frame_after_reset == 0) {
      /* Threshold for pitch estimator */
      thrhld_Q13 = ((int) ((0.6f) * ((long) 1 << (13)) + 0.5))/* Inlines.SILK_CONST(0.6f, 13) */;
      thrhld_Q13 = Inlines.silk_SMLABB(thrhld_Q13,
          ((int) ((-0.004f) * ((long) 1 << (13)) + 0.5))/* Inlines.SILK_CONST(-0.004f, 13) */,
          psEnc.pitchEstimationLPCOrder);
      thrhld_Q13 = Inlines.silk_SMLAWB(thrhld_Q13,
          ((int) ((-0.1f) * ((long) 1 << (21)) + 0.5))/* Inlines.SILK_CONST(-0.1f, 21) */,
          psEnc.speech_activity_Q8);
      thrhld_Q13 = Inlines.silk_SMLABB(thrhld_Q13,
          ((int) ((-0.15f) * ((long) 1 << (13)) + 0.5))/* Inlines.SILK_CONST(-0.15f, 13) */,
          Inlines.silk_RSHIFT(psEnc.prevSignalType, 1));
      thrhld_Q13 = Inlines.silk_SMLAWB(thrhld_Q13,
          ((int) ((-0.1f) * ((long) 1 << (14)) + 0.5))/* Inlines.SILK_CONST(-0.1f, 14) */,
          psEnc.input_tilt_Q15);
      thrhld_Q13 = Inlines.silk_SAT16(thrhld_Q13);

      /**
       * **************************************
       */
      /* Call pitch estimator */
      /**
       * **************************************
       */
      BoxedValueShort boxed_lagIndex = new BoxedValueShort(psEnc.indices.lagIndex);
      BoxedValueByte boxed_contourIndex = new BoxedValueByte(psEnc.indices.contourIndex);
      BoxedValueInt boxed_LTPcorr = new BoxedValueInt(psEnc.LTPCorr_Q15);
      if (PitchAnalysisCore.silk_pitch_analysis_core(res, psEncCtrl.pitchL, boxed_lagIndex,
          boxed_contourIndex,
          boxed_LTPcorr, psEnc.prevLag, psEnc.pitchEstimationThreshold_Q16,
          (int) thrhld_Q13, psEnc.fs_kHz, psEnc.pitchEstimationComplexity, psEnc.nb_subfr) == 0) {
        psEnc.indices.signalType = SilkConstants.TYPE_VOICED;
      } else {
        psEnc.indices.signalType = SilkConstants.TYPE_UNVOICED;
      }

      psEnc.indices.lagIndex = boxed_lagIndex.Val;
      psEnc.indices.contourIndex = boxed_contourIndex.Val;
      psEnc.LTPCorr_Q15 = boxed_LTPcorr.Val;
    } else {
      Arrays.MemSet(psEncCtrl.pitchL, 0, SilkConstants.MAX_NB_SUBFR);
      psEnc.indices.lagIndex = 0;
      psEnc.indices.contourIndex = 0;
      psEnc.LTPCorr_Q15 = 0;
    }
  }
}
