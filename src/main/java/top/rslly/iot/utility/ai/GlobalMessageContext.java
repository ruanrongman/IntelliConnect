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

import java.util.Map;

public final class GlobalMessageContext {
  public static final String PRODUCT_ID = "productId";
  public static final String CHAT_ID = "chatId";
  public static final String CONVERSATION_CHAT_ID = "conversationChatId";
  public static final String QUEUE_MAP = "queueMap";
  public static final String MEMORY = "memory";

  private GlobalMessageContext() {}

  public static void putChatIds(Map<String, Object> globalMessage, String streamChatId,
      String conversationChatId) {
    globalMessage.put(CHAT_ID, streamChatId);
    globalMessage.put(CONVERSATION_CHAT_ID,
        isBlank(conversationChatId) ? streamChatId : conversationChatId);
  }

  public static String chatId(Map<String, Object> globalMessage) {
    return stringValue(globalMessage, CHAT_ID);
  }

  public static String memoryChatId(Map<String, Object> globalMessage) {
    String conversationChatId = stringValue(globalMessage, CONVERSATION_CHAT_ID);
    if (!isBlank(conversationChatId)) {
      return conversationChatId;
    }
    return chatId(globalMessage);
  }

  private static String stringValue(Map<String, Object> globalMessage, String key) {
    if (globalMessage == null) {
      return null;
    }
    Object value = globalMessage.get(key);
    return value == null ? null : value.toString();
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
