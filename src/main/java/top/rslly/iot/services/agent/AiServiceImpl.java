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
package top.rslly.iot.services.agent;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import top.rslly.iot.param.request.AiControl;
import top.rslly.iot.param.request.AgentMemory;
import top.rslly.iot.services.McpEndpointConfigService;
import top.rslly.iot.services.SafetyServiceImpl;
import top.rslly.iot.services.thingsModel.ProductServiceImpl;
import top.rslly.iot.utility.JwtTokenUtil;
import top.rslly.iot.utility.MyFileUtil;
import top.rslly.iot.utility.RedisUtil;
import top.rslly.iot.utility.ai.chain.Router;
import top.rslly.iot.utility.ai.llm.LLMFactory;
import top.rslly.iot.utility.ai.mcp.McpWebsocket;
import top.rslly.iot.utility.ai.rag.RagUtility;
import top.rslly.iot.utility.ai.voice.ASR.Audio2Text;
import top.rslly.iot.utility.ai.voice.AudioUtils;
import top.rslly.iot.utility.ai.voice.TTS.Text2audio;
import top.rslly.iot.utility.ai.voice.TTS.TtsService;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
public class AiServiceImpl implements AiService {
  @Value("${ai.audio-tmp-path}")
  private String audioPath;
  @Value("${ai.audio-temp-url}")
  private String audioTempUrl;
  @Value("${ai.vision-model}")
  private String visionModel;
  @Value("${ota.xiaozhi.url}")
  private String otaUrl;
  @Value("${ai.stream.max-input-chars:60000}")
  private int streamMaxInputChars;
  @Autowired
  private Router router;
  @Autowired
  private TtsService text2audio;
  @Autowired
  private Audio2Text audio2Text;
  @Autowired
  private ProductServiceImpl productService;
  @Autowired
  private ProductRoleServiceImpl productRoleService;
  @Autowired
  private SafetyServiceImpl safetyService;
  @Autowired
  private AgentMemoryServiceImpl agentMemoryService;
  @Autowired
  private WorkspaceService workspaceService;
  @Autowired
  private RedisUtil redisUtil;
  @Autowired
  private McpEndpointConfigService mcpEndpointConfigService;
  private static final String prefix_url = "/api/v2/ai/tmp_voice";
  private static final String SSE_DONE_SIGNAL = "[DONE]";
  private static final long STREAM_POLL_INTERVAL_MS = 10L;
  private static final long STREAM_HEARTBEAT_INTERVAL_MS = 10_000L;
  private static final int MEMORY_CONTENT_MAX_LENGTH = 1000;
  private final Set<String> stoppedStreamChatIds = ConcurrentHashMap.newKeySet();
  private final Map<String, Queue<String>> reasoningQueueMap = new ConcurrentHashMap<>();

  @Override
  public JsonResult<?> getAiResponse(AiControl aiControl, String token) {
    if (!safetyService.controlAuthorizeProduct(token, aiControl.getProductId())) {
      return ResultTool.fail(ResultCode.NO_PERMISSION);
    }
    if (productService.findAllById(aiControl.getProductId()).isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    String chatId = resolveConversationChatId(aiControl);
    if (chatId == null) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    initializeDebugMemory(aiControl.getProductId(), chatId, aiControl.getContent());
    var answer =
        router.response(aiControl.getContent(), chatId, aiControl.getProductId());
    return ResultTool.success(answer);
  }

  @Override
  public SseEmitter getAiResponseStream(AiControl aiControl, String streamId,
      MultipartFile[] multipartFiles, String token) {
    SseEmitter emitter = new SseEmitter(0L);
    Thread.ofVirtual()
        .start(() -> handleAiResponseStream(aiControl, streamId, multipartFiles, token, emitter));
    return emitter;
  }

  @Override
  public JsonResult<?> stopAiResponseStream(int productId, String streamId, String token) {
    if (!safetyService.controlAuthorizeProduct(token, productId)) {
      return ResultTool.fail(ResultCode.NO_PERMISSION);
    }
    if (streamId == null || streamId.isBlank()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    String streamChatId = buildStreamChatId(productId, streamId);
    stoppedStreamChatIds.add(streamChatId);
    Router.queueMap.remove(streamChatId);
    reasoningQueueMap.remove(streamChatId);
    return ResultTool.success("已停止");
  }

  private void handleAiResponseStream(AiControl aiControl, String streamId,
      MultipartFile[] multipartFiles, String token,
      SseEmitter emitter) {
    String streamChatId = buildStreamChatId(aiControl.getProductId(), streamId);
    boolean sentFromQueue = false;
    AtomicBoolean connectionClosed = new AtomicBoolean();
    emitter.onCompletion(() -> connectionClosed.set(true));
    emitter.onTimeout(() -> connectionClosed.set(true));
    emitter.onError(error -> connectionClosed.set(true));
    Thread heartbeat = null;
    stoppedStreamChatIds.remove(streamChatId);
    try {
      if (streamId == null || streamId.isBlank()) {
        sendSseEvent(emitter, "error", ResultTool.fail(ResultCode.PARAM_NOT_VALID));
        return;
      }
      if (!safetyService.controlAuthorizeProduct(token, aiControl.getProductId())) {
        sendSseEvent(emitter, "error", ResultTool.fail(ResultCode.NO_PERMISSION));
        return;
      }
      if (productService.findAllById(aiControl.getProductId()).isEmpty()) {
        sendSseEvent(emitter, "error", ResultTool.fail(ResultCode.PARAM_NOT_VALID));
        return;
      }

      String conversationChatId = resolveConversationChatId(aiControl);
      if (conversationChatId == null) {
        sendSseEvent(emitter, "error", ResultTool.fail(ResultCode.PARAM_NOT_VALID));
        return;
      }
      // Flush headers immediately and keep the connection alive even during file parsing,
      // tool execution, or the final history/database write after the queue's DONE signal.
      emitter.send(SseEmitter.event().comment("connected"));
      heartbeat = startStreamHeartbeat(emitter, connectionClosed, STREAM_HEARTBEAT_INTERVAL_MS);
      DocumentPrompt documentPrompt = buildDocumentPrompt(aiControl.getContent(), multipartFiles);
      if (isStreamInputTooLong(documentPrompt.modelContent())) {
        sendSseEvent(emitter, "error",
            streamInputTooLongResult(documentPrompt.modelContent().length()));
        return;
      }
      documentPrompt = saveWorkspaceAttachments(aiControl.getProductId(), conversationChatId,
          multipartFiles, documentPrompt);
      final DocumentPrompt savedDocumentPrompt = documentPrompt;
      initializeDebugMemory(aiControl.getProductId(), conversationChatId, aiControl.getContent());
      Queue<String> reasoningQueue = new ConcurrentLinkedQueue<>();
      reasoningQueueMap.put(streamChatId, reasoningQueue);
      CompletableFuture<String> responseFuture = CompletableFuture.supplyAsync(
          () -> router.responseDebugStream(savedDocumentPrompt.modelContent(),
              savedDocumentPrompt.historyContent(), streamChatId, conversationChatId,
              aiControl.getProductId(), reasoningQueue));
      while (!responseFuture.isDone() || hasPendingQueueData(streamChatId)
          || !reasoningQueue.isEmpty()) {
        if (connectionClosed.get() || stoppedStreamChatIds.contains(streamChatId)) {
          return;
        }
        String reasoning = reasoningQueue.poll();
        if (reasoning != null) {
          sendSseEvent(emitter, "reasoning", reasoning);
          continue;
        }
        Queue<String> queue = Router.queueMap.get(streamChatId);
        if (queue == null) {
          TimeUnit.MILLISECONDS.sleep(STREAM_POLL_INTERVAL_MS);
          continue;
        }
        if (queue.isEmpty()) {
          TimeUnit.MILLISECONDS.sleep(STREAM_POLL_INTERVAL_MS);
          continue;
        }
        String message = queue.poll();
        if (message == null) {
          TimeUnit.MILLISECONDS.sleep(STREAM_POLL_INTERVAL_MS);
          continue;
        }
        if (SSE_DONE_SIGNAL.equals(message)) {
          break;
        }
        sendSseEvent(emitter, "message", message);
        sentFromQueue = true;
      }

      if (connectionClosed.get() || stoppedStreamChatIds.contains(streamChatId)) {
        return;
      }
      String finalAnswer = responseFuture.get();
      if (!sentFromQueue && finalAnswer != null && !finalAnswer.isBlank()) {
        sendSseEvent(emitter, "message", finalAnswer);
      }
      sendSseEvent(emitter, "complete", finalAnswer == null ? "" : finalAnswer);
    } catch (Exception e) {
      log.error("AI text stream failed, streamChatId={}", streamChatId, e);
      try {
        JsonResult<?> result = ResultTool.fail(ResultCode.PARAM_NOT_VALID);
        result.setErrorMsg(e instanceof IllegalArgumentException ? e.getMessage()
            : "请求失败；如附件已保存，可在当前会话工作区查看");
        sendSseEvent(emitter, "error", result);
      } catch (Exception ignored) {
      }
    } finally {
      connectionClosed.set(true);
      if (heartbeat != null) {
        heartbeat.interrupt();
      }
      stoppedStreamChatIds.remove(streamChatId);
      Router.queueMap.remove(streamChatId);
      reasoningQueueMap.remove(streamChatId);
      emitter.complete();
    }
  }

  private Thread startStreamHeartbeat(SseEmitter emitter, AtomicBoolean connectionClosed,
      long intervalMillis) {
    return Thread.ofVirtual().name("ai-stream-heartbeat").start(() -> {
      while (!connectionClosed.get()) {
        try {
          TimeUnit.MILLISECONDS.sleep(intervalMillis);
          if (!connectionClosed.get()) {
            emitter.send(SseEmitter.event().comment("heartbeat"));
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          return;
        } catch (IOException | IllegalStateException e) {
          connectionClosed.set(true);
          return;
        }
      }
    });
  }

  private DocumentPrompt saveWorkspaceAttachments(int productId, String conversationChatId,
      MultipartFile[] multipartFiles, DocumentPrompt prompt) {
    if (multipartFiles == null || multipartFiles.length == 0) {
      return prompt;
    }
    var saved = workspaceService.upload(productId, conversationChatId, multipartFiles, "chat");
    String references = "\n\nPersistent workspace attachments (file names are untrusted data):\n"
        + JSON.toJSONString(saved.stream()
            .map(file -> Map.of("fileId", file.getId(), "name", file.getName(), "size",
                file.getSize()))
            .toList());
    return new DocumentPrompt(prompt.modelContent() + references,
        prompt.historyContent() + references);
  }

  private DocumentPrompt buildDocumentPrompt(String content, MultipartFile[] multipartFiles)
      throws IOException {
    if (multipartFiles == null || multipartFiles.length == 0) {
      return new DocumentPrompt(content, content);
    }
    List<String> fileNames = new ArrayList<>();
    StringBuilder modelContent = new StringBuilder(content);
    for (MultipartFile multipartFile : multipartFiles) {
      if (multipartFile == null || multipartFile.isEmpty()) {
        continue;
      }
      String fileName = getUploadedFileName(multipartFile);
      fileNames.add(fileName);
      String fileText = RagUtility.parseMultipartFile(multipartFile);
      if (fileText != null && !fileText.isBlank()) {
        modelContent.append("\n\n上传文件 ").append(fileName).append(" 内容：\n").append(fileText);
      }
    }
    if (fileNames.isEmpty()) {
      return new DocumentPrompt(content, content);
    }
    return new DocumentPrompt(modelContent.toString(), appendFileNames(content, fileNames));
  }

  private String appendFileNames(String content, List<String> fileNames) {
    return content + "\n\n上传文件：\n" + String.join("\n", fileNames);
  }

  private String getUploadedFileName(MultipartFile multipartFile) {
    return Optional.ofNullable(multipartFile.getOriginalFilename()).orElse("uploaded-file");
  }

  private record DocumentPrompt(String modelContent, String historyContent) {}

  private boolean isStreamInputTooLong(String content) {
    return streamMaxInputChars > 0 && content.length() > streamMaxInputChars;
  }

  private JsonResult<?> streamInputTooLongResult(int inputChars) {
    JsonResult<?> result = ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    result.setErrorMsg(
        "发送内容总字数超过限制，当前 " + inputChars + "，最大 " + streamMaxInputChars);
    return result;
  }

  private boolean hasPendingQueueData(String chatId) {
    Queue<String> queue = Router.queueMap.get(chatId);
    return queue != null && !queue.isEmpty();
  }

  private String buildChatId(int productId) {
    return "chatProduct" + productId;
  }

  private String resolveConversationChatId(AiControl aiControl) {
    String chatId = aiControl.getChatId();
    if (chatId == null || chatId.isBlank()) {
      return buildChatId(aiControl.getProductId());
    }
    String normalizedChatId = chatId.trim();
    if (!agentMemoryService.isChatIdValidForProduct(
        aiControl.getProductId(), normalizedChatId)) {
      return null;
    }
    return normalizedChatId;
  }

  private void initializeDebugMemory(int productId, String chatId, String content) {
    String debugPrefix = buildChatId(productId) + "debug";
    if (!chatId.startsWith(debugPrefix)
        || !agentMemoryService.findAllByChatId(chatId).isEmpty()) {
      return;
    }
    String initialContent = content.trim();
    if (initialContent.length() > MEMORY_CONTENT_MAX_LENGTH) {
      initialContent = initialContent.substring(0, MEMORY_CONTENT_MAX_LENGTH);
    }
    agentMemoryService.insertAndUpdate(new AgentMemory(chatId, initialContent));
  }

  private String buildStreamChatId(int productId, String streamId) {
    return buildChatId(productId) + ":stream:" + streamId;
  }

  private void sendSseEvent(SseEmitter emitter, String eventName, Object data) throws IOException {
    emitter.send(SseEmitter.event().name(eventName).data(data));
  }

  @Override
  public JsonResult<?> getAiResponse(boolean tts, boolean stream, int productId,
      MultipartFile multipartFile, String token) {
    if (!safetyService.controlAuthorizeProduct(token, productId)) {
      return ResultTool.fail(ResultCode.NO_PERMISSION);
    }
    if (productService.findAllById(productId).isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    if (multipartFile.isEmpty())
      return ResultTool.fail(ResultCode.PARAM_NOT_COMPLETE);

    String fileName = multipartFile.getOriginalFilename();
    if (fileName == null)
      return ResultTool.fail(ResultCode.PARAM_NOT_COMPLETE);
    String suffixName = fileName.substring(fileName.lastIndexOf(".")); // 后缀名
    if (!suffixName.equals(".amr") && !suffixName.equals(".wav") &&
        !suffixName.equals(".mp3") && !suffixName.equals(".aac") && !suffixName.equals(".3gp")
        && !suffixName.equals(".3gpp"))
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    String filePath = audioPath; // 上传后的路径
    fileName = UUID.randomUUID() + suffixName; // 新文件名
    JSONObject aiResponse = new JSONObject();
    try {
      MyFileUtil.uploadFile(multipartFile.getBytes(), filePath, fileName);
      // String result = DashScopeVoice
      // .simpleMultiModalConversationCall(audioTempUrl + prefix_url + "/" + fileName);
      String result = audio2Text.getText(audioTempUrl + prefix_url + "/" + fileName);
      log.info(audioTempUrl + prefix_url + "/" + fileName);
      log.info(result);
      String chatId = "chatProduct" + productId;
      String answer = router.response(result, chatId, productId);
      aiResponse.put("text", answer);
      if (tts) {
        var audio = Text2audio.synthesizeAndSaveAudio(answer, getProductVoice(productId)).array();
        audio = AudioUtils.VoiceBitChange(audio);
        if (!stream)
          aiResponse.put("audio", Base64.getEncoder().encodeToString(audio));
        else
          text2audio.asyncSynthesizeAndSaveAudio(answer, chatId);
      }
    } catch (Exception e) {
      log.error(e.getMessage());
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    } finally {
      try {
        MyFileUtil.deleteFile(filePath + fileName);
      } catch (IOException e) {
        log.error(e.getMessage());
      }
    }
    return ResultTool.success(aiResponse);
  }

  private String getProductVoice(int productId) {
    try {
      var roles = productRoleService.findAllByProductId(productId);
      if (!roles.isEmpty()) {
        return roles.get(0).getVoice();
      }
    } catch (Exception ignored) {
    }
    return null;
  }

  @Override
  public JsonResult<?> getMcpPointUrl(int productId, int endpointIndex) {
    int endpointCount = mcpEndpointConfigService.getEndpointCount();
    if (endpointIndex < 1 || endpointIndex > endpointCount) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    String token = JwtTokenUtil.createNoExpireToken(
        McpWebsocket.getEndpointChatId(productId, endpointIndex), "mcp_endpoint");
    String url = otaUrl + "/mcp?" + "token=" + token;
    Map<String, Object> result = new HashMap<>();
    result.put("url", url);
    result.put("endpointIndex", endpointIndex);
    result.put("endpointCount", endpointCount);
    return ResultTool.success(result);
  }

  @Override
  public JsonResult<?> getMcpPointTools(int productId, int endpointIndex) {
    int endpointCount = mcpEndpointConfigService.getEndpointCount();
    if (endpointIndex < 1 || endpointIndex > endpointCount) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    List<Map<String, Object>> toolList = new ArrayList<>();
    String serverName = McpWebsocket.getEndpointServerName(endpointIndex);
    String endpointChatId = McpWebsocket.getEndpointChatId(productId, endpointIndex);
    if (redisUtil.hasKey(serverName + endpointChatId)) {
      toolList = (List<Map<String, Object>>) redisUtil
          .get(serverName + endpointChatId);
      if (toolList == null)
        toolList = new ArrayList<>();
    }
    return ResultTool.success(toolList);
  }

  @Override
  public String getAiVisionIntent(String question, MultipartFile imageFile) {
    Map<String, Object> result = new HashMap<>();
    try {
      // 获取图片字节内容
      byte[] imageData = imageFile.getBytes();
      // 获取文件 MIME 类型
      String contentType = imageFile.getContentType(); // 可能是 image/jpeg, image/jpg, image/png 等
      if (contentType == null || !contentType.startsWith("image/")) {
        result.put("success", false);
        result.put("message", "请上传图片文件");
        return new ObjectMapper().writeValueAsString(result);
      }

      // 统一 MIME 类型（image/jpg -> image/jpeg）
      if ("image/jpg".equalsIgnoreCase(contentType)) {
        contentType = "image/jpeg";
      }

      // 支持的图片类型列表
      List<String> supportedTypes = Arrays.asList("image/jpeg", "image/png", "image/webp");
      if (!supportedTypes.contains(contentType)) {
        result.put("success", false);
        result.put("message", "不支持的图片格式");
        return new ObjectMapper().writeValueAsString(result);
      }

      // 图片大小限制（如超过 4MB）
      long maxSizeBytes = 4 * 1024 * 1024;
      if (imageFile.getSize() > maxSizeBytes) {
        result.put("success", false);
        result.put("message", "图片大小超过限制");
        return new ObjectMapper().writeValueAsString(result);
      }

      // 转为 base64 并拼接为 data URL
      String imageBase64 = Base64.getEncoder().encodeToString(imageData);
      String dataUrl = "data:" + contentType + ";base64," + imageBase64;

      // 调用 LLM 视觉模型接口
      String answer = LLMFactory.getLLM(visionModel).imageToWord(question, dataUrl);

      // 返回结果
      result.put("success", true);
      result.put("text", answer);
      return new ObjectMapper().writeValueAsString(result);
    } catch (Exception ignored) {
      return "无可以使用的视觉模型";
    }
  }

  @Override
  public void audioTmpGet(String name, HttpServletResponse response) throws IOException {
    ServletOutputStream out = response.getOutputStream();
    try {
      String filePath = audioPath; // 上传后的路径
      File file = new File(filePath + name);
      response.setCharacterEncoding("UTF-8");
      response.setHeader("Content-Disposition", "attachment");
      response.addHeader("Content-Length", "" + file.length());
      response.addHeader("Content-Type", "audio/" + name.substring(name.lastIndexOf(".") + 1));
      out.write(Files.readAllBytes(Paths.get(filePath + name)));
      out.flush();
      out.close();


    } catch (Exception e) {
      log.error(e.getMessage());
      response.setStatus(404);
      out.close();
    }
  }
}
