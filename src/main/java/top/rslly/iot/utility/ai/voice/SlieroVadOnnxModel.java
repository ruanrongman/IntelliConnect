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

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SlieroVadOnnxModel {
  private static final Object SESSION_INIT_LOCK = new Object();
  private static final Object SESSION_RUN_LOCK = new Object();
  private static volatile OrtSession sharedSession;
  private float[][][] state;
  private float[][] context;
  private volatile boolean closed;
  // Define the last sample rate
  private int lastSr = 0;
  // Define the last batch size
  private int lastBatchSize = 0;
  // Define a list of supported sample rates
  private static final List<Integer> SAMPLE_RATES = Arrays.asList(8000, 16000);

  // Constructor
  public SlieroVadOnnxModel(String modelPath) throws OrtException {
    initializeSharedSession(modelPath);
    resetStates();
    closed = false;
  }

  private static void initializeSharedSession(String modelPath) throws OrtException {
    if (sharedSession != null) {
      return;
    }
    synchronized (SESSION_INIT_LOCK) {
      if (sharedSession != null) {
        return;
      }
      OrtEnvironment env = OrtEnvironment.getEnvironment();
      try (OrtSession.SessionOptions opts = new OrtSession.SessionOptions()) {
        opts.setInterOpNumThreads(1);
        opts.setIntraOpNumThreads(1);
        opts.addCPU(true);
        sharedSession = env.createSession(modelPath, opts);
      }
    }
  }

  /**
   * Reset states
   */
  void resetStates() {
    state = new float[2][1][128];
    context = new float[0][];
    lastSr = 0;
    lastBatchSize = 0;
  }

  public void close() throws OrtException {
    closed = true;
    resetStates();
  }

  /**
   * Define inner class ValidationResult
   */
  public static class ValidationResult {
    public final float[][] x;
    public final int sr;

    // Constructor
    public ValidationResult(float[][] x, int sr) {
      this.x = x;
      this.sr = sr;
    }
  }

  /**
   * Function to validate input data
   */
  private ValidationResult validateInput(float[][] x, int sr) {
    // Process the input data with dimension 1
    if (x.length == 1) {
      x = new float[][] {x[0]};
    }
    // Throw an exception when the input data dimension is greater than 2
    if (x.length > 2) {
      throw new IllegalArgumentException("Incorrect audio data dimension: " + x[0].length);
    }

    // Process the input data when the sample rate is not equal to 16000 and is a multiple of 16000
    if (sr != 16000 && (sr % 16000 == 0)) {
      int step = sr / 16000;
      float[][] reducedX = new float[x.length][];

      for (int i = 0; i < x.length; i++) {
        float[] current = x[i];
        float[] newArr = new float[(current.length + step - 1) / step];

        for (int j = 0, index = 0; j < current.length; j += step, index++) {
          newArr[index] = current[j];
        }

        reducedX[i] = newArr;
      }

      x = reducedX;
      sr = 16000;
    }

    // If the sample rate is not in the list of supported sample rates, throw an exception
    if (!SAMPLE_RATES.contains(sr)) {
      throw new IllegalArgumentException(
          "Only supports sample rates " + SAMPLE_RATES + " (or multiples of 16000)");
    }

    // If the input audio block is too short, throw an exception
    if (((float) sr) / x[0].length > 31.25) {
      throw new IllegalArgumentException("Input audio is too short");
    }

    // Return the validated result
    return new ValidationResult(x, sr);
  }

  private static float[][] concatenate(float[][] a, float[][] b) {
    if (a.length != b.length) {
      throw new IllegalArgumentException("The number of rows in both arrays must be the same.");
    }

    int rows = a.length;
    int colsA = a[0].length;
    int colsB = b[0].length;
    float[][] result = new float[rows][colsA + colsB];

    for (int i = 0; i < rows; i++) {
      System.arraycopy(a[i], 0, result[i], 0, colsA);
      System.arraycopy(b[i], 0, result[i], colsA, colsB);
    }

    return result;
  }

  private static float[][] getLastColumns(float[][] array, int contextSize) {
    int rows = array.length;
    int cols = array[0].length;

    if (contextSize > cols) {
      throw new IllegalArgumentException(
          "contextSize cannot be greater than the number of columns in the array.");
    }

    float[][] result = new float[rows][contextSize];

    for (int i = 0; i < rows; i++) {
      System.arraycopy(array[i], cols - contextSize, result[i], 0, contextSize);
    }

    return result;
  }

  /**
   * Method to call the ONNX model
   */
  public float[] call(float[][] x, int sr) throws OrtException {
    if (closed) {
      throw new IllegalStateException("VAD model instance already closed");
    }
    ValidationResult result = validateInput(x, sr);
    x = result.x;
    sr = result.sr;
    int numberSamples = 256;
    if (sr == 16000) {
      numberSamples = 512;
    }

    if (x[0].length != numberSamples) {
      throw new IllegalArgumentException("Provided number of samples is " + x[0].length
          + " (Supported values: 256 for 8000 sample rate, 512 for 16000)");
    }

    int batchSize = x.length;

    int contextSize = 32;
    if (sr == 16000) {
      contextSize = 64;
    }

    if (lastBatchSize == 0) {
      resetStates();
    }
    if (lastSr != 0 && lastSr != sr) {
      resetStates();
    }
    if (lastBatchSize != 0 && lastBatchSize != batchSize) {
      resetStates();
    }

    if (context.length == 0) {
      context = new float[batchSize][contextSize];
    }

    x = concatenate(context, x);

    OrtEnvironment env = OrtEnvironment.getEnvironment();

    OnnxTensor inputTensor = null;
    OnnxTensor stateTensor = null;
    OnnxTensor srTensor = null;
    OrtSession.Result ortOutputs = null;

    try {
      // Create input tensors
      inputTensor = OnnxTensor.createTensor(env, x);
      stateTensor = OnnxTensor.createTensor(env, state);
      srTensor = OnnxTensor.createTensor(env, new long[] {sr});

      Map<String, OnnxTensor> inputs = new HashMap<>();
      inputs.put("input", inputTensor);
      inputs.put("sr", srTensor);
      inputs.put("state", stateTensor);

      // Call the ONNX model for calculation
      synchronized (SESSION_RUN_LOCK) {
        if (sharedSession == null) {
          throw new IllegalStateException("VAD shared session is not initialized");
        }
        ortOutputs = sharedSession.run(inputs);
      }
      // Get the output results
      float[][] output = (float[][]) ortOutputs.get(0).getValue();
      state = (float[][][]) ortOutputs.get(1).getValue();

      context = getLastColumns(x, contextSize);
      lastSr = sr;
      lastBatchSize = batchSize;
      return output[0];
    } finally {
      if (inputTensor != null) {
        inputTensor.close();
      }
      if (stateTensor != null) {
        stateTensor.close();
      }
      if (srTensor != null) {
        srTensor.close();
      }
      if (ortOutputs != null) {
        ortOutputs.close();
      }
    }
  }

  public static void closeSharedSession() throws OrtException {
    synchronized (SESSION_INIT_LOCK) {
      if (sharedSession != null) {
        sharedSession.close();
        sharedSession = null;
      }
    }
  }
}
