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
import top.rslly.iot.services.agent.LlmProviderInformationServiceImpl;
import top.rslly.iot.services.agent.ProductLlmModelServiceImpl;
import top.rslly.iot.utility.ai.llm.LLM;
import top.rslly.iot.utility.ai.llm.LLMFactory;

@Component
@Slf4j
public class LlmDiyUtility {
  @Autowired
  private ProductLlmModelServiceImpl productLlmModelService;
  @Autowired
  private LlmProviderInformationServiceImpl llmProviderInformationService;

  public LLM getDiyLlm(int productId, String llmName, String toolsId) {
    if (!productLlmModelService.findAllByProductId(productId).isEmpty()) {
      var productLlmModelEntityList =
          productLlmModelService.findAllByProductIdAndToolsId(productId, toolsId);
      if (productLlmModelEntityList.isEmpty()) {
        return LLMFactory.getLLM(llmName);
      } else {
        var productLlmModelEntity = productLlmModelEntityList.get(0);
        var providerInformation =
            llmProviderInformationService.findAllById(productLlmModelEntity.getProviderId());
        if (!providerInformation.isEmpty()) {
          log.info("llmName:{}", productLlmModelEntity.getModelName());
          return LLMFactory.getLLM(productLlmModelEntity.getModelName(),
              providerInformation.get(0).getBaseUrl(),
              providerInformation.get(0).getAppKey());

        } else {
          return LLMFactory.getLLM(llmName);
        }
      }
    } else {
      return LLMFactory.getLLM(llmName);
    }
  }
}
