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

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.rslly.iot.utility.HttpRequestUtils;
import top.rslly.iot.utility.RedisUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Component
@Slf4j
public class DealWx {
  @Autowired
  private HttpRequestUtils httpRequestUtils;
  @Autowired
  private RedisUtil redisUtil;
  @Value("${wx.appid}")
  private String appid;
  @Value("${wx.templateUrl}")
  private String projectUrl;

  public String getAccessToken(String appid, String appSecret) throws IOException {
    String url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid="
        + appid + "&secret=" + appSecret;
    Response s = httpRequestUtils.httpGet(url);
    return Objects.requireNonNull(s.body()).string();
  }

  public String getOpenid(String code, String microappid, String microappsecret)
      throws IOException {
    String url = "https://api.weixin.qq.com/sns/jscode2session?appid=" + microappid + "&secret="
        + microappsecret + "&js_code=" + code + "&grant_type=authorization_code";
    return Objects.requireNonNull(httpRequestUtils.httpGet(url).body()).string();
  }

  public void templatePost(JSONObject reqdata, String templateid, String openid) {
    String accessToken = (String) redisUtil.get(appid);
    String url =
        "https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=" + accessToken;
    // String reqBody = "{\"touser\":\"" + openid + "\", \"template_id\":\"" + templateid + "\",
    // \"url\":\"" + fxurl + "\", \"data\": " + reqdata + "}";
    // 优化reqbody, 不用拼接
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("touser", openid);
    jsonObject.put("template_id", templateid);
    jsonObject.put("url", projectUrl);
    jsonObject.put("data", reqdata);
    try {
      // log.info("发送模板消息{}", jsonObject.toJSONString());
      httpRequestUtils.asyncPostByJson(url, jsonObject.toJSONString());
    } catch (IOException e) {
      log.error("发送模板消息失败{}", e.getMessage());
    }
  }

  public void sendContent(String openid, String content, String microappid) throws IOException {
    String token = (String) redisUtil.get(microappid);
    String url = "https://api.weixin.qq.com/cgi-bin/message/custom/send?access_token=" + token;
    // String jsonstr = "{\n" + " \"touser\":\"" + openid + "\",\n" + " \"msgtype\":\"text\",\n"
    // + " \"text\":\n" + " {\n" + " \"content\":\"" + content + "\"\n" + " }\n" + "}";
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("touser", openid);
    jsonObject.put("msgtype", "text");
    Map<String, String> textMap = new HashMap<>();
    textMap.put("content", content);
    jsonObject.put("text", textMap);
    httpRequestUtils.asyncPostByJson(url, jsonObject.toJSONString());
  }

  public String getMedia(String mediaId, String microappid) throws IOException {
    log.info(mediaId);
    String token = (String) redisUtil.get(microappid);
    String url = "https://api.weixin.qq.com/cgi-bin/media/get?access_token=" + token + "&media_id="
        + mediaId;
    log.info(url);
    return url;
    // Response response=httpRequestUtils.httpGet(url);
    // System.out.println(response.body());
  }



}
