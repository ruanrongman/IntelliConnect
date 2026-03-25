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

import ai.onnxruntime.OrtException;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Allen Hu
 * @date 2025/2/22
 */
@Slf4j
public class SlieroVadListener {
  private static final Object MODEL_FILE_LOCK = new Object();
  private static volatile String cachedModelPath;

  public static final String MODEL_PATH = "/vad/silero_vad.onnx";
  public static final int SAMPLE_RATE = 16000;
  public static final float START_THRESHOLD = 0.6f;
  public static final float END_THRESHOLD = 0.45f;
  public static final int MIN_SILENCE_DURATION_MS = 700;
  public static final int SPEECH_PAD_MS = 60;

  private final int sampleRate;
  private final float startThreshold;
  private final float endThreshold;
  private final int minSilenceDurationMs;
  private final int speechPadMs;

  private SlieroVadDetector vadDetector;
  private final AtomicBoolean stop = new AtomicBoolean(false);
  private final ReentrantReadWriteLock lifecycleLock = new ReentrantReadWriteLock();

  public SlieroVadListener() {
    this.sampleRate = SAMPLE_RATE;
    this.startThreshold = START_THRESHOLD;
    this.endThreshold = END_THRESHOLD;
    this.minSilenceDurationMs = MIN_SILENCE_DURATION_MS;
    this.speechPadMs = SPEECH_PAD_MS;
  }

  public void init() {
    lifecycleLock.writeLock().lock();
    try {
      String modelPath = loadModelFromResources();
      this.vadDetector = new SlieroVadDetector(modelPath, startThreshold, endThreshold, sampleRate,
          minSilenceDurationMs, speechPadMs);
      stop.set(false);
    } catch (OrtException | IOException e) {
      log.error("Error initializing the VAD detector: ", e);
      this.vadDetector = null;
      stop.set(true);
    } finally {
      lifecycleLock.writeLock().unlock();
    }
  }

  private String loadModelFromResources() throws IOException {
    if (cachedModelPath != null) {
      return cachedModelPath;
    }
    synchronized (MODEL_FILE_LOCK) {
      if (cachedModelPath != null) {
        return cachedModelPath;
      }
      try (InputStream is = getClass().getResourceAsStream(MODEL_PATH)) {
        if (is == null) {
          throw new IOException("VAD model not found in resources: " + MODEL_PATH);
        }

        // 创建临时文件
        File tempFile = File.createTempFile("silero_vad", ".onnx");
        tempFile.deleteOnExit();

        // 复制资源内容到临时文件
        Files.copy(is, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        cachedModelPath = tempFile.getAbsolutePath();
        return cachedModelPath;
      }
    }
  }

  public void destroy() {
    lifecycleLock.writeLock().lock();
    try {
      if (stop.compareAndSet(false, true) && vadDetector != null) {
        this.vadDetector.close();
        this.vadDetector = null;
      }
    } catch (OrtException e) {
      log.error("Error closing the VAD detector: ", e);
    } finally {
      lifecycleLock.writeLock().unlock();
    }
  }

  public Map<String, Double> listen(byte[] data) {
    lifecycleLock.readLock().lock();
    try {
      if (stop.get() || vadDetector == null) {
        return null;
      }
      return vadDetector.apply(data, true);
    } catch (IllegalArgumentException ignored) {
    } catch (Exception e) {
      log.error("Error applying VAD detector: ", e);
    } finally {
      lifecycleLock.readLock().unlock();
    }
    return null;
  }

  public boolean isStop() {
    return stop.get();
  }

  public boolean isReady() {
    return !stop.get() && vadDetector != null;
  }

  public static void destroySharedResources() {
    try {
      SlieroVadOnnxModel.closeSharedSession();
    } catch (OrtException e) {
      log.error("Error closing shared VAD model session", e);
    }
  }
}
