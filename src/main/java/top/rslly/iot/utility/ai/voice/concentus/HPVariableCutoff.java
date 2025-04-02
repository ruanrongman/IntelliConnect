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

class HPVariableCutoff {

  /// <summary>
  /// High-pass filter with cutoff frequency adaptation based on pitch lag statistics
  /// </summary>
  /// <param name="state_Fxx">I/O Encoder states</param>
  static void silk_HP_variable_cutoff(SilkChannelEncoder[] state_Fxx) {
    int quality_Q15;
    int pitch_freq_Hz_Q16, pitch_freq_log_Q7, delta_freq_Q7;
    SilkChannelEncoder psEncC1 = state_Fxx[0];

    /* Adaptive cutoff frequency: estimate low end of pitch frequency range */
    if (psEncC1.prevSignalType == SilkConstants.TYPE_VOICED) {
      /* difference, in log domain */
      pitch_freq_Hz_Q16 = Inlines.silk_DIV32_16(
          Inlines.silk_LSHIFT(Inlines.silk_MUL(psEncC1.fs_kHz, 1000), 16), psEncC1.prevLag);
      pitch_freq_log_Q7 = Inlines.silk_lin2log(pitch_freq_Hz_Q16) - (16 << 7);

      /* adjustment based on quality */
      quality_Q15 = psEncC1.input_quality_bands_Q15[0];
      pitch_freq_log_Q7 =
          Inlines.silk_SMLAWB(pitch_freq_log_Q7,
              Inlines.silk_SMULWB(Inlines.silk_LSHIFT(-quality_Q15, 2), quality_Q15),
              pitch_freq_log_Q7 - (Inlines.silk_lin2log(
                  ((int) ((TuningParameters.VARIABLE_HP_MIN_CUTOFF_HZ) * ((long) 1 << (16))
                      + 0.5))/*
                              * Inlines.SILK_CONST(TuningParameters.VARIABLE_HP_MIN_CUTOFF_HZ, 16)
                              */)
                  - (16 << 7)));

      /* delta_freq = pitch_freq_log - psEnc.variable_HP_smth1; */
      delta_freq_Q7 = pitch_freq_log_Q7 - Inlines.silk_RSHIFT(psEncC1.variable_HP_smth1_Q15, 8);
      if (delta_freq_Q7 < 0) {
        /* less smoothing for decreasing pitch frequency, to track something close to the minimum */
        delta_freq_Q7 = Inlines.silk_MUL(delta_freq_Q7, 3);
      }

      /* limit delta, to reduce impact of outliers in pitch estimation */
      delta_freq_Q7 = Inlines.silk_LIMIT_32(
          delta_freq_Q7,
          0 - ((int) ((TuningParameters.VARIABLE_HP_MAX_DELTA_FREQ) * ((long) 1 << (7)) + 0.5))/*
                                                                                                * Inlines
                                                                                                * .
                                                                                                * SILK_CONST
                                                                                                * (
                                                                                                * TuningParameters
                                                                                                * .
                                                                                                * VARIABLE_HP_MAX_DELTA_FREQ,
                                                                                                * 7)
                                                                                                */,
          ((int) ((TuningParameters.VARIABLE_HP_MAX_DELTA_FREQ) * ((long) 1 << (7)) + 0.5))/*
                                                                                            * Inlines
                                                                                            * .
                                                                                            * SILK_CONST
                                                                                            * (
                                                                                            * TuningParameters
                                                                                            * .
                                                                                            * VARIABLE_HP_MAX_DELTA_FREQ,
                                                                                            * 7)
                                                                                            */);

      /* update smoother */
      psEncC1.variable_HP_smth1_Q15 = Inlines.silk_SMLAWB(psEncC1.variable_HP_smth1_Q15,
          Inlines.silk_SMULBB(psEncC1.speech_activity_Q8, delta_freq_Q7),
          ((int) ((TuningParameters.VARIABLE_HP_SMTH_COEF1) * ((long) 1 << (16)) + 0.5))/*
                                                                                         * Inlines.
                                                                                         * SILK_CONST
                                                                                         * (
                                                                                         * TuningParameters
                                                                                         * .
                                                                                         * VARIABLE_HP_SMTH_COEF1,
                                                                                         * 16)
                                                                                         */);

      /* limit frequency range */
      psEncC1.variable_HP_smth1_Q15 = Inlines.silk_LIMIT_32(psEncC1.variable_HP_smth1_Q15,
          Inlines.silk_LSHIFT(Inlines.silk_lin2log(TuningParameters.VARIABLE_HP_MIN_CUTOFF_HZ), 8),
          Inlines.silk_LSHIFT(Inlines.silk_lin2log(TuningParameters.VARIABLE_HP_MAX_CUTOFF_HZ), 8));
    }
  }
}
