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

class Downmix {

  /// <summary>
  ///
  /// </summary>
  /// <typeparam name="T">The type of signal being handled (either short or float)</typeparam>
  /// <param name="_x"></param>
  /// <param name="sub"></param>
  /// <param name="subframe"></param>
  /// <param name="offset"></param>
  /// <param name="c1"></param>
  /// <param name="c2"></param>
  /// <param name="C"></param>
  static void downmix_int(short[] x, int x_ptr, int[] sub, int sub_ptr, int subframe, int offset,
      int c1, int c2, int C) {
    int scale;
    int j;
    for (j = 0; j < subframe; j++) {
      sub[j + sub_ptr] = x[(j + offset) * C + c1];
    }
    if (c2 > -1) {
      for (j = 0; j < subframe; j++) {
        sub[j + sub_ptr] += x[(j + offset) * C + c2];
      }
    } else if (c2 == -2) {
      int c;
      for (c = 1; c < C; c++) {
        for (j = 0; j < subframe; j++) {
          sub[j + sub_ptr] += x[(j + offset) * C + c];
        }
      }
    }
    scale = (1 << CeltConstants.SIG_SHIFT);
    if (C == -2) {
      scale /= C;
    } else {
      scale /= 2;
    }
    for (j = 0; j < subframe; j++) {
      sub[j + sub_ptr] *= scale;
    }
  }
}
