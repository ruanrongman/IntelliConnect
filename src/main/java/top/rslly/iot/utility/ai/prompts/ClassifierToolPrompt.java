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

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.rslly.iot.models.AdminConfigEntity;
import top.rslly.iot.services.AdminConfigServiceImpl;
import top.rslly.iot.services.agent.McpServerServiceImpl;
import top.rslly.iot.services.agent.ProductRouterSetServiceImpl;
import top.rslly.iot.services.agent.ProductSkillsServiceImpl;
import top.rslly.iot.services.agent.ProductToolsBanServiceImpl;
import top.rslly.iot.utility.ai.DescriptionUtil;
import top.rslly.iot.utility.ai.mcp.McpWebsocket;
import top.rslly.iot.utility.ai.promptTemplate.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class ClassifierToolPrompt {
  @Autowired
  private McpServerServiceImpl mcpServerService;
  @Autowired
  private McpWebsocket mcpWebsocket;
  @Autowired
  private ProductRouterSetServiceImpl productRouterSetService;
  @Autowired
  private ProductToolsBanServiceImpl productToolsBanService;
  @Autowired
  private DescriptionUtil descriptionUtil;
  @Autowired
  private ProductSkillsServiceImpl productSkillsService;
  @Autowired
  private AdminConfigServiceImpl adminConfigService;
  @Value("${ai.classifier.include.thought:true}")
  private boolean includeThought;
  private static final int MAX_ROUTER_RULES_CHARS = 1200;
  private static final int MAX_MCP_DESCRIPTION_CHARS = 6000;
  private static final String classifierPrompt =
      """
           You are a router. Pick exactly one task for the latest user input.

           CRITICAL RULES - FOLLOW THESE FIRST - YOUR ACCURACY DEPENDS ON IT:
           1. YOUR #1 RULE: PRIORITIZE THE LATEST USER INPUT ABOVE EVERYTHING ELSE.
           2. If the latest user input is a CLEAR, COMPLETE, STANDALONE REQUEST → IT IS A NEW TOPIC. YOU MUST IGNORE ALL PREVIOUS TOPICS FROM HISTORY.
           3. Only use conversation history when the latest input is ambiguous OR it CLEARLY CONTINUES the previous topic.
           4. Switch phrases like "等一下", "先别", "不用了", "算了", "换个话题", "别查了" mean the previous task should not be continued - start fresh.
           5. Specifically regarding weather (task 1):
              - ONLY keep task 1 when this is a FOLLOW-UP question about weather.
              - If the latest request is ANYTHING ELSE NOT ABOUT WEATHER → YOU MUST NOT CHOOSE TASK 1, EVEN IF THE LAST TOPIC WAS WEATHER.
           6. Examples (study these carefully):
              - Correct: Previous topic was *Query weather* → Current request: "讲个故事" → NEW TOPIC → route to *Common chat or fallback*, NOT *Query weather*
              - Correct: Previous topic was *Query weather* → Current request: "那明天呢" → FOLLOW-UP → keep *Query weather*
              - Correct: Previous topic was *Control and query electrical* → Current request: "讲个笑话" → NEW TOPIC → route to *Common chat or fallback*
              - Correct: Previous topic was *Play or recommend music* → Current request: "推荐另一首" → FOLLOW-UP → keep *Play or recommend music*
           Additional rules:
           - Args must be short, normalized, and minimal. Do not copy the user's wording or include filler.
           - Task 2 must not be used for xiaozhi_device operations.
           - If task 10 exists and the request is about xiaozhi_device or seeing what is in front of the user, choose task 10.
           - Output JSON only. No markdown and no explanation.
           Output:
           {
           {thought}  "action": {
               "value": "task_number (string) or "" (empty string when no task selected)",
               "args": "minimal normalized instruction, never null"
             }
           }
           {router_set_section}Available tasks(JSON): {task_map}
           {memory_map_section}
           Recent conversation:
           {recent_conversation}
          """;

  public String getClassifierTool(int productId, String chatId, String recentConversation) {
    Map<String, String> classifierMap = new HashMap<>();
    classifierMap.put("1", "Query weather");
    classifierMap.put("2", "Control and query electrical, excluding music and xiaozhi_device");
    classifierMap.put("3", "Play or recommend music");
    classifierMap.put("4", "Complex multi-step task");
    classifierMap.put("5", "Common chat or fallback");
    if (!chatId.startsWith("chatProduct")) {
      classifierMap.put("6", "Bind or unbind products");
      classifierMap.put("7", "Switch controlled products");
    }
    classifierMap.put("8", "Schedule or reminder");
    classifierMap.put("9", "Role or voice configuration");
    String mcpDescription = buildMcpDescription(productId, chatId);
    if (!mcpDescription.isBlank()) {
      classifierMap.put("10", mcpDescription);
    }
    if (chatId.startsWith("chatProduct")) {
      classifierMap.put("11", "Say goodbye or step down");
    }
    List<String> banTools = productToolsBanService.getProductToolsBanList(productId);
    if (!banTools.isEmpty()) {
      for (var banTool : banTools) {
        classifierMap.remove(banTool);
      }
    }
    Map<String, String> params = new HashMap<>();
    params.put("router_set_section",
        buildSection("Custom routing rules",
            limitText(getRouterSet(productId), MAX_ROUTER_RULES_CHARS)));
    params.put("task_map", JSON.toJSONString(classifierMap));
    params.put("memory_map_section",
        buildSection("Long-term memory labels",
            defaultText(descriptionUtil.getAgentLongMemory(productId), "")));
    params.put("recent_conversation", defaultText(recentConversation, "none"));
    if (getIncludeThoughtConfig()) {
      params.put("thought", "  \"thought\": \"Brief reason in Chinese.\",\n");
    } else {
      params.put("thought", "");
    }
    return StringUtils.formatString(classifierPrompt, params);
  }

  private String buildMcpDescription(int productId, String chatId) {
    var mcpServerList = mcpServerService.findAllByProductId(productId);
    var productSkillsEntities = productSkillsService.findAllByProductId(productId);
    boolean hasMcp =
        !productSkillsEntities.isEmpty()
            || !mcpServerList.isEmpty()
            || mcpWebsocket.isRunning(McpWebsocket.DEVICE_SERVER_NAME, chatId)
            || mcpWebsocket.isRunning(McpWebsocket.ENDPOINT_SERVER_NAME, "mcp" + productId);
    if (!hasMcp) {
      return "";
    }

    StringBuilder mcpServerString = new StringBuilder();
    for (var mcpServerEntity : mcpServerList) {
      appendWithSeparator(mcpServerString, mcpServerEntity.getDescription());
    }
    for (var productSkillsEntity : productSkillsEntities) {
      appendWithSeparator(mcpServerString, productSkillsEntity.getName());
    }
    if (mcpWebsocket.isRunning(McpWebsocket.DEVICE_SERVER_NAME, chatId)) {
      appendWithSeparator(mcpServerString,
          mcpWebsocket.getIntention(McpWebsocket.DEVICE_SERVER_NAME, chatId));
    }
    if (mcpWebsocket.isRunning(McpWebsocket.ENDPOINT_SERVER_NAME, "mcp" + productId)) {
      appendWithSeparator(mcpServerString,
          mcpWebsocket.getIntention(McpWebsocket.ENDPOINT_SERVER_NAME, "mcp" + productId));
    }
    return limitText(mcpServerString.toString(), MAX_MCP_DESCRIPTION_CHARS);
  }

  private void appendWithSeparator(StringBuilder builder, String text) {
    if (text == null || text.isBlank()) {
      return;
    }
    if (!builder.isEmpty()) {
      builder.append(" | ");
    }
    builder.append(text.trim());
  }

  private String getRouterSet(int productId) {
    var productRouterSetList = productRouterSetService.findAllByProductId(productId);
    if (productRouterSetList.isEmpty()) {
      return "";
    }
    return productRouterSetList.get(0).getPrompt();
  }

  private String buildSection(String title, String content) {
    if (content == null || content.isBlank()) {
      return "";
    }
    return title + ": " + content + "\n";
  }

  private String defaultText(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }

  private String limitText(String value, int maxChars) {
    if (value == null) {
      return "";
    }
    String normalized = value.replace("\r", " ").replace("\n", " ").trim();
    if (normalized.length() <= maxChars) {
      return normalized;
    }
    return normalized.substring(0, maxChars) + "...";
  }

  private boolean getIncludeThoughtConfig() {
    try {
      List<AdminConfigEntity> configs =
          adminConfigService.findAllBySetKey("ai_classifier_include_thought");
      if (!configs.isEmpty() && configs.get(0).getSetValue() != null) {
        return Boolean.parseBoolean(configs.get(0).getSetValue());
      }
    } catch (Exception e) {
      log.error("从数据库获取AI分类器thought配置失败", e);
    }
    return includeThought;
  }
}
