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
/// Struct for CNG
/// </summary>
class CNGState {

  final int[] CNG_exc_buf_Q14 = new int[SilkConstants.MAX_FRAME_LENGTH];
  final short[] CNG_smth_NLSF_Q15 = new short[SilkConstants.MAX_LPC_ORDER];
  final int[] CNG_synth_state = new int[SilkConstants.MAX_LPC_ORDER];
  int CNG_smth_Gain_Q16 = 0;
  int rand_seed = 0;
  int fs_kHz = 0;

  void Reset() {
    Arrays.MemSet(CNG_exc_buf_Q14, 0, SilkConstants.MAX_FRAME_LENGTH);
    Arrays.MemSet(CNG_smth_NLSF_Q15, (short) 0, SilkConstants.MAX_LPC_ORDER);
    Arrays.MemSet(CNG_synth_state, 0, SilkConstants.MAX_LPC_ORDER);
    CNG_smth_Gain_Q16 = 0;
    rand_seed = 0;
    fs_kHz = 0;
  }
}
