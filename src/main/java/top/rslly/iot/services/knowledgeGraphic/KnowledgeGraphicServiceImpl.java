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
import top.rslly.iot.param.request.KnowledgeGraphicNode;
import top.rslly.iot.services.knowledgeGraphic.dbo.KnowledgeGraphic;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

import java.util.ArrayList;
import java.util.List;
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
          KnowledgeGraphicNodeEntity to = knowledgeGraphicNodeRepository.getById(relation.getTo());
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
  public JsonResult<?> addNode(String name, String des, int id) {
    KnowledgeGraphicNodeEntity node = new KnowledgeGraphicNodeEntity();
    node.setName(name);
    node.setDes(des);
    node.setId(id);
    knowledgeGraphicNodeRepository.save(node);
    return ResultTool.success();
  }

  @Override
  public JsonResult<?> getNode(String name) {
    KnowledgeGraphicNodeEntity node = knowledgeGraphicNodeRepository.findByName(name);
    if (node == null) {
      return ResultTool.fail();
    }
    return ResultTool.success(node);
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
}
