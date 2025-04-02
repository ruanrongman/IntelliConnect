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

class EncodePulses {

  /// <summary>
  ///
  /// </summary>
  /// <param name="pulses_comb">(O)</param>
  /// <param name="pulses_in">(I)</param>
  /// <param name="max_pulses"> I max value for sum of pulses</param>
  /// <param name="len">I number of output values</param>
  /// <returns>return ok</returns>
  static int combine_and_check(
      int[] pulses_comb,
      int pulses_comb_ptr,
      int[] pulses_in,
      int pulses_in_ptr,
      int max_pulses,
      int len) {
    for (int k = 0; k < len; k++) {
      int k2p = 2 * k + pulses_in_ptr;
      int sum = pulses_in[k2p] + pulses_in[k2p + 1];
      if (sum > max_pulses) {
        return 1;
      }
      pulses_comb[pulses_comb_ptr + k] = sum;
    }
    return 0;
  }

  /// <summary>
  ///
  /// </summary>
  /// <param name="pulses_comb">(O)</param>
  /// <param name="pulses_in">(I)</param>
  /// <param name="max_pulses"> I max value for sum of pulses</param>
  /// <param name="len">I number of output values</param>
  /// <returns>return ok</returns>
  static int combine_and_check(
      int[] pulses_comb,
      int[] pulses_in,
      int max_pulses,
      int len) {
    for (int k = 0; k < len; k++) {
      int sum = pulses_in[2 * k] + pulses_in[2 * k + 1];
      if (sum > max_pulses) {
        return 1;
      }
      pulses_comb[k] = sum;
    }
    return 0;
  }

  /// <summary>
  /// Encode quantization indices of excitation
  /// </summary>
  /// <param name="psRangeEnc">I/O compressor data structure</param>
  /// <param name="signalType">I Signal type</param>
  /// <param name="quantOffsetType">I quantOffsetType</param>
  /// <param name="pulses">I quantization indices</param>
  /// <param name="frame_length">I Frame length</param>
  static void silk_encode_pulses(
      EntropyCoder psRangeEnc,
      int signalType,
      int quantOffsetType,
      byte[] pulses,
      int frame_length) {
    int i, k, j, iter, bit, nLS, scale_down, RateLevelIndex = 0;
    int abs_q, minSumBits_Q5, sumBits_Q5;
    int[] abs_pulses;
    int[] sum_pulses;
    int[] nRshifts;
    int[] pulses_comb = new int[8];
    int abs_pulses_ptr;
    int pulses_ptr;
    short[] nBits_ptr;

    Arrays.MemSet(pulses_comb, 0, 8);

    /**
     * *************************
     */
    /* Prepare for shell coding */
    /**
     * *************************
     */
    /* Calculate number of shell blocks */
    Inlines.OpusAssert(
        1 << SilkConstants.LOG2_SHELL_CODEC_FRAME_LENGTH == SilkConstants.SHELL_CODEC_FRAME_LENGTH);
    iter = Inlines.silk_RSHIFT(frame_length, SilkConstants.LOG2_SHELL_CODEC_FRAME_LENGTH);
    if (iter * SilkConstants.SHELL_CODEC_FRAME_LENGTH < frame_length) {
      Inlines.OpusAssert(frame_length == 12 * 10);
      /* Make sure only happens for 10 ms @ 12 kHz */
      iter++;
      Arrays.MemSetWithOffset(pulses, (byte) 0, frame_length,
          SilkConstants.SHELL_CODEC_FRAME_LENGTH);
    }

    /* Take the absolute value of the pulses */
    abs_pulses = new int[iter * SilkConstants.SHELL_CODEC_FRAME_LENGTH];
    Inlines.OpusAssert((SilkConstants.SHELL_CODEC_FRAME_LENGTH & 3) == 0);

    // unrolled loop
    for (i = 0; i < iter * SilkConstants.SHELL_CODEC_FRAME_LENGTH; i += 4) {
      abs_pulses[i + 0] = (int) Inlines.silk_abs(pulses[i + 0]);
      abs_pulses[i + 1] = (int) Inlines.silk_abs(pulses[i + 1]);
      abs_pulses[i + 2] = (int) Inlines.silk_abs(pulses[i + 2]);
      abs_pulses[i + 3] = (int) Inlines.silk_abs(pulses[i + 3]);
    }

    /* Calc sum pulses per shell code frame */
    sum_pulses = new int[iter];
    nRshifts = new int[iter];
    abs_pulses_ptr = 0;
    for (i = 0; i < iter; i++) {
      nRshifts[i] = 0;

      while (true) {
        /* 1+1 . 2 */
        scale_down = combine_and_check(pulses_comb, 0, abs_pulses, abs_pulses_ptr,
            SilkTables.silk_max_pulses_table[0], 8);
        /* 2+2 . 4 */
        scale_down +=
            combine_and_check(pulses_comb, pulses_comb, SilkTables.silk_max_pulses_table[1], 4);
        /* 4+4 . 8 */
        scale_down +=
            combine_and_check(pulses_comb, pulses_comb, SilkTables.silk_max_pulses_table[2], 2);
        /* 8+8 . 16 */
        scale_down += combine_and_check(sum_pulses, i, pulses_comb, 0,
            SilkTables.silk_max_pulses_table[3], 1);

        if (scale_down != 0) {
          /* We need to downscale the quantization signal */
          nRshifts[i]++;
          for (k =
              abs_pulses_ptr; k < abs_pulses_ptr + SilkConstants.SHELL_CODEC_FRAME_LENGTH; k++) {
            abs_pulses[k] = Inlines.silk_RSHIFT(abs_pulses[k], 1);
          }
        } else {
          /* Jump out of while(1) loop and go to next shell coding frame */
          break;
        }
      }

      abs_pulses_ptr += SilkConstants.SHELL_CODEC_FRAME_LENGTH;
    }

    /**
     * ***********
     */
    /* Rate level */
    /**
     * ***********
     */
    /* find rate level that leads to fewest bits for coding of pulses per block info */
    minSumBits_Q5 = Integer.MAX_VALUE;
    for (k = 0; k < SilkConstants.N_RATE_LEVELS - 1; k++) {
      nBits_ptr = SilkTables.silk_pulses_per_block_BITS_Q5[k];
      sumBits_Q5 = SilkTables.silk_rate_levels_BITS_Q5[signalType >> 1][k];
      for (i = 0; i < iter; i++) {
        if (nRshifts[i] > 0) {
          sumBits_Q5 += nBits_ptr[SilkConstants.SILK_MAX_PULSES + 1];
        } else {
          sumBits_Q5 += nBits_ptr[sum_pulses[i]];
        }
      }
      if (sumBits_Q5 < minSumBits_Q5) {
        minSumBits_Q5 = sumBits_Q5;
        RateLevelIndex = k;
      }
    }

    psRangeEnc.enc_icdf(RateLevelIndex, SilkTables.silk_rate_levels_iCDF[signalType >> 1], 8);

    /**
     * ************************************************
     */
    /* Sum-Weighted-Pulses Encoding */
    /**
     * ************************************************
     */
    for (i = 0; i < iter; i++) {
      if (nRshifts[i] == 0) {
        psRangeEnc.enc_icdf(sum_pulses[i], SilkTables.silk_pulses_per_block_iCDF[RateLevelIndex],
            8);
      } else {
        psRangeEnc.enc_icdf(SilkConstants.SILK_MAX_PULSES + 1,
            SilkTables.silk_pulses_per_block_iCDF[RateLevelIndex], 8);
        for (k = 0; k < nRshifts[i] - 1; k++) {
          psRangeEnc.enc_icdf(SilkConstants.SILK_MAX_PULSES + 1,
              SilkTables.silk_pulses_per_block_iCDF[SilkConstants.N_RATE_LEVELS - 1], 8);
        }

        psRangeEnc.enc_icdf(sum_pulses[i],
            SilkTables.silk_pulses_per_block_iCDF[SilkConstants.N_RATE_LEVELS - 1], 8);
      }
    }

    /**
     * ***************
     */
    /* Shell Encoding */
    /**
     * ***************
     */
    for (i = 0; i < iter; i++) {
      if (sum_pulses[i] > 0) {
        ShellCoder.silk_shell_encoder(psRangeEnc, abs_pulses,
            i * SilkConstants.SHELL_CODEC_FRAME_LENGTH);
      }
    }

    /**
     * *************
     */
    /* LSB Encoding */
    /**
     * *************
     */
    for (i = 0; i < iter; i++) {
      if (nRshifts[i] > 0) {
        pulses_ptr = i * SilkConstants.SHELL_CODEC_FRAME_LENGTH;
        nLS = nRshifts[i] - 1;
        for (k = 0; k < SilkConstants.SHELL_CODEC_FRAME_LENGTH; k++) {
          abs_q = (byte) Inlines.silk_abs(pulses[pulses_ptr + k]);
          for (j = nLS; j > 0; j--) {
            bit = Inlines.silk_RSHIFT(abs_q, j) & 1;
            psRangeEnc.enc_icdf(bit, SilkTables.silk_lsb_iCDF, 8);
          }
          bit = abs_q & 1;
          psRangeEnc.enc_icdf(bit, SilkTables.silk_lsb_iCDF, 8);
        }
      }
    }

    /**
     * *************
     */
    /* Encode signs */
    /**
     * *************
     */
    CodeSigns.silk_encode_signs(psRangeEnc, pulses, frame_length, signalType, quantOffsetType,
        sum_pulses);
  }
}
