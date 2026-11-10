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

import com.alibaba.fastjson.JSON;
import org.junit.jupiter.api.Test;
import top.rslly.iot.models.KnowledgeGraphicAttributeEntity;
import top.rslly.iot.models.KnowledgeGraphicNodeEntity;
import top.rslly.iot.models.KnowledgeGraphicRelationEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class KnowledgeGraphicContextQueryTest {

  /** hub(1) -> 20 spokes (100..119); spoke 100 -> leaf 200; matched node 2 -> hub. */
  private final Map<Long, KnowledgeGraphicNodeEntity> nodes = new HashMap<>();
  private final Map<Long, List<KnowledgeGraphicRelationEntity>> edges = new HashMap<>();
  private final Map<Long, List<KnowledgeGraphicAttributeEntity>> attributes = new HashMap<>();
  private final KnowledgeGraphicContextQuery.GraphAccess access =
      new KnowledgeGraphicContextQuery.GraphAccess() {
        @Override
        public List<KnowledgeGraphicRelationEntity> outgoing(long nodeId) {
          return edges.getOrDefault(nodeId, List.of());
        }

        @Override
        public KnowledgeGraphicNodeEntity node(long nodeId) {
          return nodes.get(nodeId);
        }

        @Override
        public List<KnowledgeGraphicAttributeEntity> attributes(long nodeId) {
          return attributes.getOrDefault(nodeId, List.of());
        }
      };

  KnowledgeGraphicContextQueryTest() {
    node(1, "hub");
    node(2, "matched");
    edge(2, 1);
    for (long spoke = 100; spoke < 120; spoke++) {
      node(spoke, "spoke" + spoke);
      edge(1, spoke);
    }
    node(200, "leaf");
    edge(100, 200);
    KnowledgeGraphicAttributeEntity attribute = new KnowledgeGraphicAttributeEntity();
    attribute.setName("tag");
    attribute.setBelong(2);
    attributes.put(2L, List.of(attribute));
  }

  @Test
  void depthOneKeepsMatchedNodeWithDirectNeighbours() {
    var result = KnowledgeGraphicContextQuery.build(List.of(nodes.get(2L)),
        new KnowledgeGraphicContextQuery.Limits(0.78, 1, 10, 10_000), access);

    assertEquals(List.of("matched", "hub"), names(result));
    assertEquals(1, result.graphic().relations.size());
    assertTrue(result.graphic().getNode("matched").attributes.contains("tag"));
  }

  @Test
  void capsFanOutOfHubNodes() {
    var result = KnowledgeGraphicContextQuery.build(List.of(nodes.get(1L)),
        new KnowledgeGraphicContextQuery.Limits(0.78, 2, 10, 100_000), access);

    // hub + 10 of its 20 spokes + leaf reached through the first spoke.
    assertEquals(12, result.includedNodes().size());
    assertTrue(names(result).contains("leaf"));
  }

  @Test
  void stopsAtCharacterBudgetButAlwaysKeepsMatchedNodes() {
    var result = KnowledgeGraphicContextQuery.build(List.of(nodes.get(1L), nodes.get(2L)),
        new KnowledgeGraphicContextQuery.Limits(0.78, 3, 50, 300), access);

    assertTrue(names(result).containsAll(List.of("hub", "matched")));
    assertTrue(result.includedNodes().size() < 22);
    String json = JSON.toJSONString(result.graphic());
    assertTrue(json.length() < 600, json);
  }

  @Test
  void depthZeroAndSharedNodesAreNotDuplicated() {
    var onlyMatched = KnowledgeGraphicContextQuery.build(List.of(nodes.get(2L), nodes.get(2L)),
        new KnowledgeGraphicContextQuery.Limits(0.78, 0, 10, 10_000), access);
    assertEquals(List.of("matched"), names(onlyMatched));

    var shared = KnowledgeGraphicContextQuery.build(List.of(nodes.get(2L), nodes.get(1L)),
        new KnowledgeGraphicContextQuery.Limits(0.78, 1, 50, 100_000), access);
    assertEquals(shared.includedNodes().size(),
        shared.includedNodes().stream().map(KnowledgeGraphicNodeEntity::getId)
            .collect(Collectors.toSet()).size());
  }

  private List<String> names(KnowledgeGraphicContextQuery.Result result) {
    return result.includedNodes().stream().map(KnowledgeGraphicNodeEntity::getName).toList();
  }

  private void node(long id, String name) {
    KnowledgeGraphicNodeEntity node = new KnowledgeGraphicNodeEntity();
    node.setId(id);
    node.setName(name);
    node.setDes("description of " + name);
    nodes.put(id, node);
  }

  private void edge(long from, long to) {
    KnowledgeGraphicRelationEntity relation = new KnowledgeGraphicRelationEntity();
    relation.setFrom(from);
    relation.setTo(to);
    relation.setDes("links");
    edges.computeIfAbsent(from, key -> new ArrayList<>()).add(relation);
  }
}
