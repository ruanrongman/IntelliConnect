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
import top.rslly.iot.models.AgentMemoryEntity;
import top.rslly.iot.services.agent.AgentMemoryServiceImpl;
import top.rslly.iot.utility.HttpRequestUtils;
import top.rslly.iot.utility.ai.IcAiException;
import top.rslly.iot.utility.ai.ModelMessage;
import top.rslly.iot.utility.ai.ModelMessageRole;
import top.rslly.iot.utility.ai.llm.LLM;
import top.rslly.iot.utility.ai.llm.LLMFactory;
import top.rslly.iot.utility.ai.prompts.ClassifierToolPrompt;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Data
@Component
@Slf4j
public class ClassifierTool {
  @Autowired
  private ClassifierToolPrompt classifierToolPrompt;
  @Autowired
  private HttpRequestUtils httpRequestUtils;
  public static final ConcurrentHashMap<String, Object> dataMap = new ConcurrentHashMap<>();
  public static final Lock lock = new ReentrantLock();
  public static final Condition dataCondition = lock.newCondition();
  @Value("${ai.classifierTool-llm}")
  private String llmName;
  @Value("${ai.classifierTool-speedUp}")
  private boolean speedUp;
  private String name = "classifierTool";
  private String description = """
      This tool is used to classify users' intentions
      Args: user question(str)
      """;

  public Map<String, Object> run(String question, Map<String, Object> globalMessage) {
    LLM llm = LLMFactory.getLLM(llmName);
    List<ModelMessage> messages = new ArrayList<>();
    Map<String, Object> resultMap = new HashMap<>();
    // 使用 Optional 进行类型安全的转换
    List<ModelMessage> memory =
        Optional.ofNullable((List<ModelMessage>) globalMessage.get("memory"))
            .orElse(Collections.emptyList());
    ModelMessage systemMessage =
        new ModelMessage(ModelMessageRole.SYSTEM.value(), classifierToolPrompt.getClassifierTool());
    ModelMessage userMessage = new ModelMessage(ModelMessageRole.USER.value(), question);
    // log.info("systemMessage: " + systemMessage.getContent());
    if (!memory.isEmpty()) {
      // messages.addAll(memory);
      systemMessage.setContent(classifierToolPrompt.getClassifierTool() + memory);
    }
    messages.add(systemMessage);
    messages.add(userMessage);
    if (speedUp) {
      dataMap.clear();
      llm.streamJsonChat(question, messages, true,
          new ClassifierToolEventSourceListener(question, 6));
      lock.lock();
      try {
        while (dataMap.get("args") == null) {
          dataCondition.await(); // 等待数据就绪
        }
        // System.out.println("Consumer: Received data - " + dataMap.get("value"));
        log.info("Consumer: Received data - {}", dataMap);
        resultMap.put("value",
            JSONObject.parseArray(dataMap.get("value").toString(), String.class));
        resultMap.put("args", dataMap.get("args"));
        resultMap.put("answer", dataMap.get("answer"));
      } catch (Exception e) {
        log.error("ClassifierTool error{}", e.getMessage());
        resultMap.put("answer", "对不起你购买的产品尚不支持这个请求或者设备不在线，请检查你的小程序的设置");
        return resultMap;
      } finally {
        lock.unlock();
      }
    } else {
      var obj = llm.jsonChat(question, messages, true).getJSONObject("action");
      var answer = (String) obj.get("answer");
      try {
        var value = process_llm_value(obj);
        resultMap.put("value", value.get("value"));
        resultMap.put("args", value.get("args"));
        resultMap.put("answer", answer);

      } catch (Exception e) {
        resultMap.put("answer", answer);
        return resultMap;
      }
    }
    return resultMap;
  }

  private Map<String, Object> process_llm_value(JSONObject jsonObject) throws IcAiException {
    Map<String, Object> resultMap = new HashMap<>();
    if (jsonObject.get("code").equals("200") || jsonObject.get("code").equals(200)) {
      var valueJson = jsonObject.getJSONArray("value");
      var argsJson = jsonObject.getString("args");
      resultMap.put("value", JSONObject.parseArray(valueJson.toJSONString(), String.class));
      resultMap.put("args", argsJson);
      return resultMap;
    } else
      throw new IcAiException("llm response error");
  }
}
