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
package top.rslly.iot.utility;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JwtTokenUtil {
  // Token请求头
  public static final String TOKEN_HEADER = "Authorization";
  // Token前缀
  public static final String TOKEN_PREFIX = "Bearer ";

  // 签名主题
  public static final String SUBJECT = "piconjo";
  // 过期时间 1000 * 60 * 60 * 24
  public static final long EXPIRITION = 1000 * 60 * 60 * 24 * 7;
  // 应用密钥
  public static final String APPSECRET_KEY = "XXXXXXX";
  // 角色权限声明
  private static final String ROLE_CLAIMS = "role";

  /**
   * 生成Token
   */
  public static String createToken(String username, String role) {
    Map<String, Object> map = new HashMap<>();
    map.put(ROLE_CLAIMS, role);

    String token = Jwts.builder()
        .setSubject(username).setClaims(map)
        .claim("username", username)
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + EXPIRITION))
        .signWith(SignatureAlgorithm.HS256, APPSECRET_KEY).compact();
    return token;
  }

  /**
   * 生成永不过期的Token
   */
  public static String createNoExpireToken(String username, String role) {
    Map<String, Object> map = new HashMap<>();
    map.put(ROLE_CLAIMS, role);

    return Jwts.builder()
        .setSubject(username).setClaims(map)
        .claim("username", username)
        .setIssuedAt(new Date())
        .setExpiration(null)
        .signWith(SignatureAlgorithm.HS256, APPSECRET_KEY).compact();
  }

  /**
   * 校验Token
   */
  public static Claims checkJWT(String token) {
    try {
      final Claims claims =
          Jwts.parser().setSigningKey(APPSECRET_KEY).parseClaimsJws(token).getBody();
      return claims;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * 从Token中获取username
   */
  public static String getUsername(String token) {
    Claims claims = Jwts.parser().setSigningKey(APPSECRET_KEY).parseClaimsJws(token).getBody();
    return claims.get("username").toString();
  }

  /**
   * 从Token中获取用户角色
   */
  public static String getUserRole(String token) {
    Claims claims = Jwts.parser().setSigningKey(APPSECRET_KEY).parseClaimsJws(token).getBody();
    return claims.get("role").toString();
  }

  /**
   * 校验Token是否过期
   */
  public static boolean isExpiration(String token) {
    Claims claims = Jwts.parser().setSigningKey(APPSECRET_KEY).parseClaimsJws(token).getBody();
    return claims.getExpiration().before(new Date());
  }
}
