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
/// Decoder super struct
/// </summary>
class SilkDecoder {

  final SilkChannelDecoder[] channel_state =
      new SilkChannelDecoder[SilkConstants.DECODER_NUM_CHANNELS];
  final StereoDecodeState sStereo = new StereoDecodeState();
  int nChannelsAPI = 0;
  int nChannelsInternal = 0;
  int prev_decode_only_middle = 0;

  SilkDecoder() {
    for (int c = 0; c < SilkConstants.DECODER_NUM_CHANNELS; c++) {
      channel_state[c] = new SilkChannelDecoder();
    }
  }

  void Reset() {
    for (int c = 0; c < SilkConstants.DECODER_NUM_CHANNELS; c++) {
      channel_state[c].Reset();
    }
    sStereo.Reset();
    nChannelsAPI = 0;
    nChannelsInternal = 0;
    prev_decode_only_middle = 0;
  }
}
