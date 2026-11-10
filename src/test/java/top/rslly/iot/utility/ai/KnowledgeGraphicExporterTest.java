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

import static org.junit.jupiter.api.Assertions.*;

import com.alibaba.fastjson.JSON;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import top.rslly.iot.services.knowledgeGraphic.dbo.KnowledgeGraphic;

class KnowledgeGraphicExporterTest {
  private final KnowledgeGraphicExporter exporter = new KnowledgeGraphicExporter();
  private static final long MAXIMUM = 1024 * 1024;

  @Test
  void preservesCompleteJsonAndAllDirectedRelationsInStandaloneMermaid() throws Exception {
    var graph = new KnowledgeGraphic();
    graph.addNode("温度传感器", "完整描述<script>data</script>");
    graph.addNode("空调", "设备");
    graph.addAttribute("温度传感器", "精度0.1℃");
    graph.addRelation("温度传感器", "控制", "空调");
    graph.addRelation("空调", "自身", "空调");
    graph.addRelation("温度传感器", "监测", "空调");
    String mermaid = text(graph, "mermaid");
    assertEquals("flowchart LR\n  n0[\"温度传感器\"]\n  n1[\"空调\"]\n"
        + "  n0 -->|\"控制\"| n1\n  n0 -->|\"监测\"| n1\n  n1 -->|\"自身\"| n1\n", mermaid);
    var json = JSON.parseObject(text(graph, "json"), KnowledgeGraphic.class);
    assertEquals("完整描述<script>data</script>", json.getNode("温度传感器").des);
    assertTrue(json.getNode("温度传感器").attributes.contains("精度0.1℃"));
    assertEquals(3, json.relations.size());
    assertEquals(mermaid, text(graph, "mermaid"));
  }

  @Test
  void encodesSyntaxHtmlMarkdownAndDirectiveTextInsideLabels() throws Exception {
    var graph = new KnowledgeGraphic();
    String hostileName = "\"]\nclick n0 \"https://example.com\"\n%%{init: {}}%%<script>&#34;\u0001";
    graph.addNode("end", "设备");
    graph.addNode(hostileName, "设备");
    graph.addRelation("end", "控制|\"[]<> & `代码` 🌡", hostileName);
    String mermaid = text(graph, "mermaid");
    assertEquals(4, mermaid.lines().count());
    assertTrue(mermaid.contains("#34;#93; click n0 #34;https#58;#47;#47;example#46;com"));
    assertTrue(mermaid.contains("控制#124;#34;#91;#93;#60;#62; #38; #96;代码#96; #127777;"));
    assertFalse(mermaid.contains("<script>"));
    assertFalse(mermaid.contains("%%{init"));
    assertFalse(mermaid.contains("\nclick"));
    assertFalse(mermaid.contains("\u0001"));
    assertTrue(mermaid.contains("[\"end\"]"));
  }

  @Test
  void outputIsStableAcrossInsertionOrdersAndSupportsSixHundredNodes() throws Exception {
    var forward = new KnowledgeGraphic();
    var reverse = new KnowledgeGraphic();
    for (int i = 0; i < 600; i++) {
      forward.addNode("节点" + i, "说明");
      reverse.addNode("节点" + (599 - i), "说明");
    }
    for (int i = 1; i < 600; i++) {
      forward.addRelation("节点" + (i - 1), "关联", "节点" + i);
      reverse.addRelation("节点" + (599 - i), "关联", "节点" + (600 - i));
    }
    assertArrayEquals(exporter.export(forward, "mermaid", MAXIMUM),
        exporter.export(reverse, "mermaid", MAXIMUM));
    assertEquals(1200, text(forward, "mermaid").lines().count());
  }

  @Test
  void supportsEmptyGraphRejectsImagesDanglingRelationsAndOversizedOutput() throws Exception {
    var graph = new KnowledgeGraphic();
    assertEquals("flowchart LR\n  %% 暂无图谱数据\n", text(graph, "mermaid"));
    for (String format : java.util.List.of("mermaid", "json")) {
      assertThrows(IOException.class, () -> exporter.export(graph, format, 1));
    }
    for (String format : java.util.List.of("png", "svg", "html")) {
      assertThrows(IllegalArgumentException.class, () -> exporter.export(graph, format, MAXIMUM));
    }
    graph.addRelation("missing", "关联", "missing");
    assertThrows(IOException.class, () -> exporter.export(graph, "mermaid", MAXIMUM));
  }

  private String text(KnowledgeGraphic graph, String format) throws IOException {
    return new String(exporter.export(graph, format, MAXIMUM), StandardCharsets.UTF_8);
  }
}
