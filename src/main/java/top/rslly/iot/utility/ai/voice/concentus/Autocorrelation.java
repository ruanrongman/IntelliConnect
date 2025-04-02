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

class Autocorrelation {

  /* Compute autocorrelation */
  static void silk_autocorr(
      int[] results, /* O Result (length correlationCount) */
      BoxedValueInt scale, /* O Scaling of the correlation vector */
      short[] inputData, /* I Input data to correlate */
      int inputDataSize, /* I Length of input */
      int correlationCount /* I Number of correlation taps to compute */
  ) {
    int corrCount = Inlines.silk_min_int(inputDataSize, correlationCount);
    scale.Val = Autocorrelation._celt_autocorr(inputData, results, corrCount - 1, inputDataSize);
  }

  static int _celt_autocorr(
      short[] x, /* in: [0...n-1] samples x */
      int[] ac, /* out: [0...lag-1] ac values */
      int lag,
      int n) {
    int d;
    int i, k;
    int fastN = n - lag;
    int shift;
    short[] xptr;
    short[] xx = new short[n];
    Inlines.OpusAssert(n > 0);
    xptr = x;

    shift = 0;
    {
      int ac0;
      ac0 = 1 + (n << 7);
      if ((n & 1) != 0) {
        ac0 += Inlines.SHR32(Inlines.MULT16_16(xptr[0], xptr[0]), 9);
      }
      for (i = (n & 1); i < n; i += 2) {
        ac0 += Inlines.SHR32(Inlines.MULT16_16(xptr[i], xptr[i]), 9);
        ac0 += Inlines.SHR32(Inlines.MULT16_16(xptr[i + 1], xptr[i + 1]), 9);
      }
      shift = Inlines.celt_ilog2(ac0) - 30 + 10;
      shift = (shift) / 2;
      if (shift > 0) {
        for (i = 0; i < n; i++) {
          xx[i] = (short) (Inlines.PSHR32(xptr[i], shift));
        }
        xptr = xx;
      } else {
        shift = 0;
      }
    }
    CeltPitchXCorr.pitch_xcorr(xptr, xptr, ac, fastN, lag + 1);
    for (k = 0; k <= lag; k++) {
      for (i = k + fastN, d = 0; i < n; i++) {
        d = Inlines.MAC16_16(d, xptr[i], xptr[i - k]);
      }
      ac[k] += d;
    }
    shift = 2 * shift;
    if (shift <= 0) {
      ac[0] += Inlines.SHL32((int) 1, -shift);
    }
    if (ac[0] < 268435456) {
      int shift2 = 29 - Inlines.EC_ILOG(ac[0]);
      for (i = 0; i <= lag; i++) {
        ac[i] = Inlines.SHL32(ac[i], shift2);
      }
      shift -= shift2;
    } else if (ac[0] >= 536870912) {
      int shift2 = 1;
      if (ac[0] >= 1073741824) {
        shift2++;
      }
      for (i = 0; i <= lag; i++) {
        ac[i] = Inlines.SHR32(ac[i], shift2);
      }
      shift += shift2;
    }

    return shift;
  }

  static int _celt_autocorr(
      int[] x, /* in: [0...n-1] samples x */
      int[] ac, /* out: [0...lag-1] ac values */
      int[] window,
      int overlap,
      int lag,
      int n) {
    int d;
    int i, k;
    int fastN = n - lag;
    int shift;
    int[] xptr;
    int[] xx = new int[n];

    Inlines.OpusAssert(n > 0);
    Inlines.OpusAssert(overlap >= 0);

    if (overlap == 0) {
      xptr = x;
    } else {
      for (i = 0; i < n; i++) {
        xx[i] = x[i];
      }
      for (i = 0; i < overlap; i++) {
        xx[i] = Inlines.MULT16_16_Q15(x[i], window[i]);
        xx[n - i - 1] = Inlines.MULT16_16_Q15(x[n - i - 1], window[i]);
      }
      xptr = xx;
    }

    shift = 0;

    int ac0;
    ac0 = 1 + (n << 7);
    if ((n & 1) != 0) {
      ac0 += Inlines.SHR32(Inlines.MULT16_16(xptr[0], xptr[0]), 9);
    }

    for (i = (n & 1); i < n; i += 2) {
      ac0 += Inlines.SHR32(Inlines.MULT16_16(xptr[i], xptr[i]), 9);
      ac0 += Inlines.SHR32(Inlines.MULT16_16(xptr[i + 1], xptr[i + 1]), 9);
    }

    shift = Inlines.celt_ilog2(ac0) - 30 + 10;
    shift = (shift) / 2;
    if (shift > 0) {
      for (i = 0; i < n; i++) {
        xx[i] = (Inlines.PSHR32(xptr[i], shift));
      }
      xptr = xx;
    } else {
      shift = 0;
    }

    CeltPitchXCorr.pitch_xcorr(xptr, xptr, ac, fastN, lag + 1);
    for (k = 0; k <= lag; k++) {
      for (i = k + fastN, d = 0; i < n; i++) {
        d = Inlines.MAC16_16(d, xptr[i], xptr[i - k]);
      }
      ac[k] += d;
    }

    shift = 2 * shift;
    if (shift <= 0) {
      ac[0] += Inlines.SHL32((int) 1, -shift);
    }
    if (ac[0] < 268435456) {
      int shift2 = 29 - Inlines.EC_ILOG(ac[0]);
      for (i = 0; i <= lag; i++) {
        ac[i] = Inlines.SHL32(ac[i], shift2);
      }
      shift -= shift2;
    } else if (ac[0] >= 536870912) {
      int shift2 = 1;
      if (ac[0] >= 1073741824) {
        shift2++;
      }
      for (i = 0; i <= lag; i++) {
        ac[i] = Inlines.SHR32(ac[i], shift2);
      }
      shift += shift2;
    }

    return shift;
  }

  private static final int QC = 10;
  private static final int QS = 14;

  /* Autocorrelations for a warped frequency axis */
  static void silk_warped_autocorrelation(
      int[] corr, /* O Result [order + 1] */
      BoxedValueInt scale, /* O Scaling of the correlation vector */
      short[] input, /* I Input data to correlate */
      int warping_Q16, /* I Warping coefficient */
      int length, /* I Length of input */
      int order /* I Correlation order (even) */
  ) {
    int n, i, lsh;
    int tmp1_QS, tmp2_QS;
    int[] state_QS = new int[SilkConstants.MAX_SHAPE_LPC_ORDER + 1];// = { 0 };
    long[] corr_QC = new long[SilkConstants.MAX_SHAPE_LPC_ORDER + 1];// = { 0 };

    /* Order must be even */
    Inlines.OpusAssert((order & 1) == 0);
    Inlines.OpusAssert(2 * QS - QC >= 0);

    /* Loop over samples */
    for (n = 0; n < length; n++) {
      tmp1_QS = Inlines.silk_LSHIFT32((int) input[n], QS);
      /* Loop over allpass sections */
      for (i = 0; i < order; i += 2) {
        /* Output of allpass section */
        tmp2_QS = Inlines.silk_SMLAWB(state_QS[i], state_QS[i + 1] - tmp1_QS, warping_Q16);
        state_QS[i] = tmp1_QS;
        corr_QC[i] += Inlines.silk_RSHIFT64(Inlines.silk_SMULL(tmp1_QS, state_QS[0]), 2 * QS - QC);
        /* Output of allpass section */
        tmp1_QS = Inlines.silk_SMLAWB(state_QS[i + 1], state_QS[i + 2] - tmp2_QS, warping_Q16);
        state_QS[i + 1] = tmp2_QS;
        corr_QC[i + 1] +=
            Inlines.silk_RSHIFT64(Inlines.silk_SMULL(tmp2_QS, state_QS[0]), 2 * QS - QC);
      }
      state_QS[order] = tmp1_QS;
      corr_QC[order] +=
          Inlines.silk_RSHIFT64(Inlines.silk_SMULL(tmp1_QS, state_QS[0]), 2 * QS - QC);
    }

    lsh = Inlines.silk_CLZ64(corr_QC[0]) - 35;
    lsh = Inlines.silk_LIMIT(lsh, -12 - QC, 30 - QC);
    scale.Val = -(QC + lsh);
    Inlines.OpusAssert(scale.Val >= -30 && scale.Val <= 12);
    if (lsh >= 0) {
      for (i = 0; i < order + 1; i++) {
        corr[i] = (int) (Inlines.silk_LSHIFT64(corr_QC[i], lsh));
      }
    } else {
      for (i = 0; i < order + 1; i++) {
        corr[i] = (int) (Inlines.silk_RSHIFT64(corr_QC[i], -lsh));
      }
    }
    Inlines.OpusAssert(corr_QC[0] >= 0);
    /* If breaking, decrease QC */
  }
}
