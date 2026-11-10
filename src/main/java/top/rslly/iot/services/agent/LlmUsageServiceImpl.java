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

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.influxdb.InfluxDB;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import top.rslly.iot.dao.LlmUsageRepository;
import top.rslly.iot.models.LlmUsageEntity;
import top.rslly.iot.utility.ai.ConversationMemoryPolicy;
import top.rslly.iot.utility.ai.llm.usage.LlmCallRecord;
import top.rslly.iot.utility.ai.llm.usage.LlmUsage;
import top.rslly.iot.utility.ai.llm.usage.LlmUsageAccumulator;
import top.rslly.iot.utility.ai.llm.usage.LlmUsageSummary;
import top.rslly.iot.utility.ai.llm.usage.LlmUsageTracker;
import top.rslly.iot.utility.influxdb.InfluxProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Persists every model call reported by {@link LlmUsageTracker} and serves aggregated stats.
 * Storage follows {@code storage.database}: InfluxDB measurement {@code llm_usage} or MySQL table
 * {@code llm_usage}.
 */
@Service
@Slf4j
public class LlmUsageServiceImpl implements LlmUsageService {
  static final String MEASUREMENT = "llm_usage";
  private static final int MAX_STATS_ROWS = 20000;

  private final LlmUsageRepository llmUsageRepository;
  private final ObjectProvider<InfluxDB> influxDbProvider;
  private final InfluxProperty influxProperty;
  private final ConversationMemoryPolicy conversationMemoryPolicy;
  private final ExecutorService writer =
      Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("llm-usage-", 0).factory());
  private final AtomicLong failedWrites = new AtomicLong();

  @Value("${storage.database}")
  private String database;
  @Value("${ai.usage.enabled:true}")
  private boolean enabled;
  @Value("${ai.usage.persist:true}")
  private boolean persist;
  @Value("${ai.usage.stream-include-usage:true}")
  private boolean streamIncludeUsage;
  @Value("${ai.usage.cache-hit-price-ratio:0.2}")
  private double cacheHitPriceRatio;
  @Value("${ai.usage.cache-creation-price-ratio:1.25}")
  private double cacheCreationPriceRatio;
  @Value("${ai.usage.min-cacheable-tokens:1024}")
  private long minCacheableTokens;

  public LlmUsageServiceImpl(LlmUsageRepository llmUsageRepository,
      ObjectProvider<InfluxDB> influxDbProvider, InfluxProperty influxProperty,
      ConversationMemoryPolicy conversationMemoryPolicy) {
    this.llmUsageRepository = llmUsageRepository;
    this.influxDbProvider = influxDbProvider;
    this.influxProperty = influxProperty;
    this.conversationMemoryPolicy = conversationMemoryPolicy;
  }

  @PostConstruct
  void register() {
    LlmUsageTracker.configure(enabled, streamIncludeUsage, cacheHitPriceRatio,
        cacheCreationPriceRatio, minCacheableTokens);
    LlmUsageTracker.setSink(enabled && persist ? this::persistAsync : null);
  }

  @PreDestroy
  void shutdown() {
    LlmUsageTracker.setSink(null);
    writer.shutdown();
    try {
      writer.awaitTermination(5, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private void persistAsync(LlmCallRecord record) {
    writer.execute(() -> {
      try {
        persist(record);
      } catch (Exception e) {
        long failures = failedWrites.incrementAndGet();
        if (failures == 1 || failures % 100 == 0) {
          log.warn("LLM usage persistence failed ({} failures so far)", failures, e);
        }
      }
    });
  }

  void persist(LlmCallRecord record) {
    if (useInflux()) {
      InfluxDB influxDB = influxDbProvider.getIfAvailable();
      if (influxDB != null) {
        influxDB.write(BatchPoints.database(influxProperty.getDataBaseName())
            .point(toPoint(record))
            .build());
      }
      return;
    }
    llmUsageRepository.save(toEntity(record));
  }

  @Override
  public LlmUsageSummary stats(int productId, long fromMillis, String chatId) {
    LlmUsageAccumulator accumulator = new LlmUsageAccumulator(resolveContextWindow(productId));
    List<LlmCallRecord> records =
        useInflux() ? queryInflux(productId, fromMillis) : queryMysql(productId, fromMillis);
    for (LlmCallRecord record : records) {
      if (chatId == null || chatId.isBlank() || chatId.equals(record.chatId())) {
        accumulator.add(record);
      }
    }
    return accumulator.summary();
  }

  private long resolveContextWindow(int productId) {
    try {
      return conversationMemoryPolicy.resolveForProduct(productId).contextWindowTokens();
    } catch (RuntimeException e) {
      return 0L;
    }
  }

  private boolean useInflux() {
    return "influxdb".equals(database);
  }

  static Point toPoint(LlmCallRecord record) {
    LlmUsage usage = record.usage();
    return Point.measurement(MEASUREMENT)
        .time(record.time(), TimeUnit.MILLISECONDS)
        .tag("product_id", String.valueOf(record.productId() == null ? -1 : record.productId()))
        .tag("scene", Objects.toString(record.scene(), "unknown"))
        .tag("model", Objects.toString(record.model(), "unknown"))
        .addField("provider", Objects.toString(record.provider(), ""))
        .addField("chat_id", Objects.toString(record.chatId(), ""))
        .addField("prompt_tokens", usage == null ? 0L : usage.promptTokens())
        .addField("cached_tokens", usage == null ? 0L : usage.cachedTokens())
        .addField("cache_creation_tokens", usage == null ? 0L : usage.cacheCreationTokens())
        .addField("completion_tokens", usage == null ? 0L : usage.completionTokens())
        .addField("reasoning_tokens", usage == null ? 0L : usage.reasoningTokens())
        .addField("estimated_prompt_tokens", record.estimatedPromptTokens())
        .addField("latency_ms", record.latencyMs())
        .addField("usage_reported", record.usageReported())
        .addField("stream", record.stream())
        .build();
  }

  private static LlmUsageEntity toEntity(LlmCallRecord record) {
    LlmUsage usage = record.usage();
    LlmUsageEntity entity = new LlmUsageEntity();
    entity.setTime(record.time());
    entity.setProductId(record.productId());
    entity.setChatId(truncate(record.chatId(), 255));
    entity.setScene(truncate(record.scene(), 128));
    entity.setModel(truncate(record.model(), 128));
    entity.setProvider(truncate(record.provider(), 255));
    if (usage != null) {
      entity.setPromptTokens(usage.promptTokens());
      entity.setCachedTokens(usage.cachedTokens());
      entity.setCacheCreationTokens(usage.cacheCreationTokens());
      entity.setCompletionTokens(usage.completionTokens());
      entity.setReasoningTokens(usage.reasoningTokens());
    }
    entity.setEstimatedPromptTokens(record.estimatedPromptTokens());
    entity.setLatencyMs(record.latencyMs());
    entity.setUsageReported(record.usageReported());
    entity.setStream(record.stream());
    return entity;
  }

  private List<LlmCallRecord> queryMysql(int productId, long fromMillis) {
    return llmUsageRepository
        .findTop20000ByProductIdAndTimeGreaterThanEqualOrderByTimeDesc(productId, fromMillis)
        .stream()
        .map(entity -> new LlmCallRecord(entity.getTime(), entity.getProvider(),
            entity.getModel(), entity.getScene(), entity.getProductId(), entity.getChatId(),
            entity.isUsageReported()
                ? new LlmUsage(entity.getPromptTokens(), entity.getCachedTokens(),
                    entity.getCacheCreationTokens(), entity.getCompletionTokens(),
                    entity.getReasoningTokens())
                : null,
            entity.getEstimatedPromptTokens(), entity.getLatencyMs(), entity.isStream()))
        .toList();
  }

  @SuppressWarnings("deprecation")
  private List<LlmCallRecord> queryInflux(int productId, long fromMillis) {
    InfluxDB influxDB = influxDbProvider.getIfAvailable();
    if (influxDB == null) {
      return List.of();
    }
    // productId is an int and fromMillis a long, so nothing user-controlled reaches the query.
    String sql = "SELECT * FROM \"" + MEASUREMENT + "\" WHERE time >= " + fromMillis
        + "ms AND \"product_id\" = '" + productId + "' ORDER BY time DESC LIMIT "
        + MAX_STATS_ROWS;
    QueryResult result = influxDB.query(new Query(sql, influxProperty.getDataBaseName()));
    return parseInflux(result, productId);
  }

  static List<LlmCallRecord> parseInflux(QueryResult result, int productId) {
    if (result == null || result.getResults() == null) {
      return List.of();
    }
    List<LlmCallRecord> records = new ArrayList<>();
    for (QueryResult.Result item : result.getResults()) {
      if (item.getSeries() == null) {
        continue;
      }
      for (QueryResult.Series series : item.getSeries()) {
        List<String> columns = series.getColumns();
        if (columns == null || series.getValues() == null) {
          continue;
        }
        for (List<Object> row : series.getValues()) {
          Row values = new Row(columns, row);
          LlmUsage usage = values.bool("usage_reported")
              ? new LlmUsage(values.number("prompt_tokens"), values.number("cached_tokens"),
                  values.number("cache_creation_tokens"), values.number("completion_tokens"),
                  values.number("reasoning_tokens"))
              : null;
          records.add(new LlmCallRecord(0L, values.text("provider"), values.text("model"),
              values.text("scene"), productId, values.text("chat_id"), usage,
              values.number("estimated_prompt_tokens"), values.number("latency_ms"),
              values.bool("stream")));
        }
      }
    }
    return records;
  }

  private record Row(List<String> columns, List<Object> values) {
    private Object get(String column) {
      int index = columns.indexOf(column);
      return index < 0 || index >= values.size() ? null : values.get(index);
    }

    private long number(String column) {
      Object value = get(column);
      return value instanceof Number number ? number.longValue() : 0L;
    }

    private boolean bool(String column) {
      Object value = get(column);
      return value instanceof Boolean flag ? flag : Boolean.parseBoolean(String.valueOf(value));
    }

    private String text(String column) {
      Object value = get(column);
      return value == null ? null : String.valueOf(value);
    }
  }

  private static String truncate(String value, int maxLength) {
    if (value == null || value.length() <= maxLength) {
      return value;
    }
    return value.substring(0, maxLength);
  }
}
