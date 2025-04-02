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

public enum OpusBandwidth {
  OPUS_BANDWIDTH_UNKNOWN, OPUS_BANDWIDTH_AUTO, OPUS_BANDWIDTH_NARROWBAND, OPUS_BANDWIDTH_MEDIUMBAND, OPUS_BANDWIDTH_WIDEBAND, OPUS_BANDWIDTH_SUPERWIDEBAND, OPUS_BANDWIDTH_FULLBAND
}


// Helpers to port over uses of OpusBandwidth as an integer
class OpusBandwidthHelpers {

  static int GetOrdinal(OpusBandwidth bw) {
    switch (bw) {
      case OPUS_BANDWIDTH_NARROWBAND:
        return 1;
      case OPUS_BANDWIDTH_MEDIUMBAND:
        return 2;
      case OPUS_BANDWIDTH_WIDEBAND:
        return 3;
      case OPUS_BANDWIDTH_SUPERWIDEBAND:
        return 4;
      case OPUS_BANDWIDTH_FULLBAND:
        return 5;
    }

    return -1;
  }

  static OpusBandwidth GetBandwidth(int ordinal) {
    switch (ordinal) {
      case 1:
        return OpusBandwidth.OPUS_BANDWIDTH_NARROWBAND;
      case 2:
        return OpusBandwidth.OPUS_BANDWIDTH_MEDIUMBAND;
      case 3:
        return OpusBandwidth.OPUS_BANDWIDTH_WIDEBAND;
      case 4:
        return OpusBandwidth.OPUS_BANDWIDTH_SUPERWIDEBAND;
      case 5:
        return OpusBandwidth.OPUS_BANDWIDTH_FULLBAND;
    }

    return OpusBandwidth.OPUS_BANDWIDTH_AUTO;
  }

  static OpusBandwidth MIN(OpusBandwidth a, OpusBandwidth b) {
    if (GetOrdinal(a) < GetOrdinal(b)) {
      return a;
    }
    return b;
  }

  static OpusBandwidth MAX(OpusBandwidth a, OpusBandwidth b) {
    if (GetOrdinal(a) > GetOrdinal(b)) {
      return a;
    }
    return b;
  }

  static OpusBandwidth SUBTRACT(OpusBandwidth a, int b) {
    return GetBandwidth(GetOrdinal(a) - b);
  }
}
