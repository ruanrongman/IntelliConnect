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
/// Note that since most API-level errors are detected and thrown as
/// OpusExceptions, direct use of this class is not usually needed
/// </summary>
public class OpusError {

  /**
   * No error
   */
  public static int OPUS_OK = 0;

  /**
   * One or more invalid/out of range arguments
   */
  public static int OPUS_BAD_ARG = -1;

  /**
   * Not enough bytes allocated in the buffer
   */
  public static int OPUS_BUFFER_TOO_SMALL = -2;

  /**
   * An public error was detected
   */
  public static int OPUS_INTERNAL_ERROR = -3;

  /**
   * The compressed data passed is corrupted
   */
  public static int OPUS_INVALID_PACKET = -4;

  /**
   * Invalid/unsupported request number
   */
  public static int OPUS_UNIMPLEMENTED = -5;

  /**
   * An encoder or decoder structure is invalid or already freed
   */
  public static int OPUS_INVALID_STATE = -6;

  /**
   * Memory allocation has failed
   */
  public static int OPUS_ALLOC_FAIL = -7;
}
