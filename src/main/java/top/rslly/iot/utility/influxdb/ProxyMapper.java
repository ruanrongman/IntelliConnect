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
package top.rslly.iot.utility.influxdb;

import lombok.extern.slf4j.Slf4j;
import top.rslly.iot.utility.influxdb.ano.Delete;
import top.rslly.iot.utility.influxdb.ano.Insert;
import top.rslly.iot.utility.influxdb.ano.Select;
import top.rslly.iot.utility.influxdb.executor.Executor;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;

@Slf4j
public class ProxyMapper implements InvocationHandler {
  private ParameterHandler parameterHandler;
  private Executor executor;

  public ProxyMapper(ParameterHandler parameterHandler, Executor executor) {
    this.parameterHandler = parameterHandler;
    this.executor = executor;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) {
    Annotation[] annotations = method.getAnnotations();

    if (annotations.length == 1) {
      Annotation annotation = annotations[0];
      Class<? extends Annotation> annotationType = annotation.annotationType();
      if (annotationType == Select.class) {
        Select selectAnnotation = method.getAnnotation(Select.class);
        // 拼接sql
        String sql = selectAnnotation.value();
        Parameter[] parameters = method.getParameters();
        sql = parameterHandler.handleParameter(parameters, args, sql);
        // log.info(sql);
        // 查询结果
        Class<?> resultType = selectAnnotation.resultType();
        return executor.query(resultType, sql);
      } else if (annotationType == Insert.class) {
        executor.insert(args);
      } else if (annotationType == Delete.class) {
        Delete deleteAnnotation = method.getAnnotation(Delete.class);
        // 拼接sql
        String sql = deleteAnnotation.value();
        Parameter[] parameters = method.getParameters();

        sql = parameterHandler.handleParameter(parameters, args, sql);
        // 执行sql
        executor.delete(sql);
      }
    }
    return null;
  }
}
