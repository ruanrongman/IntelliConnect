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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import top.rslly.iot.param.request.AiControl;
import top.rslly.iot.services.SafetyServiceImpl;
import top.rslly.iot.services.agent.AiService;
import top.rslly.iot.utility.MyFileUtil;
import top.rslly.iot.utility.ai.chain.Router;
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
import java.util.Base64;
import java.util.UUID;

@Service
@Slf4j
public class AiServiceImpl implements AiService {
  @Value("${ai.audio-tmp-path}")
  private String audioPath;
  @Value("${ai.audio-temp-url}")
  private String audioTempUrl;
  @Autowired
  private Router router;
  @Autowired
  private Text2audio text2audio;
  @Autowired
  private Audio2Text audio2Text;
  @Autowired
  private SafetyServiceImpl safetyService;
  private static final String prefix_url = "/api/v2/ai/tmp_voice";

  @Override
  public JsonResult<?> getAiResponse(AiControl aiControl, String token) {
    if (!safetyService.controlAuthorizeProduct(token, aiControl.getProductId())) {
      return ResultTool.fail(ResultCode.NO_PERMISSION);
    }
    var answer =
        router.response(aiControl.getContent(), aiControl.getChatId(), aiControl.getProductId());
    return ResultTool.success(answer);
  }

  @Override
  public JsonResult<?> getAiResponse(String chatId, boolean tts, boolean stream, int productId,
      MultipartFile multipartFile, String token) {
    if (!safetyService.controlAuthorizeProduct(token, productId)) {
      return ResultTool.fail(ResultCode.NO_PERMISSION);
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
