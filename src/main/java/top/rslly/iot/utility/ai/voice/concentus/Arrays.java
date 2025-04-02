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

class Arrays {

  static int[][] InitTwoDimensionalArrayInt(int x, int y) {
    return new int[x][y];
  }

  static float[][] InitTwoDimensionalArrayFloat(int x, int y) {
    return new float[x][y];
  }

  static short[][] InitTwoDimensionalArrayShort(int x, int y) {
    return new short[x][y];
  }

  static byte[][] InitTwoDimensionalArrayByte(int x, int y) {
    return new byte[x][y];
  }

  static byte[][][] InitThreeDimensionalArrayByte(int x, int y, int z) {
    return new byte[x][y][z];
  }

  static void MemSet(byte[] array, byte value) {
    java.util.Arrays.fill(array, value);
  }

  static void MemSet(short[] array, short value) {
    java.util.Arrays.fill(array, value);
  }

  static void MemSet(int[] array, int value) {
    java.util.Arrays.fill(array, value);
  }

  static void MemSet(float[] array, float value) {
    java.util.Arrays.fill(array, value);
  }

  static void MemSet(byte[] array, byte value, int length) {
    java.util.Arrays.fill(array, 0, length, value);
  }

  static void MemSet(short[] array, short value, int length) {
    java.util.Arrays.fill(array, 0, length, value);
  }

  static void MemSet(int[] array, int value, int length) {
    java.util.Arrays.fill(array, 0, length, value);
  }

  static void MemSet(float[] array, float value, int length) {
    java.util.Arrays.fill(array, 0, length, value);
  }

  static void MemSetWithOffset(byte[] array, byte value, int offset, int length) {
    java.util.Arrays.fill(array, offset, offset + length, value);
  }

  static void MemSetWithOffset(short[] array, short value, int offset, int length) {
    java.util.Arrays.fill(array, offset, offset + length, value);
  }

  static void MemSetWithOffset(int[] array, int value, int offset, int length) {
    java.util.Arrays.fill(array, offset, offset + length, value);
  }

  static void MemMove(byte[] array, int src_idx, int dst_idx, int length) {
    System.arraycopy(array, src_idx, array, dst_idx, length);
  }

  static void MemMove(short[] array, int src_idx, int dst_idx, int length) {
    System.arraycopy(array, src_idx, array, dst_idx, length);
  }

  static void MemMove(int[] array, int src_idx, int dst_idx, int length) {
    System.arraycopy(array, src_idx, array, dst_idx, length);
  }

}
