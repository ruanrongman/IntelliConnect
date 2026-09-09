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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.rslly.iot.services.UserConfigServiceImpl;
import top.rslly.iot.services.agent.LlmProviderInformationServiceImpl;
import top.rslly.iot.services.agent.ProductLlmModelServiceImpl;
import top.rslly.iot.utility.ai.llm.LLM;
import top.rslly.iot.utility.ai.llm.LLMFactory;

@Component
@Slf4j
public class LlmDiyUtility {
  public static final String WEB_SEARCH_CONFIG_KEY = "web-search.enabled";

  @Autowired
  private ProductLlmModelServiceImpl productLlmModelService;
  @Autowired
  private LlmProviderInformationServiceImpl llmProviderInformationService;
  @Autowired
  private UserConfigServiceImpl userConfigService;

  public LLM getDiyLlm(int productId, String llmName, String toolsId) {
    return getDiyLlm(productId, llmName, toolsId, false);
  }

  /**
   * Gets a product model and optionally enables provider web search for this call path. The product
   * setting is read once per model acquisition so changes apply to the next request.
   */
  public LLM getDiyLlm(int productId, String llmName, String toolsId,
      boolean requestWebSearch) {
    boolean webSearchEnabled = requestWebSearch && isWebSearchEnabled(productId);
    if (!productLlmModelService.findAllByProductId(productId).isEmpty()) {
      var productLlmModelEntityList =
          productLlmModelService.findAllByProductIdAndToolsId(productId, toolsId);
      if (productLlmModelEntityList.isEmpty()) {
        return LLMFactory.getLLM(llmName, webSearchEnabled);
      } else {
        var productLlmModelEntity = productLlmModelEntityList.get(0);
        var providerInformation =
            llmProviderInformationService.findAllById(productLlmModelEntity.getProviderId());
        if (!providerInformation.isEmpty()) {
          log.info("llmName:{}", productLlmModelEntity.getModelName());
          return LLMFactory.getLLM(productLlmModelEntity.getModelName(),
              providerInformation.get(0).getBaseUrl(),
              providerInformation.get(0).getAppKey(),
              Boolean.TRUE.equals(productLlmModelEntity.getThinking()),
              LLMFactory.normalizeThinkingBudget(productLlmModelEntity.getThinkingBudget()),
              webSearchEnabled);

        } else {
          return LLMFactory.getLLM(llmName, webSearchEnabled);
        }
      }
    } else {
      return LLMFactory.getLLM(llmName, webSearchEnabled);
    }
  }

  private boolean isWebSearchEnabled(int productId) {
    if (userConfigService == null || productId <= 0) {
      return false;
    }
    try {
      String configuredValue = userConfigService.getConfigValue(productId, WEB_SEARCH_CONFIG_KEY);
      return configuredValue != null && "true".equals(configuredValue.trim());
    } catch (RuntimeException e) {
      log.warn("Unable to read web search setting, keeping it disabled, productId={}", productId,
          e);
      return false;
    }
  }
}
