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

class DecodePulses {

  /**
   * ******************************************
   */
  /* Decode quantization indices of excitation */
  /**
   * ******************************************
   */
  static void silk_decode_pulses(
      EntropyCoder psRangeDec, /* I/O Compressor data structure */
      short[] pulses, /* O Excitation signal */
      int signalType, /* I Sigtype */
      int quantOffsetType, /* I quantOffsetType */
      int frame_length /* I Frame length */
  ) {
    int i, j, k, iter, abs_q, nLS, RateLevelIndex;
    int[] sum_pulses = new int[SilkConstants.MAX_NB_SHELL_BLOCKS];
    int[] nLshifts = new int[SilkConstants.MAX_NB_SHELL_BLOCKS];
    int pulses_ptr;

    /**
     * ******************
     */
    /* Decode rate level */
    /**
     * ******************
     */
    RateLevelIndex = psRangeDec.dec_icdf(SilkTables.silk_rate_levels_iCDF[signalType >> 1], 8);

    /* Calculate number of shell blocks */
    Inlines.OpusAssert(
        1 << SilkConstants.LOG2_SHELL_CODEC_FRAME_LENGTH == SilkConstants.SHELL_CODEC_FRAME_LENGTH);
    iter = Inlines.silk_RSHIFT(frame_length, SilkConstants.LOG2_SHELL_CODEC_FRAME_LENGTH);
    if (iter * SilkConstants.SHELL_CODEC_FRAME_LENGTH < frame_length) {
      Inlines.OpusAssert(frame_length == 12 * 10);
      /* Make sure only happens for 10 ms @ 12 kHz */
      iter++;
    }

    /**
     * ************************************************
     */
    /* Sum-Weighted-Pulses Decoding */
    /**
     * ************************************************
     */
    for (i = 0; i < iter; i++) {
      nLshifts[i] = 0;
      sum_pulses[i] = psRangeDec.dec_icdf(SilkTables.silk_pulses_per_block_iCDF[RateLevelIndex], 8);

      /* LSB indication */
      while (sum_pulses[i] == SilkConstants.SILK_MAX_PULSES + 1) {
        nLshifts[i]++;
        /* When we've already got 10 LSBs, we shift the table to not allow (SILK_MAX_PULSES + 1) */
        sum_pulses[i] = psRangeDec.dec_icdf(
            SilkTables.silk_pulses_per_block_iCDF[SilkConstants.N_RATE_LEVELS - 1],
            (nLshifts[i] == 10 ? 1 : 0), 8);
      }
    }

    /**
     * ************************************************
     */
    /* Shell decoding */
    /**
     * ************************************************
     */
    for (i = 0; i < iter; i++) {
      if (sum_pulses[i] > 0) {
        ShellCoder.silk_shell_decoder(pulses,
            Inlines.silk_SMULBB(i, SilkConstants.SHELL_CODEC_FRAME_LENGTH), psRangeDec,
            sum_pulses[i]);
      } else {
        Arrays.MemSetWithOffset(pulses, (byte) 0,
            Inlines.silk_SMULBB(i, SilkConstants.SHELL_CODEC_FRAME_LENGTH),
            SilkConstants.SHELL_CODEC_FRAME_LENGTH);
      }
    }

    /**
     * ************************************************
     */
    /* LSB Decoding */
    /**
     * ************************************************
     */
    for (i = 0; i < iter; i++) {
      if (nLshifts[i] > 0) {
        nLS = nLshifts[i];
        pulses_ptr = Inlines.silk_SMULBB(i, SilkConstants.SHELL_CODEC_FRAME_LENGTH);
        for (k = 0; k < SilkConstants.SHELL_CODEC_FRAME_LENGTH; k++) {
          abs_q = pulses[pulses_ptr + k];
          for (j = 0; j < nLS; j++) {
            abs_q = Inlines.silk_LSHIFT(abs_q, 1);
            abs_q += psRangeDec.dec_icdf(SilkTables.silk_lsb_iCDF, 8);
          }
          pulses[pulses_ptr + k] = (short) (abs_q);
        }
        /* Mark the number of pulses non-zero for sign decoding. */
        sum_pulses[i] |= nLS << 5;
      }
    }

    /**
     * *************************************
     */
    /* Decode and add signs to pulse signal */
    /**
     * *************************************
     */
    CodeSigns.silk_decode_signs(psRangeDec, pulses, frame_length, signalType, quantOffsetType,
        sum_pulses);
  }
}
