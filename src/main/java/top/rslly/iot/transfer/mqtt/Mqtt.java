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
package top.rslly.iot.transfer.mqtt;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class Mqtt implements ApplicationRunner {


  private String Host;
  private String Username;
  private String Password;

  @Value("${mqtt.Host}")
  public void setHost(String host) {
    this.Host = host;
    log.info("用户名{}", host);
  }

  @Value("${mqtt.username}")
  public void setUsername(String username) {
    this.Username = username;
    log.info("用户名{}", username);
  }

  @Value("${mqtt.password}")
  public void setPassword(String password) {
    this.Password = password;
    log.info("密码{}", password);
  }

  @Override
  @Order(1)
  public void run(ApplicationArguments args) throws Exception {
    MqttConnectionUtils.start(Host, Username, Password);
  }
}
