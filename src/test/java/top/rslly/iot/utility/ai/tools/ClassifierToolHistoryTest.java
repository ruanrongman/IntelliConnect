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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alibaba.fastjson.JSON;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import top.rslly.iot.utility.ai.LlmDiyUtility;
import top.rslly.iot.utility.ai.ModelMessage;
import top.rslly.iot.utility.ai.llm.LLM;
import top.rslly.iot.utility.ai.prompts.ClassifierToolPrompt;

class ClassifierToolHistoryTest {
  @Test
  void retainsFileIdentityAndTrailingOfferWithinBoundedHistorySentToRouter() {
    ClassifierTool tool = new ClassifierTool();
    ClassifierToolPrompt prompt = mock(ClassifierToolPrompt.class);
    LlmDiyUtility llmDiyUtility = mock(LlmDiyUtility.class);
    LLM llm = mock(LLM.class);
    ReflectionTestUtils.setField(tool, "classifierToolPrompt", prompt);
    ReflectionTestUtils.setField(tool, "llmDiyUtility", llmDiyUtility);
    ReflectionTestUtils.setField(tool, "llmName", "test");
    ReflectionTestUtils.setField(tool, "speedUp", false);
    when(llmDiyUtility.getDiyLlm(12, "test", "classifier", true)).thenReturn(llm);
    when(prompt.getClassifierTool(eq(12), eq("stream-2"), anyString(), anyBoolean()))
        .thenReturn("router system");
    when(llm.jsonChat(eq("好的"), org.mockito.ArgumentMatchers.anyList(), eq(true)))
        .thenReturn(JSON.parseObject("{\"action\":{\"value\":\"4\",\"args\":\"读取 part1.txt\"}}"));
    List<ModelMessage> memory = new ArrayList<>();
    memory.add(new ModelMessage("user", "old unrelated topic"));
    memory.add(new ModelMessage("assistant", "old unrelated answer"));
    for (int i = 0; i < 4; i++) {
      memory.add(new ModelMessage(i % 2 == 0 ? "user" : "assistant", "旧记录".repeat(5000)));
    }
    memory.add(new ModelMessage("user", "列出当前会话工作区文件"));
    String offer = "需要我打开 part1.txt 看看里面写了什么吗？";
    memory.add(new ModelMessage("assistant", "part1.txt; fileId=file-1; "
        + "长文件列表".repeat(5000) + offer));

    Map<String, Object> result = tool.run("好的", Map.of("productId", 12,
        "chatId", "stream-2", "memory", memory));

    ArgumentCaptor<String> recentConversation = ArgumentCaptor.forClass(String.class);
    verify(prompt).getClassifierTool(eq(12), eq("stream-2"), recentConversation.capture(),
        eq(false));
    String history = recentConversation.getValue();
    assertTrue(history.contains("part1.txt; fileId=file-1;"));
    assertTrue(history.endsWith(offer + "\n"));
    assertTrue(history.contains("- assistant:"));
    assertTrue(history.length() < 19000);
    assertFalse(history.contains("old unrelated"));
    assertEquals(List.of("4"), result.get("value"));
    assertEquals(8, memory.size());
  }
}
