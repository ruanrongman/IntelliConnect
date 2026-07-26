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
package top.rslly.iot.utility.ai.prompts;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import top.rslly.iot.services.AdminConfigServiceImpl;
import top.rslly.iot.services.McpEndpointConfigService;
import top.rslly.iot.services.agent.McpServerServiceImpl;
import top.rslly.iot.services.agent.ProductRouterSetServiceImpl;
import top.rslly.iot.services.agent.ProductSkillsServiceImpl;
import top.rslly.iot.services.agent.ProductToolsBanServiceImpl;
import top.rslly.iot.utility.ai.DescriptionUtil;
import top.rslly.iot.utility.ai.mcp.McpWebsocket;
import top.rslly.iot.utility.ai.tools.KnowledgeTool;

class ClassifierKnowledgeRouteTest {
  private ClassifierToolPrompt prompt;
  private KnowledgeTool knowledgeTool;
  private ProductToolsBanServiceImpl productToolsBanService;

  @BeforeEach
  void setUp() {
    prompt = new ClassifierToolPrompt();
    knowledgeTool = Mockito.mock(KnowledgeTool.class);
    productToolsBanService = Mockito.mock(ProductToolsBanServiceImpl.class);
    DescriptionUtil descriptionUtil = Mockito.mock(DescriptionUtil.class);
    ReflectionTestUtils.setField(prompt, "knowledgeTool", knowledgeTool);
    ReflectionTestUtils.setField(prompt, "productToolsBanService", productToolsBanService);
    ReflectionTestUtils.setField(prompt, "descriptionUtil", descriptionUtil);
    ReflectionTestUtils.setField(prompt, "mcpServerService",
        Mockito.mock(McpServerServiceImpl.class));
    ReflectionTestUtils.setField(prompt, "productSkillsService",
        Mockito.mock(ProductSkillsServiceImpl.class));
    ReflectionTestUtils.setField(prompt, "productRouterSetService",
        Mockito.mock(ProductRouterSetServiceImpl.class));
    ReflectionTestUtils.setField(prompt, "adminConfigService",
        Mockito.mock(AdminConfigServiceImpl.class));
    ReflectionTestUtils.setField(prompt, "mcpWebsocket", Mockito.mock(McpWebsocket.class));
    ReflectionTestUtils.setField(prompt, "mcpEndpointConfigService",
        Mockito.mock(McpEndpointConfigService.class));
    ReflectionTestUtils.setField(prompt, "includeThought", false);
    when(descriptionUtil.getAgentLongMemory(12)).thenReturn("");
  }

  @Test
  void addsKnowledgeCoverageToLegacyAgentTask() {
    when(productToolsBanService.getProductToolsBanList(12)).thenReturn(List.of());
    when(knowledgeTool.getRoutingDescription(12)).thenReturn("knowledge coverage");

    String result = prompt.getClassifierTool(12, "chatProduct12", "");

    assertTrue(result.contains("Complex multi-step task | knowledge coverage"));
  }
}
