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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import top.rslly.iot.services.UserConfigServiceImpl;
import top.rslly.iot.services.agent.LlmProviderInformationServiceImpl;
import top.rslly.iot.services.agent.ProductLlmModelServiceImpl;
import top.rslly.iot.utility.ai.llm.LLM;
import top.rslly.iot.utility.ai.llm.LLMFactory;

@ExtendWith(MockitoExtension.class)
class LlmDiyUtilityTest {
  @Mock
  private ProductLlmModelServiceImpl productLlmModelService;
  @Mock
  private LlmProviderInformationServiceImpl llmProviderInformationService;
  @Mock
  private UserConfigServiceImpl userConfigService;
  @InjectMocks
  private LlmDiyUtility llmDiyUtility;

  @BeforeEach
  void setUp() {
    new LLMFactory().setDashScopeApiKey("test-key");
    when(productLlmModelService.findAllByProductId(1)).thenReturn(List.of());
  }

  @Test
  void defaultsToDisabledAndSeparatesExplicitlyEnabledProductInstance() {
    when(productLlmModelService.findAllByProductId(2)).thenReturn(List.of());
    when(userConfigService.getConfigValue(2, LlmDiyUtility.WEB_SEARCH_CONFIG_KEY))
        .thenReturn("true");

    LLM disabled = llmDiyUtility.getDiyLlm(1, "dashscope-qwen-plus", "classifier");
    LLM enabled = llmDiyUtility.getDiyLlm(2, "dashscope-qwen-plus", "classifier", true);
    LLM enabledAgain = llmDiyUtility.getDiyLlm(2, "dashscope-qwen-plus", "classifier", true);

    assertSame(enabled, enabledAgain);
    assertNotSame(disabled, enabled);
    assertFalse((Boolean) ReflectionTestUtils.getField(disabled, "webSearchEnabled"));
    assertTrue((Boolean) ReflectionTestUtils.getField(enabled, "webSearchEnabled"));
  }

  @Test
  void onlyExactTrueEnablesProductWebSearch() {
    when(productLlmModelService.findAllByProductId(2)).thenReturn(List.of());
    when(userConfigService.getConfigValue(1, LlmDiyUtility.WEB_SEARCH_CONFIG_KEY))
        .thenReturn("invalid");
    when(userConfigService.getConfigValue(2, LlmDiyUtility.WEB_SEARCH_CONFIG_KEY))
        .thenReturn("false");

    LLM invalid = llmDiyUtility.getDiyLlm(1, "dashscope-invalid-search", "classifier", true);
    LLM disabled = llmDiyUtility.getDiyLlm(2, "dashscope-false-search", "classifier", true);

    assertFalse((Boolean) ReflectionTestUtils.getField(invalid, "webSearchEnabled"));
    assertFalse((Boolean) ReflectionTestUtils.getField(disabled, "webSearchEnabled"));
  }

  @Test
  void uppercaseTrueIsInvalidAndKeepsWebSearchDisabled() {
    when(userConfigService.getConfigValue(1, LlmDiyUtility.WEB_SEARCH_CONFIG_KEY))
        .thenReturn("TRUE");

    LLM llm = llmDiyUtility.getDiyLlm(1, "dashscope-uppercase-search", "classifier", true);

    assertFalse((Boolean) ReflectionTestUtils.getField(llm, "webSearchEnabled"));
  }

  @Test
  void configReadFailureKeepsWebSearchDisabled() {
    when(userConfigService.getConfigValue(1, LlmDiyUtility.WEB_SEARCH_CONFIG_KEY))
        .thenThrow(new IllegalStateException("config unavailable"));

    LLM llm = llmDiyUtility.getDiyLlm(1, "dashscope-config-error", "classifier", true);

    assertFalse((Boolean) ReflectionTestUtils.getField(llm, "webSearchEnabled"));
  }
}
