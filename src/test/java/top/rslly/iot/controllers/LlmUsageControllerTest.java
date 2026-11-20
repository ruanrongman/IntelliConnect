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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import top.rslly.iot.param.request.LlmUsageTrendQuery;
import top.rslly.iot.param.response.LlmUsageTrendResponse;
import top.rslly.iot.services.agent.LlmUsageTrendService;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

class LlmUsageControllerTest {
  private LlmUsageTrendService service;
  private MockMvc mvc;

  @BeforeEach
  void setup() {
    service = mock(LlmUsageTrendService.class);
    var controller = new LlmUsageController();
    ReflectionTestUtils.setField(controller, "trendService", service);
    mvc = MockMvcBuilders.standaloneSetup(controller).build();
  }

  @Test
  void defaultsToWeekAndSerializesAllMetrics() throws Exception {
    doReturn(ResultTool.success(new LlmUsageTrendResponse("week", "Asia/Shanghai", "2026-11-11",
        "2026-11-17", List.of(new LlmUsageTrendResponse.Point("2026-11-17", 1200, 1000,
            200, 400, 100, 0.4, 3, 2)))))
                .when(service).getTrend(eq("Bearer token"), any());

    mvc.perform(get("/api/v2/llm/usage/trend").header("Authorization", "Bearer token"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.timezone").value("Asia/Shanghai"))
        .andExpect(jsonPath("$.data.points[0].totalTokens").value(1200))
        .andExpect(jsonPath("$.data.points[0].promptTokens").value(1000))
        .andExpect(jsonPath("$.data.points[0].completionTokens").value(200))
        .andExpect(jsonPath("$.data.points[0].cachedTokens").value(400))
        .andExpect(jsonPath("$.data.points[0].cacheCreationTokens").value(100))
        .andExpect(jsonPath("$.data.points[0].cacheHitRate").value(0.4));
    var query = ArgumentCaptor.forClass(LlmUsageTrendQuery.class);
    verify(service).getTrend(eq("Bearer token"), query.capture());
    org.junit.jupiter.api.Assertions.assertEquals("week", query.getValue().getPeriod());
    org.junit.jupiter.api.Assertions.assertNull(query.getValue().getProductId());
  }

  @Test
  void passesProductAndPeriodAndPreservesPermissionFailure() throws Exception {
    doReturn(ResultTool.fail(ResultCode.NO_PERMISSION)).when(service).getTrend(anyString(), any());

    mvc.perform(get("/api/v2/llm/usage/trend").header("Authorization", "token")
        .param("period", "day").param("productId", "7"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.errorCode").value(3001));
    verify(service).getTrend(eq("token"), argThat(query -> "day".equals(query.getPeriod())
        && Integer.valueOf(7).equals(query.getProductId())));
  }

  @Test
  void rejectsInvalidPeriodsAndProductIdsBeforeQuerying() throws Exception {
    for (String period : List.of("", "year", "DAY")) {
      mvc.perform(get("/api/v2/llm/usage/trend").header("Authorization", "token")
          .param("period", period)).andExpect(status().isBadRequest());
    }
    for (String id : List.of("0", "-1", "abc")) {
      mvc.perform(get("/api/v2/llm/usage/trend").header("Authorization", "token")
          .param("productId", id)).andExpect(status().isBadRequest());
    }
    verifyNoInteractions(service);
  }

  @Test
  void requiresAuthorizationHeader() throws Exception {
    mvc.perform(get("/api/v2/llm/usage/trend")).andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }
}
