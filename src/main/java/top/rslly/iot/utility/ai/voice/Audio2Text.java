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
package top.rslly.iot.utility.ai.voice;

import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam;
import com.alibaba.dashscope.audio.asr.transcription.*;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.rslly.iot.utility.MyFileUtil;
import ws.schild.jave.Encoder;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class Audio2Text {
  private String apiKey;

  @Value("${ai.audio-tmp-path}")
  private String audioPath;

  @Value("${ai.dashscope-key}")
  public void setApiKey(String apiKey) {
    // 填写自己的api key
    this.apiKey = apiKey;
  }

  public String getText(String url) {
    try {
      var param =
          TranscriptionParam.builder()
              // 若没有将API Key配置到环境变量中，需将下面这行代码注释放开，并将apiKey替换为自己的API Key
              .apiKey(apiKey)
              .model("paraformer-v2")
              // “language_hints”只支持paraformer-v2和paraformer-realtime-v2模型
              .fileUrls(List.of(url))
              .parameter("language_hints", new String[] {"zh", "en"})
              .build();
      Transcription transcription = new Transcription();
      // 提交转写请求
      TranscriptionResult result = transcription.asyncCall(param);
      // 打印TaskId
      System.out.println("TaskId: " + result.getTaskId());
      // 等待转写完成
      result =
          transcription.wait(
              TranscriptionQueryParam.FromTranscriptionParam(param, result.getTaskId()));
      // 获取转写结果
      List<TranscriptionTaskResult> taskResultList = result.getResults();
      if (taskResultList != null && taskResultList.size() > 0) {
        TranscriptionTaskResult taskResult = taskResultList.get(0);
        // 获取转写结果的url
        String transcriptionUrl = taskResult.getTranscriptionUrl();
        // 通过Http获取url内对应的结果
        HttpURLConnection connection =
            (HttpURLConnection) new URL(transcriptionUrl).openConnection();
        connection.setRequestMethod("GET");
        connection.connect();
        BufferedReader reader =
            new BufferedReader(new InputStreamReader(connection.getInputStream()));
        // 格式化输出json结果
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        var JsonStr = gson.toJson(gson.fromJson(reader, JsonObject.class));
        JSONObject jsonObject = JSON.parseObject(JsonStr);
        String res = jsonObject.getJSONArray("transcripts").getJSONObject(0).getString("text");
        log.info("语音转换结果{}", res);
        return res;
      } else
        return "语音识别失败";
    } catch (Exception e) {
      System.out.println("error: " + e);
      return "语音识别失败";
    }

  }

  public String getTextRealtime(File file, int sampleRate, String format) {
    // 创建Recognition实例
    Recognition recognizer = new Recognition();
    // 创建RecognitionParam
    RecognitionParam param =
        RecognitionParam.builder()
            // 若没有将API Key配置到环境变量中，需将下面这行代码注释放开，并将apiKey替换为自己的API Key
            .apiKey(apiKey)
            .model("paraformer-realtime-v2")
            .format(format)
            .sampleRate(sampleRate)
            // “language_hints”只支持paraformer-v2和paraformer-realtime-v2模型
            .parameter("language_hints", new String[] {"zh", "en"})
            .build();

    try {
      // System.out.println("识别结果：" + recognizer.call(param, file));
      return recognizer.call(param, file);
    } catch (Exception e) {
      log.error("语音识别失败{}", e.getMessage());
      return "语音识别失败";
    }
  }

  public String getTextRealtime(String url, int sampleRate, String format) {
    String filePath = audioPath; // 上传后的路径
    String fileName = UUID.randomUUID() + "." + format; // 新文件名
    try {
      InputStream in = new URL(url).openStream();
      MyFileUtil.uploadFile(in.readAllBytes(), filePath, fileName);
      return this.getTextRealtime(new File(filePath + fileName), sampleRate, format);
    } catch (Exception e) {
      e.printStackTrace();
      log.error("语音识别失败{}", e.getMessage());
      return "语音识别失败";
    } finally {
      try {
        MyFileUtil.deleteFile(filePath + fileName);
      } catch (Exception e) {
        log.error("语音识别失败{}", e.getMessage());
      }
    }
  }

}
