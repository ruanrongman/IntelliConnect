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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.rslly.iot.utility.ai.mcp.McpAgent;
import top.rslly.iot.utility.ai.tools.KnowledgeTool;

@ExtendWith(MockitoExtension.class)
class DescriptionUtilTest {
  @Mock
  private KnowledgeTool knowledgeTool;
  @Mock
  private McpAgent mcpAgent;

  @InjectMocks
  private DescriptionUtil descriptionUtil;

  @Test
  void exposesSameStableKnowledgeMetadataToReactAndFunctionCalling() {
    when(knowledgeTool.getName()).thenReturn("knowledgeTool");
    when(knowledgeTool.getDescription(12)).thenReturn("knowledge description");
    when(mcpAgent.getName()).thenReturn("mcpAgent");
    when(mcpAgent.getDescription(12, "chatProduct12")).thenReturn("mcp description");

    Map<String, String> toolDescriptions =
        descriptionUtil.getToolDescriptions(12, "chatProduct12");
    String reactDescriptions = descriptionUtil.getTools(12, "chatProduct12");

    assertEquals(List.of("controlTool", "weatherTool", "musicTool", "knowledgeTool", "mcpAgent"),
        List.copyOf(toolDescriptions.keySet()));
    assertEquals("knowledge description", toolDescriptions.get("knowledgeTool"));
    assertTrue(reactDescriptions.indexOf("controlTool") < reactDescriptions.indexOf("weatherTool"));
    assertTrue(
        reactDescriptions.indexOf("weatherTool") < reactDescriptions.indexOf("knowledgeTool"));
    assertTrue(reactDescriptions.contains("knowledge description"));
  }

  @Test
  void omitsUnavailableKnowledgeTool() {
    when(knowledgeTool.getDescription(12)).thenReturn("");
    when(mcpAgent.getName()).thenReturn("mcpAgent");
    when(mcpAgent.getDescription(12, "chatProduct12")).thenReturn("");

    Map<String, String> toolDescriptions =
        descriptionUtil.getToolDescriptions(12, "chatProduct12");

    assertFalse(toolDescriptions.containsKey("knowledgeTool"));
  }
}
