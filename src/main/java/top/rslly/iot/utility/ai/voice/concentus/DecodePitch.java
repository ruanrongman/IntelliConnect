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

class DecodePitch {

  static void silk_decode_pitch(
      short lagIndex, /* I */
      byte contourIndex, /* O */
      int[] pitch_lags, /* O 4 pitch values */
      int Fs_kHz, /* I sampling frequency (kHz) */
      int nb_subfr /* I number of sub frames */
  ) {
    int lag, k, min_lag, max_lag;
    byte[][] Lag_CB_ptr;

    if (Fs_kHz == 8) {
      if (nb_subfr == SilkConstants.PE_MAX_NB_SUBFR) {
        Lag_CB_ptr = SilkTables.silk_CB_lags_stage2;
      } else {
        Inlines.OpusAssert(nb_subfr == SilkConstants.PE_MAX_NB_SUBFR >> 1);
        Lag_CB_ptr = SilkTables.silk_CB_lags_stage2_10_ms;
      }
    } else if (nb_subfr == SilkConstants.PE_MAX_NB_SUBFR) {
      Lag_CB_ptr = SilkTables.silk_CB_lags_stage3;
    } else {
      Inlines.OpusAssert(nb_subfr == SilkConstants.PE_MAX_NB_SUBFR >> 1);
      Lag_CB_ptr = SilkTables.silk_CB_lags_stage3_10_ms;
    }

    min_lag = Inlines.silk_SMULBB(SilkConstants.PE_MIN_LAG_MS, Fs_kHz);
    max_lag = Inlines.silk_SMULBB(SilkConstants.PE_MAX_LAG_MS, Fs_kHz);
    lag = min_lag + lagIndex;

    for (k = 0; k < nb_subfr; k++) {
      pitch_lags[k] = lag + Lag_CB_ptr[k][contourIndex];
      pitch_lags[k] = Inlines.silk_LIMIT(pitch_lags[k], min_lag, max_lag);
    }
  }
}
