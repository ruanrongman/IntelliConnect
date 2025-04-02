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

public class OpusException extends Exception {

  private String _message;
  private int _opus_error_code;

  public OpusException() {
    this("", 0);
  }

  public OpusException(String message) {
    this(message, 1);
  }

  public OpusException(String message, int opus_error_code) {
    _message = message + ": " + CodecHelpers.opus_strerror(opus_error_code);
    _opus_error_code = opus_error_code;
  }

  @Override
  public String getMessage() {
    return _message;
  }
}
