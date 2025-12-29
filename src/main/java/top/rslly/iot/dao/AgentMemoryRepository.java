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
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import top.rslly.iot.models.AgentMemoryEntity;

import java.util.List;

public interface AgentMemoryRepository extends JpaRepository<AgentMemoryEntity, Long> {
  // 以chatId开头的记录
  List<AgentMemoryEntity> findAllById(int id);

  List<AgentMemoryEntity> findAllByChatIdStartingWith(String chatId);

  List<AgentMemoryEntity> findAllByChatId(String chatId);

  @Transactional
  List<AgentMemoryEntity> deleteAllByChatId(String chatId);

  @Transactional
  List<AgentMemoryEntity> deleteAllById(int id);

  // 以chatId开头的记录
  @Modifying
  @Transactional
  List<AgentMemoryEntity> deleteAllByChatIdStartingWith(String chatId);
}
