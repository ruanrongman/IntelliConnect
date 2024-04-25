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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.rslly.iot.utility.HttpRequestUtils;
import top.rslly.iot.utility.ai.Glm;
import top.rslly.iot.utility.ai.Prompt;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
public class ChatTool {
  @Autowired
  private Prompt prompt;
  @Autowired
  private HttpRequestUtils httpRequestUtils;
  private String name = "chatTool";
  private String description = """
      This tool is used to chat with people
      Args: user question(str)
      """;

  public String run(String question, List<ChatMessage> memory) {
    Glm glm = new Glm();
    List<ChatMessage> messages = new ArrayList<>();

    ChatMessage systemMessage =
        new ChatMessage(ChatMessageRole.SYSTEM.value(), prompt.getChatTool());
    ChatMessage userMessage = new ChatMessage(ChatMessageRole.USER.value(), question);
    messages.add(systemMessage);
    if (!memory.isEmpty())
      messages.addAll(memory);
    messages.add(userMessage);
    return glm.commonChat(question, messages, true);
  }
}
