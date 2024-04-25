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
package top.rslly.iot.utility.influxdb.executor;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDB.ConsistencyLevel;
import org.influxdb.InfluxDBFactory;
import org.influxdb.annotation.Measurement;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.influxdb.dto.Point.Builder;
import org.influxdb.dto.Pong;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.influxdb.dto.QueryResult.Result;
import org.influxdb.dto.QueryResult.Series;
import org.nutz.lang.Lang;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import top.rslly.iot.utility.influxdb.InfluxProperty;
import top.rslly.iot.utility.influxdb.ReflectUtils;
import top.rslly.iot.utility.influxdb.ano.Tag;
import top.rslly.iot.utility.influxdb.ano.TimeColumn;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ExecutorImpl implements Executor {

  private InfluxDB influxDB;

  @Autowired
  private InfluxProperty influxProperty;
  @Value("${storage.database}")
  private String database;

  public static long convertToUnixTimestamp(String timestamp) {
    Instant instant = Instant.parse(timestamp);
    long seconds = instant.getEpochSecond();
    int milliseconds = instant.get(ChronoField.MILLI_OF_SECOND);
    return seconds * 1000 + milliseconds;
  }

  @Bean
  @SuppressWarnings("deprecation")
  public InfluxDB influxDbBuild() {
    if (!database.equals("influxdb")) {
      return null;
    }
    if (influxDB == null) {
      influxDB = InfluxDBFactory.connect(influxProperty.getUrl(), influxProperty.getUser(),
          influxProperty.getPassword());
    }
    try {
      if (!influxDB.databaseExists(influxProperty.getDataBaseName())) {
        influxDB.createDatabase(influxProperty.getDataBaseName());
      }
    } catch (Exception e) {
      // 该数据库可能设置动态代理，不支持创建数据库
      e.printStackTrace();
    } finally {
      influxDB.setRetentionPolicy(influxProperty.getRetention());
    }
    influxDB.setLogLevel(InfluxDB.LogLevel.NONE);
    return influxDB;
  }

  @Override
  public Boolean ping() {
    boolean isConnected = false;
    Pong pong;
    try {
      pong = influxDB.ping();
      if (pong != null) {
        isConnected = true;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return isConnected;
  }

  @Override
  @SuppressWarnings("deprecation")
  public void createDataBase(String... dataBaseName) {
    if (dataBaseName.length > 0) {
      influxDB.createDatabase(dataBaseName[0]);
      return;
    }
    if (influxProperty.getDataBaseName() == null) {
      log.error("如参数不指定数据库名,配置文件 spring.influx.dataBaseName 必须指定");
      return;
    }
    influxDB.createDatabase(influxProperty.getDataBaseName());
  }

  @Override
  @SuppressWarnings("deprecation")
  public void deleteDataBase(String... dataBaseName) {
    if (dataBaseName.length > 0) {
      influxDB.deleteDatabase(dataBaseName[0]);
      return;
    }
    if (influxProperty.getDataBaseName() == null) {
      log.error("如参数不指定数据库名,配置文件 spring.influx.dataBaseName 必须指定");
      return;
    }
    influxDB.deleteDatabase(influxProperty.getDataBaseName());
  }

  @Override
  public <T> void insert(T object) {
    // 构建一个Entity
    Object first = Lang.first(object);
    Class<?> clazz = first.getClass();
    // 表名
    boolean isAnnot = clazz.isAnnotationPresent(Measurement.class);
    if (!isAnnot) {
      log.error("插入的数据对应实体类需要@Measurement注解");
      return;
    }
    Measurement annotation = clazz.getAnnotation(Measurement.class);
    // 表名
    String measurement = annotation.name();
    Field[] arrfield = clazz.getDeclaredFields();
    // 数据长度
    int size = Lang.eleSize(object);
    List<String> tagFields = ReflectUtils.getFields(object, Tag.class);
    String timeField = ReflectUtils.getField(object, TimeColumn.class);
    if (tagFields.isEmpty()) {
      log.error("插入多条数据需对应实体类字段有@Tag注解");
      return;
    }
    BatchPoints batchPoints = BatchPoints.database(influxProperty.getDataBaseName())
        // 一致性
        .consistency(ConsistencyLevel.ALL).build();
    for (int i = 0; i < size; i++) {
      Map<String, Object> map = new HashMap<>();
      Builder builder = Point.measurement(measurement);
      for (Field field : arrfield) {
        // 私有属性需要开启
        field.setAccessible(true);
        Object result = first;
        try {
          if (size > 1) {
            List objects = (List) (object);
            result = objects.get(i);
          }
          if (tagFields.contains(field.getName())) {
            builder.tag(field.getName(), field.get(result).toString());
          } else if (field.getName().equals(timeField)) {
            builder.time(Long.parseLong(field.get(result).toString()), annotation.timeUnit());
          } else {
            map.put(field.getName(), field.get(result));
          }
        } catch (IllegalAccessException e) {
          log.error("实体转换出错");
          e.printStackTrace();
        }
      }
      builder.fields(map);
      batchPoints.point(builder.build());
    }
    influxDB.write(batchPoints);
  }

  @Override
  public <T> List<T> query(Class<T> clazz, String sql) {
    if (influxProperty.getDataBaseName() == null) {
      log.error("查询数据时配置文件 spring.influx.dataBaseName 必须指定");
      return null;
    }
    QueryResult results = influxDB.query(new Query(sql, influxProperty.getDataBaseName()));
    if (results != null) {
      if (results.getResults() == null) {
        return null;
      }
      List<Object> list = new ArrayList<>();

      for (Result result : results.getResults()) {
        List<Series> series = result.getSeries();
        if (series == null) {
          // list.add(null);
          continue;
        }
        for (Series serie : series) {
          List<List<Object>> values = serie.getValues();
          List<String> columns = serie.getColumns();
          // 构建Bean
          list.addAll(getQueryData(clazz, columns, values));
        }
      }
      // log.info(list.toString());
      return JSON.parseArray(JSON.toJSONString(list), clazz);
    }
    return null;
  }

  @Override
  public void delete(String sql) {
    influxDB.query(new Query(sql, influxProperty.getDataBaseName()));
  }

  /**
   * 自动转换对应Pojo
   *
   * @param values
   * @return
   */
  public <T> List<T> getQueryData(Class<T> clazz, List<String> columns, List<List<Object>> values) {
    List results = new ArrayList<>();
    for (List<Object> list : values) {
      BeanWrapperImpl bean = null;
      Object result = null;
      try {
        result = clazz.getDeclaredConstructor().newInstance();
        bean = new BeanWrapperImpl(result);
      } catch (InstantiationException | IllegalAccessException | NoSuchMethodException
          | InvocationTargetException e) {
        e.printStackTrace();
      }
      String timeField = ReflectUtils.getField(result, TimeColumn.class);
      for (int i = 0; i < list.size(); i++) {
        // 字段名
        String filedName = columns.get(i);
        if (filedName.equals("Tag")) {
          continue;
        }
        try {
          Field field = clazz.getDeclaredField(filedName);
        } catch (NoSuchFieldException e) {
          continue;
        }
        // 值
        Object value = list.get(i);
        if (filedName.equals(timeField)) {
          value = convertToUnixTimestamp((String) value);
        }
        if (bean != null) {
          bean.setPropertyValue(filedName, value);
        }
      }
      results.add(result);
    }
    return results;
  }
}
