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

class TonalityAnalysisState {

  boolean enabled = false;
  final float[] angle = new float[240];
  final float[] d_angle = new float[240];
  final float[] d2_angle = new float[240];
  final int[] inmem = new int[OpusConstants.ANALYSIS_BUF_SIZE];
  int mem_fill;
  /* number of usable samples in the buffer */
  final float[] prev_band_tonality = new float[OpusConstants.NB_TBANDS];
  float prev_tonality;
  final float[][] E =
      Arrays.InitTwoDimensionalArrayFloat(OpusConstants.NB_FRAMES, OpusConstants.NB_TBANDS);
  final float[] lowE = new float[OpusConstants.NB_TBANDS];
  final float[] highE = new float[OpusConstants.NB_TBANDS];
  final float[] meanE = new float[OpusConstants.NB_TOT_BANDS];
  final float[] mem = new float[32];
  final float[] cmean = new float[8];
  final float[] std = new float[9];
  float music_prob;
  float Etracker;
  float lowECount;
  int E_count;
  int last_music;
  int last_transition;
  int count;
  final float[] subframe_mem = new float[3];
  int analysis_offset;
  /**
   * Probability of having speech for time i to DETECT_SIZE-1 (and music before). pspeech[0] is the
   * probability that all frames in the window are speech.
   */
  final float[] pspeech = new float[OpusConstants.DETECT_SIZE];
  /**
   * Probability of having music for time i to DETECT_SIZE-1 (and speech before). pmusic[0] is the
   * probability that all frames in the window are music.
   */
  final float[] pmusic = new float[OpusConstants.DETECT_SIZE];
  float speech_confidence;
  float music_confidence;
  int speech_confidence_count;
  int music_confidence_count;
  int write_pos;
  int read_pos;
  int read_subframe;
  final AnalysisInfo[] info = new AnalysisInfo[OpusConstants.DETECT_SIZE];

  TonalityAnalysisState() {
    for (int c = 0; c < OpusConstants.DETECT_SIZE; c++) {
      info[c] = new AnalysisInfo();
    }
  }

  void Reset() {
    Arrays.MemSet(angle, 0f, 240);
    Arrays.MemSet(d_angle, 0f, 240);
    Arrays.MemSet(d2_angle, 0f, 240);
    Arrays.MemSet(inmem, 0, OpusConstants.ANALYSIS_BUF_SIZE);
    mem_fill = 0;
    Arrays.MemSet(prev_band_tonality, 0f, OpusConstants.NB_TBANDS);
    prev_tonality = 0;
    for (int c = 0; c < OpusConstants.NB_FRAMES; c++) {
      Arrays.MemSet(E[c], 0f, OpusConstants.NB_TBANDS);
    }
    Arrays.MemSet(lowE, 0f, OpusConstants.NB_TBANDS);
    Arrays.MemSet(highE, 0f, OpusConstants.NB_TBANDS);
    Arrays.MemSet(meanE, 0f, OpusConstants.NB_TOT_BANDS);
    Arrays.MemSet(mem, 0f, 32);
    Arrays.MemSet(cmean, 0f, 8);
    Arrays.MemSet(std, 0f, 9);
    music_prob = 0;
    Etracker = 0;
    lowECount = 0;
    E_count = 0;
    last_music = 0;
    last_transition = 0;
    count = 0;
    Arrays.MemSet(subframe_mem, 0f, 3);
    analysis_offset = 0;
    Arrays.MemSet(pspeech, 0f, OpusConstants.DETECT_SIZE);
    Arrays.MemSet(pmusic, 0f, OpusConstants.DETECT_SIZE);
    speech_confidence = 0;
    music_confidence = 0;
    speech_confidence_count = 0;
    music_confidence_count = 0;
    write_pos = 0;
    read_pos = 0;
    read_subframe = 0;
    for (int c = 0; c < OpusConstants.DETECT_SIZE; c++) {
      info[c].Reset();
    }
  }
}
