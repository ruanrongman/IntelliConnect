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

class StereoEncodeState {

  final short[] pred_prev_Q13 = new short[2];
  final short[] sMid = new short[2];
  final short[] sSide = new short[2];
  final int[] mid_side_amp_Q0 = new int[4];
  short smth_width_Q14 = 0;
  short width_prev_Q14 = 0;
  short silent_side_len = 0;
  final byte[][][] predIx =
      Arrays.InitThreeDimensionalArrayByte(SilkConstants.MAX_FRAMES_PER_PACKET, 2, 3);
  final byte[] mid_only_flags = new byte[SilkConstants.MAX_FRAMES_PER_PACKET];

  void Reset() {
    Arrays.MemSet(pred_prev_Q13, (short) 0, 2);
    Arrays.MemSet(sMid, (short) 0, 2);
    Arrays.MemSet(sSide, (short) 0, 2);
    Arrays.MemSet(mid_side_amp_Q0, 0, 4);
    smth_width_Q14 = 0;
    width_prev_Q14 = 0;
    silent_side_len = 0;
    for (int x = 0; x < SilkConstants.MAX_FRAMES_PER_PACKET; x++) {
      for (int y = 0; y < 2; y++) {
        Arrays.MemSet(predIx[x][y], (byte) 0, 3);
      }
    }

    Arrays.MemSet(mid_only_flags, (byte) 0, SilkConstants.MAX_FRAMES_PER_PACKET);
  }
}
