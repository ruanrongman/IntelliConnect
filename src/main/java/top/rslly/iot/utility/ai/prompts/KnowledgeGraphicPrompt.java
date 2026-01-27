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
import top.rslly.iot.services.knowledgeGraphic.KnowledgeGraphicService;
import top.rslly.iot.utility.ai.promptTemplate.StringUtils;

import java.util.HashMap;
import java.util.Map;

@Component
public class KnowledgeGraphicPrompt {
  @Autowired
  private KnowledgeGraphicService knowledgeGraphicService;

  private final static String knowledgeGraphicPrompt =
      """
            Please refer to the following user conversation to see if it involves any relation of the following knowledge graphic.
            If the conversation contains or updates information related to the knowledge graphic, please update them accordingly.
            Otherwise, do not add to the nodes and relations lists. If the conversation mentioned entities or relations that
            haven't bee added to the knowledge graphic, add them to the graphic.

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
            - Each node struct would be like this:
                ```
                {
                    "name": "Entity name",
                    "des": "Description of the entity",
                    "attributes":[]
                }
                ```
            - Each attribute should less than 10 words.
            - Each relation struct would be like this:
                ```
                {
                    "des": "Relation description",
                    "from": "Source node name",
                    "to": "Target node name"
                }
                ```
            - If relation mentions a new entity(whatever source or target), please add it to the nodes list.
            ## Current Conversation
            Below is the current conversation consisting of interleaving human and assistant history.
          """;

  public String getKnowledgeGraphicPrompt(int productId) {
    String knowledgeGraphic = knowledgeGraphicService.getKnowledgeGraphicJSON(productId);
    Map<String, String> map = new HashMap<>();
    map.put("graphic", knowledgeGraphic);
    return StringUtils.formatString(knowledgeGraphicPrompt, map);
  }
}
