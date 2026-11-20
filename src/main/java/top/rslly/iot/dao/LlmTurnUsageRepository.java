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
package top.rslly.iot.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import top.rslly.iot.models.LlmTurnUsageEntity;

import java.util.List;

public interface LlmTurnUsageRepository extends JpaRepository<LlmTurnUsageEntity, Long> {
  /** The service supplies the time range, bucket origin and width; SQL only filters and sums. */
  @Query(
      value = """
          SELECT (u.time - :startTime) DIV :bucketMillis AS bucketIndex,
                 SUM(COALESCE(CAST(JSON_UNQUOTE(JSON_EXTRACT(u.usage_json, '$.promptTokens')) AS SIGNED), 0)) AS promptTokens,
                 SUM(COALESCE(CAST(JSON_UNQUOTE(JSON_EXTRACT(u.usage_json, '$.completionTokens')) AS SIGNED), 0)) AS completionTokens,
                 SUM(COALESCE(CAST(JSON_UNQUOTE(JSON_EXTRACT(u.usage_json, '$.cachedTokens')) AS SIGNED), 0)) AS cachedTokens,
                 SUM(COALESCE(CAST(JSON_UNQUOTE(JSON_EXTRACT(u.usage_json, '$.cacheCreationTokens')) AS SIGNED), 0)) AS cacheCreationTokens,
                 SUM(COALESCE(CAST(JSON_UNQUOTE(JSON_EXTRACT(u.usage_json, '$.calls')) AS SIGNED), 0)) AS calls,
                 SUM(COALESCE(CAST(JSON_UNQUOTE(JSON_EXTRACT(u.usage_json, '$.reportedCalls')) AS SIGNED), 0)) AS reportedCalls
          FROM llm_turn_usage u
          WHERE u.product_id IN (:productIds) AND u.time >= :startTime AND u.time < :endTime
          GROUP BY bucketIndex
          ORDER BY bucketIndex
          """,
      nativeQuery = true)
  List<TrendBucket> aggregateTrend(@Param("productIds") List<Integer> productIds,
      @Param("startTime") long startTime, @Param("endTime") long endTime,
      @Param("bucketMillis") long bucketMillis);

  interface TrendBucket {
    long getBucketIndex();

    long getPromptTokens();

    long getCompletionTokens();

    long getCachedTokens();

    long getCacheCreationTokens();

    long getCalls();

    long getReportedCalls();
  }

  List<LlmTurnUsageEntity> findAllByChatIdAndProductIdOrderByTimeAscIdAsc(String chatId,
      Integer productId);

  @Modifying
  @Transactional
  void deleteAllByChatId(String chatId);

  @Modifying
  @Transactional
  void deleteAllByProductId(Integer productId);
}
