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

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import top.rslly.iot.utility.JwtTokenUtil;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;


import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

/**
 * 验证用户名密码正确后 生成一个token并将token返回给客户端
 */
public class JWTAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

  private final AuthenticationManager authenticationManager;

  public JWTAuthenticationFilter(AuthenticationManager authenticationManager) {
    super.setFilterProcessesUrl("/api/v2/login");
    this.authenticationManager = authenticationManager;
  }

  /**
   * 验证操作 接收并解析用户凭证
   */
  @Override
  public Authentication attemptAuthentication(HttpServletRequest request,
      HttpServletResponse response) throws AuthenticationException {
    // 从输入json流中获取到登录的信息
    // 创建一个token并调用authenticationManager.authenticate() 让Spring security进行验证
    // return authenticationManager.authenticate(new
    // UsernamePasswordAuthenticationToken(request.getParameter("username"),request.getParameter("password")));
    response.setCharacterEncoding("UTF-8");
    response.setContentType("application/json; charset=utf-8");
    if (request.getContentType().equals(MediaType.APPLICATION_JSON_UTF8_VALUE)
        || request.getContentType().equals(MediaType.APPLICATION_JSON_VALUE)) {
      ObjectMapper mapper = new ObjectMapper();
      UsernamePasswordAuthenticationToken authRequest = null;
      try (InputStream is = request.getInputStream()) {
        Map<String, String> authenticationBean = mapper.readValue(is, Map.class);
        /*
         * authRequest = new UsernamePasswordAuthenticationToken(
         * authenticationBean.get("username"), authenticationBean.get("password")); return
         * this.getAuthenticationManager().authenticate(authRequest);
         */
        return authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
            authenticationBean.get("username"), authenticationBean.get("password")));
      } catch (IOException e) {
        e.printStackTrace();
        /*
         * authRequest = new UsernamePasswordAuthenticationToken( "", ""); return
         * this.getAuthenticationManager().authenticate(authRequest);
         */
        return authenticationManager.authenticate(new UsernamePasswordAuthenticationToken("", ""));
      } /*
         * finally { setDetails(request, authRequest); return
         * this.getAuthenticationManager().authenticate(authRequest); }
         */
    } else {
      // return super.attemptAuthentication(request, response);
      return authenticationManager.authenticate(new UsernamePasswordAuthenticationToken("", ""));
    }
  }


  /**
   * 验证【成功】后调用的方法 若验证成功 生成token并返回
   */
  @Override
  protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
      FilterChain chain, Authentication authResult) throws IOException {
    User user = (User) authResult.getPrincipal();

    // 从User中获取权限信息
    Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
    // 创建Token
    String token = JwtTokenUtil.createToken(user.getUsername(), authorities.toString());

    // 设置编码 防止乱码问题
    response.setCharacterEncoding("UTF-8");
    response.setContentType("application/json; charset=utf-8");
    // 在请求头里返回创建成功的token
    // 设置请求头为带有"Bearer "前缀的token字符串
    // response.setHeader("token", JwtTokenUtil.TOKEN_PREFIX + token);
    // System.out.println(token);
    // 处理编码方式 防止中文乱码
    response.setContentType("text/json;charset=utf-8");
    // 将反馈塞到HttpServletResponse中返回给前台
    JsonResult result = ResultTool.success(JwtTokenUtil.TOKEN_PREFIX + token);
    response.getWriter().write(JSON.toJSONString(result));
  }

  /**
   * 验证【失败】调用的方法
   */
  @Override
  protected void unsuccessfulAuthentication(HttpServletRequest request,
      HttpServletResponse response, AuthenticationException failed)
      throws IOException, ServletException {
    String returnData = "";
    // 账号过期
    if (failed instanceof AccountExpiredException) {
      returnData = JSON.toJSONString(ResultTool.fail(ResultCode.USER_ACCOUNT_EXPIRED));
    }
    // 密码错误
    else if (failed instanceof BadCredentialsException) {
      returnData = JSON.toJSONString(ResultTool.fail(ResultCode.USER_CREDENTIALS_ERROR));
    }
    // 密码过期
    else if (failed instanceof CredentialsExpiredException) {
      returnData = JSON.toJSONString(ResultTool.fail(ResultCode.USER_CREDENTIALS_EXPIRED));
    }
    // 账号不可用
    else if (failed instanceof DisabledException) {
      returnData = JSON.toJSONString(ResultTool.fail(ResultCode.USER_ACCOUNT_DISABLE));
    }
    // 账号锁定
    else if (failed instanceof LockedException) {
      returnData = JSON.toJSONString(ResultTool.fail(ResultCode.USER_ACCOUNT_LOCKED));
    }
    // 用户不存在
    else if (failed instanceof InternalAuthenticationServiceException) {
      returnData = JSON.toJSONString(ResultTool.fail(ResultCode.USER_ACCOUNT_NOT_EXIST));
    }
    // 其他错误
    else {
      returnData = JSON.toJSONString(ResultTool.fail(ResultCode.COMMON_FAIL));
    }

    // 处理编码方式 防止中文乱码
    response.setContentType("text/json;charset=utf-8");
    // 将反馈塞到HttpServletResponse中返回给前台
    response.getWriter().write(returnData);


  }
}
