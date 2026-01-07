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

import org.nutz.json.Json;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.rslly.iot.dao.KnowledgeGraphicAttributeRepository;
import top.rslly.iot.dao.KnowledgeGraphicNodeRepository;
import top.rslly.iot.dao.KnowledgeGraphicRelationRepository;
import top.rslly.iot.models.KnowledgeGraphicAttributeEntity;
import top.rslly.iot.models.KnowledgeGraphicNodeEntity;
import top.rslly.iot.models.KnowledgeGraphicRelationEntity;
import top.rslly.iot.param.request.KnowledgeGraphicAttribute;
import top.rslly.iot.param.request.KnowledgeGraphicNode;
import top.rslly.iot.param.request.KnowledgeGraphicRelation;
import top.rslly.iot.services.knowledgeGraphic.dbo.KnowledgeGraphic;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Stack;

@Service
public class KnowledgeGraphicServiceImpl implements KnowledgeGraphicService {
  @Autowired
  private KnowledgeGraphicNodeRepository knowledgeGraphicNodeRepository;

  @Autowired
  private KnowledgeGraphicAttributeRepository knowledgeGraphicAttributeRepository;

  @Autowired
  private KnowledgeGraphicRelationRepository knowledgeGraphicRelationRepository;

  @Override
  public JsonResult<?> getKnowledgeGraphic(KnowledgeGraphicNodeEntity rootNode, int maxDepth) {
    if (rootNode == null || maxDepth <= 0) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    if (maxDepth >= 20) {
      // Too deep will cause performance problem.
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    KnowledgeGraphic knowledgeGraphic = new KnowledgeGraphic();
    Stack<KnowledgeGraphicNodeEntity> nodeStack = new Stack<>();
    List<KnowledgeGraphicNodeEntity> nodeList =
        knowledgeGraphicNodeRepository.findAllByProductUid(rootNode.getProductUid());
    for (KnowledgeGraphicNodeEntity node : nodeList) {
      nodeStack.push(node);
      knowledgeGraphic.addNode(node.getName());
    }
    while (!nodeStack.isEmpty() && maxDepth > 0) {
      List<KnowledgeGraphicNodeEntity> nextNodeList = new ArrayList<>();
      for (KnowledgeGraphicNodeEntity node : nodeList) {
        List<KnowledgeGraphicRelationEntity> relationList =
            knowledgeGraphicRelationRepository.getAllByFrom(node.getId());
        for (KnowledgeGraphicRelationEntity relation : relationList) {
          KnowledgeGraphicNodeEntity to = this.getNodeById(relation.getTo());
          // Null is not on consideration, cause relation must with from and to
          nextNodeList.add(to);
          knowledgeGraphic.addRelation(node.getName(), relation.getDes(), to.getName());
        }
        List<KnowledgeGraphicAttributeEntity> attributes =
            knowledgeGraphicAttributeRepository.getAllByBelong(node.getId());
        for (KnowledgeGraphicAttributeEntity attribute : attributes) {
          knowledgeGraphic.addAttribute(node.getName(), attribute.getName());
        }
      }
      nodeStack.clear();
      nodeList = nextNodeList;
      for (KnowledgeGraphicNodeEntity node : nodeList) {
        nodeStack.push(node);
        knowledgeGraphic.addNode(node.getName());
      }
      maxDepth--;
    }
    return ResultTool.success(knowledgeGraphic);
  }

  @Override
  public JsonResult<?> getKnowledgeGraphicByNodeId(long id, int maxDepth) {
    KnowledgeGraphicNodeEntity node = knowledgeGraphicNodeRepository.findById(id).orElse(null);
    if (node == null) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    return this.getKnowledgeGraphic(node, maxDepth);
  }

  @Override
  public JsonResult<?> addNode(KnowledgeGraphicNodeEntity node) {
    knowledgeGraphicNodeRepository.save(node);
    return ResultTool.success();
  }

  @Override
  public JsonResult<?> addNode(KnowledgeGraphicNode node) {
    KnowledgeGraphicNodeEntity nodeDb = knowledgeGraphicNodeRepository.findByName(node.name);
    if (nodeDb == null) {
      nodeDb = new KnowledgeGraphicNodeEntity();
      nodeDb.setName(node.name);
    }
    nodeDb.setDes(node.des);
    nodeDb.setProductUid(node.productUid);
    nodeDb = knowledgeGraphicNodeRepository.save(nodeDb);
    if (!node.attributes.isEmpty()) {
      this.addAttributes(node.attributes, nodeDb.getId());
    }
    return ResultTool.success();
  }

  @Override
  public JsonResult<?> addNode(String name, String des, int productUid) {
    KnowledgeGraphicNodeEntity node = new KnowledgeGraphicNodeEntity();
    node.setName(name);
    node.setDes(des);
    node.setProductUid(productUid);
    knowledgeGraphicNodeRepository.save(node);
    return ResultTool.success();
  }

  @Override
  public JsonResult<?> getNode(String name, int productUid) {
    KnowledgeGraphicNodeEntity node =
        knowledgeGraphicNodeRepository.findByNameAndProductUid(name, productUid);
    if (node == null) {
      return ResultTool.fail();
    }
    return ResultTool.success(node);
  }

  @Override
  public JsonResult<?> getNodes(int productUid) {
    List<KnowledgeGraphicNodeEntity> nodeList =
        knowledgeGraphicNodeRepository.findAllByProductUid(productUid);
    return ResultTool.success(nodeList);
  }

  @Override
  public KnowledgeGraphicNodeEntity getNodeById(long id) {
    return knowledgeGraphicNodeRepository.findById(id).orElse(null);
  }

  @Override
  public List<KnowledgeGraphicNodeEntity> getNodesById(long id) {
    return knowledgeGraphicNodeRepository.findAllById(id);
  }

  @Override
  public JsonResult<?> getNodeRelations(String name) {
    KnowledgeGraphicNodeEntity node = knowledgeGraphicNodeRepository.findByName(name);
    if (node == null) {
      return ResultTool.fail();
    }
    List<KnowledgeGraphicRelationEntity> relations =
        knowledgeGraphicRelationRepository.getAllByFrom(node.getId());
    return ResultTool.success(relations);
  }

  @Override
  public JsonResult<?> getNodeRelations(long id) {
    List<KnowledgeGraphicRelationEntity> relations =
        knowledgeGraphicRelationRepository.getAllByTo(id);
    return ResultTool.success(relations);
  }

  @Override
  public JsonResult<?> addAttribute(String name, long belong) {
    KnowledgeGraphicAttributeEntity attribute = knowledgeGraphicAttributeRepository.getByName(name);
    if (attribute != null) {
      return ResultTool.success();
    }
    attribute = new KnowledgeGraphicAttributeEntity();
    attribute.setName(name);
    attribute.setBelong(belong);
    knowledgeGraphicAttributeRepository.save(attribute);
    return ResultTool.success();
  }

  @Override
  public JsonResult<?> addAttributes(List<String> attributes, long belong) {
    for (String attribute : attributes) {
      this.addAttribute(attribute, belong);
    }
    return ResultTool.success();
  }

  @Override
  public JsonResult<?> addAttributes(KnowledgeGraphicNode node) {
    KnowledgeGraphicNodeEntity nodeDb = knowledgeGraphicNodeRepository.findByName(node.name);
    if (nodeDb == null) {
      return ResultTool.fail();
    }
    return this.addAttributes(node.attributes, nodeDb.getId());
  }

  @Override
  public JsonResult<?> addAttribute(KnowledgeGraphicAttribute attribute) {
    return this.addAttribute(attribute.name, attribute.belong);
  }

  @Override
  @Deprecated
  public JsonResult<?> deleteNode(String name) {
    // Don't use this method unless you can make sure there is only node named what you want to
    // delete.
    KnowledgeGraphicNodeEntity node = knowledgeGraphicNodeRepository.findByName(name);
    if (node == null) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    knowledgeGraphicNodeRepository.delete(node);
    // Relation must with from and to, whatever witch of them was deleted, the relation must be
    // deleted.
    knowledgeGraphicRelationRepository.deleteAllByFrom(node.getId());
    knowledgeGraphicRelationRepository.deleteAllByTo(node.getId());
    knowledgeGraphicAttributeRepository.deleteByBelong(node.getId());
    return ResultTool.success();
  }

  @Override
  public JsonResult<?> deleteNode(String name, int productUid) {
    KnowledgeGraphicNodeEntity node =
        knowledgeGraphicNodeRepository.findByNameAndProductUid(name, productUid);
    if (node == null) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    knowledgeGraphicNodeRepository.delete(node);
    return ResultTool.success();
  }

  @Override
  public JsonResult<?> deleteNode(long id) {
    KnowledgeGraphicNodeEntity node = knowledgeGraphicNodeRepository.findById(id).orElse(null);
    if (node == null) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    knowledgeGraphicNodeRepository.deleteById(id);
    knowledgeGraphicRelationRepository.deleteAllByFrom(id);
    knowledgeGraphicRelationRepository.deleteAllByTo(id);
    knowledgeGraphicAttributeRepository.deleteByBelong(id);
    return ResultTool.success();
  }

  @Override
  public JsonResult<?> deleteNodes(int productUid) {
    List<KnowledgeGraphicNodeEntity> nodes =
        knowledgeGraphicNodeRepository.findAllByProductUid(productUid);
    if (nodes.isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    for (KnowledgeGraphicNodeEntity node : nodes) {
      if (!this.deleteNode(node.getId()).getSuccess()) {
        return ResultTool.fail();
      }
    }
    return ResultTool.success();
  }

  @Override
  public JsonResult<?> updateNode(KnowledgeGraphicNodeEntity node) {
    knowledgeGraphicNodeRepository.save(node);
    return ResultTool.success();
  }

  @Override
  public JsonResult<?> updateNode(String name, String des, long id) {
    KnowledgeGraphicNodeEntity node = this.getNodeById(id);
    if (node == null) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    return ResultTool.success();
  }

  @Override
  public JsonResult<?> updateNode(KnowledgeGraphicNode node) {
    // When updating node, you must supply node id!
    if (node.getId() <= 0)
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    KnowledgeGraphicNodeEntity nodeDb = this.getNodeById(node.getId());
    if (nodeDb == null) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    nodeDb.setDes(node.getDes());
    knowledgeGraphicNodeRepository.save(nodeDb);
    if (!node.attributes.isEmpty()) {
      this.addAttributes(node.attributes, nodeDb.getId());
    }
    return ResultTool.success();
  }

  @Override
  public JsonResult<?> addRelation(KnowledgeGraphicRelationEntity relation) {
    knowledgeGraphicRelationRepository.save(relation);
    return ResultTool.success();
  }

  @Override
  public JsonResult<?> addRelation(String des, long from, long to) {
    if (this.getNodeById(from) == null || this.getNodeById(to) == null) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    KnowledgeGraphicRelationEntity relation =
        knowledgeGraphicRelationRepository.getByFromAndTo(from, to);
    if (relation == null) {
      relation = new KnowledgeGraphicRelationEntity();
      relation.setFrom(from);
      relation.setTo(to);
    }
    relation.setDes(des);
    knowledgeGraphicRelationRepository.save(relation);
    return ResultTool.success();
  }

  @Override
  public JsonResult<?> addRelation(KnowledgeGraphicRelation relation) {
    return this.addRelation(relation.getDes(), relation.getFrom(), relation.getTo());
  }

  @Override
  public JsonResult<?> addRelation(String des, String fromName, String toName) {
    KnowledgeGraphicNodeEntity fromNode = knowledgeGraphicNodeRepository.findByName(fromName);
    KnowledgeGraphicNodeEntity toNode = knowledgeGraphicNodeRepository.findByName(toName);
    if (fromNode == null || toNode == null) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    return this.addRelation(des, fromNode.getId(), toNode.getId());
  }

  @Override
  public JsonResult<?> deleteRelation(KnowledgeGraphicRelationEntity relation) {
    knowledgeGraphicRelationRepository.delete(relation);
    return ResultTool.success();
  }

  @Override
  public JsonResult<?> deleteRelation(long id) {
    knowledgeGraphicRelationRepository.deleteById(id);
    return ResultTool.success();
  }

  @Override
  public JsonResult<?> deleteRelationsByTo(long to) {
    knowledgeGraphicRelationRepository.deleteAllByTo(to);
    return ResultTool.success();
  }

  @Override
  public JsonResult<?> deleteRelationsByFrom(long from) {
    knowledgeGraphicRelationRepository.deleteAllByFrom(from);
    return ResultTool.success();
  }

  @Override
  public JsonResult<?> updateRelation(KnowledgeGraphicRelationEntity relation) {
    knowledgeGraphicRelationRepository.save(relation);
    return ResultTool.success();
  }

  @Override
  public JsonResult<?> updateRelation(String des, long id) {
    KnowledgeGraphicRelationEntity relation =
        knowledgeGraphicRelationRepository.findById(id).orElse(null);
    if (relation == null) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    relation.setDes(des);
    knowledgeGraphicRelationRepository.save(relation);
    return ResultTool.success();
  }

  @Override
  public JsonResult<?> updateRelation(KnowledgeGraphicRelation relation) {
    return this.updateRelation(relation.des, relation.id);
  }

  @Override
  @Deprecated
  public JsonResult<?> deleteAttribute(String name) {
    // Don't use this method unless you can make sure there is only attribute named what you want to
    // delete.
    knowledgeGraphicAttributeRepository.deleteAllByName(name);
    return ResultTool.success();
  }

  @Override
  public JsonResult<?> deleteAttribute(long id) {
    knowledgeGraphicAttributeRepository.deleteById(id);
    return ResultTool.success();
  }

  @Override
  public JsonResult<?> deleteAttribute(String name, long belong) {
    KnowledgeGraphicNodeEntity node = knowledgeGraphicNodeRepository.findById(belong).orElse(null);
    if (node == null) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    knowledgeGraphicAttributeRepository.deleteByBelongAndName(belong, name);
    return ResultTool.success();
  }

  @Override
  public JsonResult<?> deleteAttribute(KnowledgeGraphicAttribute attribute) {
    KnowledgeGraphicAttributeEntity attributeDb = knowledgeGraphicAttributeRepository
        .getByNameAndBelong(attribute.getName(), attribute.getBelong());
    if (attributeDb == null) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    knowledgeGraphicAttributeRepository.delete(attributeDb);
    return ResultTool.success();
  }

  @Override
  public JsonResult<?> deleteAttributesByBelong(long belong) {
    knowledgeGraphicAttributeRepository.deleteByBelong(belong);
    return ResultTool.success();
  }

  @Override
  public JsonResult<?> updateAttribute(String name, long id) {
    KnowledgeGraphicAttributeEntity attribute =
        knowledgeGraphicAttributeRepository.findById(id).orElse(null);
    if (attribute == null) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    attribute.setName(name);
    knowledgeGraphicAttributeRepository.save(attribute);
    return ResultTool.success();
  }

  @Override
  public JsonResult<?> updateAttribute(String oldName, String newName, long belong) {
    KnowledgeGraphicAttributeEntity attribute =
        knowledgeGraphicAttributeRepository.getByNameAndBelong(oldName, belong);
    if (attribute == null) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    attribute.setName(newName);
    knowledgeGraphicAttributeRepository.save(attribute);
    return ResultTool.success();
  }

  @Override
  public JsonResult<?> getAttributes(long belong) {
    List<KnowledgeGraphicAttributeEntity> attributes =
        knowledgeGraphicAttributeRepository.findByBelong(belong);
    return ResultTool.success(attributes);
  }
}
