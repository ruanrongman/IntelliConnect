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
package top.rslly.iot.utility;

public class KnowledgeGraphicTextLimit {
  public static final int NODE_NAME_MAX_LENGTH = 20;
  public static final int TEXT_MAX_LENGTH = 255;

  private KnowledgeGraphicTextLimit() {}

  public static String normalizeNodeName(String value) {
    return normalize(value, NODE_NAME_MAX_LENGTH);
  }

  public static String normalizeText(String value) {
    return normalize(value, TEXT_MAX_LENGTH);
  }

  private static String normalize(String value, int maxLength) {
    if (value == null) {
      return null;
    }
    String normalized = value.trim();
    if (normalized.length() > maxLength) {
      return normalized.substring(0, maxLength);
    }
    return normalized;
  }
}
