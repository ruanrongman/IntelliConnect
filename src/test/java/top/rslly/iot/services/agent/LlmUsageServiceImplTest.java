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
package top.rslly.iot.services.agent;

import org.influxdb.dto.Point;
import org.influxdb.dto.QueryResult;
import org.junit.jupiter.api.Test;
import top.rslly.iot.utility.ai.llm.usage.LlmCallRecord;
import top.rslly.iot.utility.ai.llm.usage.LlmUsage;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LlmUsageServiceImplTest {

  @Test
  void writesTagsAndIntegerFieldsToInflux() {
    LlmCallRecord record = new LlmCallRecord(1_700_000_000_000L, "dashscope.aliyuncs.com",
        "qwen-plus", "ChatTool.run", 3, "chatProduct3", new LlmUsage(2000, 1024, 0, 80, 5), 1900,
        1234, true);

    String line = LlmUsageServiceImpl.toPoint(record).lineProtocol();

    assertTrue(line.startsWith("llm_usage,model=qwen-plus,product_id=3,scene=ChatTool.run "));
    assertTrue(line.contains("prompt_tokens=2000i"));
    assertTrue(line.contains("cached_tokens=1024i"));
    assertTrue(line.contains("usage_reported=true"));
    assertTrue(line.contains("chat_id=\"chatProduct3\""));
  }

  @Test
  void writesMissingUsageWithoutNulls() {
    LlmCallRecord record = new LlmCallRecord(1L, null, null, null, null, null, null, 0, 0, true);

    Point point = LlmUsageServiceImpl.toPoint(record);

    assertTrue(point.lineProtocol().contains("usage_reported=false"));
    assertTrue(point.lineProtocol().contains("product_id=-1"));
  }

  @Test
  void parsesInfluxRowsIncludingFloatNumbers() {
    QueryResult.Series series = new QueryResult.Series();
    series.setName("llm_usage");
    series.setColumns(List.of("time", "cache_creation_tokens", "cached_tokens", "chat_id",
        "completion_tokens", "estimated_prompt_tokens", "latency_ms", "model", "product_id",
        "prompt_tokens", "provider", "reasoning_tokens", "scene", "stream", "usage_reported"));
    series.setValues(List.of(
        Arrays.asList("2026-11-10T00:00:00Z", 0.0, 1024.0, "c1", 80.0, 1900.0, 1234.0,
            "qwen-plus", "3", 2000.0, "host", 5.0, "ChatTool.run", true, true),
        Arrays.asList("2026-11-10T00:00:01Z", 0.0, 0.0, "c1", 0.0, 10.0, 99.0, "qwen-plus",
            "3", 0.0, "host", 0.0, "Agent.run", true, false)));
    QueryResult.Result item = new QueryResult.Result();
    item.setSeries(List.of(series));
    QueryResult result = new QueryResult();
    result.setResults(List.of(item));

    List<LlmCallRecord> records = LlmUsageServiceImpl.parseInflux(result, 3);

    assertEquals(2, records.size());
    assertEquals(new LlmUsage(2000, 1024, 0, 80, 5), records.get(0).usage());
    assertEquals("ChatTool.run", records.get(0).scene());
    assertEquals(1234, records.get(0).latencyMs());
    assertNull(records.get(1).usage());
  }
}
