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
package top.rslly.iot.services.knowledgeGraphic.dbo;

import lombok.Data;
import lombok.NoArgsConstructor;
import top.rslly.iot.models.KnowledgeGraphicAttributeEntity;
import top.rslly.iot.models.KnowledgeGraphicNodeEntity;
import top.rslly.iot.models.KnowledgeGraphicRelationEntity;

import java.util.*;

@Data
public class KnowledgeGraphic {

  public KnowledgeGraphic() {
    this.nodes = new HashSet<>();
    this.relations = new HashSet<>();
  }

  public Set<Node> nodes;
  public Set<Relation> relations;

  public void addNode(String name) {
    Node node = this.getNode(name);
    if (node == null) {
      node = new Node(name);
      this.nodes.add(node);
    }
  }

  public void addNode(String name, String des) {
    Node node = this.getNode(name);
    if (node == null) {
      node = new Node(name);
      node.des = des;
      this.nodes.add(node);
    }
  }

  public Node getNode(String name) {
    for (Node node : nodes) {
      if (node.name.equals(name))
        return node;
    }
    return null;
  }

  public void addAttribute(String nodeName, String attributeName) {
    Node aimNode = this.getNode(nodeName);
    if (aimNode == null)
      return;
    aimNode.attributes.add(attributeName);
  }

  public void addRelation(String nodeName, String relationName, String toNode) {
    Relation relation = new Relation(relationName, nodeName, toNode);
    this.relations.add(relation);
  }

  @Data
  public static class Node {
    Node() {
      this.name = "";
      this.attributes = new HashSet<>();
    }

    Node(String name) {
      this.name = name;
      this.attributes = new HashSet<>();
    }

    public String name;

    public String des;

    public Set<String> attributes;

    @Override
    public boolean equals(Object o) {
      if (this == o)
        return true;
      if (o == null || getClass() != o.getClass())
        return false;
      Node node = (Node) o;
      return name.equals(node.name);
    }
  }

  @Data
  @NoArgsConstructor
  public static class Relation {
    Relation(String name, String from, String to) {
      this.name = name;
      this.from = from;
      this.to = to;
    }

    public String name;

    public String from;

    public String to;

    @Override
    public int hashCode() {
      return Objects.hash(name, from, to);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null || getClass() != obj.getClass())
        return false;
      Relation relation = (Relation) obj;
      return Objects.equals(name, relation.name) && Objects.equals(from, relation.from)
          && Objects.equals(to, relation.to);
    }
  }
}
