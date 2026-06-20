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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import top.rslly.iot.models.KnowledgeGraphicNodeEntity;
import top.rslly.iot.services.knowledgeGraphic.KnowledgeGraphicService;
import top.rslly.iot.services.knowledgeGraphic.dbo.KnowledgeGraphic;
import top.rslly.iot.services.knowledgeGraphic.dbo.KnowledgeGraphicFileProgress;
import top.rslly.iot.services.thingsModel.ProductService;
import top.rslly.iot.utility.ai.IcAiException;
import top.rslly.iot.utility.ai.LlmDiyUtility;
import top.rslly.iot.utility.ai.ModelMessage;
import top.rslly.iot.utility.ai.ModelMessageRole;
import top.rslly.iot.utility.ai.llm.LLM;
import top.rslly.iot.utility.ai.prompts.KnowledgeGraphicPrompt;
import top.rslly.iot.utility.ai.rag.RagUtility;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
@Data
@Slf4j
public class KnowledgeGraphicTool {
  @Autowired
  private ProductService productService;
  @Autowired
  private KnowledgeGraphicService knowledgeGraphicService;
  @Autowired
  private LlmDiyUtility llmDiyUtility;
  @Autowired
  private KnowledgeGraphicPrompt knowledgeGraphicPrompt;
  @Autowired
  @Qualifier("taskExecutor")
  private TaskExecutor taskExecutor;
  @Value("${ai.knowledge_graphic_llm}")
  private String llmName;
  @Value("${ai.knowledge_graphic.max_graphic_length:30000}")
  private int maxGraphicLength;
  @Value("${ai.knowledge_graphic.chunk_chars:12000}")
  private int chunkChars;
  @Value("${ai.knowledge_graphic.chunk_overlap_chars:500}")
  private int chunkOverlapChars;
  private final Map<String, KnowledgeGraphicFileProgress> fileProgressMap =
      new ConcurrentHashMap<>();
  private String name = "knowledgeGraphicTool";
  private String description = """
      This tool is working for knowledge graphic generation to help LLM
      handle relations between entities.
      """;

  public String run(String question) {
    return null;
  }

  @Async("taskExecutor")
  public void run(String question, Map<String, Object> globalMessage) {
    int productId = (int) globalMessage.get("productId");
    if (productService.findAllById(productId).isEmpty())
      return;
    List<ModelMessage> memory =
        Optional.ofNullable((List<ModelMessage>) globalMessage.get("memory"))
            .orElse(Collections.emptyList());
    String sourceText = buildConversationSource(question, memory);
    try {
      generateFromText(productId, sourceText, false, true, null);
    } catch (Exception e) {
      log.error("Knowledge Graphic extract error:\n{}", e.getMessage());
    }
  }

  private @NonNull ModelMessage getUserMessage(String knowledgeSystemPrompt) {
    ModelMessage userMessage;
    if (knowledgeSystemPrompt.length() > maxGraphicLength / 2) {
      log.info("Knowledge start summary");
      userMessage = new ModelMessage(ModelMessageRole.USER.value(),
          """
              The graph has reached its maximum capacity.
              Summarize the graph immediately, ensuring that the most recent content is not lost,
              and begin generating the graph.
              """);
    } else {
      userMessage = new ModelMessage(ModelMessageRole.USER.value(),
          "Start generate knowledge graphic.");
    }
    return userMessage;
  }

  public JsonResult<?> startFileGeneration(int productId, MultipartFile multipartFile) {
    return startFileGeneration(productId, new MultipartFile[] {multipartFile});
  }

  public JsonResult<?> startFileGeneration(int productId, MultipartFile[] multipartFiles) {
    if (multipartFiles == null || multipartFiles.length == 0) {
      return ResultTool.fail(ResultCode.PARAM_NOT_COMPLETE);
    }
    List<MultipartFile> files = Arrays.stream(multipartFiles)
        .filter(Objects::nonNull)
        .toList();
    if (files.isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_COMPLETE);
    }
    for (MultipartFile multipartFile : files) {
      JsonResult<?> validateResult = validateUploadFile(productId, multipartFile);
      if (!validateResult.getSuccess()) {
        return validateResult;
      }
    }
    String taskId = UUID.randomUUID().toString();
    String progressFileName = files.size() == 1 ? files.get(0).getOriginalFilename()
        : files.size() + " 个文件";
    KnowledgeGraphicFileProgress progress =
        KnowledgeGraphicFileProgress.create(taskId, progressFileName);
    fileProgressMap.put(taskId, progress);
    try {
      Path tempDir = Files.createTempDirectory("kg-upload-");
      List<UploadFile> uploadFiles = new ArrayList<>();
      for (int i = 0; i < files.size(); i++) {
        MultipartFile multipartFile = files.get(i);
        String originalFileName = multipartFile.getOriginalFilename();
        String extension = RagUtility.getFileExtension(originalFileName).toLowerCase(Locale.ROOT);
        Path tempFile = tempDir.resolve("upload-" + i + "." + extension);
        multipartFile.transferTo(tempFile.toFile());
        uploadFiles.add(new UploadFile(tempFile, originalFileName));
      }
      taskExecutor.execute(() -> processFileGenerationTask(productId, uploadFiles, tempDir,
          progress));
      return ResultTool.success(Map.of("taskId", taskId));
    } catch (IOException e) {
      progress.fail("文件保存失败：" + e.getMessage());
      return ResultTool.fail(ResultCode.COMMON_FAIL);
    }
  }

  private JsonResult<?> validateUploadFile(int productId, MultipartFile multipartFile) {
    if (multipartFile == null || multipartFile.isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_COMPLETE);
    }
    if (productService.findAllById(productId).isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    String originalFileName = multipartFile.getOriginalFilename();
    if (originalFileName == null || originalFileName.isBlank()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_COMPLETE);
    }
    String extension = RagUtility.getFileExtension(originalFileName).toLowerCase(Locale.ROOT);
    if (!extension.equals("zip") && !RagUtility.isSupportedKnowledgeFile(originalFileName)) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    return ResultTool.success();
  }

  public JsonResult<?> getFileGenerationProgress(String taskId) {
    KnowledgeGraphicFileProgress progress = fileProgressMap.get(taskId);
    if (progress == null) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    return ResultTool.success(progress);
  }

  private void processFileGenerationTask(int productId, List<UploadFile> uploadFiles, Path tempDir,
      KnowledgeGraphicFileProgress progress) {
    try {
      progress.update("parsing", 5, "正在解析文件");
      List<SourceDocument> documents = new ArrayList<>();
      for (UploadFile uploadFile : uploadFiles) {
        progress.update("parsing", 5, "正在解析文件：" + uploadFile.fileName());
        documents.addAll(parseSourceDocuments(uploadFile.path(), uploadFile.fileName(), tempDir));
      }
      if (documents.isEmpty()) {
        throw new IllegalArgumentException("没有可处理的文件内容");
      }
      List<ChunkDocument> chunks = new ArrayList<>();
      for (SourceDocument document : documents) {
        List<String> textChunks = splitText(document.text());
        for (int i = 0; i < textChunks.size(); i++) {
          chunks.add(new ChunkDocument(document.fileName(), textChunks.get(i), i + 1,
              textChunks.size()));
        }
      }
      if (chunks.isEmpty()) {
        throw new IllegalArgumentException("没有可处理的文本内容");
      }
      progress.update("chunking", 10, "已完成文本分块，共 " + chunks.size() + " 个片段");
      LLM llm = llmDiyUtility.getDiyLlm(productId, llmName, "knowledgeGraphic");
      GraphMergeResult totalResult = new GraphMergeResult(0, 0);
      for (int i = 0; i < chunks.size(); i++) {
        ChunkDocument chunk = chunks.get(i);
        int percent = 10 + (int) Math.floor((i * 80.0) / chunks.size());
        progress.updateChunk(chunk.fileName(), i + 1, chunks.size(), percent,
            "正在抽取图谱：" + chunk.fileName() + " (" + chunk.indexInFile() + "/"
                + chunk.totalInFile() + ")");
        totalResult = totalResult.add(generateSingleChunk(productId, chunk.text(), llm, true,
            false));
      }
      if (totalResult.isEmpty()) {
        throw new IllegalStateException("未从文件中抽取到可写入的节点或关系");
      }
      progress.update("writing", 95, "正在写入图谱数据");
      GraphMergeResult persistedResult = getPersistedGraphResult(productId);
      if (persistedResult.isEmpty()) {
        throw new IllegalStateException("图谱数据已处理但写入后查询为空，请检查后端运行进程和数据库连接");
      }
      progress.complete("知识图谱生成完成，写入节点 " + totalResult.nodeCount() + " 个，关系 "
          + totalResult.relationCount() + " 个，当前图谱共 " + persistedResult.nodeCount()
          + " 个节点、" + persistedResult.relationCount() + " 条关系");
    } catch (Exception e) {
      log.error("Knowledge graphic file generation failed", e);
      progress.fail("知识图谱生成失败：" + e.getMessage());
    } finally {
      deleteDirectory(tempDir);
    }
  }

  private List<SourceDocument> parseSourceDocuments(Path file, String fileName, Path tempDir)
      throws IOException {
    String extension = RagUtility.getFileExtension(fileName).toLowerCase(Locale.ROOT);
    if (!extension.equals("zip")) {
      String text = RagUtility.parseFile(file.toFile());
      if (text == null || text.isBlank()) {
        return List.of();
      }
      return List.of(new SourceDocument(fileName, text));
    }
    List<SourceDocument> documents = new ArrayList<>();
    Path unzipDir = tempDir.resolve("zip").normalize();
    Files.createDirectories(unzipDir);
    try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(file))) {
      ZipEntry entry;
      while ((entry = zipInputStream.getNextEntry()) != null) {
        if (entry.isDirectory()) {
          continue;
        }
        String entryName = entry.getName();
        if (!RagUtility.isSupportedKnowledgeFile(entryName)) {
          continue;
        }
        Path entryPath = unzipDir.resolve(entryName).normalize();
        if (!entryPath.startsWith(unzipDir)) {
          throw new IllegalArgumentException("zip 文件包含非法路径：" + entryName);
        }
        Files.createDirectories(entryPath.getParent());
        Files.copy(zipInputStream, entryPath, StandardCopyOption.REPLACE_EXISTING);
        String text = RagUtility.parseFile(entryPath.toFile());
        if (text != null && !text.isBlank()) {
          documents.add(new SourceDocument(entryName, text));
        }
      }
    }
    return documents;
  }

  private void generateFromText(int productId, String sourceText, boolean fileMode,
      boolean allowDelete, KnowledgeGraphicFileProgress progress) {
    List<String> chunks = splitText(sourceText);
    if (chunks.isEmpty()) {
      return;
    }
    LLM llm = llmDiyUtility.getDiyLlm(productId, llmName, "knowledgeGraphic");
    for (int i = 0; i < chunks.size(); i++) {
      if (progress != null) {
        int percent = 10 + (int) Math.floor((i * 80.0) / chunks.size());
        progress.updateChunk(progress.getFileName(), i + 1, chunks.size(), percent,
            "正在抽取图谱片段 " + (i + 1) + "/" + chunks.size());
      }
      generateSingleChunk(productId, chunks.get(i), llm, fileMode, allowDelete);
    }
  }

  private GraphMergeResult generateSingleChunk(int productId, String chunk, LLM llm,
      boolean fileMode,
      boolean allowDelete) {
    String systemPrompt = fileMode
        ? knowledgeGraphicPrompt.getKnowledgeGraphicExtractPrompt(productId)
        : knowledgeGraphicPrompt.getKnowledgeGraphicPrompt(productId);
    if (systemPrompt.length() > maxGraphicLength) {
      log.error("Knowledge Graphic extract error:\n{}", "Knowledge Graphic is too long.");
      return new GraphMergeResult(0, 0);
    }
    List<ModelMessage> messages = new ArrayList<>();
    messages.add(new ModelMessage(ModelMessageRole.SYSTEM.value(), systemPrompt));
    messages.add(new ModelMessage(ModelMessageRole.USER.value(), buildChunkUserMessage(chunk,
        fileMode, systemPrompt)));
    JSONObject response = llm.jsonChat(String.valueOf(messages.get(1).getContent()), messages,
        false);
    if (response == null || response.getJSONObject("action") == null) {
      return new GraphMergeResult(0, 0);
    }
    return process_llm_result(response.getJSONObject("action"), productId, allowDelete);
  }

  private String buildChunkUserMessage(String chunk, boolean fileMode, String systemPrompt) {
    String prefix = fileMode ? "Source text:\n" : "Current conversation:\n";
    return getUserMessage(systemPrompt).getContent() + "\n\n" + prefix + chunk;
  }

  private List<String> splitText(String text) {
    if (text == null || text.isBlank()) {
      return List.of();
    }
    int size = Math.max(1000, chunkChars);
    int overlap = Math.max(0, Math.min(chunkOverlapChars, size - 1));
    if (text.length() <= size) {
      return List.of(text);
    }
    List<String> chunks = new ArrayList<>();
    int start = 0;
    while (start < text.length()) {
      int end = Math.min(text.length(), start + size);
      chunks.add(text.substring(start, end));
      if (end >= text.length()) {
        break;
      }
      start = end - overlap;
    }
    return chunks;
  }

  private String buildConversationSource(String question, List<ModelMessage> memory) {
    StringBuilder sourceBuilder = new StringBuilder();
    for (ModelMessage message : memory) {
      sourceBuilder.append(message.getRole()).append(": ").append(message.getContent())
          .append("\n");
    }
    if (question != null && !question.isBlank()) {
      sourceBuilder.append(ModelMessageRole.USER.value()).append(": ").append(question);
    }
    return sourceBuilder.toString();
  }

  private GraphMergeResult process_llm_result(JSONObject jsonObject, int productId,
      boolean allowDelete) {
    KnowledgeGraphic graphic =
        normalizeGraphicJson(jsonObject).toJavaObject(KnowledgeGraphic.class);
    if (graphic.nodes == null) {
      graphic.nodes = new HashSet<>();
    }
    if (graphic.relations == null) {
      graphic.relations = new HashSet<>();
    }
    int nodeCount = 0;
    int relationCount = 0;
    for (KnowledgeGraphic.Node node : graphic.nodes) {
      if (node.name == null || node.name.isBlank()) {
        continue;
      }
      String nodeAction = Optional.ofNullable(node.nodeAction).orElse("New");
      if (allowDelete && nodeAction.equalsIgnoreCase("Delete")) {
        KnowledgeGraphicNodeEntity nodeDb = (KnowledgeGraphicNodeEntity) knowledgeGraphicService
            .getNode(node.name, productId).getData();
        if (nodeDb != null) {
          knowledgeGraphicService.deleteNode(nodeDb.getId());
        }
        continue;
      } else if (!allowDelete && nodeAction.equalsIgnoreCase("Delete")) {
        continue;
      }
      KnowledgeGraphicNodeEntity nodeDb = (KnowledgeGraphicNodeEntity) knowledgeGraphicService
          .addNode(node.name, node.des, productId).getData();
      if (nodeDb != null) {
        if (node.attributes != null) {
          knowledgeGraphicService.addAttributes(node.attributes.stream().toList(), nodeDb.getId());
        }
        nodeCount++;
      }
    }
    for (KnowledgeGraphic.Relation relation : graphic.relations) {
      if (relation.name == null || relation.from == null || relation.to == null
          || relation.name.isBlank() || relation.from.isBlank() || relation.to.isBlank()) {
        continue;
      }
      String relationAction = Optional.ofNullable(relation.relationAction).orElse("New");
      if (allowDelete && relationAction.equalsIgnoreCase("Delete")) {
        KnowledgeGraphicNodeEntity nodeFrom = (KnowledgeGraphicNodeEntity) knowledgeGraphicService
            .getNode(relation.from, productId).getData();
        KnowledgeGraphicNodeEntity nodeTo = (KnowledgeGraphicNodeEntity) knowledgeGraphicService
            .getNode(relation.to, productId).getData();
        if (nodeFrom != null && nodeTo != null) {
          knowledgeGraphicService.deleteRelationByFromAndTo(nodeFrom.getId(), nodeTo.getId());
        }
        continue;
      } else if (!allowDelete && relationAction.equalsIgnoreCase("Delete")) {
        continue;
      }
      JsonResult<?> result =
          knowledgeGraphicService.addRelation(relation.name, relation.from, relation.to, productId);
      if (result.getSuccess()) {
        relationCount++;
      }
    }
    return new GraphMergeResult(nodeCount, relationCount);
  }

  private JSONObject normalizeGraphicJson(JSONObject jsonObject) {
    JSONObject normalized = new JSONObject();
    normalized.put("nodes", normalizeNodes(jsonObject.getJSONArray("nodes")));
    normalized.put("relations", normalizeRelations(jsonObject.getJSONArray("relations")));
    return normalized;
  }

  private JSONArray normalizeNodes(JSONArray nodes) {
    JSONArray normalizedNodes = new JSONArray();
    if (nodes == null) {
      return normalizedNodes;
    }
    for (Object item : nodes) {
      if (!(item instanceof JSONObject node)) {
        continue;
      }
      JSONObject normalizedNode = new JSONObject();
      String name = firstText(node, "name", "label", "title", "entity");
      if (name == null || name.isBlank()) {
        continue;
      }
      String des = firstText(node, "des", "description", "desc", "detail", "summary");
      normalizedNode.put("name", name);
      normalizedNode.put("des", des == null || des.isBlank() ? name : des);
      JSONArray attributes = node.getJSONArray("attributes");
      normalizedNode.put("attributes", attributes == null ? new JSONArray() : attributes);
      normalizedNode.put("nodeAction", firstText(node, "nodeAction", "action"));
      normalizedNodes.add(normalizedNode);
    }
    return normalizedNodes;
  }

  private JSONArray normalizeRelations(JSONArray relations) {
    JSONArray normalizedRelations = new JSONArray();
    if (relations == null) {
      return normalizedRelations;
    }
    for (Object item : relations) {
      if (!(item instanceof JSONObject relation)) {
        continue;
      }
      JSONObject normalizedRelation = new JSONObject();
      String name = firstText(relation, "name", "des", "description", "relation", "type");
      String from = firstText(relation, "from", "source", "sourceNode", "start");
      String to = firstText(relation, "to", "target", "targetNode", "end");
      if (name == null || from == null || to == null || name.isBlank() || from.isBlank()
          || to.isBlank()) {
        continue;
      }
      normalizedRelation.put("name", name);
      normalizedRelation.put("from", from);
      normalizedRelation.put("to", to);
      normalizedRelation.put("relationAction", firstText(relation, "relationAction", "action"));
      normalizedRelations.add(normalizedRelation);
    }
    return normalizedRelations;
  }

  private String firstText(JSONObject object, String... keys) {
    for (String key : keys) {
      String value = object.getString(key);
      if (value != null && !value.isBlank()) {
        return value.trim();
      }
    }
    return null;
  }

  private GraphMergeResult getPersistedGraphResult(int productId) {
    JsonResult<?> result = knowledgeGraphicService.getKnowledgeGraphicByProductId(productId);
    if (result == null || !result.getSuccess()
        || !(result.getData()instanceof KnowledgeGraphic graphic)) {
      return new GraphMergeResult(0, 0);
    }
    return new GraphMergeResult(
        graphic.nodes == null ? 0 : graphic.nodes.size(),
        graphic.relations == null ? 0 : graphic.relations.size());
  }

  private void deleteDirectory(Path path) {
    if (path == null || !Files.exists(path)) {
      return;
    }
    try (var stream = Files.walk(path)) {
      stream.sorted(Comparator.reverseOrder())
          .forEach(item -> {
            try {
              Files.deleteIfExists(item);
            } catch (IOException e) {
              log.warn("Failed to delete temp file: {}", item);
            }
          });
    } catch (IOException e) {
      log.warn("Failed to clean temp directory: {}", path);
    }
  }

  private record SourceDocument(String fileName, String text) {}

  private record ChunkDocument(String fileName, String text, int indexInFile, int totalInFile) {}

  private record UploadFile(Path path, String fileName) {}

  private record GraphMergeResult(int nodeCount, int relationCount) {
    GraphMergeResult add(GraphMergeResult other) {
      return new GraphMergeResult(nodeCount + other.nodeCount,
          relationCount + other.relationCount);
    }

    boolean isEmpty() {
      return nodeCount == 0 && relationCount == 0;
    }
  }
}
