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
package top.rslly.iot.utility.ai.tools;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.rslly.iot.models.KnowledgeGraphicNodeEntity;
import top.rslly.iot.param.request.KnowledgeGraphicNode;
import top.rslly.iot.services.knowledgeGraphic.KnowledgeGraphicService;
import top.rslly.iot.services.knowledgeGraphic.dbo.KnowledgeGraphic;
import top.rslly.iot.services.thingsModel.ProductService;
import top.rslly.iot.utility.ai.IcAiException;
import top.rslly.iot.utility.ai.LlmDiyUtility;
import top.rslly.iot.utility.ai.ModelMessage;
import top.rslly.iot.utility.ai.ModelMessageRole;
import top.rslly.iot.utility.ai.llm.LLM;
import top.rslly.iot.utility.ai.prompts.KnowledgeGraphicPrompt;

import java.util.*;

@Component
@Data
@Slf4j
public class KnowledgeGraphicTool implements BaseTool<String> {
  @Autowired
  private ProductService productService;
  @Autowired
  private KnowledgeGraphicService knowledgeGraphicService;
  @Autowired
  private LlmDiyUtility llmDiyUtility;
  @Autowired
  private KnowledgeGraphicPrompt knowledgeGraphicPrompt;
  @Value("${ai.knowledge_graphic_llm}")
  private String llmName;
  private String name = "knowledgeGraphicTool";
  private String description = """
      This tool is working for knowledge graphic generation to help LLM
      handle relations between entities.
      """;

  @Override
  public String run(String question) {
    return null;
  }

  @Override
  public String run(String question, Map<String, Object> globalMessage) {
    int productId = (int) globalMessage.get("productId");
    if (productService.findAllById(productId).isEmpty())
      return "";
    LLM llm = llmDiyUtility.getDiyLlm(productId, llmName, "knowledgeGraphic");
    List<ModelMessage> memory =
        Optional.ofNullable((List<ModelMessage>) globalMessage.get("memory"))
            .orElse(Collections.emptyList());
    List<ModelMessage> messages = new ArrayList<>();
    ModelMessage systemMessage = new ModelMessage(ModelMessageRole.SYSTEM.value(),
        knowledgeGraphicPrompt.getKnowledgeGraphicPrompt(productId) + memory);
    ModelMessage userMessage = new ModelMessage(ModelMessageRole.USER.value(),
        "Start generate knowledge graphic.");
    messages.add(systemMessage);
    messages.add(userMessage);
    var obj = llm.jsonChat(question, messages, true).getJSONObject("action");
    try {
      process_llm_result(obj, productId);
    } catch (Exception e) {
      log.error("Knowledge Graphic extract error:\n{}", e.getMessage());
    }
    return null;
  }

  private void process_llm_result(JSONObject jsonObject, int productId) {
    KnowledgeGraphic graphic = jsonObject.toJavaObject(KnowledgeGraphic.class);
    for (KnowledgeGraphic.Node node : graphic.nodes) {
      if (node.nodeAction.equals("Delete")) {
        KnowledgeGraphicNodeEntity nodeDb = (KnowledgeGraphicNodeEntity) knowledgeGraphicService
            .getNode(node.name, productId).getData();
        knowledgeGraphicService.deleteNode(nodeDb.getId());
        continue;
      }
      KnowledgeGraphicNodeEntity nodeDb = (KnowledgeGraphicNodeEntity) knowledgeGraphicService
          .addNode(node.name, node.des, productId).getData();
      knowledgeGraphicService.addAttributes(node.attributes.stream().toList(), nodeDb.getId());
    }
    for (KnowledgeGraphic.Relation relation : graphic.relations) {
      if (relation.relationAction.equals("Delete")) {
        KnowledgeGraphicNodeEntity nodeFrom = (KnowledgeGraphicNodeEntity) knowledgeGraphicService
            .getNode(relation.from, productId).getData();
        KnowledgeGraphicNodeEntity nodeTo = (KnowledgeGraphicNodeEntity) knowledgeGraphicService
            .getNode(relation.to, productId).getData();
        knowledgeGraphicService.deleteRelationByFromAndTo(nodeFrom.getId(), nodeTo.getId());
        continue;
      }
      knowledgeGraphicService.addRelation(relation.name, relation.from, relation.to, productId);
    }
  }
}
