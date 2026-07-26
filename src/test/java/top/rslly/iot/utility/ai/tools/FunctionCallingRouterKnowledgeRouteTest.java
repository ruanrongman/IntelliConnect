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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import top.rslly.iot.services.McpEndpointConfigService;
import top.rslly.iot.services.agent.McpServerServiceImpl;
import top.rslly.iot.services.agent.ProductSkillsServiceImpl;
import top.rslly.iot.services.agent.ProductToolsBanServiceImpl;
import top.rslly.iot.utility.ai.llm.FunctionToolSpec;
import top.rslly.iot.utility.ai.mcp.McpWebsocket;

class FunctionCallingRouterKnowledgeRouteTest {
  private FunctionCallingRouterTool routerTool;
  private KnowledgeTool knowledgeTool;
  private ProductToolsBanServiceImpl productToolsBanService;

  @BeforeEach
  void setUp() {
    routerTool = new FunctionCallingRouterTool();
    knowledgeTool = Mockito.mock(KnowledgeTool.class);
    productToolsBanService = Mockito.mock(ProductToolsBanServiceImpl.class);
    ReflectionTestUtils.setField(routerTool, "knowledgeTool", knowledgeTool);
    ReflectionTestUtils.setField(routerTool, "productToolsBanService", productToolsBanService);
    ReflectionTestUtils.setField(routerTool, "mcpServerService",
        Mockito.mock(McpServerServiceImpl.class));
    ReflectionTestUtils.setField(routerTool, "productSkillsService",
        Mockito.mock(ProductSkillsServiceImpl.class));
    ReflectionTestUtils.setField(routerTool, "mcpWebsocket", Mockito.mock(McpWebsocket.class));
    ReflectionTestUtils.setField(routerTool, "mcpEndpointConfigService",
        Mockito.mock(McpEndpointConfigService.class));
  }

  @Test
  void addsKnowledgeCoverageToAgentRouteWhenAvailable() {
    when(productToolsBanService.getProductToolsBanList(12)).thenReturn(List.of());
    when(knowledgeTool.getRoutingDescription(12)).thenReturn("knowledge coverage");

    List<FunctionToolSpec> specs = ReflectionTestUtils.invokeMethod(routerTool,
        "buildToolSpecs", 12, "chatProduct12");

    FunctionToolSpec agentRoute = specs.stream()
        .filter(spec -> spec.name().equals("route_agent"))
        .findFirst().orElseThrow();
    assertTrue(agentRoute.description().contains("knowledge coverage"));
  }

  @Test
  void keepsAgentRouteFreeOfKnowledgeCoverageWhenUnavailable() {
    when(productToolsBanService.getProductToolsBanList(12)).thenReturn(List.of("knowledge"));
    when(knowledgeTool.getRoutingDescription(12)).thenReturn("");

    List<FunctionToolSpec> specs = ReflectionTestUtils.invokeMethod(routerTool,
        "buildToolSpecs", 12, "chatProduct12");

    FunctionToolSpec agentRoute = specs.stream()
        .filter(spec -> spec.name().equals("route_agent"))
        .findFirst().orElseThrow();
    assertFalse(agentRoute.description().contains("knowledge coverage"));
  }
}
