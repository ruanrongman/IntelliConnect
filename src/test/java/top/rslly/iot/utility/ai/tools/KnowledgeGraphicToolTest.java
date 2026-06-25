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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import top.rslly.iot.services.UserConfigServiceImpl;

class KnowledgeGraphicToolTest {

  @Test
  void nodeLimitsAllowSixHundred() {
    KnowledgeGraphicTool tool = new KnowledgeGraphicTool();
    UserConfigServiceImpl userConfigService = Mockito.mock(UserConfigServiceImpl.class);
    Mockito.when(userConfigService.getConfigValue(1, "knowledge_graph.node.limit"))
        .thenReturn("600");
    Mockito.when(userConfigService.getConfigValue(1, "knowledge_graph.node.hard_limit"))
        .thenReturn("600");
    ReflectionTestUtils.setField(tool, "userConfigService", userConfigService);

    Integer expectedNodeLimit = ReflectionTestUtils.invokeMethod(tool, "getExpectedNodeLimit", 1);
    Integer hardNodeLimit = ReflectionTestUtils.invokeMethod(tool, "getHardNodeLimit", 1);

    Assertions.assertEquals(600, expectedNodeLimit);
    Assertions.assertEquals(600, hardNodeLimit);
  }

  @Test
  void invalidNodeLimitsFallbackToTheirDefaults() {
    KnowledgeGraphicTool tool = new KnowledgeGraphicTool();
    UserConfigServiceImpl userConfigService = Mockito.mock(UserConfigServiceImpl.class);
    Mockito.when(userConfigService.getConfigValue(1, "knowledge_graph.node.limit"))
        .thenReturn("invalid");
    Mockito.when(userConfigService.getConfigValue(1, "knowledge_graph.node.hard_limit"))
        .thenReturn("invalid");
    ReflectionTestUtils.setField(tool, "userConfigService", userConfigService);

    Integer expectedNodeLimit = ReflectionTestUtils.invokeMethod(tool, "getExpectedNodeLimit", 1);
    Integer hardNodeLimit = ReflectionTestUtils.invokeMethod(tool, "getHardNodeLimit", 1);

    Assertions.assertEquals(100, expectedNodeLimit);
    Assertions.assertEquals(300, hardNodeLimit);
  }
}
