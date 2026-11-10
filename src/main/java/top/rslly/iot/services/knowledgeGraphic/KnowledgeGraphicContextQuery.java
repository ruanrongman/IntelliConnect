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

import top.rslly.iot.models.KnowledgeGraphicAttributeEntity;
import top.rslly.iot.models.KnowledgeGraphicNodeEntity;
import top.rslly.iot.models.KnowledgeGraphicRelationEntity;
import top.rslly.iot.services.knowledgeGraphic.dbo.KnowledgeGraphic;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the bounded neighbourhood of the matched nodes that chat prompts inject on every turn. Hub
 * nodes (dozens of outgoing edges) made unbounded BFS inject thousands of characters, so the
 * expansion is capped by depth, fan-out per node and a total character budget. Matched nodes are
 * always kept so the budget never hides the actual hit.
 */
final class KnowledgeGraphicContextQuery {
  /** Approximate JSON overhead of a serialized node, relation and attribute. */
  private static final int NODE_OVERHEAD_CHARS = 40;
  private static final int RELATION_OVERHEAD_CHARS = 30;
  private static final int ATTRIBUTE_OVERHEAD_CHARS = 3;

  private KnowledgeGraphicContextQuery() {}

  interface GraphAccess {
    List<KnowledgeGraphicRelationEntity> outgoing(long nodeId);

    KnowledgeGraphicNodeEntity node(long nodeId);

    List<KnowledgeGraphicAttributeEntity> attributes(long nodeId);
  }

  /**
   * @param minScore Chroma relevance score, {@code (1 + cosine) / 2}
   * @param maxDepth hops expanded from the matched nodes; 0 keeps only the matched nodes
   */
  record Limits(double minScore, int maxDepth, int maxNeighbors, int maxChars) {}

  /** @param includedNodes every node present in {@code graphic}, matched nodes first */
  record Result(KnowledgeGraphic graphic, List<KnowledgeGraphicNodeEntity> includedNodes) {}

  static Result build(List<KnowledgeGraphicNodeEntity> matchedNodes, Limits limits,
      GraphAccess access) {
    KnowledgeGraphic graphic = new KnowledgeGraphic();
    Map<Long, KnowledgeGraphicNodeEntity> included = new LinkedHashMap<>();
    int remainingChars = limits.maxChars();
    List<KnowledgeGraphicNodeEntity> layer = new ArrayList<>();
    for (KnowledgeGraphicNodeEntity node : matchedNodes) {
      if (node == null || included.containsKey(node.getId())) {
        continue;
      }
      List<KnowledgeGraphicAttributeEntity> attributes = access.attributes(node.getId());
      remainingChars -= nodeChars(node, attributes);
      include(graphic, included, node, attributes);
      layer.add(node);
    }

    boolean budgetExhausted = remainingChars <= 0;
    for (int depth = 0; depth < limits.maxDepth() && !layer.isEmpty()
        && !budgetExhausted; depth++) {
      List<KnowledgeGraphicNodeEntity> nextLayer = new ArrayList<>();
      for (KnowledgeGraphicNodeEntity node : layer) {
        int expanded = 0;
        for (KnowledgeGraphicRelationEntity relation : access.outgoing(node.getId())) {
          if (expanded >= limits.maxNeighbors()) {
            break;
          }
          KnowledgeGraphicNodeEntity target = access.node(relation.getTo());
          if (target == null) {
            continue;
          }
          boolean newNode = !included.containsKey(target.getId());
          List<KnowledgeGraphicAttributeEntity> attributes =
              newNode ? access.attributes(target.getId()) : List.of();
          int cost = relationChars(node, relation, target)
              + (newNode ? nodeChars(target, attributes) : 0);
          if (cost > remainingChars) {
            budgetExhausted = true;
            break;
          }
          remainingChars -= cost;
          expanded++;
          if (newNode) {
            include(graphic, included, target, attributes);
            nextLayer.add(target);
          }
          graphic.addRelation(node.getName(), relation.getDes(), target.getName());
        }
        if (budgetExhausted) {
          break;
        }
      }
      layer = nextLayer;
    }
    return new Result(graphic, new ArrayList<>(included.values()));
  }

  private static void include(KnowledgeGraphic graphic,
      Map<Long, KnowledgeGraphicNodeEntity> included, KnowledgeGraphicNodeEntity node,
      List<KnowledgeGraphicAttributeEntity> attributes) {
    included.put(node.getId(), node);
    graphic.addNode(node.getName(), node.getDes());
    for (KnowledgeGraphicAttributeEntity attribute : attributes) {
      graphic.addAttribute(node.getName(), attribute.getName());
    }
  }

  private static int nodeChars(KnowledgeGraphicNodeEntity node,
      List<KnowledgeGraphicAttributeEntity> attributes) {
    int chars = NODE_OVERHEAD_CHARS + length(node.getName()) + length(node.getDes());
    for (KnowledgeGraphicAttributeEntity attribute : attributes) {
      chars += ATTRIBUTE_OVERHEAD_CHARS + length(attribute.getName());
    }
    return chars;
  }

  private static int relationChars(KnowledgeGraphicNodeEntity from,
      KnowledgeGraphicRelationEntity relation, KnowledgeGraphicNodeEntity to) {
    return RELATION_OVERHEAD_CHARS + length(from.getName()) + length(relation.getDes())
        + length(to.getName());
  }

  private static int length(String value) {
    return value == null ? 0 : value.length();
  }
}
