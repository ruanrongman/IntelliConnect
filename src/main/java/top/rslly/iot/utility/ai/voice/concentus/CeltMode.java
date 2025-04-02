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

class CeltMode {

  int Fs = 0;
  int overlap = 0;

  int nbEBands = 0;
  int effEBands = 0;
  int[] preemph = {0, 0, 0, 0};

  /// <summary>
  /// Definition for each "pseudo-critical band"
  /// </summary>
  short[] eBands = null;

  int maxLM = 0;
  int nbShortMdcts = 0;
  int shortMdctSize = 0;

  /// <summary>
  /// Number of lines in allocVectors
  /// </summary>
  int nbAllocVectors = 0;

  /// <summary>
  /// Number of bits in each band for several rates
  /// </summary>
  short[] allocVectors = null;
  short[] logN = null;

  int[] window = null;
  MDCTLookup mdct = new MDCTLookup();
  PulseCache cache = new PulseCache();

  private CeltMode() {}

  static final CeltMode mode48000_960_120 = new CeltMode();

  static {
    mode48000_960_120.Fs = 48000;
    mode48000_960_120.overlap = 120;
    mode48000_960_120.nbEBands = 21;
    mode48000_960_120.effEBands = 21;
    mode48000_960_120.preemph = new int[] {27853, 0, 4096, 8192};
    mode48000_960_120.eBands = CeltTables.eband5ms;
    mode48000_960_120.maxLM = 3;
    mode48000_960_120.nbShortMdcts = 8;
    mode48000_960_120.shortMdctSize = 120;
    mode48000_960_120.nbAllocVectors = 11;
    mode48000_960_120.allocVectors = CeltTables.band_allocation;
    mode48000_960_120.logN = CeltTables.logN400;
    mode48000_960_120.window = CeltTables.window120;
    mode48000_960_120.mdct = new MDCTLookup();

    mode48000_960_120.mdct.n = 1920;
    mode48000_960_120.mdct.maxshift = 3;
    mode48000_960_120.mdct.kfft = new FFTState[] {
        CeltTables.fft_state48000_960_0,
        CeltTables.fft_state48000_960_1,
        CeltTables.fft_state48000_960_2,
        CeltTables.fft_state48000_960_3,};

    mode48000_960_120.mdct.trig = CeltTables.mdct_twiddles960;
    mode48000_960_120.cache = new PulseCache();
    mode48000_960_120.cache.size = 392;
    mode48000_960_120.cache.index = CeltTables.cache_index50;
    mode48000_960_120.cache.bits = CeltTables.cache_bits50;
    mode48000_960_120.cache.caps = CeltTables.cache_caps50;
  };
}
