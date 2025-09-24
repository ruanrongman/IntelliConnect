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


import cn.hutool.core.lang.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import top.rslly.iot.models.UserEntity;
import top.rslly.iot.services.UserService;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Component
public class UserDetailsServiceImpl implements UserDetailsService {
  /*
   * @Autowired private PasswordEncoder passwordEncoder;
   */

  @Autowired
  private UserService userService;

  // private Logger LOG = Logger.getLogger(String.valueOf(UserDetailsServiceImpl.class));

  @Override
  public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
    if (s == null || "".equals(s)) {
      throw new RuntimeException("用户不能为空");
    }
    List<UserEntity> user;
    String userName;
    if (Validator.isEmail(s)) {
      user = userService.findAllByEmail(s);
    } else {
      user = userService.findAllByUsername(s);
    }
    userName = user.get(0).getUsername();
    if (user.isEmpty()) {
      throw new RuntimeException("用户不存在");
    }
    // System.out.println(user.get(0));

    List<SimpleGrantedAuthority> authorities = new ArrayList<>();

    authorities.add(new SimpleGrantedAuthority("ROLE_" + user.get(0).getRole()));

    String pwd = user.get(0).getPassword();
    // String cryptPwd = BCrypt.hashpw(pwd, BCrypt.gensalt());
    // String cryptPwd = passwordEncoder.encode(pwd);

    // LOG.info("加密后的密码为: {}"+cryptPwd);



    return new org.springframework.security.core.userdetails.User(userName, pwd, authorities);
  }
}
