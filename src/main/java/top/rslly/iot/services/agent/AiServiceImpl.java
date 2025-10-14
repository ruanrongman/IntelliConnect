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

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import top.rslly.iot.param.request.AiControl;
import top.rslly.iot.services.SafetyServiceImpl;
import top.rslly.iot.services.agent.AiService;
import top.rslly.iot.services.thingsModel.ProductServiceImpl;
import top.rslly.iot.utility.JwtTokenUtil;
import top.rslly.iot.utility.MyFileUtil;
import top.rslly.iot.utility.RedisUtil;
import top.rslly.iot.utility.ai.chain.Router;
import top.rslly.iot.utility.ai.llm.LLMFactory;
import top.rslly.iot.utility.ai.mcp.McpProtocolSend;
import top.rslly.iot.utility.ai.mcp.McpWebsocket;
import top.rslly.iot.utility.ai.mcp.McpWebsocketEndpoint;
import top.rslly.iot.utility.ai.voice.Audio2Text;
import top.rslly.iot.utility.ai.voice.Text2audio;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.result.ResultCode;
import top.rslly.iot.utility.result.ResultTool;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

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
  @Autowired
  private Router router;
  @Autowired
  private Text2audio text2audio;
  @Autowired
  private Audio2Text audio2Text;
  @Autowired
  private ProductServiceImpl productService;
  @Autowired
  private SafetyServiceImpl safetyService;
  @Autowired
  private RedisUtil redisUtil;
  private static final String prefix_url = "/api/v2/ai/tmp_voice";

  @Override
  public JsonResult<?> getAiResponse(AiControl aiControl, String token) {
    if (!safetyService.controlAuthorizeProduct(token, aiControl.getProductId())) {
      return ResultTool.fail(ResultCode.NO_PERMISSION);
    }
    if (productService.findAllById(aiControl.getProductId()).isEmpty()) {
      return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    }
    String chatId = "chatProduct" + aiControl.getProductId();
    var answer =
        router.response(aiControl.getContent(), chatId, aiControl.getProductId());
    return ResultTool.success(answer);
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
        var audio = Text2audio.synthesizeAndSaveAudio(answer).array();
        audio = Text2audio.VoiceBitChange(audio);
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

  @Override
  public JsonResult<?> getMcpPointUrl(int productId) {
    String token = JwtTokenUtil.createNoExpireToken("mcp" + productId, "mcp_endpoint");
    String url = otaUrl + "/mcp?" + "token=" + token;
    return ResultTool.success(url);
  }

  @Override
  public JsonResult<?> getMcpPointTools(int productId) {
    List<Map<String, Object>> toolList = new ArrayList<>();
    if (redisUtil.hasKey(McpWebsocket.ENDPOINT_SERVER_NAME + "mcp" + productId)) {
      toolList = (List<Map<String, Object>>) redisUtil
          .get(McpWebsocket.ENDPOINT_SERVER_NAME + "mcp" + productId);
      if (toolList == null) {
        return ResultTool.fail(ResultCode.PARAM_NOT_VALID);
      }
    } else
      ResultTool.fail(ResultCode.PARAM_NOT_VALID);
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
