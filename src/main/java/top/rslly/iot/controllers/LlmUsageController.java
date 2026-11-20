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

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.rslly.iot.param.request.LlmUsageTrendQuery;
import top.rslly.iot.services.agent.LlmUsageTrendService;
import top.rslly.iot.utility.result.JsonResult;

@RestController
@RequestMapping("/api/v2/llm/usage")
public class LlmUsageController {
  @Autowired
  private LlmUsageTrendService trendService;

  @Operation(summary = "Token 使用趋势", description = "默认汇总当前用户全部可访问产品，按北京时间统计今天（按小时）、最近7天或30天")
  @GetMapping("/trend")
  public JsonResult<?> trend(@RequestHeader("Authorization") String token,
      @Valid @ModelAttribute LlmUsageTrendQuery query) {
    return trendService.getTrend(token, query);
  }
}
