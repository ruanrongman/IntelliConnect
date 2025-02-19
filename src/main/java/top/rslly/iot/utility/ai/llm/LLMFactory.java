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
package top.rslly.iot.utility.ai.llm;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.utils.Constants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LLMFactory {
  private static String deepSeekApiKey;

  @Value("${ai.deepSeek-key}")
  public void setApiKey(String apiKey) {
    deepSeekApiKey = apiKey;
  }

  public static LLM getLLM(String llmName) {
    if (llmName.equals("glm")) {
      return new Glm();
    } else if (llmName.equals("deepSeek")) {
      return new DeepSeek(deepSeekApiKey);
    } else if (llmName.equals("silicon-deepSeek-v3")) {
      return new DeepSeek("https://api.siliconflow.cn", "deepseek-ai/DeepSeek-V3", deepSeekApiKey);
    } else if (llmName.equals("silicon-Qwen2.5-7B-Instruct")) {
      return new DeepSeek("https://api.siliconflow.cn", "Qwen/Qwen2.5-7B-Instruct", deepSeekApiKey);
    } else if (llmName.equals("silicon-deepSeek-v2.5")) {
      return new DeepSeek("https://api.siliconflow.cn", "deepseek-ai/DeepSeek-V2.5",
          deepSeekApiKey);
    } else {
      return new DeepSeek(deepSeekApiKey);
    }
  }
}
