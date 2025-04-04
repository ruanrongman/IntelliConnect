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

class Schur {

  /* Faster than schur64(), but much less accurate. */
  /* uses SMLAWB(), requiring armv5E and higher. */
  static int silk_schur( /* O Returns residual energy */
      short[] rc_Q15, /* O reflection coefficients [order] Q15 */
      int[] c, /* I correlations [order+1] */
      int order /* I prediction order */
  ) {
    int k, n, lz;
    int[][] C = Arrays.InitTwoDimensionalArrayInt(SilkConstants.SILK_MAX_ORDER_LPC + 1, 2);
    int Ctmp1, Ctmp2, rc_tmp_Q15;

    Inlines.OpusAssert(
        order == 6 || order == 8 || order == 10 || order == 12 || order == 14 || order == 16);

    /* Get number of leading zeros */
    lz = Inlines.silk_CLZ32(c[0]);

    /* Copy correlations and adjust level to Q30 */
    if (lz < 2) {
      /* lz must be 1, so shift one to the right */
      for (k = 0; k < order + 1; k++) {
        C[k][0] = C[k][1] = Inlines.silk_RSHIFT(c[k], 1);
      }
    } else if (lz > 2) {
      /* Shift to the left */
      lz -= 2;
      for (k = 0; k < order + 1; k++) {
        C[k][0] = C[k][1] = Inlines.silk_LSHIFT(c[k], lz);
      }
    } else {
      /* No need to shift */
      for (k = 0; k < order + 1; k++) {
        C[k][0] = C[k][1] = c[k];
      }
    }

    for (k = 0; k < order; k++) {
      /* Check that we won't be getting an unstable rc, otherwise stop here. */
      if (Inlines.silk_abs_int32(C[k + 1][0]) >= C[0][1]) {
        if (C[k + 1][0] > 0) {
          rc_Q15[k] = (short) (0 - ((int) ((.99f) * ((long) 1 << (15)) + 0.5))/*
                                                                               * Inlines.SILK_CONST(
                                                                               * .99f, 15)
                                                                               */);
        } else {
          rc_Q15[k] = (short) (((int) ((.99f) * ((long) 1 << (15)) + 0.5))/*
                                                                           * Inlines.SILK_CONST(.
                                                                           * 99f, 15)
                                                                           */);
        }
        k++;
        break;
      }

      /* Get reflection coefficient */
      rc_tmp_Q15 = 0 - Inlines.silk_DIV32_16(C[k + 1][0],
          Inlines.silk_max_32(Inlines.silk_RSHIFT(C[0][1], 15), 1));

      /* Clip (shouldn't happen for properly conditioned inputs) */
      rc_tmp_Q15 = Inlines.silk_SAT16(rc_tmp_Q15);

      /* Store */
      rc_Q15[k] = (short) rc_tmp_Q15;

      /* Update correlations */
      for (n = 0; n < order - k; n++) {
        Ctmp1 = C[n + k + 1][0];
        Ctmp2 = C[n][1];
        C[n + k + 1][0] = Inlines.silk_SMLAWB(Ctmp1, Inlines.silk_LSHIFT(Ctmp2, 1), rc_tmp_Q15);
        C[n][1] = Inlines.silk_SMLAWB(Ctmp2, Inlines.silk_LSHIFT(Ctmp1, 1), rc_tmp_Q15);
      }
    }

    for (; k < order; k++) {
      rc_Q15[k] = 0;
    }

    /* return residual energy */
    return Inlines.silk_max_32(1, C[0][1]);
  }

  /* Slower than schur(), but more accurate. */
  /* Uses SMULL(), available on armv4 */
  static int silk_schur64( /* O returns residual energy */
      int[] rc_Q16, /* O Reflection coefficients [order] Q16 */
      int[] c, /* I Correlations [order+1] */
      int order /* I Prediction order */
  ) {
    int k, n;
    int[][] C = Arrays.InitTwoDimensionalArrayInt(SilkConstants.SILK_MAX_ORDER_LPC + 1, 2);
    int Ctmp1_Q30, Ctmp2_Q30, rc_tmp_Q31;

    Inlines.OpusAssert(
        order == 6 || order == 8 || order == 10 || order == 12 || order == 14 || order == 16);

    /* Check for invalid input */
    if (c[0] <= 0) {
      Arrays.MemSet(rc_Q16, 0, order);
      return 0;
    }

    for (k = 0; k < order + 1; k++) {
      C[k][0] = C[k][1] = c[k];
    }

    for (k = 0; k < order; k++) {
      /* Check that we won't be getting an unstable rc, otherwise stop here. */
      if (Inlines.silk_abs_int32(C[k + 1][0]) >= C[0][1]) {
        if (C[k + 1][0] > 0) {
          rc_Q16[k] =
              -((int) ((.99f) * ((long) 1 << (16)) + 0.5))/* Inlines.SILK_CONST(.99f, 16) */;
        } else {
          rc_Q16[k] = ((int) ((.99f) * ((long) 1 << (16)) + 0.5))/* Inlines.SILK_CONST(.99f, 16) */;
        }
        k++;
        break;
      }

      /* Get reflection coefficient: divide two Q30 values and get result in Q31 */
      rc_tmp_Q31 = Inlines.silk_DIV32_varQ(-C[k + 1][0], C[0][1], 31);

      /* Save the output */
      rc_Q16[k] = Inlines.silk_RSHIFT_ROUND(rc_tmp_Q31, 15);

      /* Update correlations */
      for (n = 0; n < order - k; n++) {
        Ctmp1_Q30 = C[n + k + 1][0];
        Ctmp2_Q30 = C[n][1];

        /* Multiply and add the highest int32 */
        C[n + k + 1][0] =
            Ctmp1_Q30 + Inlines.silk_SMMUL(Inlines.silk_LSHIFT(Ctmp2_Q30, 1), rc_tmp_Q31);
        C[n][1] = Ctmp2_Q30 + Inlines.silk_SMMUL(Inlines.silk_LSHIFT(Ctmp1_Q30, 1), rc_tmp_Q31);
      }
    }

    for (; k < order; k++) {
      rc_Q16[k] = 0;
    }

    return Inlines.silk_max_32(1, C[0][1]);
  }
}
