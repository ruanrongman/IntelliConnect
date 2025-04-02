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
/// Prefilter state
/// </summary>
class SilkPrefilterState {

  final short[] sLTP_shp = new short[SilkConstants.LTP_BUF_LENGTH];
  final int[] sAR_shp = new int[SilkConstants.MAX_SHAPE_LPC_ORDER + 1];
  int sLTP_shp_buf_idx = 0;
  int sLF_AR_shp_Q12 = 0;
  int sLF_MA_shp_Q12 = 0;
  int sHarmHP_Q2 = 0;
  int rand_seed = 0;
  int lagPrev = 0;

  SilkPrefilterState() {

  }

  void Reset() {
    Arrays.MemSet(sLTP_shp, (short) 0, SilkConstants.LTP_BUF_LENGTH);
    Arrays.MemSet(sAR_shp, 0, SilkConstants.MAX_SHAPE_LPC_ORDER + 1);
    sLTP_shp_buf_idx = 0;
    sLF_AR_shp_Q12 = 0;
    sLF_MA_shp_Q12 = 0;
    sHarmHP_Q2 = 0;
    rand_seed = 0;
    lagPrev = 0;
  }
}
