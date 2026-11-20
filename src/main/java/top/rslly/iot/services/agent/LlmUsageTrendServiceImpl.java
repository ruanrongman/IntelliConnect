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

import jakarta.annotation.Resource;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.rslly.iot.dao.LlmTurnUsageRepository;
import top.rslly.iot.dao.ProductRepository;
import top.rslly.iot.dao.UserProductBindRepository;
import top.rslly.iot.dao.UserRepository;
import top.rslly.iot.dao.WxProductBindRepository;
import top.rslly.iot.dao.WxUserRepository;
import top.rslly.iot.models.ProductEntity;
import top.rslly.iot.models.UserProductBindEntity;
import top.rslly.iot.models.WxProductBindEntity;
import top.rslly.iot.param.request.LlmUsageTrendQuery;
import top.rslly.iot.param.response.LlmUsageTrendResponse;
import top.rslly.iot.param.response.LlmUsageTrendResponse.Point;
import top.rslly.iot.utility.JwtTokenUtil;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

@Service
@Slf4j
public class LlmUsageTrendServiceImpl implements LlmUsageTrendService {
  private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
  private Clock clock = Clock.systemUTC();

  @Resource
  private LlmTurnUsageRepository usageRepository;
  @Resource
  private ProductRepository productRepository;
  @Resource
  private UserRepository userRepository;
  @Resource
  private UserProductBindRepository userProductBindRepository;
  @Resource
  private WxUserRepository wxUserRepository;
  @Resource
  private WxProductBindRepository wxProductBindRepository;

  @Override
  public JsonResult<?> getTrend(String token, LlmUsageTrendQuery query) {
    String period = query.getPeriod();
    Integer productId = query.getProductId();
    if (period == null || !List.of("day", "week", "month").contains(period)
        || (productId != null && productId <= 0)) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    try {
      String tokenDeal = token.replace(JwtTokenUtil.TOKEN_PREFIX, "");
      String role = JwtTokenUtil.getUserRole(tokenDeal);
      String username = JwtTokenUtil.getUsername(tokenDeal);
      List<Integer> productIds;
      if ("ROLE_wx_user".equals(role)) {
        var users = wxUserRepository.findAllByName(username);
        if (users.isEmpty()) {
          return ResultTool.fail(ResultCode.NO_PERMISSION);
        }
        var user = users.getFirst();
        productIds =
            wxProductBindRepository.findAllByAppidAndOpenid(user.getAppid(), user.getOpenid())
                .stream().map(WxProductBindEntity::getProductId).distinct().toList();
      } else if (!"[ROLE_admin]".equals(role)) {
        var users = userRepository.findAllByUsername(username);
        if (users.isEmpty()) {
          return ResultTool.fail(ResultCode.NO_PERMISSION);
        }
        productIds = userProductBindRepository.findAllByUserId(users.getFirst().getId())
            .stream().map(UserProductBindEntity::getProductId).distinct().toList();
      } else {
        productIds = productRepository.findAll().stream().map(ProductEntity::getId).toList();
      }
      if (!"[ROLE_admin]".equals(role) && !productIds.isEmpty()) {
        productIds = productRepository.findAllByIdIn(productIds).stream()
            .map(ProductEntity::getId).toList();
      }
      if (productId != null) {
        if (!productIds.contains(productId)) {
          return ResultTool.fail(ResultCode.NO_PERMISSION);
        }
        productIds = List.of(productId);
      }

      boolean hourly = "day".equals(period);
      var now = clock.instant();
      LocalDate today = now.atZone(ZONE).toLocalDate();
      LocalDate start = switch (period) {
        case "day" -> today;
        case "month" -> today.minusDays(29);
        default -> today.minusDays(6);
      };
      Duration bucketWidth = hourly ? Duration.ofHours(1) : Duration.ofDays(1);
      DateTimeFormatter labelFormat =
          DateTimeFormatter.ofPattern(hourly ? "yyyy-MM-dd HH:mm" : "yyyy-MM-dd");
      Map<Long, Point> buckets = new HashMap<>();
      if (!productIds.isEmpty()) {
        // Beijing has fixed 24-hour days for the supported recent ranges. All calendar and
        // zone decisions stay here; the repository only groups offsets from this origin.
        var usageBuckets = usageRepository.aggregateTrend(productIds,
            start.atStartOfDay(ZONE).toInstant().toEpochMilli(), now.toEpochMilli() + 1,
            bucketWidth.toMillis());
        for (var bucket : usageBuckets) {
          String label =
              start.atStartOfDay().plus(bucketWidth.multipliedBy(bucket.getBucketIndex()))
                  .format(labelFormat);
          long prompt = bucket.getPromptTokens();
          long completion = bucket.getCompletionTokens();
          long cached = bucket.getCachedTokens();
          buckets.put(bucket.getBucketIndex(),
              new Point(label, prompt + completion, prompt, completion, cached,
                  bucket.getCacheCreationTokens(),
                  prompt == 0 ? 0 : (double) cached / prompt,
                  bucket.getCalls(), bucket.getReportedCalls()));
        }
      }
      List<Point> points = new ArrayList<>();
      int bucketCount = hourly ? now.atZone(ZONE).getHour() + 1 : "month".equals(period) ? 30 : 7;
      for (long index = 0; index < bucketCount; index++) {
        String label =
            start.atStartOfDay().plus(bucketWidth.multipliedBy(index)).format(labelFormat);
        points.add(buckets.getOrDefault(index, new Point(label, 0, 0, 0, 0, 0, 0, 0, 0)));
      }
      return ResultTool.success(new LlmUsageTrendResponse(period, ZONE.getId(), start.toString(),
          today.toString(), points));
    } catch (RuntimeException e) {
      log.warn("load token usage trend failed, period={}, productId={}", period, productId, e);
      var result = ResultTool.fail(ResultCode.COMMON_FAIL);
      result.setErrorMsg("Token 用量查询失败，请稍后重试");
      return result;
    }
  }
}
