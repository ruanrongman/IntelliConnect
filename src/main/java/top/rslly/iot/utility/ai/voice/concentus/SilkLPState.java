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

/// <summary>
/// Variable cut-off low-pass filter state
/// </summary>
class SilkLPState {

  /// <summary>
  /// Low pass filter state
  /// </summary>
  final int[] In_LP_State = new int[2];

  /// <summary>
  /// Counter which is mapped to a cut-off frequency
  /// </summary>
  int transition_frame_no = 0;

  /// <summary>
  /// Operating mode, <0: switch down, >0: switch up; 0: do nothing
  /// </summary>
  int mode = 0;

  void Reset() {
    In_LP_State[0] = 0;
    In_LP_State[1] = 0;
    transition_frame_no = 0;
    mode = 0;
  }

  /* Low-pass filter with variable cutoff frequency based on */
  /* piece-wise linear interpolation between elliptic filters */
  /* Start by setting psEncC.mode <> 0; */
  /* Deactivate by setting psEncC.mode = 0; */
  void silk_LP_variable_cutoff(
      short[] frame, /* I/O Low-pass filtered output signal */
      int frame_ptr,
      int frame_length /* I Frame length */
  ) {
    int[] B_Q28 = new int[SilkConstants.TRANSITION_NB];
    int[] A_Q28 = new int[SilkConstants.TRANSITION_NA];
    int fac_Q16 = 0;
    int ind = 0;

    Inlines.OpusAssert(this.transition_frame_no >= 0
        && this.transition_frame_no <= SilkConstants.TRANSITION_FRAMES);

    /* Run filter if needed */
    if (this.mode != 0) {
      /* Calculate index and interpolation factor for interpolation */
      fac_Q16 =
          Inlines.silk_LSHIFT(SilkConstants.TRANSITION_FRAMES - this.transition_frame_no, 16 - 6);

      ind = Inlines.silk_RSHIFT(fac_Q16, 16);
      fac_Q16 -= Inlines.silk_LSHIFT(ind, 16);

      Inlines.OpusAssert(ind >= 0);
      Inlines.OpusAssert(ind < SilkConstants.TRANSITION_INT_NUM);

      /* Interpolate filter coefficients */
      Filters.silk_LP_interpolate_filter_taps(B_Q28, A_Q28, ind, fac_Q16);

      /* Update transition frame number for next frame */
      this.transition_frame_no = Inlines.silk_LIMIT(this.transition_frame_no + this.mode, 0,
          SilkConstants.TRANSITION_FRAMES);

      /* ARMA low-pass filtering */
      Inlines.OpusAssert(SilkConstants.TRANSITION_NB == 3 && SilkConstants.TRANSITION_NA == 2);
      Filters.silk_biquad_alt(frame, frame_ptr, B_Q28, A_Q28, this.In_LP_State, frame, frame_ptr,
          frame_length, 1);
    }
  }
}
