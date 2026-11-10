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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import top.rslly.iot.services.AdminConfigServiceImpl;
import top.rslly.iot.services.McpEndpointConfigService;
import top.rslly.iot.services.agent.*;
import top.rslly.iot.utility.ai.DescriptionUtil;
import top.rslly.iot.utility.ai.mcp.McpWebsocket;
import top.rslly.iot.utility.ai.sandbox.SandboxService;
import top.rslly.iot.utility.ai.tools.KnowledgeGraphicAgentTool;
import top.rslly.iot.utility.ai.tools.KnowledgeTool;

class ClassifierGraphRouteTest {
  @Test
  void keepsGraphRouteAndExplainsDisabledSwitch() {
    ClassifierToolPrompt prompt = new ClassifierToolPrompt();
    KnowledgeGraphicAgentTool graph = mock(KnowledgeGraphicAgentTool.class);
    KnowledgeTool knowledge = mock(KnowledgeTool.class);
    ReflectionTestUtils.setField(prompt, "knowledgeGraphicAgentTool", graph);
    ReflectionTestUtils.setField(prompt, "knowledgeTool", knowledge);
    ReflectionTestUtils.setField(prompt, "sandboxService", mock(SandboxService.class));
    ReflectionTestUtils.setField(prompt, "productToolsBanService",
        mock(ProductToolsBanServiceImpl.class));
    ReflectionTestUtils.setField(prompt, "mcpServerService", mock(McpServerServiceImpl.class));
    ReflectionTestUtils.setField(prompt, "productSkillsService",
        mock(ProductSkillsServiceImpl.class));
    ReflectionTestUtils.setField(prompt, "mcpWebsocket", mock(McpWebsocket.class));
    ReflectionTestUtils.setField(prompt, "mcpEndpointConfigService",
        mock(McpEndpointConfigService.class));
    ReflectionTestUtils.setField(prompt, "descriptionUtil", mock(DescriptionUtil.class));
    ReflectionTestUtils.setField(prompt, "productRouterSetService",
        mock(ProductRouterSetServiceImpl.class));
    ReflectionTestUtils.setField(prompt, "adminConfigService", mock(AdminConfigServiceImpl.class));
    when(knowledge.getRoutingDescription(12)).thenReturn("");
    when(graph.getRoutingDescription(12)).thenReturn("knowledgeGraphicAgentTool switch ON");
    assertTrue(
        prompt.getClassifierTool(12, "chatProduct12", "").contains("knowledgeGraphicAgentTool"));
    when(graph.getRoutingDescription(12)).thenReturn("knowledgeGraphicAgentTool switch OFF");
    String disabled = prompt.getClassifierTool(12, "chatProduct12", "");
    assertTrue(disabled.contains("knowledgeGraphicAgentTool"));
    assertTrue(disabled.contains("switch OFF"));
  }
}
