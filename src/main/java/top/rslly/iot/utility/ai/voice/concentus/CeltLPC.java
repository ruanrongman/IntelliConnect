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

class CeltLPC {

  static void celt_lpc(
      int[] _lpc, /* out: [0...p-1] LPC coefficients */
      int[] ac, /* in: [0...p] autocorrelation values */
      int p) {
    int i, j;
    int r;
    int error = ac[0];
    int[] lpc = new int[p];

    // Arrays.MemSet(lpc, 0, p); strictly, this is not necessary since the runtime zeroes memory for
    // us
    if (ac[0] != 0) {
      for (i = 0; i < p; i++) {
        /* Sum up this iteration's reflection coefficient */
        int rr = 0;
        for (j = 0; j < i; j++) {
          rr += Inlines.MULT32_32_Q31(lpc[j], ac[i - j]);
        }
        rr += Inlines.SHR32(ac[i + 1], 3);
        r = 0 - Inlines.frac_div32(Inlines.SHL32(rr, 3), error);
        /* Update LPC coefficients and total error */
        lpc[i] = Inlines.SHR32(r, 3);

        for (j = 0; j < (i + 1) >> 1; j++) {
          int tmp1, tmp2;
          tmp1 = lpc[j];
          tmp2 = lpc[i - 1 - j];
          lpc[j] = tmp1 + Inlines.MULT32_32_Q31(r, tmp2);
          lpc[i - 1 - j] = tmp2 + Inlines.MULT32_32_Q31(r, tmp1);
        }

        error = error - Inlines.MULT32_32_Q31(Inlines.MULT32_32_Q31(r, r), error);

        /* Bail out once we get 30 dB gain */
        if (error < Inlines.SHR32(ac[0], 10)) {
          break;
        }
      }
    }

    for (i = 0; i < p; i++) {
      _lpc[i] = Inlines.ROUND16((lpc[i]), 16);
    }
  }

  static void celt_iir(
      int[] _x,
      int _x_ptr,
      int[] den,
      int[] _y,
      int _y_ptr,
      int N,
      int ord,
      int[] mem) {
    int i, j;
    int[] rden = new int[ord];
    int[] y = new int[N + ord];
    Inlines.OpusAssert((ord & 3) == 0);

    BoxedValueInt _sum0 = new BoxedValueInt(0);
    BoxedValueInt _sum1 = new BoxedValueInt(0);
    BoxedValueInt _sum2 = new BoxedValueInt(0);
    BoxedValueInt _sum3 = new BoxedValueInt(0);
    int sum0, sum1, sum2, sum3;

    for (i = 0; i < ord; i++) {
      rden[i] = den[ord - i - 1];
    }
    for (i = 0; i < ord; i++) {
      y[i] = (0 - mem[ord - i - 1]);
    }
    for (; i < N + ord; i++) {
      y[i] = 0;
    }
    for (i = 0; i < N - 3; i += 4) {
      /* Unroll by 4 as if it were an FIR filter */
      _sum0.Val = _x[_x_ptr + i];
      _sum1.Val = _x[_x_ptr + i + 1];
      _sum2.Val = _x[_x_ptr + i + 2];
      _sum3.Val = _x[_x_ptr + i + 3];
      Kernels.xcorr_kernel(rden, y, i, _sum0, _sum1, _sum2, _sum3, ord);
      sum0 = _sum0.Val;
      sum1 = _sum1.Val;
      sum2 = _sum2.Val;
      sum3 = _sum3.Val;

      /* Patch up the result to compensate for the fact that this is an IIR */
      y[i + ord] = (0 - Inlines.ROUND16((sum0), CeltConstants.SIG_SHIFT));
      _y[_y_ptr + i] = sum0;
      sum1 = Inlines.MAC16_16(sum1, y[i + ord], den[0]);
      y[i + ord + 1] = (0 - Inlines.ROUND16((sum1), CeltConstants.SIG_SHIFT));
      _y[_y_ptr + i + 1] = sum1;
      sum2 = Inlines.MAC16_16(sum2, y[i + ord + 1], den[0]);
      sum2 = Inlines.MAC16_16(sum2, y[i + ord], den[1]);
      y[i + ord + 2] = (0 - Inlines.ROUND16((sum2), CeltConstants.SIG_SHIFT));
      _y[_y_ptr + i + 2] = sum2;

      sum3 = Inlines.MAC16_16(sum3, y[i + ord + 2], den[0]);
      sum3 = Inlines.MAC16_16(sum3, y[i + ord + 1], den[1]);
      sum3 = Inlines.MAC16_16(sum3, y[i + ord], den[2]);
      y[i + ord + 3] = (0 - Inlines.ROUND16((sum3), CeltConstants.SIG_SHIFT));
      _y[_y_ptr + i + 3] = sum3;
    }
    for (; i < N; i++) {
      int sum = _x[_x_ptr + i];
      for (j = 0; j < ord; j++) {
        sum -= Inlines.MULT16_16(rden[j], y[i + j]);
      }
      y[i + ord] = Inlines.ROUND16((sum), CeltConstants.SIG_SHIFT);
      _y[_y_ptr + i] = sum;
    }
    for (i = 0; i < ord; i++) {
      mem[i] = (_y[_y_ptr + N - i - 1]);
    }
  }
}
