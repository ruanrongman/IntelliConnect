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
package top.rslly.iot.utility.ai.toolAgent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import top.rslly.iot.utility.ai.DescriptionUtil;
import top.rslly.iot.utility.ai.llm.FunctionToolSpec;

class AgentKnowledgeToolRegistrationTest {

  @Test
  void registersKnowledgeToolWithSameNameAndDescriptionInFunctionMode() {
    DescriptionUtil descriptionUtil = org.mockito.Mockito.mock(DescriptionUtil.class);
    Map<String, String> descriptions = new LinkedHashMap<>();
    descriptions.put("controlTool", "control");
    descriptions.put("knowledgeTool", "knowledge description");
    when(descriptionUtil.getToolDescriptions(12, "chatProduct12")).thenReturn(descriptions);
    Agent agent = new Agent();
    ReflectionTestUtils.setField(agent, "descriptionUtil", descriptionUtil);

    List<FunctionToolSpec> specs = ReflectionTestUtils.invokeMethod(agent,
        "buildFunctionToolSpecs", 12, "chatProduct12");

    assertEquals(List.of("controlTool", "knowledgeTool"),
        specs.stream().map(FunctionToolSpec::name).toList());
    assertEquals("knowledge description", specs.get(1).description());
  }
}
