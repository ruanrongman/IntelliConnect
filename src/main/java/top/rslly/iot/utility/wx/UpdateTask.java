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

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import top.rslly.iot.utility.RedisUtil;

import java.io.IOException;

@Component
public class UpdateTask {

  @Value("${wx.appid}")
  private String appid;
  @Value("${wx.appsecret}")
  private String appsecret;
  @Value("${wx.micro.appid}")
  private String microappid;
  @Value("${wx.micro.appsecret}")
  private String microappsecret;
  @Value("${wx.micro.appid2}")
  private String microappid2;
  @Value("${wx.micro.appsecret2}")
  private String microappsecret2;
  @Value("${wx.debug}")
  private String debug;
  @Autowired
  private DealWx dealWx;
  @Autowired
  private RedisUtil redisUtil;
  private static final Logger LOG = LoggerFactory.getLogger(UpdateTask.class);

  @Scheduled(initialDelay = 1000, fixedDelay = 1000 * 3600)
  public void task() {
    // Timer timer = new Timer();
    // timer.schedule(new TimerTask() {
    // public void run() {
    try {
      LOG.info(debug);
      if (debug.equals("false")) {
        String token =
            (String) JSON.parseObject(dealWx.getAccessToken(appid, appsecret)).get("access_token");
        redisUtil.set(appid, token, 7200);
        String microtoken = (String) JSON
            .parseObject(dealWx.getAccessToken(microappid, microappsecret)).get("access_token");
        redisUtil.set(microappid, microtoken, 7200);
        String microtoken2 = (String) JSON
            .parseObject(dealWx.getAccessToken(microappid2, microappsecret2)).get("access_token");
        redisUtil.set(microappid2, microtoken2, 7200);
        // System.out.println(token);
        LOG.info("====获取到测试号token为" + token + "====");
        LOG.info("====获取到操作小程序token为" + microtoken + "====");
        LOG.info("====获取到宣传号小程序token为" + microtoken2 + "====");
      } else
        LOG.info("调试模式，不获取token");
    } catch (IOException e) {
      e.printStackTrace();
    }
    // System.out.println("11232");
  }
  // }, 1000 , 1000*3600);
}

