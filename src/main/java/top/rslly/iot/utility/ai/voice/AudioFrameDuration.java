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
package top.rslly.iot.utility.ai.voice;

import top.rslly.iot.utility.smartVoice.XiaoZhiWebsocket;

import java.util.Set;

public class AudioFrameDuration {
  public static final int DEFAULT_FRAME_DURATION_MS = 60;
  public static final int DEFAULT_SAMPLE_RATE = 16000;
  public static final int VAD_SAMPLE_RATE = 16000;
  public static final int SAMPLE_RATE = DEFAULT_SAMPLE_RATE;
  private static final Set<Integer> SUPPORTED_FRAME_DURATIONS = Set.of(20, 40, 60);
  private static final Set<Integer> SUPPORTED_SAMPLE_RATES = Set.of(16000, 24000, 48000);

  public static int resolveFrameDurationMs() {
    return DEFAULT_FRAME_DURATION_MS;
  }

  public static int resolveFrameDurationMs(String chatId) {
    return resolveOutboundFrameDurationMs(chatId);
  }

  public static int resolveOutboundFrameDurationMs(String chatId) {
    Integer frameDurationMs = XiaoZhiWebsocket.getOutboundFrameDurationMs(chatId);
    return frameDurationMs == null ? resolveFrameDurationMs() : normalize(frameDurationMs);
  }

  public static int resolveInboundFrameDurationMs(String chatId) {
    Integer frameDurationMs = XiaoZhiWebsocket.getInboundFrameDurationMs(chatId);
    return frameDurationMs == null ? DEFAULT_FRAME_DURATION_MS : normalize(frameDurationMs);
  }

  public static int resolveFrameSizeSamples() {
    return resolveSampleRate() * resolveFrameDurationMs() / 1000;
  }

  public static int resolveFrameSizeSamples(String chatId) {
    return resolveOutboundSampleRate(chatId) * resolveOutboundFrameDurationMs(chatId) / 1000;
  }

  public static int resolveVadFrameSizeSamples(String chatId) {
    return VAD_SAMPLE_RATE * resolveInboundFrameDurationMs(chatId) / 1000;
  }

  public static int resolveInboundFrameSizeSamples(String chatId) {
    return resolveInboundSampleRate(chatId) * resolveInboundFrameDurationMs(chatId) / 1000;
  }

  public static int resolveSampleRate() {
    return DEFAULT_SAMPLE_RATE;
  }

  public static int resolveSampleRate(String chatId) {
    return resolveOutboundSampleRate(chatId);
  }

  public static int resolveOutboundSampleRate(String chatId) {
    Integer sampleRate = XiaoZhiWebsocket.getOutboundSampleRate(chatId);
    return sampleRate == null ? resolveSampleRate() : normalizeSampleRate(sampleRate);
  }

  public static int resolveInboundSampleRate(String chatId) {
    Integer sampleRate = XiaoZhiWebsocket.getInboundSampleRate(chatId);
    return sampleRate == null ? DEFAULT_SAMPLE_RATE : normalizeSampleRate(sampleRate);
  }

  public static boolean isSupported(int frameDurationMs) {
    return SUPPORTED_FRAME_DURATIONS.contains(frameDurationMs);
  }

  public static boolean isSupportedSampleRate(int sampleRate) {
    return SUPPORTED_SAMPLE_RATES.contains(sampleRate);
  }

  public static boolean isSupportedAudioParams(int sampleRate, int frameDurationMs) {
    return isSupportedSampleRate(sampleRate) && isSupported(frameDurationMs);
  }

  public static int parseFrameDurationOrDefault(String value, int defaultValue) {
    try {
      return normalize(Integer.parseInt(value));
    } catch (Exception e) {
      return normalize(defaultValue);
    }
  }

  public static int parseSampleRateOrDefault(String value, int defaultValue) {
    try {
      return normalizeSampleRate(Integer.parseInt(value));
    } catch (Exception e) {
      return normalizeSampleRate(defaultValue);
    }
  }

  public static int normalize(int frameDurationMs) {
    return isSupported(frameDurationMs) ? frameDurationMs : DEFAULT_FRAME_DURATION_MS;
  }

  public static int normalizeSampleRate(int sampleRate) {
    return isSupportedSampleRate(sampleRate) ? sampleRate : DEFAULT_SAMPLE_RATE;
  }
}
