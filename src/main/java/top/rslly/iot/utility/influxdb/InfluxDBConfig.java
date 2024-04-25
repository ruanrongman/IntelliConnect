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

import org.springframework.context.ApplicationContext;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.support.ResourcePatternResolver;

@Configuration
public class InfluxDBConfig implements EnvironmentAware {
  private Environment env;

  @Bean(name = "proxyMapperRegister")
  public ProxyMapperRegister proxyMapperRegister(ResourcePatternResolver resourcePatternResolver,
      ApplicationContext applicationContext) {
    String mapperLocation = env.getProperty("spring.influx.mapper-location");
    return new ProxyMapperRegister(resourcePatternResolver, applicationContext, mapperLocation);
  }

  @Override
  public void setEnvironment(Environment environment) {
    this.env = environment;
  }
}
