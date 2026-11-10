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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import top.rslly.iot.services.UserConfigServiceImpl;
import top.rslly.iot.services.agent.WorkspaceService;
import top.rslly.iot.services.knowledgeGraphic.dbo.KnowledgeGraphic;
import top.rslly.iot.services.knowledgeGraphic.dbo.KnowledgeGraphicFileProgress;
import top.rslly.iot.utility.ai.KnowledgeGraphicExporter;
import top.rslly.iot.utility.ai.sandbox.SandboxService;
import top.rslly.iot.utility.ai.tools.KnowledgeGraphicTool;
import top.rslly.iot.utility.ai.workspace.WorkspaceProperty;
import top.rslly.iot.utility.result.JsonResult;

@Service
@Slf4j
public class KnowledgeGraphicAgentServiceImpl implements KnowledgeGraphicAgentService {
  private final KnowledgeGraphicService graphService;
  private final KnowledgeGraphicTool generator;
  private final UserConfigServiceImpl config;
  private final WorkspaceService workspace;
  private final WorkspaceProperty property;
  private final SandboxService sandbox;
  private final KnowledgeGraphicExporter exporter;
  private final TaskExecutor executor;
  private final Set<Integer> busyProducts = ConcurrentHashMap.newKeySet();
  private final Map<String, Task> tasks = new ConcurrentHashMap<>();

  public KnowledgeGraphicAgentServiceImpl(KnowledgeGraphicService graphService,
      KnowledgeGraphicTool generator, UserConfigServiceImpl config, WorkspaceService workspace,
      WorkspaceProperty property, SandboxService sandbox, KnowledgeGraphicExporter exporter,
      @Qualifier("taskExecutor") TaskExecutor executor) {
    this.graphService = graphService;
    this.generator = generator;
    this.config = config;
    this.workspace = workspace;
    this.property = property;
    this.sandbox = sandbox;
    this.exporter = exporter;
    this.executor = executor;
  }

  @Override
  public boolean isEnabled(int productId) {
    try {
      return productId > 0
          && "true".equals(config.getConfigValue(productId, "knowledge_graph.toggle"));
    } catch (Exception e) {
      log.warn("Knowledge graph Agent configuration unavailable, productId={}", productId);
      return false;
    }
  }

  private void checkEnabled(int productId) {
    if (!isEnabled(productId)) {
      throw new IllegalArgumentException("知识图谱自动生成开关已关闭，Agent 无法调用知识图谱工具。");
    }
  }

  private void authorize(int productId, String chatId) {
    checkEnabled(productId);
    // Reuse the workspace's product/conversation ownership check, including sandbox-only
    // operations.
    workspace.listFiles(productId, chatId, 1, 1);
  }

  @Override
  public JsonResult<?> read(int productId, String chatId) {
    authorize(productId, chatId);
    return graphService.getKnowledgeGraphicByProductId(productId);
  }

  @Override
  public JsonResult<?> query(int productId, String chatId, String query) {
    authorize(productId, chatId);
    return graphService.queryKnowledgeGraphicJsonResult(query, productId);
  }

  @Override
  public Map<String, String> startImport(int productId, String chatId, List<Source> sources) {
    authorize(productId, chatId);
    acquire(productId);
    KnowledgeGraphicTool.PreparedFiles prepared = null;
    String taskId = UUID.randomUUID().toString();
    try {
      if (sources == null || sources.isEmpty() || sources.size() > 100) {
        throw new IllegalArgumentException("Provide between 1 and 100 source files");
      }
      List<KnowledgeGraphicTool.FileSource> files = new ArrayList<>();
      for (Source source : sources) {
        checkEnabled(productId);
        if ("workspace".equals(source.source())) {
          var download = workspace.download(productId, chatId, source.fileId());
          String filename;
          try (InputStream input = download.stream()) {
            filename = download.file().getName();
          }
          files.add(new KnowledgeGraphicTool.FileSource(filename,
              () -> workspace.download(productId, chatId, source.fileId()).stream()));
        } else if ("sandbox".equals(source.source())) {
          requireSandbox();
          String path = sandboxPath(source.path());
          files.add(new KnowledgeGraphicTool.FileSource(path.substring(path.lastIndexOf('/') + 1),
              () -> {
                byte[] bytes =
                    sandbox.exportWorkspaceFile(chatId, path, property.getMaxFileBytes());
                if (bytes == null) {
                  throw new IllegalArgumentException(
                      "Sandbox source missing, unsafe, or oversized");
                }
                return new ByteArrayInputStream(bytes);
              }));
        } else {
          throw new IllegalArgumentException("Supported sources: workspace, sandbox");
        }
      }
      prepared = generator.prepareFiles(files, property.getMaxFileBytes());
      checkEnabled(productId);
      KnowledgeGraphicFileProgress progress = KnowledgeGraphicFileProgress.create(taskId,
          files.size() == 1 ? files.getFirst().fileName() : files.size() + " 个文件");
      tasks.put(taskId, new Task(productId, chatId, progress));
      KnowledgeGraphicTool.PreparedFiles staged = prepared;
      executor.execute(() -> {
        try (staged) {
          generator.generateFiles(productId, staged, progress, () -> checkEnabled(productId));
        } catch (Exception e) {
          log.warn("Knowledge graph Agent generation failed, taskId={}", taskId, e);
          progress.fail("知识图谱生成失败：" + e.getMessage());
        } finally {
          busyProducts.remove(productId);
        }
      });
      return Map.of("taskId", taskId);
    } catch (IOException | RuntimeException e) {
      if (prepared != null) {
        prepared.close();
      }
      tasks.remove(taskId);
      busyProducts.remove(productId);
      if (e instanceof IllegalArgumentException invalid) {
        throw invalid;
      }
      throw new IllegalArgumentException("无法读取源文件或启动图谱生成任务", e);
    }
  }

  @Override
  public KnowledgeGraphicFileProgress progress(int productId, String chatId, String taskId) {
    authorize(productId, chatId);
    Task task = tasks.get(taskId);
    if (task == null || task.productId != productId || !task.chatId.equals(chatId)) {
      throw new IllegalArgumentException("当前会话中不存在此图谱生成任务");
    }
    return task.progress.snapshot();
  }

  @Override
  public JsonResult<?> clear(int productId, String chatId) {
    authorize(productId, chatId);
    acquire(productId);
    try {
      checkEnabled(productId);
      return graphService.deleteNodes(productId);
    } finally {
      busyProducts.remove(productId);
    }
  }

  private void acquire(int productId) {
    if (!busyProducts.add(productId)) {
      throw new IllegalArgumentException("知识图谱正在生成或清空，请稍后重试。");
    }
  }

  @Override
  public Map<String, Object> export(int productId, String chatId, String format, String target,
      String name, String path) {
    authorize(productId, chatId);
    if (!Set.of("mermaid", "json").contains(format)
        || !Set.of("workspace", "sandbox").contains(target)) {
      throw new IllegalArgumentException("Unsupported export format or target");
    }
    if ("workspace".equals(target) && path != null) {
      throw new IllegalArgumentException("path is only supported for sandbox exports");
    }
    String extension = "mermaid".equals(format) ? "mmd" : "json";
    String filename = name == null
        ? "knowledge-graphic-" + productId + "-" + Instant.now().toEpochMilli() + "-"
            + UUID.randomUUID().toString().substring(0, 8) + "." + extension
        : name;
    validateName(filename, extension);
    String destination = null;
    if ("sandbox".equals(target)) {
      requireSandbox();
      destination = path == null ? "/workspace/exports/" + filename : sandboxPath(path);
      validateName(destination.substring(destination.lastIndexOf('/') + 1), extension);
      if (name != null && !destination.endsWith("/" + name)) {
        throw new IllegalArgumentException("name must match the filename in path");
      }
    }
    JsonResult<?> result = graphService.getKnowledgeGraphicByProductId(productId);
    if (result == null || !Boolean.TRUE.equals(result.getSuccess())
        || !(result.getData()instanceof KnowledgeGraphic graph)) {
      throw new IllegalArgumentException("无法读取当前产品知识图谱");
    }
    byte[] bytes;
    try {
      checkEnabled(productId);
      bytes = exporter.export(graph, format, property.getMaxFileBytes());
    } catch (IOException e) {
      throw new IllegalArgumentException("图谱导出失败：" + e.getMessage(), e);
    }
    if (bytes.length > property.getMaxFileBytes()) {
      throw new IllegalArgumentException("导出文件超过单文件大小限制");
    }
    checkEnabled(productId);
    if ("workspace".equals(target)) {
      var file = workspace.writeBinary(productId, chatId, filename, bytes);
      return Map.of("fileId", file.getId(), "name", file.getName(), "size", file.getSize(),
          "downloadPath", "/api/v2/workspaces/files/" + file.getId()
              + "/download?productId=" + productId + "&chatId="
              + URLEncoder.encode(chatId, StandardCharsets.UTF_8));
    }
    if (!sandbox.importWorkspaceFile(chatId, destination, new ByteArrayInputStream(bytes),
        property.getMaxFileBytes())) {
      throw new IllegalArgumentException("沙箱保存失败：目标已存在、路径不安全或沙箱不可用");
    }
    return Map.of("sandboxPath", destination, "size", bytes.length);
  }

  private void requireSandbox() {
    if (!sandbox.isAvailable()) {
      throw new IllegalArgumentException("沙箱不可用；可将 Mermaid 或 JSON 文件保存到工作区。");
    }
  }

  private void validateName(String name, String format) {
    if (name.isBlank() || name.length() > 255 || name.contains("/") || name.contains("\\")
        || name.chars().anyMatch(Character::isISOControl)
        || !name.toLowerCase(java.util.Locale.ROOT).endsWith("." + format)) {
      throw new IllegalArgumentException("Provide a filename with the selected format's extension");
    }
  }

  private String sandboxPath(String path) {
    if (path == null || path.isBlank() || path.contains("\\")
        || path.chars().anyMatch(Character::isISOControl)) {
      throw new IllegalArgumentException("Invalid sandbox path");
    }
    String absolute = path.startsWith("/") ? path : "/workspace/" + path;
    if (!absolute.startsWith("/workspace/") || absolute.length() > 2048) {
      throw new IllegalArgumentException("Sandbox path must be inside /workspace");
    }
    for (String part : absolute.substring("/workspace/".length()).split("/", -1)) {
      if (part.isBlank() || part.equals(".") || part.equals("..")) {
        throw new IllegalArgumentException("Invalid sandbox path component");
      }
    }
    return absolute;
  }

  private record Task(int productId, String chatId, KnowledgeGraphicFileProgress progress) {}
}
