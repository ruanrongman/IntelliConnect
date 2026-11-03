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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.rslly.iot.services.agent.ProductToolsBanServiceImpl;
import top.rslly.iot.services.agent.WorkspaceService;
import top.rslly.iot.utility.ai.GlobalMessageContext;
import top.rslly.iot.utility.ai.pdf.MarkdownPdfProperty;
import top.rslly.iot.utility.ai.pdf.MarkdownPdfRenderer;
import top.rslly.iot.utility.ai.sandbox.SandboxService;

/** Renders sandbox Markdown in Java and saves the resulting PDF directly to conversation files. */
@Component
@RequiredArgsConstructor
@Slf4j
public class MarkdownToPdfTool implements BaseTool<String> {
  public static final String BAN_KEY = "markdownToPdf";
  public static final String NAME = "markdownToPdfTool";
  public static final String ROUTING_DESCRIPTION =
      "Convert Markdown files to PDF with Chinese typography and automatically save the PDF to conversation files";

  private final SandboxService sandboxService;
  private final WorkspaceService workspaceService;
  private final ProductToolsBanServiceImpl productToolsBanService;
  private final ObjectMapper objectMapper;
  private final MarkdownPdfRenderer renderer;
  private final MarkdownPdfProperty property;

  @Getter
  private final String name = NAME;
  @Getter
  private final String description =
      """
          Convert a UTF-8 Markdown file from the current cloud computer to PDF using the Java backend.
          Prefer this tool for Markdown-to-PDF tasks instead of assembling conversion commands or installing dependencies.
          Args: a JSON string, e.g. {"path":"reports/report.md","outputName":"report.pdf","title":"Report"}
          - path: required .md/.markdown path inside the current conversation's sandbox /workspace.
          - outputName: optional PDF filename without directories; defaults to the source filename with .pdf.
          - title: optional document title; defaults to the source filename without extension.
          - Supports Chinese/English, headings, lists, tables, fenced code, local raster images and A4 pagination.
            Image paths are relative to the Markdown file, inside /workspace. Remote images are skipped
            with warnings; download them first. Raw HTML is escaped. Math, SVG and Mermaid are not
            rendered; prepare them as local PNG/JPEG images first. Default source limit is 2 MiB.
          - Rendering runs entirely in Java; no conversion scripts, packages or image changes are required in the sandbox.
            Chinese rendering uses a TrueType font configured on the backend through ai.pdf.font-path.
          - PDF is validated and saved directly to persistent conversation files. Returns success, pages,
            warnings, fileId, name, size and downloadPath. Do not export the PDF again or look for it in
            the sandbox. downloadPath requires the user's existing Authorization header; it is not a public link.
          - Product and conversation are supplied by the server; do not specify productId/chatId.
          """;

  public boolean isAvailable(int productId) {
    try {
      return sandboxService.isAvailable()
          && !productToolsBanService.getProductToolsBanList(productId).contains(BAN_KEY);
    } catch (Exception e) {
      log.warn("Cannot check Markdown PDF tool availability for product {}", productId, e);
      return false;
    }
  }

  @Override
  public String run(String question) {
    return error("INVALID_CONTEXT", "A valid product and conversation context is required.");
  }

  @Override
  public String run(String question, Map<String, Object> context) {
    String chatId = GlobalMessageContext.memoryChatId(context);
    if (context == null || !(context.get(GlobalMessageContext.PRODUCT_ID)instanceof Number product)
        || product.intValue() <= 0 || chatId == null || chatId.isBlank()) {
      return run(question);
    }
    int productId = product.intValue();
    try {
      // Always recheck, including nested agents and tool registries built before settings changed.
      if (productToolsBanService.getProductToolsBanList(productId).contains(BAN_KEY)) {
        return error("TOOL_DISABLED", "Markdown to PDF is disabled for this product.");
      }
      if (!sandboxService.isAvailable()) {
        return error("SANDBOX_UNAVAILABLE", "The cloud computer is disabled or unavailable.");
      }
      JsonNode args = question == null ? null : objectMapper.readTree(question);
      if (args == null || !args.isObject() || args.has("productId") || args.has("chatId")) {
        return error("INVALID_ARGUMENTS", "Provide a JSON object without productId/chatId.");
      }
      var fields = args.fieldNames();
      while (fields.hasNext()) {
        if (!java.util.Set.of("path", "outputName", "title").contains(fields.next())) {
          return error("INVALID_ARGUMENTS", "Supported fields are path, outputName and title.");
        }
      }
      String source = workspacePath(required(args, "path"));
      if (!source.toLowerCase(java.util.Locale.ROOT).matches(".*\\.(md|markdown)$")) {
        return error("INVALID_ARGUMENTS", "path must identify a .md or .markdown file.");
      }
      String stem = source.substring(source.lastIndexOf('/') + 1, source.lastIndexOf('.'));
      String name = args.has("outputName") ? required(args, "outputName") : stem + ".pdf";
      if (name.length() > 255 || name.contains("/") || name.contains("\\")
          || name.chars().anyMatch(Character::isISOControl)
          || !name.toLowerCase(java.util.Locale.ROOT).endsWith(".pdf")) {
        return error("INVALID_ARGUMENTS",
            "outputName must be a PDF filename without directories, at most 255 characters.");
      }
      String title = args.has("title") ? required(args, "title") : stem;
      if (title.length() > 256) {
        return error("INVALID_ARGUMENTS", "title must not exceed 256 characters.");
      }
      byte[] markdown =
          sandboxService.exportWorkspaceFile(chatId, source, property.getMaxSourceBytes());
      if (markdown == null) {
        return error("SOURCE_UNAVAILABLE",
            "Cannot read Markdown from the current sandbox: check path, lifetime and source size limit.");
      }
      var rendered = renderer.render(markdown, source, title,
          path -> sandboxService.exportWorkspaceFile(chatId, path, property.getMaxImageBytes()));
      top.rslly.iot.models.WorkspaceFileEntity file;
      try {
        file = workspaceService.writeBinary(productId, chatId, name, rendered.bytes());
      } catch (Exception e) {
        log.warn("Cannot save generated PDF to conversation {}", chatId, e);
        return error("STORAGE_FAILED",
            "PDF could not be saved to conversation files; check storage availability and capacity. "
                + limit(e.getMessage()));
      }
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("success", true);
      result.put("pages", rendered.pages());
      result.put("warnings", rendered.warnings());
      result.put("fileId", file.getId());
      result.put("name", file.getName());
      result.put("size", file.getSize());
      result.put("downloadPath", "/api/v2/workspaces/files/" + file.getId()
          + "/download?productId=" + productId + "&chatId="
          + java.net.URLEncoder.encode(chatId, StandardCharsets.UTF_8));
      return objectMapper.writeValueAsString(result);
    } catch (MarkdownPdfRenderer.PdfException e) {
      return error(e.getCode(), limit(e.getMessage()));
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      return error("INVALID_ARGUMENTS", "Arguments must be a valid JSON object.");
    } catch (IllegalArgumentException e) {
      return error("INVALID_ARGUMENTS", limit(e.getMessage()));
    } catch (Exception e) {
      log.warn("Markdown PDF conversion failed for conversation {}", chatId, e);
      return error("CONVERSION_FAILED",
          "Markdown PDF conversion failed; check backend font and conversation file storage availability.");
    }
  }

  private String workspacePath(String value) {
    if (value.contains("\\") || value.length() > 2048
        || value.chars().anyMatch(Character::isISOControl)) {
      throw new IllegalArgumentException("Use a POSIX path inside /workspace.");
    }
    String path = SandboxFileSupport.resolvePath(value);
    if (!path.startsWith(SandboxService.WORKSPACE_DIR + "/")) {
      throw new IllegalArgumentException("Path must be inside /workspace.");
    }
    for (String part : path.substring(SandboxService.WORKSPACE_DIR.length() + 1).split("/", -1)) {
      if (part.isEmpty() || part.equals(".") || part.equals("..")) {
        throw new IllegalArgumentException("Path must not contain empty, . or .. components.");
      }
    }
    return path;
  }

  private String required(JsonNode args, String field) {
    if (!args.path(field).isTextual() || args.path(field).asText().isBlank()) {
      throw new IllegalArgumentException(field + " must be a non-empty string.");
    }
    return args.path(field).asText();
  }

  private String error(String code, String message) {
    try {
      return objectMapper.writeValueAsString(Map.of("success", false, "code", code,
          "message", message));
    } catch (Exception e) {
      throw new IllegalStateException("Cannot serialize PDF tool result", e);
    }
  }

  private String limit(String message) {
    return message == null ? "" : message.substring(0, Math.min(message.length(), 1500));
  }
}
