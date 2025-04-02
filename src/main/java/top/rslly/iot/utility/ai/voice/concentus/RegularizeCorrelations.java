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

class RegularizeCorrelations {

  /* Add noise to matrix diagonal */
  static void silk_regularize_correlations(
      int[] XX, /* I/O Correlation matrices */
      int XX_ptr,
      int[] xx, /* I/O Correlation values */
      int xx_ptr,
      int noise, /* I Noise to add */
      int D /* I Dimension of XX */
  ) {
    int i;
    for (i = 0; i < D; i++) {
      Inlines.MatrixSet(XX, XX_ptr, i, i, D,
          Inlines.silk_ADD32(Inlines.MatrixGet(XX, XX_ptr, i, i, D), noise));
    }
    xx[xx_ptr] += noise;
  }
}
