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
import top.rslly.iot.param.request.KnowledgeGraphicAttribute;
import top.rslly.iot.param.request.KnowledgeGraphicNode;
import top.rslly.iot.param.request.KnowledgeGraphicRelation;
import top.rslly.iot.services.knowledgeGraphic.dbo.KnowledgeGraphic;
import top.rslly.iot.utility.result.JsonResult;

import java.util.List;

public interface KnowledgeGraphicService {

  JsonResult<?> getKnowledgeGraphic(KnowledgeGraphicNodeEntity rootNode, int maxDepth);

  JsonResult<?> getKnowledgeGraphicByNodeId(long id, int maxDepth);

  JsonResult<?> addNode(KnowledgeGraphicNodeEntity node);

  JsonResult<?> addNode(KnowledgeGraphicNode node);

  JsonResult<?> addNode(String name, String des, int productUid);

  JsonResult<?> getNode(String name, int productUid);

  JsonResult<?> getNodes(int productUid);

  KnowledgeGraphicNodeEntity getNodeById(long id);

  JsonResult<?> deleteNode(String name);

  JsonResult<?> deleteNode(String name, int productUid);

  JsonResult<?> deleteNode(long id);

  JsonResult<?> deleteNodes(int productUid);

  JsonResult<?> updateNode(KnowledgeGraphicNodeEntity node);

  JsonResult<?> updateNode(String name, String des, long id);

  JsonResult<?> updateNode(KnowledgeGraphicNode node);

  List<KnowledgeGraphicNodeEntity> getNodesById(long id);

  JsonResult<?> addRelation(KnowledgeGraphicRelationEntity relation);

  JsonResult<?> addRelation(String des, long from, long to);

  JsonResult<?> addRelation(KnowledgeGraphicRelation relation);

  JsonResult<?> addRelation(String des, String fromName, String toName);

  JsonResult<?> deleteRelation(KnowledgeGraphicRelationEntity relation);

  JsonResult<?> deleteRelation(long id);

  JsonResult<?> deleteRelationsByFrom(long from);

  JsonResult<?> deleteRelationsByTo(long to);

  JsonResult<?> updateRelation(KnowledgeGraphicRelationEntity relation);

  JsonResult<?> updateRelation(String des, long id);

  JsonResult<?> updateRelation(KnowledgeGraphicRelation relation);

  JsonResult<?> getNodeRelations(String name);

  JsonResult<?> getNodeRelations(long id);

  JsonResult<?> addAttribute(String name, long belong);

  JsonResult<?> addAttributes(List<String> attributes, long belong);

  JsonResult<?> addAttributes(KnowledgeGraphicNode node);

  JsonResult<?> addAttribute(KnowledgeGraphicAttribute attribute);

  JsonResult<?> deleteAttribute(String name);

  JsonResult<?> deleteAttribute(long id);

  JsonResult<?> deleteAttribute(String name, long belong);

  JsonResult<?> deleteAttribute(KnowledgeGraphicAttribute attribute);

  JsonResult<?> deleteAttributesByBelong(long belong);

  JsonResult<?> updateAttribute(String name, long id);

  JsonResult<?> updateAttribute(String oldName, String newName, long belong);

  JsonResult<?> getAttributes(long belong);
}
