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
package top.rslly.iot.services.agent;

import top.rslly.iot.models.AgentMemoryEntity;
import top.rslly.iot.param.request.AgentMemory;
import top.rslly.iot.utility.result.JsonResult;

import java.util.List;

public interface AgentMemoryService {
  List<AgentMemoryEntity> findAllById(int id);

  List<AgentMemoryEntity> findAllByChatIdStartingWith(String chatId);

  List<AgentMemoryEntity> findAllByChatId(String chatId);

  JsonResult<?> getMemory(String token);

  JsonResult<?> updateMemory(AgentMemory agentMemory);

  JsonResult<?> deleteMemory(int id);

  AgentMemoryEntity insertAndUpdate(AgentMemory agentMemory);
}
