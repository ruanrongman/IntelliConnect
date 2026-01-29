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
package top.rslly.iot.utility.script.js;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.rslly.iot.param.request.ControlParam;
import top.rslly.iot.services.iot.HardWareServiceImpl;

import jakarta.annotation.PostConstruct;
import java.util.List;

@Component
public class JsUtils {
  private static JsUtils imp1;
  @Autowired
  private HardWareServiceImpl hardWareService;

  @PostConstruct
  public void init() {
    imp1 = this;
    imp1.hardWareService = this.hardWareService;
  }

  public static boolean control(String name, String mode, String functionName, String status,
      int qos, List<String> key,
      List<String> value) {
    ControlParam controlParam = new ControlParam(name, mode, functionName, status, qos, key, value);
    try {
      var res = imp1.hardWareService.control(controlParam).getErrorCode();
      return res == 200;
    } catch (MqttException e) {
      return false;
    }
  }

  public static void timeSleep(int second) throws InterruptedException {
    Thread.sleep(second * 1000L);
  }
}
