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

public class OpusConstants {

  /// <summary>
  /// Auto/default setting
  /// </summary>
  public static final int OPUS_AUTO = -1000;

  /// <summary>
  /// Maximum bitrate
  /// </summary>
  public static final int OPUS_BITRATE_MAX = -1;

  // from analysis.c
  public static final int NB_FRAMES = 8;
  public static final int NB_TBANDS = 18;
  public static final int NB_TOT_BANDS = 21;
  public static final int NB_TONAL_SKIP_BANDS = 9;
  public static final int ANALYSIS_BUF_SIZE = 720;
  /* 15 ms at 48 kHz */
  public static final int DETECT_SIZE = 200;

  public static final int MAX_ENCODER_BUFFER = 480;
}
