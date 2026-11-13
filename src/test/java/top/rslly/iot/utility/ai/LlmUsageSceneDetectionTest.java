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
package top.rslly.iot.utility.ai;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import top.rslly.iot.utility.ai.llm.usage.LlmCallRecord;
import top.rslly.iot.utility.ai.llm.usage.LlmUsage;
import top.rslly.iot.utility.ai.llm.usage.LlmUsageTracker;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LlmUsageSceneDetectionTest {

  @AfterEach
  void tearDown() {
    LlmUsageTracker.setSink(null);
  }

  @Test
  void usesFirstApplicationFrameOutsideLlmPackageAsScene() {
    List<LlmCallRecord> records = new ArrayList<>();
    LlmUsageTracker.configure(true, true, 0.2D, 1.25D, 1024L);
    LlmUsageTracker.setSink(records::add);

    Runnable call = () -> LlmUsageTracker.begin("host", "qwen-plus", List.of(), false)
        .finish(new LlmUsage(10, 0, 0, 1, 0));
    call.run();

    assertEquals("LlmUsageSceneDetectionTest.usesFirstApplicationFrameOutsideLlmPackageAsScene",
        records.getFirst().scene());
  }
}
