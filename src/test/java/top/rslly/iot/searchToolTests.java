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
package top.rslly.iot;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import top.rslly.iot.utility.ai.tools.SearchTool;
import top.rslly.iot.utility.ai.voice.TTS.TtsServiceFactory;

import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class searchToolTests {
  @Autowired
  private SearchTool searchTool;
  @Autowired
  private TtsServiceFactory ttsServiceFactory;

  @Test
  public void searchTool() {
    searchTool.run("中国队奥运会拿了多少奖牌,详细介绍每块金牌的故事");
  }

  @Test
  public void testTTsDelay() {
    long currentTime = System.currentTimeMillis();
    String text = "我们会将个人数据用于以下目的：\n" +
        "\n" +
        "用于提供、分析和维护我们的服务，例如回应您向 ChatGPT 提出的问题；\n" +
        "用于改进和开发我们的服务，并开展研究，例如开发新功能；\n" +
        "用于在我们的各项服务中个性化和定制您的使用体验，例如向您提供更相关的内容；\n" +
        "用于与您沟通，包括回复您的问题，以及向您发送有关我们服务和活动的信息，例如服务的变更或改进情况；\n" +
        "当您选择连接联系人时，用于识别其中正在使用我们服务的联系人，并在其日后加入我们的服务时通知您；\n" +
        "用于防止欺诈、非法活动或滥用我们的服务，并保护我们系统和服务的安全，包括监控在我们平台上提交或交换的内容（详见此处\u2060）；以及\n" +
        "用于履行法律义务，并保护我们的用户、OpenAI 或第三方的权利、隐私、安全或财";
    List<byte[]> audioBytes = ttsServiceFactory.getTextAudio("123", text, 1);
    System.out.println((System.currentTimeMillis() - currentTime) / 1000);
  }
}
