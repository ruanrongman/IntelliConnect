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
/// Structure for controlling encoder operation
/// </summary>
class EncControlState {

  /* I: Number of channels; 1/2 */
  int nChannelsAPI = 0;

  /* I: Number of channels; 1/2 */
  int nChannelsInternal = 0;

  /* I: Input signal sampling rate in Hertz; 8000/12000/16000/24000/32000/44100/48000 */
  int API_sampleRate = 0;

  /* I: Maximum sampling rate in Hertz; 8000/12000/16000 */
  int maxInternalSampleRate = 0;

  /* I: Minimum sampling rate in Hertz; 8000/12000/16000 */
  int minInternalSampleRate = 0;

  /* I: Soft request for sampling rate in Hertz; 8000/12000/16000 */
  int desiredInternalSampleRate = 0;

  /* I: Number of samples per packet in milliseconds; 10/20/40/60 */
  int payloadSize_ms = 0;

  /* I: Bitrate during active speech in bits/second; internally limited */
  int bitRate = 0;

  /* I: Uplink packet loss in percent (0-100) */
  int packetLossPercentage = 0;

  /* I: Complexity mode; 0 is lowest, 10 is highest complexity */
  int complexity = 0;

  /* I: Flag to enable in-band Forward Error Correction (FEC); 0/1 */
  int useInBandFEC = 0;

  /* I: Flag to enable discontinuous transmission (DTX); 0/1 */
  int useDTX = 0;

  /* I: Flag to use constant bitrate */
  int useCBR = 0;

  /* I: Maximum number of bits allowed for the frame */
  int maxBits = 0;

  /* I: Causes a smooth downmix to mono */
  int toMono = 0;

  /* I: Opus encoder is allowing us to switch bandwidth */
  int opusCanSwitch = 0;

  /* I: Make frames as independent as possible (but still use LPC) */
  int reducedDependency = 0;

  /* O: Internal sampling rate used, in Hertz; 8000/12000/16000 */
  int internalSampleRate = 0;

  /* O: Flag that bandwidth switching is allowed (because low voice activity) */
  int allowBandwidthSwitch = 0;

  /*
   * O: Flag that SILK runs in WB mode without variable LP filter (use for switching between
   * WB/SWB/FB)
   */
  int inWBmodeWithoutVariableLP = 0;

  /* O: Stereo width */
  int stereoWidth_Q14 = 0;

  /* O: Tells the Opus encoder we're ready to switch */
  int switchReady = 0;

  void Reset() {
    nChannelsAPI = 0;
    nChannelsInternal = 0;
    API_sampleRate = 0;
    maxInternalSampleRate = 0;
    minInternalSampleRate = 0;
    desiredInternalSampleRate = 0;
    payloadSize_ms = 0;
    bitRate = 0;
    packetLossPercentage = 0;
    complexity = 0;
    useInBandFEC = 0;
    useDTX = 0;
    useCBR = 0;
    maxBits = 0;
    toMono = 0;
    opusCanSwitch = 0;
    reducedDependency = 0;
    internalSampleRate = 0;
    allowBandwidthSwitch = 0;
    inWBmodeWithoutVariableLP = 0;
    stereoWidth_Q14 = 0;
    switchReady = 0;
  }

  /// <summary>
  /// Checks this encoder control struct and returns error code, if any
  /// </summary>
  /// <returns></returns>
  int check_control_input() {
    if (((API_sampleRate != 8000)
        && (API_sampleRate != 12000)
        && (API_sampleRate != 16000)
        && (API_sampleRate != 24000)
        && (API_sampleRate != 32000)
        && (API_sampleRate != 44100)
        && (API_sampleRate != 48000))
        || ((desiredInternalSampleRate != 8000)
            && (desiredInternalSampleRate != 12000)
            && (desiredInternalSampleRate != 16000))
        || ((maxInternalSampleRate != 8000)
            && (maxInternalSampleRate != 12000)
            && (maxInternalSampleRate != 16000))
        || ((minInternalSampleRate != 8000)
            && (minInternalSampleRate != 12000)
            && (minInternalSampleRate != 16000))
        || (minInternalSampleRate > desiredInternalSampleRate)
        || (maxInternalSampleRate < desiredInternalSampleRate)
        || (minInternalSampleRate > maxInternalSampleRate)) {
      Inlines.OpusAssert(false);
      return SilkError.SILK_ENC_FS_NOT_SUPPORTED;
    }
    if (payloadSize_ms != 10
        && payloadSize_ms != 20
        && payloadSize_ms != 40
        && payloadSize_ms != 60) {
      Inlines.OpusAssert(false);
      return SilkError.SILK_ENC_PACKET_SIZE_NOT_SUPPORTED;
    }
    if (packetLossPercentage < 0 || packetLossPercentage > 100) {
      Inlines.OpusAssert(false);
      return SilkError.SILK_ENC_INVALID_LOSS_RATE;
    }
    if (useDTX < 0 || useDTX > 1) {
      Inlines.OpusAssert(false);
      return SilkError.SILK_ENC_INVALID_DTX_SETTING;
    }
    if (useCBR < 0 || useCBR > 1) {
      Inlines.OpusAssert(false);
      return SilkError.SILK_ENC_INVALID_CBR_SETTING;
    }
    if (useInBandFEC < 0 || useInBandFEC > 1) {
      Inlines.OpusAssert(false);
      return SilkError.SILK_ENC_INVALID_INBAND_FEC_SETTING;
    }
    if (nChannelsAPI < 1 || nChannelsAPI > SilkConstants.ENCODER_NUM_CHANNELS) {
      Inlines.OpusAssert(false);
      return SilkError.SILK_ENC_INVALID_NUMBER_OF_CHANNELS_ERROR;
    }
    if (nChannelsInternal < 1 || nChannelsInternal > SilkConstants.ENCODER_NUM_CHANNELS) {
      Inlines.OpusAssert(false);
      return SilkError.SILK_ENC_INVALID_NUMBER_OF_CHANNELS_ERROR;
    }
    if (nChannelsInternal > nChannelsAPI) {
      Inlines.OpusAssert(false);
      return SilkError.SILK_ENC_INVALID_NUMBER_OF_CHANNELS_ERROR;
    }
    if (complexity < 0 || complexity > 10) {
      Inlines.OpusAssert(false);
      return SilkError.SILK_ENC_INVALID_COMPLEXITY_SETTING;
    }

    return SilkError.SILK_NO_ERROR;
  }
}
