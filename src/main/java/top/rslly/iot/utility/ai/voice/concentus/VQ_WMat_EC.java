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

class VQ_WMat_EC {

  /*
   * Entropy constrained matrix-weighted VQ, hard-coded to 5-element vectors, for a single input
   * data vector
   */
  static void silk_VQ_WMat_EC(
      BoxedValueByte ind, /* O index of best codebook vector */
      BoxedValueInt rate_dist_Q14, /* O best weighted quant error + mu * rate */
      BoxedValueInt gain_Q7, /* O sum of absolute LTP coefficients */
      short[] in_Q14, /* I input vector to be quantized */
      int in_Q14_ptr,
      int[] W_Q18, /* I weighting matrix */
      int W_Q18_ptr,
      byte[][] cb_Q7, /* I codebook */
      short[] cb_gain_Q7, /* I codebook effective gain */
      short[] cl_Q5, /* I code length for each codebook vector */
      int mu_Q9, /* I tradeoff betw. weighted error and rate */
      int max_gain_Q7, /* I maximum sum of absolute LTP coefficients */
      int L /* I number of vectors in codebook */
  ) {
    int k, gain_tmp_Q7;
    byte[] cb_row_Q7;
    int cb_row_Q7_ptr = 0;
    short[] diff_Q14 = new short[5];
    int sum1_Q14, sum2_Q16;

    /* Loop over codebook */
    rate_dist_Q14.Val = Integer.MAX_VALUE;
    for (k = 0; k < L; k++) {
      /* Go to next cbk vector */
      cb_row_Q7 = cb_Q7[cb_row_Q7_ptr++];
      gain_tmp_Q7 = cb_gain_Q7[k];

      diff_Q14[0] = (short) (in_Q14[in_Q14_ptr] - Inlines.silk_LSHIFT(cb_row_Q7[0], 7));
      diff_Q14[1] = (short) (in_Q14[in_Q14_ptr + 1] - Inlines.silk_LSHIFT(cb_row_Q7[1], 7));
      diff_Q14[2] = (short) (in_Q14[in_Q14_ptr + 2] - Inlines.silk_LSHIFT(cb_row_Q7[2], 7));
      diff_Q14[3] = (short) (in_Q14[in_Q14_ptr + 3] - Inlines.silk_LSHIFT(cb_row_Q7[3], 7));
      diff_Q14[4] = (short) (in_Q14[in_Q14_ptr + 4] - Inlines.silk_LSHIFT(cb_row_Q7[4], 7));

      /* Weighted rate */
      sum1_Q14 = Inlines.silk_SMULBB(mu_Q9, cl_Q5[k]);

      /* Penalty for too large gain */
      sum1_Q14 = Inlines.silk_ADD_LSHIFT32(sum1_Q14,
          Inlines.silk_max(Inlines.silk_SUB32(gain_tmp_Q7, max_gain_Q7), 0), 10);

      Inlines.OpusAssert(sum1_Q14 >= 0);

      /* first row of W_Q18 */
      sum2_Q16 = Inlines.silk_SMULWB(W_Q18[W_Q18_ptr + 1], diff_Q14[1]);
      sum2_Q16 = Inlines.silk_SMLAWB(sum2_Q16, W_Q18[W_Q18_ptr + 2], diff_Q14[2]);
      sum2_Q16 = Inlines.silk_SMLAWB(sum2_Q16, W_Q18[W_Q18_ptr + 3], diff_Q14[3]);
      sum2_Q16 = Inlines.silk_SMLAWB(sum2_Q16, W_Q18[W_Q18_ptr + 4], diff_Q14[4]);
      sum2_Q16 = Inlines.silk_LSHIFT(sum2_Q16, 1);
      sum2_Q16 = Inlines.silk_SMLAWB(sum2_Q16, W_Q18[W_Q18_ptr], diff_Q14[0]);
      sum1_Q14 = Inlines.silk_SMLAWB(sum1_Q14, sum2_Q16, diff_Q14[0]);

      /* second row of W_Q18 */
      sum2_Q16 = Inlines.silk_SMULWB(W_Q18[W_Q18_ptr + 7], diff_Q14[2]);
      sum2_Q16 = Inlines.silk_SMLAWB(sum2_Q16, W_Q18[W_Q18_ptr + 8], diff_Q14[3]);
      sum2_Q16 = Inlines.silk_SMLAWB(sum2_Q16, W_Q18[W_Q18_ptr + 9], diff_Q14[4]);
      sum2_Q16 = Inlines.silk_LSHIFT(sum2_Q16, 1);
      sum2_Q16 = Inlines.silk_SMLAWB(sum2_Q16, W_Q18[W_Q18_ptr + 6], diff_Q14[1]);
      sum1_Q14 = Inlines.silk_SMLAWB(sum1_Q14, sum2_Q16, diff_Q14[1]);

      /* third row of W_Q18 */
      sum2_Q16 = Inlines.silk_SMULWB(W_Q18[W_Q18_ptr + 13], diff_Q14[3]);
      sum2_Q16 = Inlines.silk_SMLAWB(sum2_Q16, W_Q18[W_Q18_ptr + 14], diff_Q14[4]);
      sum2_Q16 = Inlines.silk_LSHIFT(sum2_Q16, 1);
      sum2_Q16 = Inlines.silk_SMLAWB(sum2_Q16, W_Q18[W_Q18_ptr + 12], diff_Q14[2]);
      sum1_Q14 = Inlines.silk_SMLAWB(sum1_Q14, sum2_Q16, diff_Q14[2]);

      /* fourth row of W_Q18 */
      sum2_Q16 = Inlines.silk_SMULWB(W_Q18[W_Q18_ptr + 19], diff_Q14[4]);
      sum2_Q16 = Inlines.silk_LSHIFT(sum2_Q16, 1);
      sum2_Q16 = Inlines.silk_SMLAWB(sum2_Q16, W_Q18[W_Q18_ptr + 18], diff_Q14[3]);
      sum1_Q14 = Inlines.silk_SMLAWB(sum1_Q14, sum2_Q16, diff_Q14[3]);

      /* last row of W_Q18 */
      sum2_Q16 = Inlines.silk_SMULWB(W_Q18[W_Q18_ptr + 24], diff_Q14[4]);
      sum1_Q14 = Inlines.silk_SMLAWB(sum1_Q14, sum2_Q16, diff_Q14[4]);

      Inlines.OpusAssert(sum1_Q14 >= 0);

      /* find best */
      if (sum1_Q14 < rate_dist_Q14.Val) {
        rate_dist_Q14.Val = sum1_Q14;
        ind.Val = (byte) k;
        gain_Q7.Val = gain_tmp_Q7;
      }
    }
  }
}
