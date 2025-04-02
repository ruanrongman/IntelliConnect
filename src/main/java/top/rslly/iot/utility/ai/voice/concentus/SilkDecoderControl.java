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
/// Decoder control
/// </summary>
class SilkDecoderControl {

  /* Prediction and coding parameters */
  final int[] pitchL = new int[SilkConstants.MAX_NB_SUBFR];
  final int[] Gains_Q16 = new int[SilkConstants.MAX_NB_SUBFR];

  /* Holds interpolated and final coefficients */
  final short[][] PredCoef_Q12 =
      Arrays.InitTwoDimensionalArrayShort(2, SilkConstants.MAX_LPC_ORDER);
  final short[] LTPCoef_Q14 = new short[SilkConstants.LTP_ORDER * SilkConstants.MAX_NB_SUBFR];
  int LTP_scale_Q14 = 0;

  void Reset() {
    Arrays.MemSet(pitchL, 0, SilkConstants.MAX_NB_SUBFR);
    Arrays.MemSet(Gains_Q16, 0, SilkConstants.MAX_NB_SUBFR);
    Arrays.MemSet(PredCoef_Q12[0], (short) 0, SilkConstants.MAX_LPC_ORDER);
    Arrays.MemSet(PredCoef_Q12[1], (short) 0, SilkConstants.MAX_LPC_ORDER);
    Arrays.MemSet(LTPCoef_Q14, (short) 0, SilkConstants.LTP_ORDER * SilkConstants.MAX_NB_SUBFR);
    LTP_scale_Q14 = 0;
  }
}
