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

class BWExpander {

  /// <summary>
  /// Chirp (bw expand) LP AR filter (Fixed point implementation)
  /// </summary>
  /// <param name="ar">I/O AR filter to be expanded (without leading 1)</param>
  /// <param name="d">I length of ar</param>
  /// <param name="chirp">I chirp factor (typically in range (0..1) )</param>
  static void silk_bwexpander_32(
      int[] ar, /* I/O AR filter to be expanded (without leading 1) */
      int d, /* I Length of ar */
      int chirp_Q16 /* I Chirp factor in Q16 */
  ) {
    int i;
    int chirp_minus_one_Q16 = chirp_Q16 - 65536;

    for (i = 0; i < d - 1; i++) {
      ar[i] = Inlines.silk_SMULWW(chirp_Q16, ar[i]);
      chirp_Q16 += Inlines.silk_RSHIFT_ROUND(Inlines.silk_MUL(chirp_Q16, chirp_minus_one_Q16), 16);
    }
    ar[d - 1] = Inlines.silk_SMULWW(chirp_Q16, ar[d - 1]);
  }

  /// <summary>
  /// Chirp (bw expand) LP AR filter (Fixed point implementation)
  /// </summary>
  /// <param name="ar">I/O AR filter to be expanded (without leading 1)</param>
  /// <param name="d">I length of ar</param>
  /// <param name="chirp">I chirp factor (typically in range (0..1) )</param>
  static void silk_bwexpander(
      short[] ar,
      int d,
      int chirp_Q16) {
    int i;
    int chirp_minus_one_Q16 = chirp_Q16 - 65536;

    /* NB: Dont use silk_SMULWB, instead of silk_RSHIFT_ROUND( silk_MUL(), 16 ), below. */
    /* Bias in silk_SMULWB can lead to unstable filters */
    for (i = 0; i < d - 1; i++) {
      ar[i] = (short) Inlines.silk_RSHIFT_ROUND(Inlines.silk_MUL(chirp_Q16, ar[i]), 16);
      chirp_Q16 += Inlines.silk_RSHIFT_ROUND(Inlines.silk_MUL(chirp_Q16, chirp_minus_one_Q16), 16);
    }
    ar[d - 1] = (short) Inlines.silk_RSHIFT_ROUND(Inlines.silk_MUL(chirp_Q16, ar[d - 1]), 16);
  }
}
