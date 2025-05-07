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

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.rslly.iot.param.request.ProductRole;
import top.rslly.iot.services.agent.ProductRoleServiceImpl;
import top.rslly.iot.services.thingsModel.ProductServiceImpl;
import top.rslly.iot.utility.ai.IcAiException;
import top.rslly.iot.utility.ai.ModelMessage;
import top.rslly.iot.utility.ai.ModelMessageRole;
import top.rslly.iot.utility.ai.llm.LLM;
import top.rslly.iot.utility.ai.llm.LLMFactory;
import top.rslly.iot.utility.ai.prompts.ProductRolePrompt;
import top.rslly.iot.utility.ai.voice.VoiceTimbre;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Component
@Slf4j
public class ProductRoleTool implements BaseTool<String> {
  @Autowired
  private ProductServiceImpl productService;
  @Autowired
  private ProductRolePrompt productRolePrompt;
  @Autowired
  private ProductRoleServiceImpl productRoleService;
  @Value("${ai.productRoleTool-llm}")
  private String llmName;
  private String name = "productRoleTool";
  private String description = """
      This tool is used to convert user requests into specific role configurations
      Args: All user requests for role configuration (str)
      """;

  @Override
  public String run(String question) {
    return null;
  }

  @Override
  public String run(String question, Map<String, Object> globalMessage) {
    int productId = (int) globalMessage.get("productId");

    if (productService.findAllById(productId).isEmpty())
      return "产品设置错误，请检查相关设置！";
    LLM llm = LLMFactory.getLLM(llmName);
    List<ModelMessage> messages = new ArrayList<>();

    ModelMessage systemMessage =
        new ModelMessage(ModelMessageRole.SYSTEM.value(),
            productRolePrompt.getProductRoleTool(productId));
    // log.info("systemMessage: " + systemMessage.getContent());
    ModelMessage userMessage = new ModelMessage(ModelMessageRole.USER.value(), question);
    messages.add(systemMessage);
    messages.add(userMessage);
    var obj = llm.jsonChat(question, messages, true).getJSONObject("action");
    var answer = (String) obj.get("answer");
    try {
      return answer + process_llm_result(obj, productId);
    } catch (Exception e) {
      // e.printStackTrace();
      return "输入信息不全，请详细说明智能机器人的角色和名字，角色介绍以及用户名字，下面是Ai的建议:" + answer;
    }
  }

  private String process_llm_result(JSONObject llmObject, int productId) throws IcAiException {
    if (llmObject.get("code").equals("200") || llmObject.get("code").equals(200)) {
      String assistantName = llmObject.getString("assistant_name");
      String userName = llmObject.getString("user_name");
      String role = llmObject.getString("role");
      String roleIntroduction = llmObject.getString("role_introduction");
      String taskType = llmObject.getString("taskType");
      String voiceTimbre = llmObject.getString("voice_timbre");
      ProductRole productRole = new ProductRole();
      productRole.setProductId(productId);
      try {
        productRole.setVoice(VoiceTimbre.valueOf(voiceTimbre).getTimbre());
      } catch (IllegalArgumentException e) {
        productRole.setVoice(VoiceTimbre.CosyVoiceLongXiaoChun.getTimbre());
      }
      if (taskType.equals("set")) {
        if (assistantName == null || userName == null || role == null || roleIntroduction == null
            || assistantName.equals("null") || userName.equals("null") || role.equals("null")
            || roleIntroduction.equals("null"))
          throw new IcAiException("information incomplete");
        productRole.setAssistantName(assistantName);
        productRole.setUserName(userName);
        productRole.setRole(role);
        productRole.setRoleIntroduction(roleIntroduction);
        var result = productRoleService.postProductRole(productRole);
        if (result.getErrorCode() != 200) {
          var result1 = productRoleService.putProductRole(productRole);
          if (result1.getErrorCode() != 200)
            throw new IcAiException("database control error!");
        }
        return "详细角色信息如下:" + "角色为" + role + "，角色介绍为" + roleIntroduction + "用户名为" + userName
            + "，智能机器人的名字为" + assistantName + "，声音为" + voiceTimbre;
      } else if (taskType.equals("cancel")) {
        var result = productRoleService.deleteByProductId(productId);
        if (!result.isEmpty()) {
          return "平台真实响应:删除成功";
        } else
          return "平台真实响应:删除失败";
      } else
        return "";
    } else
      throw new IcAiException("llm response error");
  }
}
