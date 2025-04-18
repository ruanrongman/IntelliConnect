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

class SumSqrShift {

  /// <summary>
  /// Compute number of bits to right shift the sum of squares of a vector
  /// of int16s to make it fit in an int32
  /// </summary>
  /// <param name="energy">O Energy of x, after shifting to the right</param>
  /// <param name="shift">O Number of bits right shift applied to energy</param>
  /// <param name="x">I Input vector</param>
  /// <param name="len">I Length of input vector</param>
  static void silk_sum_sqr_shift(
      BoxedValueInt energy,
      BoxedValueInt shift,
      short[] x,
      int x_ptr,
      int len) {
    int i, shft;
    int nrg_tmp, nrg;

    nrg = 0;
    shft = 0;
    len--;

    for (i = 0; i < len; i += 2) {
      nrg = Inlines.silk_SMLABB_ovflw(nrg, x[x_ptr + i], x[x_ptr + i]);
      nrg = Inlines.silk_SMLABB_ovflw(nrg, x[x_ptr + i + 1], x[x_ptr + i + 1]);
      if (nrg < 0) {
        /* Scale down */
        nrg = ((int) Inlines.silk_RSHIFT_uint(nrg, 2));
        shft = 2;
        i += 2;
        break;
      }
    }

    for (; i < len; i += 2) {
      nrg_tmp = Inlines.silk_SMULBB(x[x_ptr + i], x[x_ptr + i]);
      nrg_tmp = Inlines.silk_SMLABB_ovflw(nrg_tmp, x[x_ptr + i + 1], x[x_ptr + i + 1]);
      nrg = ((int) Inlines.silk_ADD_RSHIFT_uint(nrg, nrg_tmp, shft));
      if (nrg < 0) {
        /* Scale down */
        nrg = ((int) Inlines.silk_RSHIFT_uint(nrg, 2));
        shft += 2;
      }
    }

    if (i == len) {
      /* One sample left to process */
      nrg_tmp = Inlines.silk_SMULBB(x[x_ptr + i], x[x_ptr + i]);
      nrg = ((int) Inlines.silk_ADD_RSHIFT_uint(nrg, nrg_tmp, shft));
    }

    /* Make sure to have at least one extra leading zero (two leading zeros in total) */
    if ((nrg & 0xC0000000) != 0) {
      nrg = ((int) Inlines.silk_RSHIFT_uint(nrg, 2));
      shft += 2;
    }

    /* Output arguments */
    shift.Val = shft;
    energy.Val = nrg;
  }

  /// <summary>
  /// Zero-index variant
  /// Compute number of bits to right shift the sum of squares of a vector
  /// of int16s to make it fit in an int32
  /// </summary>
  /// <param name="energy">O Energy of x, after shifting to the right</param>
  /// <param name="shift">O Number of bits right shift applied to energy</param>
  /// <param name="x">I Input vector</param>
  /// <param name="len">I Length of input vector</param>
  static void silk_sum_sqr_shift(
      BoxedValueInt energy,
      BoxedValueInt shift,
      short[] x,
      int len) {
    int i, shft;
    int nrg_tmp, nrg;

    nrg = 0;
    shft = 0;
    len--;

    for (i = 0; i < len; i += 2) {
      nrg = Inlines.silk_SMLABB_ovflw(nrg, x[i], x[i]);
      nrg = Inlines.silk_SMLABB_ovflw(nrg, x[i + 1], x[i + 1]);
      if (nrg < 0) {
        /* Scale down */
        nrg = ((int) Inlines.silk_RSHIFT_uint(nrg, 2));
        shft = 2;
        i += 2;
        break;
      }
    }

    for (; i < len; i += 2) {
      nrg_tmp = Inlines.silk_SMULBB(x[i], x[i]);
      nrg_tmp = Inlines.silk_SMLABB_ovflw(nrg_tmp, x[i + 1], x[i + 1]);
      nrg = ((int) Inlines.silk_ADD_RSHIFT_uint(nrg, nrg_tmp, shft));
      if (nrg < 0) {
        /* Scale down */
        nrg = ((int) Inlines.silk_RSHIFT_uint(nrg, 2));
        shft += 2;
      }
    }

    if (i == len) {
      /* One sample left to process */
      nrg_tmp = Inlines.silk_SMULBB(x[i], x[i]);
      nrg = ((int) Inlines.silk_ADD_RSHIFT_uint(nrg, nrg_tmp, shft));
    }

    /* Make sure to have at least one extra leading zero (two leading zeros in total) */
    if ((nrg & 0xC0000000) != 0) {
      nrg = ((int) Inlines.silk_RSHIFT_uint(nrg, 2));
      shft += 2;
    }

    /* Output arguments */
    shift.Val = shft;
    energy.Val = nrg;
  }
}
