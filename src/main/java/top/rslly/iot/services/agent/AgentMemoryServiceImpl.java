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

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.rslly.iot.dao.AgentMemoryRepository;
import top.rslly.iot.models.AgentMemoryEntity;
import top.rslly.iot.param.request.AgentMemory;

import javax.annotation.Resource;
import java.util.List;

@Service
public class AgentMemoryServiceImpl implements AgentMemoryService {
  @Resource
  private AgentMemoryRepository agentMemoryRepository;

  @Override
  public List<AgentMemoryEntity> findAllByChatId(String chatId) {
    return agentMemoryRepository.findAllByChatId(chatId);
  }

  @Override
  @Transactional(rollbackFor = Exception.class)
  public AgentMemoryEntity insertAndUpdate(AgentMemory agentMemory) {
    AgentMemoryEntity agentMemoryEntity = new AgentMemoryEntity();
    List<AgentMemoryEntity> agentMemoryEntities =
        agentMemoryRepository.findAllByChatId(agentMemory.getChatId());
    if (!agentMemoryEntities.isEmpty()) {
      agentMemoryEntity = agentMemoryEntities.get(0);
    }
    BeanUtils.copyProperties(agentMemory, agentMemoryEntity);
    return agentMemoryRepository.save(agentMemoryEntity);
  }
}
