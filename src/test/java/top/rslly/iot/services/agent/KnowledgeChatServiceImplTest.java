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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.rslly.iot.dao.KnowledgeChatRepository;
import top.rslly.iot.models.KnowledgeChatEntity;
import top.rslly.iot.param.request.KnowledgeChatDescription;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;

@ExtendWith(MockitoExtension.class)
class KnowledgeChatServiceImplTest {
  @Mock
  private KnowledgeChatRepository knowledgeChatRepository;

  @InjectMocks
  private KnowledgeChatServiceImpl knowledgeChatService;

  @Test
  void putKnowledgeChatUpdatesOnlyDescription() {
    KnowledgeChatEntity entity = new KnowledgeChatEntity();
    entity.setId(7);
    entity.setFilename("manual");
    entity.setProductId(12);
    entity.setStatus("success");
    entity.setDescription("old description");
    KnowledgeChatDescription request = new KnowledgeChatDescription();
    request.setId(7);
    request.setDescription("  new description  ");
    when(knowledgeChatRepository.findAllById(7)).thenReturn(List.of(entity));
    when(knowledgeChatRepository.save(entity)).thenReturn(entity);

    JsonResult<?> result = knowledgeChatService.putKnowledgeChat(request);

    assertTrue(result.getSuccess());
    assertSame(entity, result.getData());
    assertEquals("new description", entity.getDescription());
    assertEquals("manual", entity.getFilename());
    assertEquals(12, entity.getProductId());
    assertEquals("success", entity.getStatus());
    verify(knowledgeChatRepository).save(entity);
  }

  @Test
  void putKnowledgeChatRejectsUnknownId() {
    KnowledgeChatDescription request = new KnowledgeChatDescription();
    request.setId(404);
    request.setDescription("description");
    when(knowledgeChatRepository.findAllById(404)).thenReturn(List.of());

    JsonResult<?> result = knowledgeChatService.putKnowledgeChat(request);

    assertEquals(ResultCode.PARAM_NOT_VALID.getCode(), result.getErrorCode());
    verify(knowledgeChatRepository, never()).save(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void exposesOnlySuccessfulKnowledgeInStableRepositoryOrder() {
    KnowledgeChatEntity entity = new KnowledgeChatEntity();
    entity.setId(7);
    when(knowledgeChatRepository.findAllByProductIdAndStatusOrderByIdAsc(12, "success"))
        .thenReturn(List.of(entity));
    when(knowledgeChatRepository.existsByProductIdAndStatus(12, "success")).thenReturn(true);

    assertEquals(List.of(entity), knowledgeChatService.findSearchableKnowledgeByProductId(12));
    assertTrue(knowledgeChatService.hasSearchableKnowledge(12));
  }
}
