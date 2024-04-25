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
package top.rslly.iot.utility.swagger;



import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.*;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.Collections;

@Configuration
public class Swagger {
  @Bean
  public Docket createRestApi() {
    return new Docket(DocumentationType.OAS_30)

        .apiInfo(apiInfo())
        .securitySchemes(Collections.singletonList(HttpAuthenticationScheme.JWT_BEARER_BUILDER
            // 显示用
            .name("JWT").build()))
        .securityContexts(Collections.singletonList(SecurityContext.builder()
            .securityReferences(Collections.singletonList(SecurityReference.builder()
                .scopes(new AuthorizationScope[0]).reference("JWT").build()))
            // 声明作用域
            .operationSelector(o -> o.requestMappingPattern().matches("/.*")).build()))
        .select().apis(RequestHandlerSelectors.basePackage("top.rslly.iot"))
        .paths(PathSelectors.any()).build();
  }

  private ApiInfo apiInfo() {
    return new ApiInfoBuilder().title("创万联智能控制平台接口").description("开放接口文档")
        .contact(new Contact("阮振荣", "https://cwl.rslly.top", "ruanzhen1234@126.com")).version("1.8")
        .build();
  }
}
