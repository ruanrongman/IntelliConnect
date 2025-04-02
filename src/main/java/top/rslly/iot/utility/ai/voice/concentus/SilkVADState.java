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
/// VAD state
/// </summary>
class SilkVADState {

  /// <summary>
  /// Analysis filterbank state: 0-8 kHz
  /// </summary>
  final int[] AnaState = new int[2];

  /// <summary>
  /// Analysis filterbank state: 0-4 kHz
  /// </summary>
  final int[] AnaState1 = new int[2];

  /// <summary>
  /// Analysis filterbank state: 0-2 kHz
  /// </summary>
  final int[] AnaState2 = new int[2];

  /// <summary>
  /// Subframe energies
  /// </summary>
  final int[] XnrgSubfr = new int[SilkConstants.VAD_N_BANDS];

  /// <summary>
  /// Smoothed energy level in each band
  /// </summary>
  final int[] NrgRatioSmth_Q8 = new int[SilkConstants.VAD_N_BANDS];

  /// <summary>
  /// State of differentiator in the lowest band
  /// </summary>
  short HPstate = 0;

  /// <summary>
  /// Noise energy level in each band
  /// </summary>
  final int[] NL = new int[SilkConstants.VAD_N_BANDS];

  /// <summary>
  /// Inverse noise energy level in each band
  /// </summary>
  final int[] inv_NL = new int[SilkConstants.VAD_N_BANDS];

  /// <summary>
  /// Noise level estimator bias/offset
  /// </summary>
  final int[] NoiseLevelBias = new int[SilkConstants.VAD_N_BANDS];

  /// <summary>
  /// Frame counter used in the initial phase
  /// </summary>
  int counter = 0;

  void Reset() {
    Arrays.MemSet(AnaState, 0, 2);
    Arrays.MemSet(AnaState1, 0, 2);
    Arrays.MemSet(AnaState2, 0, 2);
    Arrays.MemSet(XnrgSubfr, 0, SilkConstants.VAD_N_BANDS);
    Arrays.MemSet(NrgRatioSmth_Q8, 0, SilkConstants.VAD_N_BANDS);
    HPstate = 0;
    Arrays.MemSet(NL, 0, SilkConstants.VAD_N_BANDS);
    Arrays.MemSet(inv_NL, 0, SilkConstants.VAD_N_BANDS);
    Arrays.MemSet(NoiseLevelBias, 0, SilkConstants.VAD_N_BANDS);
    counter = 0;
  }
}
