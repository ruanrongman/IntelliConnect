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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticApplicationContext;
import top.rslly.iot.services.knowledgeGraphic.KnowledgeGraphicAgentService;
import top.rslly.iot.utility.SpringBeanUtils;
import top.rslly.iot.utility.ai.Manage;
import top.rslly.iot.utility.result.ResultTool;

class KnowledgeGraphicAgentToolTest {
  private final KnowledgeGraphicAgentService service = mock(KnowledgeGraphicAgentService.class);
  private final KnowledgeGraphicAgentTool tool =
      new KnowledgeGraphicAgentTool(service, new ObjectMapper());
  private final Map<String, Object> context = Map.of("productId", 12, "chatId", "stream-id",
      "conversationChatId", "stable-conversation");

  @Test
  void describesDisabledStateWithoutHidingProtocolOrRoute() {
    when(service.isEnabled(12)).thenReturn(false);
    String description = tool.getDescription(12);
    assertTrue(description.contains("currently OFF"));
    assertTrue(description.contains("Do not prepare, list, read or transfer input files"));
    assertTrue(description.contains("\"action\":\"import\""));
    assertTrue(tool.getRoutingDescription(12).contains("even when the switch is off"));
    when(service.isEnabled(12)).thenReturn(true);
    assertTrue(tool.getDescription(12).contains("currently ON"));
    assertFalse(tool.getRoutingDescription(12).contains("currently OFF"));
  }

  @Test
  void returnsActionableDisabledErrorForEveryActionWithoutAccessingGraph() {
    for (String action : List.of("read", "query", "import", "progress", "clear", "export")) {
      var result = JSON.parseObject(tool.run("{\"action\":\"" + action + "\"}", context));
      assertFalse(result.getBooleanValue("success"));
      assertEquals(KnowledgeGraphicAgentTool.DISABLED_MESSAGE, result.getString("error"));
    }
    verify(service, times(6)).isEnabled(12);
    verifyNoMoreInteractions(service);
  }

  @Test
  void rejectsDirectEditingAndCallerSuppliedScopeOrGraphData() {
    when(service.isEnabled(12)).thenReturn(true);
    for (String action : List.of("addNode", "update", "deleteNode", "edit", "addRelation")) {
      assertFalse(JSON.parseObject(tool.run("{\"action\":\"" + action + "\"}", context))
          .getBooleanValue("success"));
    }
    for (String extra : List.of("\"productId\":13", "\"chatId\":\"other\"", "\"nodes\":[]",
        "\"content\":\"injected\"")) {
      assertFalse(JSON.parseObject(tool.run("{\"action\":\"clear\"," + extra + "}", context))
          .getBooleanValue("success"));
    }
    assertFalse(JSON.parseObject(tool.run(
        "{\"action\":\"import\",\"sources\":[{\"source\":\"workspace\",\"fileId\":\"id\",\"productId\":13}]}",
        context)).getBooleanValue("success"));
    verify(service, never()).clear(anyInt(), anyString());
    verify(service, never()).startImport(anyInt(), anyString(), anyList());
  }

  @Test
  void usesStableServerContextAndRechecksSwitchThroughRealManageDispatch() {
    when(service.isEnabled(12)).thenReturn(true, false);
    doReturn(ResultTool.success()).when(service).read(12, "stable-conversation");
    var previous = SpringBeanUtils.getApplicationContext();
    try (StaticApplicationContext spring = new StaticApplicationContext()) {
      spring.getBeanFactory().registerSingleton(tool.getName(), tool);
      SpringBeanUtils.setApplicationContext(spring);
      Manage manage = new Manage();
      String first = manage.runTool(tool.getName(), "{\"action\":\"read\"}", context);
      assertTrue(JSON.parseObject(first).getBooleanValue("success"));
      String disabled = manage.runTool(tool.getName(), "{\"action\":\"read\"}", context);
      assertTrue(disabled.contains("开关已关闭"));
      assertEquals(KnowledgeGraphicAgentTool.DISABLED_MESSAGE,
          JSON.parseObject(disabled).getString("error"));
      verify(service, times(1)).read(12, "stable-conversation");
    } finally {
      SpringBeanUtils.setApplicationContext(previous);
    }
  }

  @Test
  void forwardsOnlySupportedImportAndExportArguments() {
    when(service.isEnabled(12)).thenReturn(true);
    when(service.startImport(anyInt(), anyString(), anyList()))
        .thenReturn(Map.of("taskId", "task"));
    tool.run("""
        {"action":"import","sources":[{"source":"workspace","fileId":"real-file"},
        {"source":"sandbox","path":"/workspace/input.pdf"}]}
        """, context);
    verify(service).startImport(12, "stable-conversation", List.of(
        new KnowledgeGraphicAgentService.Source("workspace", "real-file", null),
        new KnowledgeGraphicAgentService.Source("sandbox", null, "/workspace/input.pdf")));
    tool.run(
        "{\"action\":\"export\",\"format\":\"mermaid\",\"target\":\"sandbox\",\"path\":\"exports/graph.mmd\"}",
        context);
    verify(service).export(12, "stable-conversation", "mermaid", "sandbox", null,
        "exports/graph.mmd");
  }

  @Test
  void rejectsMalformedArgumentsAndMissingContext() {
    assertFalse(JSON.parseObject(tool.run("{}", Map.of())).getBooleanValue("success"));
    when(service.isEnabled(12)).thenReturn(true);
    for (String args : List.of("[]", "not json", "{}", "{\"action\":\"query\",\"query\":12}",
        "{\"action\":\"import\",\"sources\":[]}")) {
      assertFalse(JSON.parseObject(tool.run(args, context)).getBooleanValue("success"));
    }
  }
}
