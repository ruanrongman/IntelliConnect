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
package top.rslly.iot.utility.ai.voice;

import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import com.alibaba.dashscope.utils.Constants;
import com.zhipu.oapi.ClientV4;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.rslly.iot.utility.ai.Prompt;

import java.util.Arrays;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class DashScopeVoice {
  @Autowired
  private Prompt prompt;
  private static MultiModalConversation conv;
  private static final Pattern pattern = Pattern.compile("\"(.*?)\"");

  @Value("${ai.dashscope-key}")
  public void setApiKey(String apiKey) {
    // 填写自己的api key
    Constants.apiKey = apiKey;
    conv = new MultiModalConversation();
  }

  public static String simpleMultiModalConversationCall(String url) {
    if (url.equals(""))
      return "";
    MultiModalMessage userMessage = MultiModalMessage.builder().role(Role.USER.getValue())
        .content(Arrays.asList(Collections.singletonMap("audio", url),
            Collections.singletonMap("text", "这段音频在说什么?")))
        .build();
    MultiModalConversationParam param = MultiModalConversationParam.builder()
        .model("qwen-audio-turbo")
        .message(userMessage)
        .build();
    try {
      MultiModalConversationResult result = conv.call(param);
      log.info("语音转换结果{}", result);
      Matcher matcher = pattern.matcher(result.getOutput().getChoices().get(0).getMessage()
          .getContent().get(0).get("text").toString());

      if (matcher.find()) {
        // group(1) 表示第一个括号匹配到的内容
        return matcher.group(1);
      } else {
        return result.getOutput().getChoices().get(0).getMessage().getContent().get(0).get("text")
            .toString();
      }

    } catch (Exception e) {
      log.error("语音转换失败{}", e.getMessage());
      return "语音转换失败";
    }

  }

}
