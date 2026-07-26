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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.rslly.iot.models.KnowledgeChatEntity;
import top.rslly.iot.services.agent.KnowledgeChatServiceImpl;
import top.rslly.iot.services.agent.ProductToolsBanServiceImpl;
import top.rslly.iot.utility.ai.GlobalMessageContext;

@ExtendWith(MockitoExtension.class)
class KnowledgeToolTest {
  @Mock
  private KnowledgeChatServiceImpl knowledgeChatService;
  @Mock
  private ProductToolsBanServiceImpl productToolsBanService;

  @InjectMocks
  private KnowledgeTool knowledgeTool;

  @Test
  void runsSemanticSearchWithCurrentProductAndNormalizedJsonQuery() {
    when(productToolsBanService.getProductToolsBanList(12)).thenReturn(List.of());
    when(knowledgeChatService.hasSearchableKnowledge(12)).thenReturn(true);
    when(knowledgeChatService.searchByProductId("12", "红灯闪烁"))
        .thenReturn("请检查设备网络连接");

    String result = knowledgeTool.run("{\"query\":\"红灯闪烁\"}",
        Map.of(GlobalMessageContext.PRODUCT_ID, 12));

    assertEquals("请检查设备网络连接", result);
    verify(knowledgeChatService).searchByProductId("12", "红灯闪烁");
  }

  @Test
  void rejectsDisabledKnowledgeWithoutCallingKnowledgeService() {
    when(productToolsBanService.getProductToolsBanList(12)).thenReturn(List.of("knowledge"));

    String result = knowledgeTool.run("红灯闪烁",
        Map.of(GlobalMessageContext.PRODUCT_ID, 12));

    assertEquals(KnowledgeTool.DISABLED_MESSAGE, result);
    verifyNoInteractions(knowledgeChatService);
  }

  @Test
  void returnsExplicitMessagesForEmptyQueryAndNoMatch() {
    when(productToolsBanService.getProductToolsBanList(12)).thenReturn(List.of());
    when(knowledgeChatService.hasSearchableKnowledge(12)).thenReturn(true);
    assertEquals(KnowledgeTool.EMPTY_QUERY_MESSAGE,
        knowledgeTool.run("{\"args\":\"  \"}",
            Map.of(GlobalMessageContext.PRODUCT_ID, 12)));

    when(knowledgeChatService.searchByProductId("12", "unknown")).thenReturn("");
    assertEquals(KnowledgeTool.NO_RESULT_MESSAGE,
        knowledgeTool.run("unknown", Map.of(GlobalMessageContext.PRODUCT_ID, 12)));
  }

  @Test
  void distinguishesUnavailableKnowledgeAndQueryFailures() {
    when(productToolsBanService.getProductToolsBanList(12)).thenReturn(List.of());
    when(knowledgeChatService.hasSearchableKnowledge(12)).thenReturn(false);
    assertEquals(KnowledgeTool.UNAVAILABLE_MESSAGE,
        knowledgeTool.run("query", Map.of(GlobalMessageContext.PRODUCT_ID, 12)));

    when(knowledgeChatService.hasSearchableKnowledge(12))
        .thenThrow(new IllegalStateException("database unavailable"));
    assertEquals(KnowledgeTool.ERROR_MESSAGE,
        knowledgeTool.run("query", Map.of(GlobalMessageContext.PRODUCT_ID, 12)));
  }

  @Test
  void convertsVectorSearchFailureToExplicitToolError() {
    when(productToolsBanService.getProductToolsBanList(12)).thenReturn(List.of());
    when(knowledgeChatService.hasSearchableKnowledge(12)).thenReturn(true);
    when(knowledgeChatService.searchByProductId("12", "query"))
        .thenThrow(new IllegalStateException("chroma unavailable"));

    assertEquals(KnowledgeTool.ERROR_MESSAGE,
        knowledgeTool.run("query", Map.of(GlobalMessageContext.PRODUCT_ID, 12)));
  }

  @Test
  void buildsBoundedSanitizedDescriptionInRepositoryOrder() {
    List<KnowledgeChatEntity> entries = new ArrayList<>();
    entries.add(knowledge(1, "manual\nname", "first\r\nline"));
    for (int id = 2; id <= 20; id++) {
      entries.add(knowledge(id, "file-" + id, "x".repeat(800)));
    }
    when(productToolsBanService.getProductToolsBanList(12)).thenReturn(List.of());
    when(knowledgeChatService.findSearchableKnowledgeByProductId(12)).thenReturn(entries);

    String agentDescription = knowledgeTool.getDescription(12);
    String routingDescription = knowledgeTool.getRoutingDescription(12);

    assertTrue(agentDescription.contains("manual name: first line"));
    assertFalse(agentDescription.contains("manual\nname"));
    assertTrue(agentDescription.length() <= KnowledgeTool.AGENT_DESCRIPTION_MAX_CHARS);
    assertTrue(routingDescription.length() <= KnowledgeTool.ROUTER_DESCRIPTION_MAX_CHARS);
    assertTrue(agentDescription.indexOf("manual name") < agentDescription.indexOf("file-2"));
  }

  @Test
  void omitsDescriptionWhenNoSuccessfulKnowledgeExists() {
    when(productToolsBanService.getProductToolsBanList(12)).thenReturn(List.of());
    when(knowledgeChatService.findSearchableKnowledgeByProductId(12)).thenReturn(List.of());

    assertEquals("", knowledgeTool.getDescription(12));
  }

  private KnowledgeChatEntity knowledge(int id, String filename, String description) {
    KnowledgeChatEntity entity = new KnowledgeChatEntity();
    entity.setId(id);
    entity.setProductId(12);
    entity.setStatus("success");
    entity.setFilename(filename);
    entity.setDescription(description);
    return entity;
  }
}
