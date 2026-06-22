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
package top.rslly.iot.utility.ai.prompts;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.rslly.iot.services.UserConfigServiceImpl;
import top.rslly.iot.services.agent.ProductKnowledgeGraphicPromptServiceImpl;
import top.rslly.iot.services.knowledgeGraphic.KnowledgeGraphicService;
import top.rslly.iot.utility.ai.promptTemplate.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Component
public class KnowledgeGraphicPrompt {
  @Autowired
  private KnowledgeGraphicService knowledgeGraphicService;
  @Autowired
  private ProductKnowledgeGraphicPromptServiceImpl productKnowledgeGraphicPromptService;
  @Autowired
  private UserConfigServiceImpl userConfigService;

  private final static String knowledgeGraphicPrompt =
      """
            Please refer to the following user conversation to see if it involves any relation of the following knowledge graphic.
            If the conversation contains or updates information related to the knowledge graphic, please update them accordingly.
            Otherwise, do not add to the nodes and relations lists. If the conversation mentioned entities or relations that
            haven't been added to the knowledge graphic, add them only when it does not violate the Mandatory User Requirements.

            {information}

            The current concept of the graphic and its content: {graphic}
            ## Output Format
            ```json
            {
                "thought": "The thought of what to do and why.(use Chinese)",
                "action":
                {
                    "nodes": [],
                    "relations": []
                }
            }
            ```
            ## Attention
            - Your output is JSON only and no explanation.
            - Mandatory User Requirements, when provided, have higher priority than the default extraction rules.
            - If Mandatory User Requirements define limits, apply them to the final persisted graph across all chunks, not only to the current response.
            - Before returning JSON, verify that every `New` node and relation complies with the Mandatory User Requirements.
            - Each node struct would be like this:
                ```
                {
                    "name": "Entity name",
                    "des": "Description of the entity",
                    "attributes":[],
                    "nodeAction": "`New`, `Delete` or `Update`"
                }
                ```
            - Each attribute should less than 10 words.
            - Each relation struct would be like this:
                ```
                {
                    "name": "Relation description",
                    "from": "Source node name",
                    "to": "Target node name",
                    "relationAction": "`New`, `Delete` or `Update`"
                }
                ```
            - If relation mentions a new entity(whatever source or target), please add it to the nodes list.
              Do this only when it does not violate the Mandatory User Requirements.
            ## Current Conversation
            Below is the current conversation consisting of interleaving human and assistant history.
          """;

  private final static String knowledgeGraphicExtractPrompt =
      """
            Extract and merge knowledge graph information from the source text.
            Use the current graph as context and keep existing knowledge unless the source text updates it.

            {information}

            The current concept of the graphic and its content: {graphic}
            ## Output Format
            ```json
            {
                "thought": "The thought of what to do and why.(use Chinese)",
                "action":
                {
                    "nodes": [],
                    "relations": []
                }
            }
            ```
            ## Attention
            - Your output is JSON only and no explanation.
            - Do not use chat history. Only use the source text provided by the user message.
            - Do not delete existing graph data. Use `New` or `Update` actions only.
            - Mandatory User Requirements, when provided, have higher priority than the default extraction rules.
            - If Mandatory User Requirements define limits, apply them to the final persisted graph across all chunks, not only to the current response.
            - Before returning JSON, verify that every `New` node and relation complies with the Mandatory User Requirements.
            - Each node struct would be like this:
                ```
                {
                    "name": "Entity name",
                    "des": "Description of the entity",
                    "attributes":[],
                    "nodeAction": "`New` or `Update`"
                }
                ```
            - Each attribute should less than 10 words.
            - Each relation struct would be like this:
                ```
                {
                    "name": "Relation description",
                    "from": "Source node name",
                    "to": "Target node name",
                    "relationAction": "`New` or `Update`"
                }
                ```
            - If relation mentions a new entity(whatever source or target), please add it to the nodes list.
              Do this only when it does not violate the Mandatory User Requirements.
            ## Source Text
          """;

  public String getKnowledgeGraphicPrompt(int productId) {
    String knowledgeGraphic = knowledgeGraphicService.getKnowledgeGraphicJSON(productId);
    Map<String, String> map = new HashMap<>();
    map.put("graphic", knowledgeGraphic);
    if (userConfigService.getConfigValue(productId, "knowledge_graph.prompt").equals("true")) {
      var information = productKnowledgeGraphicPromptService.findAllByProductId(productId);
      if (!information.isEmpty() && !information.getFirst().getPrompt().isEmpty()) {
        map.put("information", buildRequirementBlock(information.getFirst().getPrompt()));
      } else {
        map.put("information", "");
      }
    } else {
      map.put("information", "");
    }
    return StringUtils.formatString(knowledgeGraphicPrompt, map);
  }

  public String getKnowledgeGraphicExtractPrompt(int productId) {
    String knowledgeGraphic = knowledgeGraphicService.getKnowledgeGraphicJSON(productId);
    Map<String, String> map = new HashMap<>();
    map.put("graphic", knowledgeGraphic);
    if (userConfigService.getConfigValue(productId, "knowledge_graph.prompt").equals("true")) {
      var information = productKnowledgeGraphicPromptService.findAllByProductId(productId);
      if (!information.isEmpty() && !information.getFirst().getPrompt().isEmpty()) {
        map.put("information", buildRequirementBlock(information.getFirst().getPrompt()));
      } else {
        map.put("information", "");
      }
    } else {
      map.put("information", "");
    }
    return StringUtils.formatString(knowledgeGraphicExtractPrompt, map);
  }

  private String buildRequirementBlock(String requirement) {
    return """
          ## Mandatory User Requirements
          The following requirements are higher priority than the default extraction rules.
          They apply to the final persisted knowledge graph, including all generated chunks and merges.
          If a requirement limits the total number of nodes or relations, do not create new items once that final limit would be exceeded.

          %s
        """
        .formatted(requirement.trim());
  }
}
