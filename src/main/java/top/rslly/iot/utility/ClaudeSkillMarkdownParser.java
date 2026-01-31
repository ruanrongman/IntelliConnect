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
package top.rslly.iot.utility;

import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import top.rslly.iot.param.prompt.ClaudeSkillMeta;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class ClaudeSkillMarkdownParser {

  private static final String DELIMITER = "---";

  /**
   * 解析 Claude Skill Markdown 文件
   *
   * @param bytes Markdown 文件字节数组
   * @return ClaudeSkillMeta 包含 name 和 description
   * @throws IllegalArgumentException 当文件格式不符合规范时
   */
  public static ClaudeSkillMeta parse(byte[] bytes) {
    if (bytes == null || bytes.length == 0) {
      throw new IllegalArgumentException("File content is empty");
    }

    String markdown = new String(bytes, StandardCharsets.UTF_8);

    // 检查是否以 YAML frontmatter 开头
    if (!markdown.startsWith(DELIMITER)) {
      throw new IllegalArgumentException("Missing YAML frontmatter delimiter at start");
    }

    // 查找第二个分隔符
    int secondDelimiterIndex = markdown.indexOf(DELIMITER, DELIMITER.length());
    if (secondDelimiterIndex < 0) {
      throw new IllegalArgumentException("Missing closing YAML frontmatter delimiter");
    }

    // 提取 YAML 内容
    String yamlText = markdown
        .substring(DELIMITER.length(), secondDelimiterIndex)
        .trim();

    if (yamlText.isEmpty()) {
      throw new IllegalArgumentException("YAML frontmatter is empty");
    }

    // 解析 YAML
    LoaderOptions loaderOptions = new LoaderOptions();
    Yaml yaml = new Yaml(new SafeConstructor(loaderOptions));
    Map<String, Object> yamlMap;

    try {
      yamlMap = yaml.load(yamlText);
    } catch (Exception e) {
      log.error("Failed to parse YAML", e);
      throw new IllegalArgumentException("Invalid YAML syntax: " + e.getMessage());
    }

    if (yamlMap == null || yamlMap.isEmpty()) {
      throw new IllegalArgumentException("YAML frontmatter contains no data");
    }

    // 提取 name 和 description
    String name = Objects.toString(yamlMap.get("name"), null);
    String description = Objects.toString(yamlMap.get("description"), null);

    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("Missing or empty 'name' field in YAML frontmatter");
    }

    if (description == null || description.trim().isEmpty()) {
      throw new IllegalArgumentException(
          "Missing or empty 'description' field in YAML frontmatter");
    }

    // 构建返回对象
    ClaudeSkillMeta meta = new ClaudeSkillMeta();
    meta.setName(name.trim());
    meta.setDescription(description.trim());

    return meta;
  }
}
