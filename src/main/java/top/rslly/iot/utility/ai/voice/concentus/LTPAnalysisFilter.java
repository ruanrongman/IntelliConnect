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

class LTPAnalysisFilter {

  static void silk_LTP_analysis_filter(
      short[] LTP_res, /*
                        * O LTP residual signal of length SilkConstants.MAX_NB_SUBFR * ( pre_length
                        * + subfr_length )
                        */
      short[] x, /* I Pointer to input signal with at least max( pitchL ) preceding samples */
      int x_ptr,
      short[] LTPCoef_Q14, /*
                            * I LTP_ORDER LTP coefficients for each MAX_NB_SUBFR subframe
                            * [SilkConstants.LTP_ORDER * SilkConstants.MAX_NB_SUBFR]
                            */
      int[] pitchL, /* I Pitch lag, one for each subframe [SilkConstants.MAX_NB_SUBFR] */
      int[] invGains_Q16, /*
                           * I Inverse quantization gains, one for each subframe
                           * [SilkConstants.MAX_NB_SUBFR]
                           */
      int subfr_length, /* I Length of each subframe */
      int nb_subfr, /* I Number of subframes */
      int pre_length /* I Length of the preceding samples starting at &x[0] for each subframe */
  ) {
    int x_ptr2, x_lag_ptr;
    short[] Btmp_Q14 = new short[SilkConstants.LTP_ORDER];
    int LTP_res_ptr;
    int k, i;
    int LTP_est;

    x_ptr2 = x_ptr;
    LTP_res_ptr = 0;
    for (k = 0; k < nb_subfr; k++) {
      x_lag_ptr = x_ptr2 - pitchL[k];

      Btmp_Q14[0] = LTPCoef_Q14[k * SilkConstants.LTP_ORDER];
      Btmp_Q14[1] = LTPCoef_Q14[k * SilkConstants.LTP_ORDER + 1];
      Btmp_Q14[2] = LTPCoef_Q14[k * SilkConstants.LTP_ORDER + 2];
      Btmp_Q14[3] = LTPCoef_Q14[k * SilkConstants.LTP_ORDER + 3];
      Btmp_Q14[4] = LTPCoef_Q14[k * SilkConstants.LTP_ORDER + 4];

      /* LTP analysis FIR filter */
      for (i = 0; i < subfr_length + pre_length; i++) {
        int LTP_res_ptri = LTP_res_ptr + i;
        LTP_res[LTP_res_ptri] = x[x_ptr2 + i];

        /* Long-term prediction */
        LTP_est = Inlines.silk_SMULBB(x[x_lag_ptr + SilkConstants.LTP_ORDER / 2], Btmp_Q14[0]);
        LTP_est = Inlines.silk_SMLABB_ovflw(LTP_est, x[x_lag_ptr + 1], Btmp_Q14[1]);
        LTP_est = Inlines.silk_SMLABB_ovflw(LTP_est, x[x_lag_ptr], Btmp_Q14[2]);
        LTP_est = Inlines.silk_SMLABB_ovflw(LTP_est, x[x_lag_ptr - 1], Btmp_Q14[3]);
        LTP_est = Inlines.silk_SMLABB_ovflw(LTP_est, x[x_lag_ptr - 2], Btmp_Q14[4]);

        LTP_est = Inlines.silk_RSHIFT_ROUND(LTP_est, 14);
        /* round and . Q0 */

        /* Subtract long-term prediction */
        LTP_res[LTP_res_ptri] = (short) Inlines.silk_SAT16((int) x[x_ptr2 + i] - LTP_est);

        /* Scale residual */
        LTP_res[LTP_res_ptri] =
            (short) (Inlines.silk_SMULWB(invGains_Q16[k], LTP_res[LTP_res_ptri]));

        x_lag_ptr++;
      }

      /* Update pointers */
      LTP_res_ptr += subfr_length + pre_length;
      x_ptr2 += subfr_length;
    }
  }
}
