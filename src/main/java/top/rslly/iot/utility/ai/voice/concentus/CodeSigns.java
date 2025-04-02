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

class CodeSigns {

  private static int silk_enc_map(int a) {
    return (Inlines.silk_RSHIFT((a), 15) + 1);
  }

  private static int silk_dec_map(int a) {
    return (Inlines.silk_LSHIFT((a), 1) - 1);
  }

  /// <summary>
  /// Encodes signs of excitation
  /// </summary>
  /// <param name="psRangeEnc">I/O Compressor data structure</param>
  /// <param name="pulses">I pulse signal</param>
  /// <param name="length">I length of input</param>
  /// <param name="signalType">I Signal type</param>
  /// <param name="quantOffsetType">I Quantization offset type</param>
  /// <param name="sum_pulses">I Sum of absolute pulses per block [MAX_NB_SHELL_BLOCKS]</param>
  static void silk_encode_signs(
      EntropyCoder psRangeEnc,
      byte[] pulses,
      int length,
      int signalType,
      int quantOffsetType,
      int[] sum_pulses) {
    int i, j, p;
    short[] icdf = new short[2];
    int q_ptr;
    short[] sign_icdf = SilkTables.silk_sign_iCDF;
    int icdf_ptr;

    icdf[1] = 0;
    q_ptr = 0;
    i = Inlines.silk_SMULBB(7, Inlines.silk_ADD_LSHIFT(quantOffsetType, signalType, 1));
    icdf_ptr = i;
    length = Inlines.silk_RSHIFT(length + (SilkConstants.SHELL_CODEC_FRAME_LENGTH / 2),
        SilkConstants.LOG2_SHELL_CODEC_FRAME_LENGTH);
    for (i = 0; i < length; i++) {
      p = sum_pulses[i];
      if (p > 0) {
        icdf[0] = sign_icdf[icdf_ptr + Inlines.silk_min(p & 0x1F, 6)];
        for (j = q_ptr; j < q_ptr + SilkConstants.SHELL_CODEC_FRAME_LENGTH; j++) {
          if (pulses[j] != 0) {
            psRangeEnc.enc_icdf(silk_enc_map(pulses[j]), icdf, 8);
          }
        }
      }

      q_ptr += SilkConstants.SHELL_CODEC_FRAME_LENGTH;
    }
  }

  /// <summary>
  /// Decodes signs of excitation
  /// </summary>
  /// <param name="psRangeDec">I/O Compressor data structure</param>
  /// <param name="pulses">I/O pulse signal</param>
  /// <param name="length">I length of input</param>
  /// <param name="signalType">I Signal type</param>
  /// <param name="quantOffsetType">I Quantization offset type</param>
  /// <param name="sum_pulses">I Sum of absolute pulses per block [MAX_NB_SHELL_BLOCKS]</param>
  static void silk_decode_signs(
      EntropyCoder psRangeDec,
      short[] pulses,
      int length,
      int signalType,
      int quantOffsetType,
      int[] sum_pulses) {
    int i, j, p;
    short[] icdf = new short[2];
    int q_ptr;
    short[] icdf_table = SilkTables.silk_sign_iCDF;
    int icdf_ptr;

    icdf[1] = 0;
    q_ptr = 0;
    i = Inlines.silk_SMULBB(7, Inlines.silk_ADD_LSHIFT(quantOffsetType, signalType, 1));
    icdf_ptr = i;
    length = Inlines.silk_RSHIFT(length + SilkConstants.SHELL_CODEC_FRAME_LENGTH / 2,
        SilkConstants.LOG2_SHELL_CODEC_FRAME_LENGTH);

    for (i = 0; i < length; i++) {
      p = sum_pulses[i];

      if (p > 0) {
        icdf[0] = icdf_table[icdf_ptr + Inlines.silk_min(p & 0x1F, 6)];
        for (j = 0; j < SilkConstants.SHELL_CODEC_FRAME_LENGTH; j++) {
          if (pulses[q_ptr + j] > 0) {
            /* attach sign */
            pulses[q_ptr + j] *= (short) (silk_dec_map(psRangeDec.dec_icdf(icdf, 8)));
          }
        }
      }

      q_ptr += SilkConstants.SHELL_CODEC_FRAME_LENGTH;
    }
  }
}
