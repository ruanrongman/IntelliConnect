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

public enum OpusFramesize {
  /**
   * Error state
   */
  OPUS_FRAMESIZE_UNKNOWN,
  /// <summary>
  /// Select frame size from the argument (default)
  /// </summary>
  OPUS_FRAMESIZE_ARG,
  /// <summary>
  /// Use 2.5 ms frames
  /// </summary>
  OPUS_FRAMESIZE_2_5_MS,
  /// <summary>
  /// Use 5 ms frames
  /// </summary>
  OPUS_FRAMESIZE_5_MS,
  /// <summary>
  /// Use 10 ms frames
  /// </summary>
  OPUS_FRAMESIZE_10_MS,
  /// <summary>
  /// Use 20 ms frames
  /// </summary>
  OPUS_FRAMESIZE_20_MS,
  /// <summary>
  /// Use 40 ms frames
  /// </summary>
  OPUS_FRAMESIZE_40_MS,
  /// <summary>
  /// Use 60 ms frames
  /// </summary>
  OPUS_FRAMESIZE_60_MS,
  /// <summary>
  /// Do not use - not fully implemented. Optimize the frame size dynamically.
  /// </summary>
  OPUS_FRAMESIZE_VARIABLE
}


class OpusFramesizeHelpers {

  static int GetOrdinal(OpusFramesize size) {
    switch (size) {
      case OPUS_FRAMESIZE_ARG:
        return 1;
      case OPUS_FRAMESIZE_2_5_MS:
        return 2;
      case OPUS_FRAMESIZE_5_MS:
        return 3;
      case OPUS_FRAMESIZE_10_MS:
        return 4;
      case OPUS_FRAMESIZE_20_MS:
        return 5;
      case OPUS_FRAMESIZE_40_MS:
        return 6;
      case OPUS_FRAMESIZE_60_MS:
        return 7;
      case OPUS_FRAMESIZE_VARIABLE:
        return 8;
    }

    return -1;
  }
}
