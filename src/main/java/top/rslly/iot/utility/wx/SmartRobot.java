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
package top.rslly.iot.utility.wx;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import top.rslly.iot.services.WxProductActiveServiceImpl;
import top.rslly.iot.services.WxProductBindServiceImpl;
import top.rslly.iot.services.WxUserServiceImpl;
import top.rslly.iot.utility.Cast;
import top.rslly.iot.utility.RedisUtil;
import top.rslly.iot.utility.ai.ModelMessage;
import top.rslly.iot.utility.ai.ModelMessageRole;
import top.rslly.iot.utility.ai.chain.Router;
import top.rslly.iot.utility.ai.llm.Glm;
import top.rslly.iot.utility.ai.voice.DashScopeVoice;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class SmartRobot {
  @Autowired
  private DealWx dealWx;
  @Autowired
  private Router router;
  @Autowired
  private WxUserServiceImpl wxUserService;
  @Autowired
  private WxProductActiveServiceImpl productActiveService;
  @Autowired
  private WxProductBindServiceImpl wxProductBindService;
  @Autowired
  private RedisUtil redisUtil;

  @Async("taskExecutor")
  public void smartSendContent(String openid, String msg, String microappid) throws IOException {
    if (wxUserService.findAllByOpenid(openid).isEmpty()) {
      dealWx.sendContent(openid, "该用户未注册，请先注册再使用", microappid);
      return;
    }
    var productActiveEntities = productActiveService.findAllByOpenid(openid);
    int productId = 0;
    if (!productActiveEntities.isEmpty()) {
      productId = productActiveEntities.get(0).getProductId();
    } else {
      var wxProductBindEntities = wxProductBindService.findAllByOpenid(openid);
      if (!wxProductBindEntities.isEmpty()) {
        productId = wxProductBindEntities.get(0).getProductId();
      }
    }
    String content = router.response(msg, openid, productId, microappid);
    if (content.length() > 800) {
      dealWx.sendContent(openid, content.substring(0, 800), microappid);
      dealWx.sendContent(openid, content.substring(800), microappid);
    } else
      dealWx.sendContent(openid, content, microappid);
  }

  @Async("taskExecutor")
  public void smartImageSendContent(String openid, String imageUrl, String microappid)
      throws IOException {
    if (wxUserService.findAllByOpenid(openid).isEmpty()) {
      dealWx.sendContent(openid, "该用户未注册，请先注册再使用", microappid);
      return;
    }
    List<ModelMessage> memory;
    String content = Glm.testImageToWord(imageUrl);
    var memory_cache = redisUtil.get("memory" + openid);
    if (memory_cache != null)
      try {
        memory = Cast.castList(memory_cache, ModelMessage.class);
      } catch (Exception e) {
        e.printStackTrace();
        memory = new ArrayList<>();
      }
    else
      memory = new ArrayList<>();
    ModelMessage chatMessage = new ModelMessage(ModelMessageRole.ASSISTANT.value(), "图里有什么");
    ModelMessage userContent = new ModelMessage(ModelMessageRole.USER.value(), content);
    memory.add(userContent);
    memory.add(chatMessage);
    if (memory.size() > 6) {
      memory.subList(0, memory.size() - 6).clear();
    }
    redisUtil.set("memory" + openid, memory, 24 * 3600);
    dealWx.sendContent(openid, content, microappid);
  }

  @Async("taskExecutor")
  public void dealVoice(String openid, String url, String microappid) throws IOException {
    String result = DashScopeVoice.simpleMultiModalConversationCall(url);
    smartSendContent(openid, result, microappid);
  }
}
