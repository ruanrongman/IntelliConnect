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

class OpusMultistream {

  static int validate_layout(ChannelLayout layout) {
    int i, max_channel;

    max_channel = layout.nb_streams + layout.nb_coupled_streams;
    if (max_channel > 255) {
      return 0;
    }
    for (i = 0; i < layout.nb_channels; i++) {
      if (layout.mapping[i] >= max_channel && layout.mapping[i] != 255) {
        return 0;
      }
    }
    return 1;
  }

  static int get_left_channel(ChannelLayout layout, int stream_id, int prev) {
    int i;
    i = (prev < 0) ? 0 : prev + 1;
    for (; i < layout.nb_channels; i++) {
      if (layout.mapping[i] == stream_id * 2) {
        return i;
      }
    }
    return -1;
  }

  static int get_right_channel(ChannelLayout layout, int stream_id, int prev) {
    int i;
    i = (prev < 0) ? 0 : prev + 1;
    for (; i < layout.nb_channels; i++) {
      if (layout.mapping[i] == stream_id * 2 + 1) {
        return i;
      }
    }
    return -1;
  }

  static int get_mono_channel(ChannelLayout layout, int stream_id, int prev) {
    int i;
    i = (prev < 0) ? 0 : prev + 1;
    for (; i < layout.nb_channels; i++) {
      if (layout.mapping[i] == stream_id + layout.nb_coupled_streams) {
        return i;
      }
    }
    return -1;
  }
}
