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

class AnalysisInfo {

  boolean enabled = false;
  int valid = 0;
  float tonality = 0;
  float tonality_slope = 0;
  float noisiness = 0;
  float activity = 0;
  float music_prob = 0;
  int bandwidth = 0;

  AnalysisInfo() {}

  void Assign(AnalysisInfo other) {
    this.valid = other.valid;
    this.tonality = other.tonality;
    this.tonality_slope = other.tonality_slope;
    this.noisiness = other.noisiness;
    this.activity = other.activity;
    this.music_prob = other.music_prob;
    this.bandwidth = other.bandwidth;
  }

  void Reset() {
    valid = 0;
    tonality = 0;
    tonality_slope = 0;
    noisiness = 0;
    activity = 0;
    music_prob = 0;
    bandwidth = 0;
  }
}
