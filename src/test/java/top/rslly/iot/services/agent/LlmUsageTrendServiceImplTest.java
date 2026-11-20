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
package top.rslly.iot.services.agent;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import top.rslly.iot.dao.*;
import top.rslly.iot.models.*;
import top.rslly.iot.param.request.LlmUsageTrendQuery;
import top.rslly.iot.param.response.LlmUsageTrendResponse;
import top.rslly.iot.utility.JwtTokenUtil;
import top.rslly.iot.utility.result.JsonResult;

@ExtendWith(MockitoExtension.class)
class LlmUsageTrendServiceImplTest {
  @Mock
  private LlmTurnUsageRepository usageRepository;
  @Mock
  private ProductRepository productRepository;
  @Mock
  private UserRepository userRepository;
  @Mock
  private UserProductBindRepository userProductBindRepository;
  @Mock
  private WxUserRepository wxUserRepository;
  @Mock
  private WxProductBindRepository wxProductBindRepository;
  @InjectMocks
  private LlmUsageTrendServiceImpl service;

  @BeforeEach
  void setup() {
    setTime("2026-11-16T16:30:00Z"); // November 17 in Beijing, still November 16 in UTC.
  }

  @Test
  void defaultsToAllBoundProductsAndKeepsCacheInsideInputTotal() {
    try (var jwt = mockStatic(JwtTokenUtil.class)) {
      jwt.when(() -> JwtTokenUtil.getUserRole("token")).thenReturn("ROLE_user");
      jwt.when(() -> JwtTokenUtil.getUsername("token")).thenReturn("alice");
      var user = new UserEntity();
      user.setId(7);
      when(userRepository.findAllByUsername("alice")).thenReturn(List.of(user));
      when(userProductBindRepository.findAllByUserId(7))
          .thenReturn(List.of(binding(1), binding(2), binding(1)));
      when(productRepository.findAllByIdIn(List.of(1, 2)))
          .thenReturn(List.of(product(1), product(2)));
      when(usageRepository.aggregateTrend(eq(List.of(1, 2)), anyLong(), anyLong(), eq(86_400_000L)))
          .thenReturn(List.of(bucket(6, 1000, 60, 100, 200, 3, 2)));

      var response = data(service.getTrend("Bearer token", query("week", null)));

      assertEquals("Asia/Shanghai", response.timezone());
      assertEquals("2026-11-11", response.startDate());
      assertEquals("2026-11-17", response.endDate());
      assertEquals(7, response.points().size());
      assertEquals(0, response.points().getFirst().totalTokens());
      var point = response.points().getLast();
      assertEquals(1060, point.totalTokens());
      assertEquals(1000, point.promptTokens());
      assertEquals(60, point.completionTokens());
      assertEquals(100, point.cachedTokens());
      assertEquals(200, point.cacheCreationTokens());
      assertEquals(0.1, point.cacheHitRate(), 0.000001);
      assertEquals(3, point.calls());
      assertEquals(2, point.reportedCalls());
      verify(usageRepository).aggregateTrend(List.of(1, 2),
          Instant.parse("2026-11-10T16:00:00Z").toEpochMilli(),
          Instant.parse("2026-11-16T16:30:00Z").toEpochMilli() + 1, 86_400_000L);
      verify(productRepository, never()).findAll();
    }
  }

  @Test
  void wxUsersOnlyAggregateTheirBoundProducts() {
    try (var jwt = mockStatic(JwtTokenUtil.class)) {
      jwt.when(() -> JwtTokenUtil.getUserRole("token")).thenReturn("ROLE_wx_user");
      jwt.when(() -> JwtTokenUtil.getUsername("token")).thenReturn("wechat");
      var user = new WxUserEntity();
      user.setAppid("app");
      user.setOpenid("open");
      var bind = new WxProductBindEntity();
      bind.setProductId(8);
      when(wxUserRepository.findAllByName("wechat")).thenReturn(List.of(user));
      when(wxProductBindRepository.findAllByAppidAndOpenid("app", "open"))
          .thenReturn(List.of(bind));
      when(productRepository.findAllByIdIn(List.of(8))).thenReturn(List.of(product(8)));

      var response = data(service.getTrend("token", query("week", 8)));

      assertEquals(7, response.points().size());
      verify(usageRepository).aggregateTrend(eq(List.of(8)), anyLong(), anyLong(), eq(86_400_000L));
      verifyNoInteractions(userRepository, userProductBindRepository);
    }
  }

  @Test
  void adminsCanAggregateAllExistingProductsOrSelectOne() {
    try (var jwt = mockStatic(JwtTokenUtil.class)) {
      jwt.when(() -> JwtTokenUtil.getUserRole("token")).thenReturn("[ROLE_admin]");
      when(productRepository.findAll()).thenReturn(List.of(product(1), product(8)));

      data(service.getTrend("token", query("week", null)));
      data(service.getTrend("token", query("week", 8)));

      verify(usageRepository).aggregateTrend(eq(List.of(1, 8)), anyLong(), anyLong(), anyLong());
      verify(usageRepository).aggregateTrend(eq(List.of(8)), anyLong(), anyLong(), anyLong());
      verifyNoInteractions(userRepository, wxUserRepository, userProductBindRepository,
          wxProductBindRepository);
    }
  }

  @Test
  void rejectsOtherProductsBeforeReadingUsage() {
    try (var jwt = mockStatic(JwtTokenUtil.class)) {
      jwt.when(() -> JwtTokenUtil.getUserRole("token")).thenReturn("ROLE_user");
      jwt.when(() -> JwtTokenUtil.getUsername("token")).thenReturn("alice");
      var user = new UserEntity();
      user.setId(7);
      when(userRepository.findAllByUsername("alice")).thenReturn(List.of(user));
      when(userProductBindRepository.findAllByUserId(7)).thenReturn(List.of(binding(1)));
      when(productRepository.findAllByIdIn(List.of(1))).thenReturn(List.of(product(1)));

      var response = service.getTrend("token", query("week", 2));

      assertFalse(response.getSuccess());
      assertEquals(3001, response.getErrorCode());
      verifyNoInteractions(usageRepository);
    }
  }

  @Test
  void userWithoutBindingsGetsZeroSeriesWithoutAnUnscopedQuery() {
    try (var jwt = mockStatic(JwtTokenUtil.class)) {
      jwt.when(() -> JwtTokenUtil.getUserRole("token")).thenReturn("ROLE_user");
      jwt.when(() -> JwtTokenUtil.getUsername("token")).thenReturn("alice");
      var user = new UserEntity();
      user.setId(7);
      when(userRepository.findAllByUsername("alice")).thenReturn(List.of(user));

      var response = data(service.getTrend("token", query("month", null)));

      assertEquals(30, response.points().size());
      assertTrue(response.points().stream().allMatch(point -> point.totalTokens() == 0
          && point.cacheHitRate() == 0 && point.calls() == 0));
      verifyNoInteractions(usageRepository, productRepository);
    }
  }

  @ParameterizedTest
  @CsvSource({"ROLE_user,alice", "ROLE_wx_user,wechat"})
  void deletedOrMissingUserCannotReadUsage(String role, String name) {
    try (var jwt = mockStatic(JwtTokenUtil.class)) {
      jwt.when(() -> JwtTokenUtil.getUserRole("token")).thenReturn(role);
      jwt.when(() -> JwtTokenUtil.getUsername("token")).thenReturn(name);

      assertEquals(3001, service.getTrend("token", query("week", null)).getErrorCode());
      verifyNoInteractions(usageRepository, productRepository);
    }
  }

  @ParameterizedTest
  @CsvSource({
      "week,2027-01-01T16:00:00Z,2026-12-27,2027-01-02,7",
      "month,2024-03-01T00:00:00Z,2024-02-01,2024-03-01,30"
  })
  void fillsCompleteCalendarBucketsAcrossBoundaries(String period, String instant,
      String start, String end, int count) {
    setTime(instant);
    try (var jwt = mockStatic(JwtTokenUtil.class)) {
      jwt.when(() -> JwtTokenUtil.getUserRole("token")).thenReturn("[ROLE_admin]");
      when(productRepository.findAll()).thenReturn(List.of(product(1)));

      var response = data(service.getTrend("token", query(period, null)));

      assertEquals(start, response.startDate());
      assertEquals(end, response.endDate());
      assertEquals(count, response.points().size());
      assertEquals(start, response.points().getFirst().date());
      assertEquals(end, response.points().getLast().date());
      verify(usageRepository).aggregateTrend(eq(List.of(1)), anyLong(), anyLong(), eq(86_400_000L));
    }
  }

  @Test
  void dayViewUsesBeijingMidnightAndHourlyBucketsWithGapsFilled() {
    setTime("2026-11-16T18:30:00Z"); // November 17, 02:30 in Beijing.
    try (var jwt = mockStatic(JwtTokenUtil.class)) {
      jwt.when(() -> JwtTokenUtil.getUserRole("token")).thenReturn("[ROLE_admin]");
      when(productRepository.findAll()).thenReturn(List.of(product(1)));
      when(usageRepository.aggregateTrend(anyList(), anyLong(), anyLong(), eq(3_600_000L)))
          .thenReturn(List.of(
              bucket(0, 1000, 100, 100, 30, 3, 2),
              bucket(2, 200, 10, 20, 0, 1, 1)));

      var response = data(service.getTrend("token", query("day", null)));
      var points = response.points();

      assertEquals("2026-11-17", response.startDate());
      assertEquals("2026-11-17", response.endDate());
      assertEquals(3, points.size());
      var midnight = points.getFirst();
      assertEquals("2026-11-17 00:00", midnight.date());
      assertEquals(1100, midnight.totalTokens());
      assertEquals(1000, midnight.promptTokens());
      assertEquals(100, midnight.completionTokens());
      assertEquals(100, midnight.cachedTokens());
      assertEquals(30, midnight.cacheCreationTokens());
      assertEquals(0.1, midnight.cacheHitRate(), 0.000001);
      assertEquals(3, midnight.calls());
      assertEquals(2, midnight.reportedCalls());
      assertEquals("2026-11-17 01:00", points.get(1).date());
      assertEquals(0, points.get(1).totalTokens());
      assertEquals("2026-11-17 02:00", points.getLast().date());
      assertEquals(210, points.getLast().totalTokens());
      verify(usageRepository).aggregateTrend(List.of(1),
          Instant.parse("2026-11-16T16:00:00Z").toEpochMilli(),
          Instant.parse("2026-11-16T18:30:00Z").toEpochMilli() + 1, 3_600_000L);
    }
  }

  @ParameterizedTest
  @CsvSource({"2026-12-31T16:00:00Z,2027-01-01,1", "2026-11-17T15:59:59Z,2026-11-17,24"})
  void dayViewStopsAtCurrentHourAcrossMidnight(String instant, String date, int count) {
    setTime(instant);
    try (var jwt = mockStatic(JwtTokenUtil.class)) {
      jwt.when(() -> JwtTokenUtil.getUserRole("token")).thenReturn("[ROLE_admin]");
      var response = data(service.getTrend("token", query("day", null)));
      assertEquals(date, response.startDate());
      assertEquals(count, response.points().size());
      assertEquals(date + " 00:00", response.points().getFirst().date());
      verifyNoInteractions(usageRepository);
    }
  }

  @Test
  void zeroInputAndMissingUsageNeverProduceEstimatedTokensOrInvalidRates() {
    try (var jwt = mockStatic(JwtTokenUtil.class)) {
      jwt.when(() -> JwtTokenUtil.getUserRole("token")).thenReturn("[ROLE_admin]");
      when(productRepository.findAll()).thenReturn(List.of(product(1)));
      when(usageRepository.aggregateTrend(anyList(), anyLong(), anyLong(), anyLong()))
          .thenReturn(List.of(bucket(6, 0, 0, 0, 0, 4, 0)));

      var point = data(service.getTrend("token", query("week", null))).points().getLast();

      assertEquals(0, point.totalTokens());
      assertEquals(0, point.cacheHitRate());
      assertEquals(4, point.calls());
      assertEquals(0, point.reportedCalls());
    }
  }

  @Test
  void databaseFailureIsNotReportedAsZeroUsage() {
    try (var jwt = mockStatic(JwtTokenUtil.class)) {
      jwt.when(() -> JwtTokenUtil.getUserRole("token")).thenReturn("[ROLE_admin]");
      when(productRepository.findAll()).thenReturn(List.of(product(1)));
      when(usageRepository.aggregateTrend(anyList(), anyLong(), anyLong(), anyLong()))
          .thenThrow(new org.springframework.dao.DataAccessResourceFailureException("unavailable"));

      var result = service.getTrend("token", query("week", null));

      assertFalse(result.getSuccess());
      assertNull(result.getData());
      assertTrue(result.getErrorMsg().contains("查询失败"));
    }
  }

  @Test
  void invalidParametersNeverReachRepositories() {
    for (var query : List.of(query("invalid", null), query("year", null), query("", null),
        query(null, null),
        query("week", 0), query("week", -1))) {
      assertEquals(1001, service.getTrend("token", query).getErrorCode());
    }
    verifyNoInteractions(usageRepository, productRepository, userRepository, wxUserRepository);
  }

  private void setTime(String instant) {
    ReflectionTestUtils.setField(service, "clock",
        Clock.fixed(Instant.parse(instant), ZoneOffset.UTC));
  }

  private LlmUsageTrendQuery query(String period, Integer productId) {
    var query = new LlmUsageTrendQuery();
    query.setPeriod(period);
    query.setProductId(productId);
    return query;
  }

  private LlmUsageTrendResponse data(JsonResult<?> result) {
    assertTrue(result.getSuccess(), result.getErrorMsg());
    return (LlmUsageTrendResponse) result.getData();
  }

  private ProductEntity product(int id) {
    var product = new ProductEntity();
    product.setId(id);
    return product;
  }

  private UserProductBindEntity binding(int productId) {
    var bind = new UserProductBindEntity();
    bind.setProductId(productId);
    return bind;
  }

  private LlmTurnUsageRepository.TrendBucket bucket(long index, long input, long output,
      long cached, long creation, long calls, long reported) {
    return new LlmTurnUsageRepository.TrendBucket() {
      public long getBucketIndex() {
        return index;
      }

      public long getPromptTokens() {
        return input;
      }

      public long getCompletionTokens() {
        return output;
      }

      public long getCachedTokens() {
        return cached;
      }

      public long getCacheCreationTokens() {
        return creation;
      }

      public long getCalls() {
        return calls;
      }

      public long getReportedCalls() {
        return reported;
      }
    };
  }
}
