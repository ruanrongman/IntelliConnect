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
package top.rslly.iot.services.knowledgeGraphic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import top.rslly.iot.dao.KnowledgeGraphicAttributeRepository;
import top.rslly.iot.dao.KnowledgeGraphicNodeRepository;
import top.rslly.iot.dao.KnowledgeGraphicRelationRepository;
import top.rslly.iot.models.KnowledgeGraphicAttributeEntity;
import top.rslly.iot.models.KnowledgeGraphicNodeEntity;
import top.rslly.iot.models.KnowledgeGraphicRelationEntity;
import top.rslly.iot.services.knowledgeGraphic.dbo.KnowledgeGraphic;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;

@ExtendWith(MockitoExtension.class)
class KnowledgeGraphicServiceImplTest {
  @Mock
  private KnowledgeGraphicNodeRepository nodeRepository;
  @Mock
  private KnowledgeGraphicRelationRepository relationRepository;
  @Mock
  private KnowledgeGraphicAttributeRepository attributeRepository;
  @InjectMocks
  private KnowledgeGraphicServiceImpl service;

  @ParameterizedTest
  @CsvSource({"1, 2, 1", "2, 3, 2", "19, 3, 3"})
  void preservesDepthBoundaryDescriptionsAttributesAndCounters(int maxDepth, int nodeCount,
      int expandedCount) {
    List<KnowledgeGraphicNodeEntity> nodes = List.of(node(1, "A"), node(2, "B"), node(3, "C"));
    when(nodeRepository.findById(anyLong()))
        .thenAnswer(
            invocation -> Optional.of(nodes.get((int) (long) invocation.getArgument(0) - 1)));
    when(relationRepository.getAllByFrom(1)).thenReturn(List.of(relation(10, 1, 2)));
    if (expandedCount >= 2) {
      when(relationRepository.getAllByFrom(2)).thenReturn(List.of(relation(11, 2, 3)));
    }
    for (KnowledgeGraphicNodeEntity node : nodes.subList(0, expandedCount)) {
      when(attributeRepository.getAllByBelong(node.getId())).thenReturn(List.of(attribute(node)));
    }

    KnowledgeGraphicService subject = service;
    KnowledgeGraphic graph =
        successfulGraph(subject.getKnowledgeGraphic(nodes.getFirst(), maxDepth));

    assertEquals(nodes.subList(0, nodeCount).stream().map(KnowledgeGraphicNodeEntity::getName)
        .collect(Collectors.toSet()),
        graph.getNodes().stream().map(n -> n.name).collect(Collectors.toSet()));
    assertEquals(nodeCount == 2 ? Set.of("A->B") : Set.of("A->B", "B->C"), edges(graph));
    for (int i = 0; i < nodes.size(); i++) {
      KnowledgeGraphicNodeEntity node = nodes.get(i);
      boolean expanded = i < expandedCount;
      assertEquals(i == 0 ? 6 : 5, node.getHitTimes());
      assertEquals(expanded ? 8 : 7, node.getSearchTimes());
      if (i < nodeCount) {
        assertEquals(node.getDes(), graph.getNode(node.getName()).des);
        assertEquals(expanded ? Set.of(node.getName() + " attribute") : Set.of(),
            graph.getNode(node.getName()).attributes);
      }
      verify(nodeRepository, times(i == 0 ? 2 : expanded ? 1 : 0)).save(node);
      verify(relationRepository, times(expanded ? 1 : 0)).getAllByFrom(node.getId());
      verify(attributeRepository, times(expanded ? 1 : 0)).getAllByBelong(node.getId());
    }
  }

  @Test
  void keepsCycleSelfLoopAndSharedTargetEdgesWithoutExpandingNodesTwice() {
    Map<Long, KnowledgeGraphicNodeEntity> nodes = Map.of(
        1L, node(1, "A"), 2L, node(2, "B"), 3L, node(3, "C"), 4L, node(4, "D"));
    when(nodeRepository.findById(anyLong()))
        .thenAnswer(invocation -> Optional.of(nodes.get((long) invocation.getArgument(0))));
    when(relationRepository.getAllByFrom(1))
        .thenReturn(List.of(relation(10, 1, 2), relation(11, 1, 3)));
    when(relationRepository.getAllByFrom(2)).thenReturn(List.of(
        relation(12, 2, 1), relation(13, 2, 2), relation(14, 2, 3), relation(15, 2, 4)));
    when(relationRepository.getAllByFrom(3)).thenReturn(List.of(relation(16, 3, 4)));

    KnowledgeGraphic graph = successfulGraph(service.getKnowledgeGraphic(nodes.get(1L), 19));

    assertEquals(4, graph.getNodes().size());
    assertEquals(Set.of("A->B", "A->C", "B->A", "B->B", "B->C", "B->D", "C->D"), edges(graph));
    assertTrue(graph.getRelations().stream().allMatch(r -> "links".equals(r.name)));
    for (KnowledgeGraphicNodeEntity node : nodes.values()) {
      assertEquals(8, node.getSearchTimes());
      assertEquals(node.getId() == 1 ? 6 : 5, node.getHitTimes());
      verify(relationRepository).getAllByFrom(node.getId());
      verify(nodeRepository, times(node.getId() == 1 ? 2 : 1)).save(node);
    }
  }

  @Test
  void skipsMissingTargetLogsItsIdentifiersAndReturnsRemainingGraph() {
    KnowledgeGraphicNodeEntity root = node(1, "A");
    KnowledgeGraphicNodeEntity validTarget = node(3, "C");
    when(relationRepository.getAllByFrom(1))
        .thenReturn(List.of(relation(10, 1, 99), relation(11, 1, 3)));
    when(nodeRepository.findById(99L)).thenReturn(Optional.empty());
    when(nodeRepository.findById(3L)).thenReturn(Optional.of(validTarget));
    when(attributeRepository.getAllByBelong(1)).thenReturn(List.of(attribute(root)));
    Logger logger = (Logger) LoggerFactory.getLogger(KnowledgeGraphicServiceImpl.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
    try {
      KnowledgeGraphic graph = successfulGraph(service.getKnowledgeGraphic(root, 1));

      assertEquals(2, graph.getNodes().size());
      assertEquals(Set.of("A->C"), edges(graph));
      assertEquals(Set.of("A attribute"), graph.getNode("A").attributes);
      assertEquals(validTarget.getDes(), graph.getNode("C").des);
      assertEquals(1, appender.list.size());
      ILoggingEvent warning = appender.list.getFirst();
      assertEquals(Level.WARN, warning.getLevel());
      String message = warning.getFormattedMessage();
      assertAll(
          () -> assertTrue(message.contains("productId=12")),
          () -> assertTrue(message.contains("relationId=10")),
          () -> assertTrue(message.contains("fromNodeId=1")),
          () -> assertTrue(message.contains("toNodeId=99")));
    } finally {
      logger.detachAppender(appender);
      appender.stop();
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 0, 20, Integer.MAX_VALUE})
  void rejectsInvalidDepthWithoutReadingOrUpdatingTheGraph(int maxDepth) {
    assertEquals(ResultCode.PARAM_NOT_VALID.getCode(),
        service.getKnowledgeGraphic(node(1, "A"), maxDepth).getErrorCode());
    verifyNoInteractions(nodeRepository, relationRepository, attributeRepository);
  }

  @Test
  void rejectsMissingRootWithoutReadingOrUpdatingTheGraph() {
    assertEquals(ResultCode.PARAM_NOT_VALID.getCode(),
        service.getKnowledgeGraphic(null, 1).getErrorCode());
    verifyNoInteractions(nodeRepository, relationRepository, attributeRepository);
  }

  private KnowledgeGraphic successfulGraph(JsonResult<?> result) {
    assertEquals(200, result.getErrorCode());
    return assertInstanceOf(KnowledgeGraphic.class, result.getData());
  }

  private Set<String> edges(KnowledgeGraphic graph) {
    return graph.getRelations().stream().map(r -> r.from + "->" + r.to).collect(Collectors.toSet());
  }

  private KnowledgeGraphicNodeEntity node(long id, String name) {
    KnowledgeGraphicNodeEntity node = new KnowledgeGraphicNodeEntity();
    node.setId(id);
    node.setProductId(12);
    node.setName(name);
    node.setDes(name + " description");
    node.setHitTimes(5);
    node.setSearchTimes(7);
    return node;
  }

  private KnowledgeGraphicRelationEntity relation(long id, long from, long to) {
    KnowledgeGraphicRelationEntity relation = new KnowledgeGraphicRelationEntity();
    relation.setId(id);
    relation.setFrom(from);
    relation.setTo(to);
    relation.setDes("links");
    return relation;
  }

  private KnowledgeGraphicAttributeEntity attribute(KnowledgeGraphicNodeEntity node) {
    KnowledgeGraphicAttributeEntity attribute = new KnowledgeGraphicAttributeEntity();
    attribute.setBelong(node.getId());
    attribute.setName(node.getName() + " attribute");
    return attribute;
  }
}
