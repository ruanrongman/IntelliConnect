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



import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import top.rslly.iot.utility.JwtTokenUtil;


import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * 登录成功后 走此类进行鉴权操作
 */
public class JWTAuthorizationFilter extends BasicAuthenticationFilter {

  public JWTAuthorizationFilter(AuthenticationManager authenticationManager) {
    super(authenticationManager);

  }

  /**
   * 在过滤之前和之后执行的事件
   */
  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain chain) throws IOException, ServletException {
    String tokenHeader = request.getHeader(JwtTokenUtil.TOKEN_HEADER);
    String WsToken = request.getHeader("Sec-WebSocket-Protocol");
    // System.out.println(WsToken);
    if (tokenHeader == null)
      tokenHeader = WsToken;
    if (WsToken != null) {
      tokenHeader = JwtTokenUtil.TOKEN_PREFIX + tokenHeader;
      response.setHeader("Sec-WebSocket-Protocol", WsToken);// 关键，如果没有这个。前端将会无法正常工作，导致报错。
    }
    // 若请求头中没有Authorization信息 或是Authorization不以Bearer开头 则直接放行
    if (tokenHeader == null || !tokenHeader.startsWith(JwtTokenUtil.TOKEN_PREFIX)) {
      chain.doFilter(request, response);
      return;
    }

    // 若请求头中有token 则调用下面的方法进行解析 并设置认证信息

    SecurityContextHolder.getContext().setAuthentication(getAuthentication(tokenHeader));
    try {
      super.doFilterInternal(request, response, chain);
    } catch (IOException e) {
      // System.out.println(e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * 从token中获取用户信息并新建一个token
   *
   * @param tokenHeader 字符串形式的Token请求头
   * @return 带用户名和密码以及权限的Authentication
   */
  private UsernamePasswordAuthenticationToken getAuthentication(String tokenHeader) {
    try {
      // 去掉前缀 获取Token字符串
      String token = tokenHeader.replace(JwtTokenUtil.TOKEN_PREFIX, "");
      // 从Token中解密获取用户名
      String username = JwtTokenUtil.getUsername(token);
      // 从Token中解密获取用户角色
      String role = JwtTokenUtil.getUserRole(token);

      if (role.equals("mcp_endpoint"))
        return null;

      // 将[ROLE_XXX,ROLE_YYY]格式的角色字符串转换为数组
      String[] roles = StringUtils.strip(role, "[]").split(", ");

      /*
       * for (var s:roles) { System.out.println(s); }
       */
      Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
      for (String s : roles) {
        authorities.add(new SimpleGrantedAuthority(s));
      }
      if (username != null) {
        return new UsernamePasswordAuthenticationToken(username, null, authorities);
      }

      return null;
    } catch (Exception e) {
      // System.out.println(e.getMessage());
      return null;
    }

  }
}
