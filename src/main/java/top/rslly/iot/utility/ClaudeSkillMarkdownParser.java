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
      // 技能作者常在 description 等值中写裸冒号(如 "Use when: ..."), 这不是合法 YAML;
      // 先尝试把普通标量值加引号后重试
      try {
        yamlMap = new Yaml(new SafeConstructor(loaderOptions))
            .load(quoteRiskyScalarValues(yamlText));
      } catch (Exception retryError) {
        log.error("Failed to parse YAML", e);
        throw new IllegalArgumentException("Invalid YAML syntax: " + e.getMessage());
      }
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

  /** front-matter 的 key: value 行 */
  private static final java.util.regex.Pattern KEY_VALUE_PATTERN =
      java.util.regex.Pattern.compile("^(\\s*[A-Za-z0-9_-]+\\s*):(.*)$");

  /**
   * 把 front-matter 中含裸冒号的普通标量值加单引号, 兼容技能作者未按 YAML 规范加引号的写法.
   *
   * <p>
   * 仅处理形如 {@code key: value} 的行; 值已是引号/块/流集合写法时不处理。
   */
  private static String quoteRiskyScalarValues(String yamlText) {
    StringBuilder sb = new StringBuilder(yamlText.length() + 32);
    for (String line : yamlText.split("\n", -1)) {
      var matcher = KEY_VALUE_PATTERN.matcher(line);
      if (matcher.matches()) {
        String value = matcher.group(2).trim();
        if (needsQuoting(value)) {
          sb.append(matcher.group(1)).append(": '")
              .append(value.replace("'", "''")).append("'\n");
          continue;
        }
      }
      sb.append(line).append('\n');
    }
    return sb.toString();
  }

  private static boolean needsQuoting(String value) {
    if (value.isEmpty()) {
      return false;
    }
    char first = value.charAt(0);
    if (first == '"' || first == '\'' || first == '[' || first == '{' || first == '|'
        || first == '>' || first == '&' || first == '*') {
      return false;
    }
    return value.contains(": ") || value.endsWith(":") || value.contains(" #");
  }
}
