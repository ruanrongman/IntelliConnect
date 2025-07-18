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
package top.rslly.iot.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import top.rslly.iot.services.agent.OtaXiaozhiServiceImpl;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@RestController
@Slf4j
public class XiaoZhi {
  @Autowired
  private OtaXiaozhiServiceImpl otaXiaozhiService;

  @RequestMapping(value = "/xiaozhi/ota", method = RequestMethod.POST,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public String ota(HttpServletRequest httpServletRequest) {
    return otaXiaozhiService.otaEnable(httpServletRequest);
  }

  @RequestMapping(value = "/xiaozhi/ota/activate", method = RequestMethod.POST)
  public void otaActivate(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    otaXiaozhiService.otaActivate(request, response);
  }


}
