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
package top.rslly.iot.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import top.rslly.iot.models.MqttUserEntity;


import java.util.List;

public interface MqttUserRepository extends JpaRepository<MqttUserEntity, Long> {

  List<MqttUserEntity> findALLById(int id);

  @Query(
      value = "INSERT INTO mqtt_user(username, password, salt, is_superuser,created) VALUES (?1, SHA2(?2, 256), NULL , 0,NULL)",
      nativeQuery = true)
  List<MqttUserEntity> insertHash(String username, String password);

  @Transactional
  List<MqttUserEntity> deleteByUsername(String username);

}
