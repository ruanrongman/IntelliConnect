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
/// multi-layer perceptron processor
/// </summary>
class MultiLayerPerceptron {

  private static final int MAX_NEURONS = 100;

  static float tansig_approx(float x) {
    int i;
    float y, dy;
    float sign = 1;
    /* Tests are reversed to catch NaNs */
    if (!(x < 8)) {
      return 1;
    }
    if (!(x > -8)) {
      return -1;
    }
    if (x < 0) {
      x = -x;
      sign = -1;
    }
    i = (int) Math.floor(.5f + 25 * x);
    x -= .04f * i;
    y = OpusTables.tansig_table[i];
    dy = 1 - y * y;
    y = y + x * dy * (1 - y * x);
    return sign * y;
  }

  static void mlp_process(MLPState m, float[] input, float[] output) {
    int j;
    float[] hidden = new float[MAX_NEURONS];
    float[] W = m.weights;
    int W_ptr = 0;

    /* Copy to tmp_in */
    for (j = 0; j < m.topo[1]; j++) {
      int k;
      float sum = W[W_ptr];
      W_ptr++;
      for (k = 0; k < m.topo[0]; k++) {
        sum = sum + input[k] * W[W_ptr];
        W_ptr++;
      }
      hidden[j] = tansig_approx(sum);
    }

    for (j = 0; j < m.topo[2]; j++) {
      int k;
      float sum = W[W_ptr];
      W_ptr++;
      for (k = 0; k < m.topo[1]; k++) {
        sum = sum + hidden[k] * W[W_ptr];
        W_ptr++;
      }
      output[j] = tansig_approx(sum);
    }
  }
}
