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

class LPCInversePredGain {

  private static final int QA = 24;
  private static final int A_LIMIT = ((int) ((0.99975f) * ((long) 1 << (QA)) + 0.5))/*
                                                                                     * Inlines.
                                                                                     * SILK_CONST(0.
                                                                                     * 99975f, QA)
                                                                                     */;

  /* Compute inverse of LPC prediction gain, and */
  /* test if LPC coefficients are stable (all poles within unit circle) */
  static int LPC_inverse_pred_gain_QA( /* O Returns inverse prediction gain in energy domain, Q30 */
      int[][] A_QA, /* I Prediction coefficients [ 2 ][SILK_MAX_ORDER_LPC] */
      int order /* I Prediction order */
  ) {
    int k, n, mult2Q;
    int invGain_Q30, rc_Q31, rc_mult1_Q30, rc_mult2, tmp_QA;
    int[] Aold_QA;
    int[] Anew_QA;

    Anew_QA = A_QA[order & 1];

    invGain_Q30 = (int) 1 << 30;
    for (k = order - 1; k > 0; k--) {
      /* Check for stability */
      if ((Anew_QA[k] > A_LIMIT) || (Anew_QA[k] < -A_LIMIT)) {
        return 0;
      }

      /* Set RC equal to negated AR coef */
      rc_Q31 = 0 - Inlines.silk_LSHIFT(Anew_QA[k], 31 - QA);

      /* rc_mult1_Q30 range: [ 1 : 2^30 ] */
      rc_mult1_Q30 = ((int) 1 << 30) - Inlines.silk_SMMUL(rc_Q31, rc_Q31);
      Inlines.OpusAssert(rc_mult1_Q30 > (1 << 15));
      /* reduce A_LIMIT if fails */
      Inlines.OpusAssert(rc_mult1_Q30 <= (1 << 30));

      /* rc_mult2 range: [ 2^30 : silk_int32_MAX ] */
      mult2Q = 32 - Inlines.silk_CLZ32(Inlines.silk_abs(rc_mult1_Q30));
      rc_mult2 = Inlines.silk_INVERSE32_varQ(rc_mult1_Q30, mult2Q + 30);

      /* Update inverse gain */
      /* invGain_Q30 range: [ 0 : 2^30 ] */
      invGain_Q30 = Inlines.silk_LSHIFT(Inlines.silk_SMMUL(invGain_Q30, rc_mult1_Q30), 2);
      Inlines.OpusAssert(invGain_Q30 >= 0);
      Inlines.OpusAssert(invGain_Q30 <= (1 << 30));

      /* Swap pointers */
      Aold_QA = Anew_QA;
      Anew_QA = A_QA[k & 1];

      /* Update AR coefficient */
      for (n = 0; n < k; n++) {
        tmp_QA = Aold_QA[n] - Inlines.MUL32_FRAC_Q(Aold_QA[k - n - 1], rc_Q31, 31);
        Anew_QA[n] = Inlines.MUL32_FRAC_Q(tmp_QA, rc_mult2, mult2Q);
      }
    }

    /* Check for stability */
    if ((Anew_QA[0] > A_LIMIT) || (Anew_QA[0] < -A_LIMIT)) {
      return 0;
    }

    /* Set RC equal to negated AR coef */
    rc_Q31 = 0 - Inlines.silk_LSHIFT(Anew_QA[0], 31 - QA);

    /* Range: [ 1 : 2^30 ] */
    rc_mult1_Q30 = ((int) 1 << 30) - Inlines.silk_SMMUL(rc_Q31, rc_Q31);

    /* Update inverse gain */
    /* Range: [ 0 : 2^30 ] */
    invGain_Q30 = Inlines.silk_LSHIFT(Inlines.silk_SMMUL(invGain_Q30, rc_mult1_Q30), 2);
    Inlines.OpusAssert(invGain_Q30 >= 0);
    Inlines.OpusAssert(invGain_Q30 <= 1 << 30);

    return invGain_Q30;
  }

  /* For input in Q12 domain */
  static int silk_LPC_inverse_pred_gain( /*
                                          * O Returns inverse prediction gain in energy domain, Q30
                                          */
      short[] A_Q12, /* I Prediction coefficients, Q12 [order] */
      int order /* I Prediction order */
  ) {
    int k;
    int[][] Atmp_QA = Arrays.InitTwoDimensionalArrayInt(2, SilkConstants.SILK_MAX_ORDER_LPC);
    int[] Anew_QA;
    int DC_resp = 0;

    Anew_QA = Atmp_QA[order & 1];

    /* Increase Q domain of the AR coefficients */
    for (k = 0; k < order; k++) {
      DC_resp += (int) A_Q12[k];
      Anew_QA[k] = Inlines.silk_LSHIFT32((int) A_Q12[k], QA - 12);
    }
    /* If the DC is unstable, we don't even need to do the full calculations */
    if (DC_resp >= 4096) {
      return 0;
    }
    return LPC_inverse_pred_gain_QA(Atmp_QA, order);
  }

  static int silk_LPC_inverse_pred_gain_Q24( /*
                                              * O Returns inverse prediction gain in energy domain,
                                              * Q30
                                              */
      int[] A_Q24, /* I Prediction coefficients [order] */
      int order /* I Prediction order */
  ) {
    int k;
    int[][] Atmp_QA = Arrays.InitTwoDimensionalArrayInt(2, SilkConstants.SILK_MAX_ORDER_LPC);
    int[] Anew_QA;

    Anew_QA = Atmp_QA[order & 1];

    /* Increase Q domain of the AR coefficients */
    for (k = 0; k < order; k++) {
      Anew_QA[k] = Inlines.silk_RSHIFT32(A_Q24[k], 24 - QA);
    }

    return LPC_inverse_pred_gain_QA(Atmp_QA, order);
  }
}
