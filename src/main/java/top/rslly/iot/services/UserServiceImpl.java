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
package top.rslly.iot.services;

import cn.hutool.Hutool;
import cn.hutool.captcha.generator.RandomGenerator;
import cn.hutool.core.lang.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.rslly.iot.dao.UserRepository;
import top.rslly.iot.models.UserEntity;
import top.rslly.iot.param.request.User;
import top.rslly.iot.utility.RedisUtil;
import top.rslly.iot.utility.SendEmail;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {
  @Resource
  UserRepository userRepository;
  @Autowired
  private PasswordEncoder passwordEncoder;
  @Autowired
  private SendEmail sendEmail;
  @Autowired
  private RedisUtil redisUtil;

  @Override
  public List<UserEntity> findAllByUsername(String username) {
    return userRepository.findAllByUsername(username);
  }

  @Override
  public List<UserEntity> findAllByEmail(String email) {
    return userRepository.findAllByEmail(email);
  }

  @Override
  public List<UserEntity> findAll() {
    return userRepository.findAll();
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public UserEntity insert(UserEntity userEntity) {
    userEntity.setPassword("{bcrypt}" + passwordEncoder.encode(userEntity.getPassword()));// adapt
                                                                                          // new
                                                                                          // version
    // spring security
    return userRepository.save(userEntity);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public JsonResult<?> newUser(User user) {
    if (!userRepository.findAllByUsername(user.getUsername()).isEmpty()) {
      return ResultTool.fail(ResultCode.USER_ACCOUNT_ALREADY_EXIST);
    }
    if (!redisUtil.hasKey(user.getUsername() + user.getUserCode())) {
      return ResultTool.fail(ResultCode.USER_CODE_ERROR);
    }
    UserEntity userEntity = new UserEntity();
    userEntity.setPassword("{bcrypt}" + passwordEncoder.encode(user.getPassword()));
    userEntity.setUsername(user.getUsername());
    userEntity.setEmail(redisUtil.get(user.getUsername() + user.getUserCode()).toString());
    userEntity.setRole("guest");
    userRepository.save(userEntity);
    return ResultTool.success();
  }

  @Override
  public JsonResult<?> getUserCode(String username, String email) {
    if (!userRepository.findAllByUsername(username).isEmpty()) {
      return ResultTool.fail(ResultCode.USER_ACCOUNT_ALREADY_EXIST);
    }
    if (!userRepository.findAllByEmail(email).isEmpty()) {
      return ResultTool.fail(ResultCode.EMAIL_ALREADY_EXIST);
    }
    if (!Validator.isEmail(email)) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    Map<String, Object> map = new HashMap<>();
    // 随机生成 6 位验证码
    RandomGenerator randomGenerator = new RandomGenerator("0123456789", 6);
    map.put("code", randomGenerator.generate());
    map.put("username", username);
    sendEmail.contextLoads(new String[] {email}, "登录验证码", "emailCode", map);
    redisUtil.set(username + map.get("code"), email, 60 * 5);
    return ResultTool.success();
  }
}
