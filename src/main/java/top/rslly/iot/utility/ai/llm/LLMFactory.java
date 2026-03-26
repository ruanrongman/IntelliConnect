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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class LLMFactory {

  private static String deepSeekApiKey;
  private static String siliconFlowApiKey;
  private static String uniApiKey;
  private static String dashScopeApiKey;
  private static String glmKey;
  private static String customLLMProviderUrl;
  private static String customKey;

  @Value("${ai.glm-key}")
  public void setGlmApiKey(String apiKey) {
    glmKey = apiKey;
  }

  @Value("${ai.deepSeek-key}")
  public void setDeepSeekApiKey(String apiKey) {
    deepSeekApiKey = apiKey;
  }

  @Value("${ai.siliconFlow-Key}")
  public void setSiliconFlowApiKey(String apiKey) {
    siliconFlowApiKey = apiKey;
  }

  @Value("${ai.uniApi-Key}")
  public void setUniApiKey(String apiKey) {
    uniApiKey = apiKey;
  }

  @Value("${ai.dashscope-key}")
  public void setDashScopeApiKey(String apiKey) {
    dashScopeApiKey = apiKey;
  }

  @Value("${ai.custom-key}")
  public void setCustomKey(String key) {
    customKey = key;
  }

  @Value("${ai.custom-llm-provider-url}")
  public void setCustomLLMProviderUrl(String url) {
    customLLMProviderUrl = url;
  }

  public static LLM getLLM(String llmName) {
    if (llmName == null || llmName.trim().isEmpty()) {
      return new DeepSeek(deepSeekApiKey);
    }

    // 1. 规范化输入：只去除前后空格，保留原始大小写
    String trimmedLlmName = llmName.trim();
    // 创建一个全小写的版本，仅用于不区分大小写的匹配
    String lowerCaseLlmName = trimmedLlmName.toLowerCase();

    // 2. 处理特殊的 glm 供应商 (不区分大小写)
    if ("glm".equalsIgnoreCase(trimmedLlmName)) {
      return new Glm(glmKey);
    }

    // 3. 解析 "think" 模式 (不区分大小写)
    boolean enableThinking = false;
    boolean thinkMode = false;
    int thinkingBudget = 128; // 默认值
    String baseLlmName = trimmedLlmName; // 初始化为原始名称

    // 使用 (?i) 标志使正则表达式不区分大小写
    Pattern thinkPattern = Pattern.compile("(?i)-think-(\\d+)");
    Matcher matcher = thinkPattern.matcher(baseLlmName);
    if (matcher.find()) {
      enableThinking = true;
      thinkMode = true;
      thinkingBudget = Integer.parseInt(matcher.group(1));
      // 从原始名称中移除 "-think-数字" 部分
      baseLlmName = baseLlmName.substring(0, matcher.start());
    } else if (lowerCaseLlmName.contains("-think-no")) {
      thinkMode = true;
      baseLlmName = baseLlmName.substring(0, lowerCaseLlmName.indexOf("-think-no"));
    } else if (lowerCaseLlmName.contains("-think")) {
      // 如果只包含 "-think"
      enableThinking = true;
      thinkMode = true;
      int thinkIndex = lowerCaseLlmName.indexOf("-think");
      // 从原始名称中移除 "-think" 部分
      baseLlmName = baseLlmName.substring(0, thinkIndex);
    }

    // 去除可能产生的末尾连字符和空格
    baseLlmName = baseLlmName.trim();
    if (baseLlmName.endsWith("-")) {
      baseLlmName = baseLlmName.substring(0, baseLlmName.length() - 1);
    }

    // 4. 根据前缀路由到不同供应商 (使用小写版本匹配，从原始版本提取)
    if (lowerCaseLlmName.startsWith("silicon-")) {
      String modelName = baseLlmName.substring("silicon-".length());
      if (thinkMode) {
        return new Qwen3(siliconFlowApiKey, modelName, enableThinking,
            thinkingBudget, "https://api.siliconflow.cn/v1/chat/completions");
      }
      return new DeepSeek("https://api.siliconflow.cn", modelName, siliconFlowApiKey);
    } else if (lowerCaseLlmName.startsWith("dashscope-")) {
      String modelName = baseLlmName.substring("dashscope-".length());
      if (thinkMode) {
        return new Qwen3(dashScopeApiKey, modelName, enableThinking,
            thinkingBudget, "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions");
      }
      return new DeepSeek("https://dashscope.aliyuncs.com/compatible-mode", modelName,
          dashScopeApiKey);
    } else if (lowerCaseLlmName.startsWith("uniapi-")) {
      String modelName = baseLlmName.substring("uniapi-".length());
      if (thinkMode) {
        return new Qwen3(uniApiKey, modelName, enableThinking,
            thinkingBudget, "https://hk.uniapi.io/v1/chat/completions");
      }
      return new DeepSeek("https://hk.uniapi.io", modelName, uniApiKey);
    } else if (lowerCaseLlmName.startsWith("custom-")) {
      String modelName = baseLlmName.substring("custom-".length());
      if (thinkMode) {
        return new Qwen3(customKey, modelName, enableThinking,
            thinkingBudget, customLLMProviderUrl + "/v1/chat/completions");
      }
      return new DeepSeek(customLLMProviderUrl, modelName, customKey);
    }

    // 6. 兜底策略：如果以上都不匹配，返回默认的 DeepSeek
    return new DeepSeek(deepSeekApiKey);
  }

  public static LLM getLLM(String llmName, String baseUrl, String apiKey) {
    if (llmName == null || llmName.trim().isEmpty()) {
      return null;
    }
    return new DeepSeek(baseUrl, llmName, apiKey);
  }
}
