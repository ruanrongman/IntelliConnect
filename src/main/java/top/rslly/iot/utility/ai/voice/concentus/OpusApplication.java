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

public enum OpusApplication {
  OPUS_APPLICATION_UNIMPLEMENTED,
  /// <summary>
  /// Best for most VoIP/videoconference applications where listening quality and intelligibility
  /// matter most
  /// </summary>
  OPUS_APPLICATION_VOIP,
  /// <summary>
  /// Best for broadcast/high-fidelity application where the decoded audio should be as close as
  /// possible to the input
  /// </summary>
  OPUS_APPLICATION_AUDIO,
  /// <summary>
  /// Only use when lowest-achievable latency is what matters most. Voice-optimized modes cannot be
  /// used.
  /// </summary>
  OPUS_APPLICATION_RESTRICTED_LOWDELAY
}
