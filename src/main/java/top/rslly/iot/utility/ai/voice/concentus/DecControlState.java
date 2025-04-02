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
/// Structure for controlling decoder operation and reading decoder status
/// </summary>
class DecControlState {

  /* I: Number of channels; 1/2 */
  int nChannelsAPI = 0;

  /* I: Number of channels; 1/2 */
  int nChannelsInternal = 0;

  /* I: Output signal sampling rate in Hertz; 8000/12000/16000/24000/32000/44100/48000 */
  int API_sampleRate = 0;

  /* I: Internal sampling rate used, in Hertz; 8000/12000/16000 */
  int internalSampleRate = 0;

  /* I: Number of samples per packet in milliseconds; 10/20/40/60 */
  int payloadSize_ms = 0;

  /* O: Pitch lag of previous frame (0 if unvoiced), measured in samples at 48 kHz */
  int prevPitchLag = 0;

  void Reset() {
    nChannelsAPI = 0;
    nChannelsInternal = 0;
    API_sampleRate = 0;
    internalSampleRate = 0;
    payloadSize_ms = 0;
    prevPitchLag = 0;
  }
}
