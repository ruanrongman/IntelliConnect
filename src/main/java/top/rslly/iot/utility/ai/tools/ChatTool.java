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

import com.zhipu.oapi.service.v4.model.ChatMessage;
import com.zhipu.oapi.service.v4.model.ChatMessageRole;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.rslly.iot.utility.HttpRequestUtils;
import top.rslly.iot.utility.ai.ModelMessage;
import top.rslly.iot.utility.ai.ModelMessageRole;
import top.rslly.iot.utility.ai.llm.Glm;
import top.rslly.iot.utility.ai.Prompt;
import top.rslly.iot.utility.ai.llm.LLM;
import top.rslly.iot.utility.ai.llm.LLMFactory;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
public class ChatTool {
  @Autowired
  private Prompt prompt;
  @Autowired
  private HttpRequestUtils httpRequestUtils;
  @Value("${ai.chatTool-llm}")
  private String llmName;
  private String name = "chatTool";
  private String description = """
      This tool is used to chat with people
      Args: user question(str)
      """;

  public String run(String question, List<ModelMessage> memory) {
    LLM llm = LLMFactory.getLLM(llmName);
    List<ModelMessage> messages = new ArrayList<>();

    ModelMessage systemMessage =
        new ModelMessage(ModelMessageRole.SYSTEM.value(), prompt.getChatTool());
    ModelMessage userMessage = new ModelMessage(ModelMessageRole.USER.value(), question);
    if (!memory.isEmpty()) {
      // messages.addAll(memory);
      systemMessage.setContent(prompt.getChatTool() + memory);
    }
    messages.add(systemMessage);
    messages.add(userMessage);
    // log.info("chatTool: " + messages);
    return llm.commonChat(question, messages, true);
  }
}
