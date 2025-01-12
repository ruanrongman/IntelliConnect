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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import top.rslly.iot.dao.UserRepository;
import top.rslly.iot.models.UserEntity;
import top.rslly.iot.param.request.User;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

import javax.annotation.Resource;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {
  @Resource
  UserRepository userRepository;
  @Autowired
  private PasswordEncoder passwordEncoder;

  @Override
  public List<UserEntity> findAllByUsername(String username) {
    return userRepository.findAllByUsername(username);
  }

  @Override
  public List<UserEntity> findAll() {
    return userRepository.findAll();
  }

  @Override
  public UserEntity insert(UserEntity userEntity) {
    userEntity.setPassword("{bcrypt}" + passwordEncoder.encode(userEntity.getPassword()));// adapt
                                                                                          // new
                                                                                          // version
    // spring security
    return userRepository.save(userEntity);
  }

  @Override
  public JsonResult<?> newUser(User user) {
    if (!userRepository.findAllByUsername(user.getUsername()).isEmpty()) {
      return ResultTool.fail(ResultCode.USER_ACCOUNT_ALREADY_EXIST);
    }
    UserEntity userEntity = new UserEntity();
    userEntity.setPassword("{bcrypt}" + passwordEncoder.encode(user.getPassword()));
    userEntity.setUsername(user.getUsername());
    userEntity.setRole(user.getRole());
    userRepository.save(userEntity);
    return ResultTool.success();
  }
}
