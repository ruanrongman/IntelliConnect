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
package top.rslly.iot.utility.ai.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class LlmJsonResponseParserTest {
  private static final String CALL =
      """
          <｜DSML｜ calls>
          <｜DSML｜ invoke name="workspaceTool">
          <｜DSML｜ parameter name="args" string="false">{"action": "list", "pageNum": 1, "pageSize": 20}</｜DSML｜ parameter>
          </｜DSML｜ invoke>
          </｜DSML｜ calls>
          """;

  @Test
  void normalizesReportedWorkspaceCallWithoutNestingArgs() {
    JSONObject result = LlmJsonResponseParser.parse(CALL);

    JSONObject action = result.getJSONObject("action");
    assertEquals("workspaceTool", action.getString("name"));
    JSONObject args = action.getJSONObject("args");
    assertEquals("list", args.getString("action"));
    assertEquals(1, args.getIntValue("pageNum"));
    assertEquals(20, args.getIntValue("pageSize"));
    assertFalse(args.containsKey("args"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"json", "JSON", "xml", ""})
  void acceptsSurroundingMarkdownFence(String language) {
    assertEquals(LlmJsonResponseParser.parse(CALL),
        LlmJsonResponseParser.parse("```" + language + "\n" + CALL + "```"));
  }

  @Test
  void preservesJsonContentIncludingEmbeddedFencesAndDsmlExamples() {
    JSONObject args = new JSONObject();
    args.put("content", "```json\n{\"example\":true}\n```\n" + CALL);
    JSONObject action = new JSONObject();
    action.put("name", "finish");
    action.put("args", args);
    JSONObject response = new JSONObject();
    response.put("action", action);

    assertEquals(response, LlmJsonResponseParser.parse(response.toJSONString()));
    assertEquals(response,
        LlmJsonResponseParser.parse("```json\n" + response.toJSONString() + "\n```"));
  }

  @Test
  void preservesLiteralStringsAndTypedParametersForMcpTools() {
    String response = """
        <｜DSML｜calls>
        <｜DSML｜invoke name="server:tool">
        <｜DSML｜parameter name="text" string="true">  a & b < c
        ```json
        {}  </｜DSML｜parameter>
        <｜DSML｜parameter name="enabled" string="false">true</｜DSML｜parameter>
        <｜DSML｜parameter name="limit" string="false">20</｜DSML｜parameter>
        <｜DSML｜parameter name="items" string="false">[1,2]</｜DSML｜parameter>
        </｜DSML｜invoke>
        </｜DSML｜calls>
        """;

    JSONObject action = LlmJsonResponseParser.parse(response).getJSONObject("action");
    assertEquals("server:tool", action.getString("name"));
    JSONObject args = action.getJSONObject("args");
    assertEquals("  a & b < c\n```json\n{}  ", args.getString("text"));
    assertEquals(Boolean.TRUE, args.get("enabled"));
    assertEquals(20, args.get("limit"));
    assertEquals(JSON.parseArray("[1,2]"), args.getJSONArray("items"));
  }

  @Test
  void acceptsStringWrappedToolArgs() {
    JSONObject action =
        LlmJsonResponseParser.parse(CALL.replace("string=\"false\"", "string=\"true\""))
            .getJSONObject("action");

    assertEquals("list", JSON.parseObject(action.getString("args")).getString("action"));
  }

  @Test
  void acceptsCallWithoutParameters() {
    JSONObject action = LlmJsonResponseParser.parse("""
        <｜DSML｜calls><｜DSML｜invoke name="tool"></｜DSML｜invoke></｜DSML｜calls>
        """).getJSONObject("action");

    assertEquals(new JSONObject(), action.getJSONObject("args"));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "null", "not json", "<｜DSML｜ calls>",
      "<｜DSML｜calls><｜DSML｜invoke name=\"tool\"></｜DSML｜invoke>",
      "<｜DSML｜calls><｜DSML｜invoke name=\"tool\">garbage</｜DSML｜invoke></｜DSML｜calls>"})
  void rejectsInvalidOrIncompleteResponses(String response) {
    assertThrows(RuntimeException.class, () -> LlmJsonResponseParser.parse(response));
  }

  @Test
  void rejectsMultipleCallsInsteadOfSilentlyDroppingActions() {
    String invoke = CALL.substring(CALL.indexOf("<｜DSML｜ invoke"), CALL.indexOf("</｜DSML｜ calls>"));

    assertThrows(IllegalArgumentException.class,
        () -> LlmJsonResponseParser
            .parse(CALL.replace("</｜DSML｜ calls>", invoke + "</｜DSML｜ calls>")));
    assertThrows(IllegalArgumentException.class, () -> LlmJsonResponseParser.parse(CALL + CALL));
  }

  @Test
  void rejectsMalformedJsonAndDuplicateOrInvalidArgs() {
    assertThrows(RuntimeException.class,
        () -> LlmJsonResponseParser.parse(
            CALL.replace("{\"action\": \"list\", \"pageNum\": 1, \"pageSize\": 20}", "{broken")));
    assertThrows(IllegalArgumentException.class,
        () -> LlmJsonResponseParser.parse(
            CALL.replace("{\"action\": \"list\", \"pageNum\": 1, \"pageSize\": 20}", "null")));
    String duplicate = "<｜DSML｜ parameter name=\"args\" string=\"false\">{}</｜DSML｜ parameter>";
    assertThrows(IllegalArgumentException.class,
        () -> LlmJsonResponseParser
            .parse(CALL.replace("</｜DSML｜ invoke>", duplicate + "</｜DSML｜ invoke>")));
  }
}
