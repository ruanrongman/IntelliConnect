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
/// Struct for Packet Loss Concealment
/// </summary>
class PLCStruct {

  int pitchL_Q8 = 0;
  /* Pitch lag to use for voiced concealment */
  final short[] LTPCoef_Q14 = new short[SilkConstants.LTP_ORDER];
  /* LTP coeficients to use for voiced concealment */
  final short[] prevLPC_Q12 = new short[SilkConstants.MAX_LPC_ORDER];
  int last_frame_lost = 0;
  /* Was previous frame lost */
  int rand_seed = 0;
  /* Seed for unvoiced signal generation */
  short randScale_Q14 = 0;
  /* Scaling of unvoiced random signal */
  int conc_energy = 0;
  int conc_energy_shift = 0;
  short prevLTP_scale_Q14 = 0;
  final int[] prevGain_Q16 = new int[2];
  int fs_kHz = 0;
  int nb_subfr = 0;
  int subfr_length = 0;

  void Reset() {
    pitchL_Q8 = 0;
    Arrays.MemSet(LTPCoef_Q14, (short) 0, SilkConstants.LTP_ORDER);
    Arrays.MemSet(prevLPC_Q12, (short) 0, SilkConstants.MAX_LPC_ORDER);
    last_frame_lost = 0;
    rand_seed = 0;
    randScale_Q14 = 0;
    conc_energy = 0;
    conc_energy_shift = 0;
    prevLTP_scale_Q14 = 0;
    Arrays.MemSet(prevGain_Q16, 0, 2);
    fs_kHz = 0;
    nb_subfr = 0;
    subfr_length = 0;
  }
}
