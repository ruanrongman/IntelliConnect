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

class VorbisLayout {

  VorbisLayout(int streams, int coupled_streams, short[] map) {
    nb_streams = streams;
    nb_coupled_streams = coupled_streams;
    mapping = map;
  }

  int nb_streams;
  int nb_coupled_streams;
  short[] mapping;

  /* Index is nb_channel-1 */
  static final VorbisLayout[] vorbis_mappings = {
      new VorbisLayout(1, 0, new short[] {0}), /* 1: mono */
      new VorbisLayout(1, 1, new short[] {0, 1}), /* 2: stereo */
      new VorbisLayout(2, 1, new short[] {0, 2, 1}), /* 3: 1-d surround */
      new VorbisLayout(2, 2, new short[] {0, 1, 2, 3}), /* 4: quadraphonic surround */
      new VorbisLayout(3, 2, new short[] {0, 4, 1, 2, 3}), /* 5: 5-channel surround */
      new VorbisLayout(4, 2, new short[] {0, 4, 1, 2, 3, 5}), /* 6: 5.1 surround */
      new VorbisLayout(4, 3, new short[] {0, 4, 1, 2, 3, 5, 6}), /* 7: 6.1 surround */
      new VorbisLayout(5, 3, new short[] {0, 6, 1, 2, 3, 4, 5, 7}), /* 8: 7.1 surround */};
}
