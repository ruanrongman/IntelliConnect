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

class ProcessGains {

  /* Processing of gains */
  static void silk_process_gains(
      SilkChannelEncoder psEnc, /* I/O Encoder state */
      SilkEncoderControl psEncCtrl, /* I/O Encoder control */
      int condCoding /* I The type of conditional coding to use */
  ) {
    SilkShapeState psShapeSt = psEnc.sShape;
    int k;
    int s_Q16, InvMaxSqrVal_Q16, gain, gain_squared, ResNrg, ResNrgPart, quant_offset_Q10;

    /* Gain reduction when LTP coding gain is high */
    if (psEnc.indices.signalType == SilkConstants.TYPE_VOICED) {
      /* s = -0.5f * silk_sigmoid( 0.25f * ( psEncCtrl.LTPredCodGain - 12.0f ) ); */
      s_Q16 = 0 - Sigmoid.silk_sigm_Q15(Inlines.silk_RSHIFT_ROUND(psEncCtrl.LTPredCodGain_Q7
          - ((int) ((12.0f) * ((long) 1 << (7)) + 0.5))/* Inlines.SILK_CONST(12.0f, 7) */, 4));
      for (k = 0; k < psEnc.nb_subfr; k++) {
        psEncCtrl.Gains_Q16[k] =
            Inlines.silk_SMLAWB(psEncCtrl.Gains_Q16[k], psEncCtrl.Gains_Q16[k], s_Q16);
      }
    }

    /* Limit the quantized signal */
    /* InvMaxSqrVal = pow( 2.0f, 0.33f * ( 21.0f - SNR_dB ) ) / subfr_length; */
    InvMaxSqrVal_Q16 = Inlines.silk_DIV32_16(Inlines.silk_log2lin(
        Inlines.silk_SMULWB(
            ((int) ((21 + 16 / 0.33f) * ((long) 1 << (7)) + 0.5))
                /* Inlines.SILK_CONST(21 + 16 / 0.33f, 7) */ - psEnc.SNR_dB_Q7,
            ((int) ((0.33f) * ((long) 1 << (16)) + 0.5))/* Inlines.SILK_CONST(0.33f, 16) */)),
        psEnc.subfr_length);

    for (k = 0; k < psEnc.nb_subfr; k++) {
      /* Soft limit on ratio residual energy and squared gains */
      ResNrg = psEncCtrl.ResNrg[k];
      ResNrgPart = Inlines.silk_SMULWW(ResNrg, InvMaxSqrVal_Q16);
      if (psEncCtrl.ResNrgQ[k] > 0) {
        ResNrgPart = Inlines.silk_RSHIFT_ROUND(ResNrgPart, psEncCtrl.ResNrgQ[k]);
      } else if (ResNrgPart >= Inlines.silk_RSHIFT(Integer.MAX_VALUE, -psEncCtrl.ResNrgQ[k])) {
        ResNrgPart = Integer.MAX_VALUE;
      } else {
        ResNrgPart = Inlines.silk_LSHIFT(ResNrgPart, -psEncCtrl.ResNrgQ[k]);
      }
      gain = psEncCtrl.Gains_Q16[k];
      gain_squared = Inlines.silk_ADD_SAT32(ResNrgPart, Inlines.silk_SMMUL(gain, gain));
      if (gain_squared < Short.MAX_VALUE) {
        /* recalculate with higher precision */
        gain_squared = Inlines.silk_SMLAWW(Inlines.silk_LSHIFT(ResNrgPart, 16), gain, gain);
        Inlines.OpusAssert(gain_squared > 0);
        gain = Inlines.silk_SQRT_APPROX(gain_squared);
        /* Q8 */
        gain = Inlines.silk_min(gain, Integer.MAX_VALUE >> 8);
        psEncCtrl.Gains_Q16[k] = Inlines.silk_LSHIFT_SAT32(gain, 8);
        /* Q16 */
      } else {
        gain = Inlines.silk_SQRT_APPROX(gain_squared);
        /* Q0 */
        gain = Inlines.silk_min(gain, Integer.MAX_VALUE >> 16);
        psEncCtrl.Gains_Q16[k] = Inlines.silk_LSHIFT_SAT32(gain, 16);
        /* Q16 */
      }

    }

    /* Save unquantized gains and gain Index */
    System.arraycopy(psEncCtrl.Gains_Q16, 0, psEncCtrl.GainsUnq_Q16, 0, psEnc.nb_subfr);
    psEncCtrl.lastGainIndexPrev = psShapeSt.LastGainIndex;

    /* Quantize gains */
    BoxedValueByte boxed_lastGainIndex = new BoxedValueByte(psShapeSt.LastGainIndex);
    GainQuantization.silk_gains_quant(psEnc.indices.GainsIndices, psEncCtrl.Gains_Q16,
        boxed_lastGainIndex, condCoding == SilkConstants.CODE_CONDITIONALLY ? 1 : 0,
        psEnc.nb_subfr);
    psShapeSt.LastGainIndex = boxed_lastGainIndex.Val;

    /*
     * Set quantizer offset for voiced signals. Larger offset when LTP coding gain is low or tilt is
     * high (ie low-pass)
     */
    if (psEnc.indices.signalType == SilkConstants.TYPE_VOICED) {
      if (psEncCtrl.LTPredCodGain_Q7 + Inlines.silk_RSHIFT(psEnc.input_tilt_Q15,
          8) > ((int) ((1.0f) * ((long) 1 << (7)) + 0.5))/* Inlines.SILK_CONST(1.0f, 7) */) {
        psEnc.indices.quantOffsetType = 0;
      } else {
        psEnc.indices.quantOffsetType = 1;
      }
    }

    /* Quantizer boundary adjustment */
    quant_offset_Q10 =
        SilkTables.silk_Quantization_Offsets_Q10[psEnc.indices.signalType >> 1][psEnc.indices.quantOffsetType];
    psEncCtrl.Lambda_Q10 = ((int) ((TuningParameters.LAMBDA_OFFSET) * ((long) 1 << (10)) + 0.5))/*
                                                                                                 * Inlines
                                                                                                 * .
                                                                                                 * SILK_CONST
                                                                                                 * (
                                                                                                 * TuningParameters
                                                                                                 * .
                                                                                                 * LAMBDA_OFFSET,
                                                                                                 * 10)
                                                                                                 */
        + Inlines.silk_SMULBB(
            ((int) ((TuningParameters.LAMBDA_DELAYED_DECISIONS) * ((long) 1 << (10))
                + 0.5))/* Inlines.SILK_CONST(TuningParameters.LAMBDA_DELAYED_DECISIONS, 10) */,
            psEnc.nStatesDelayedDecision)
        + Inlines.silk_SMULWB(
            ((int) ((TuningParameters.LAMBDA_SPEECH_ACT) * ((long) 1 << (18))
                + 0.5))/* Inlines.SILK_CONST(TuningParameters.LAMBDA_SPEECH_ACT, 18) */,
            psEnc.speech_activity_Q8)
        + Inlines.silk_SMULWB(
            ((int) ((TuningParameters.LAMBDA_INPUT_QUALITY) * ((long) 1 << (12))
                + 0.5))/* Inlines.SILK_CONST(TuningParameters.LAMBDA_INPUT_QUALITY, 12) */,
            psEncCtrl.input_quality_Q14)
        + Inlines.silk_SMULWB(
            ((int) ((TuningParameters.LAMBDA_CODING_QUALITY) * ((long) 1 << (12))
                + 0.5))/* Inlines.SILK_CONST(TuningParameters.LAMBDA_CODING_QUALITY, 12) */,
            psEncCtrl.coding_quality_Q14)
        + Inlines.silk_SMULWB(
            ((int) ((TuningParameters.LAMBDA_QUANT_OFFSET) * ((long) 1 << (16))
                + 0.5))/* Inlines.SILK_CONST(TuningParameters.LAMBDA_QUANT_OFFSET, 16) */,
            quant_offset_Q10);

    Inlines.OpusAssert(psEncCtrl.Lambda_Q10 > 0);
    Inlines.OpusAssert(psEncCtrl.Lambda_Q10 < ((int) ((2) * ((long) 1 << (10)) + 0.5))/*
                                                                                       * Inlines.
                                                                                       * SILK_CONST(
                                                                                       * 2, 10)
                                                                                       */);
  }
}
