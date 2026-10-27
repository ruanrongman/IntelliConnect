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
package top.rslly.iot.utility.ai.tools;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import top.rslly.iot.utility.ai.sandbox.SandboxService;

/** Shared encoding for file tools executing Python in the remote sandbox. */
final class SandboxFileSupport {
  private SandboxFileSupport() {}

  static String resolvePath(String path) {
    if (path == null || path.isBlank()) {
      return null;
    }
    return path.startsWith("/") ? path : SandboxService.WORKSPACE_DIR + "/" + path;
  }

  static String quote(String value) {
    return "'" + value.replace("'", "'\"'\"'") + "'";
  }

  static String pythonCommand(String script, String json) {
    String encoded = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    return "python3 -c " + quote(script) + " " + quote(encoded);
  }
}
