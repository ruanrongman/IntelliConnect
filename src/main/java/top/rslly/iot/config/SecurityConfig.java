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
package top.rslly.iot.config;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

  @Autowired
  @Qualifier("userDetailsServiceImpl")
  @Lazy // solve cycle problem
  private UserDetailsService userDetailsService;
  @Autowired
  private MyAccessDeniedHandlerimplements myAccessDeniedHandlerimplements;

  @Bean
  public AuthenticationManager authenticationManager() {
    DaoAuthenticationProvider daoAuthenticationProvider = new DaoAuthenticationProvider();
    daoAuthenticationProvider.setUserDetailsService(userDetailsService);
    ProviderManager pm = new ProviderManager(daoAuthenticationProvider);
    return pm;
  }


  @Bean
  public WebSecurityCustomizer webSecurityCustomizer() throws Exception {
    // 所需要用到的静态资源，允许访问
    return (web) -> web.ignoring().antMatchers("/swagger-ui.html", "/swagger-ui/*",
        "/swagger-resources/**", "/v2/api-docs", "/v3/api-docs", "/webjars/**", "/ruan/**",
        "/api/v2/micro/**",
        "/api/v2/newUser",
        "/api/v2//getUserCode",
        "/api/v2/ai/tmp_voice/**",
        "/wxLogin",
        "/wxRegister",
        "/xiaozhi/ota/**",
        "/xiaozhi/ota/activate/**",
        // "/api/v2/startProcess/**",
        // "/api/v2/stop/**",
        // "/api/v2/reset/**",
        "/api/v2/machineStatus/**");
    // "/sse/**");
  }

  /**
   * 安全配置
   */
  @Bean
  protected SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    // 跨域共享
    http.cors().and()
        // 跨域伪造请求限制无效
        .csrf().disable()

        .authorizeRequests()


        .requestMatchers(CorsUtils::isPreFlightRequest).permitAll()
        .antMatchers("/wxLogin", "/wxmsg/**", "/wxRegister").permitAll().anyRequest()
        .authenticated().and()
        // 添加JWT登录拦截器
        .addFilter(new JWTAuthenticationFilter(authenticationManager()))
        // 添加JWT鉴权拦截器
        .addFilter(new JWTAuthorizationFilter(authenticationManager())).sessionManagement()
        // 设置Session的创建策略为：Spring Security永不创建HttpSession 不使用HttpSession来获取SecurityContext
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
        // 异常处理
        .exceptionHandling()
        // 匿名用户访问无权限资源时的异常
        .accessDeniedHandler(myAccessDeniedHandlerimplements)
        .authenticationEntryPoint(new JWTAuthenticationEntryPoint());
    return http.build();

  }

  /**
   * 跨域配置
   *
   * @return 基于URL的跨域配置信息
   */
  @Bean
  CorsConfigurationSource corsConfigurationSource() {
    final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    // 注册跨域配置
    source.registerCorsConfiguration("/**", new CorsConfiguration().applyPermitDefaultValues());
    return source;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
