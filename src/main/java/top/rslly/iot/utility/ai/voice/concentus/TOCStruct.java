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
/// Struct for TOC (Table of Contents)
/// </summary>
class TOCStruct {

  /// <summary>
  /// Voice activity for packet
  /// </summary>
  int VADFlag = 0;

  /// <summary>
  /// Voice activity for each frame in packet
  /// </summary>
  final int[] VADFlags = new int[SilkConstants.SILK_MAX_FRAMES_PER_PACKET];

  /// <summary>
  /// Flag indicating if packet contains in-band FEC
  /// </summary>
  int inbandFECFlag = 0;

  void Reset() {
    VADFlag = 0;
    Arrays.MemSet(VADFlags, 0, SilkConstants.SILK_MAX_FRAMES_PER_PACKET);
    inbandFECFlag = 0;
  }
}
