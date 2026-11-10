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
package top.rslly.iot.utility.ai;

import com.alibaba.fastjson.JSON;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import top.rslly.iot.services.knowledgeGraphic.dbo.KnowledgeGraphic;

/** Serializes database graph snapshots to standalone text files, without a rendering runtime. */
@Component
public class KnowledgeGraphicExporter {
  public byte[] export(KnowledgeGraphic graph, String format, long maximum) throws IOException {
    if (!Set.of("json", "mermaid").contains(format)) {
      throw new IllegalArgumentException("Supported formats: mermaid, json");
    }
    String content = "json".equals(format) ? JSON.toJSONString(graph) : mermaid(graph);
    byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
    if (maximum < 1 || bytes.length > maximum) {
      throw new IOException("导出文件超过单文件大小限制");
    }
    return bytes;
  }

  private String mermaid(KnowledgeGraphic graph) throws IOException {
    StringBuilder output = new StringBuilder("flowchart LR\n");
    Map<String, String> ids = new HashMap<>();
    var nodes = graph.nodes.stream().sorted(Comparator.comparing(node -> node.name)).toList();
    for (int i = 0; i < nodes.size(); i++) {
      String name = nodes.get(i).name;
      String id = "n" + i;
      ids.put(name, id);
      output.append("  ").append(id).append("[\"").append(label(name)).append("\"]\n");
    }
    var relations = graph.relations.stream()
        .sorted(Comparator.comparing((KnowledgeGraphic.Relation relation) -> relation.from)
            .thenComparing(relation -> relation.to).thenComparing(relation -> relation.name))
        .toList();
    for (var relation : relations) {
      if (!ids.containsKey(relation.from) || !ids.containsKey(relation.to)) {
        throw new IOException("知识图谱关系引用了不存在的节点");
      }
      output.append("  ").append(ids.get(relation.from)).append(" -->|\"")
          .append(label(relation.name)).append("\"| ").append(ids.get(relation.to)).append('\n');
    }
    if (nodes.isEmpty()) {
      output.append("  %% 暂无图谱数据\n");
    }
    return output.toString();
  }

  // Keep graph text inside quoted labels. Mermaid decimal entities prevent names from becoming
  // directives, links, HTML, Markdown or flowchart syntax; generated IDs never contain user data.
  private String label(String value) {
    StringBuilder escaped = new StringBuilder();
    value.codePoints().forEach(codePoint -> {
      if (Character.isLetterOrDigit(codePoint) || codePoint == ' ') {
        escaped.appendCodePoint(codePoint);
      } else if (Character.isWhitespace(codePoint)) {
        escaped.append(' ');
      } else if (!Character.isISOControl(codePoint)
          && !(codePoint >= 0xd800 && codePoint <= 0xdfff)) {
        escaped.append('#').append(codePoint).append(';');
      }
    });
    return escaped.toString();
  }
}
