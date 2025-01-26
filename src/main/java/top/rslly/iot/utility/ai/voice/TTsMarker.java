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

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.rslly.iot.utility.HttpRequestUtils;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Component
@Slf4j
public class TTsMarker {
  @Autowired
  private HttpRequestUtils httpRequestUtils;
  private static final String developerToken = "ttsmaker_demo_token";
  private static final int voiceId = 1504;
  private static final String audioFormat = "mp3";

  public byte[] Text2audio(String text) throws Exception {

    // Create TTS order
    String url = "https://api.ttsmaker.cn/v1/create-tts-order";
    String params = "{\"token\":\"" + developerToken + "\",\"text\":\"" + text
        + "\",\"voice_id\":\"" + voiceId + "\",\"audio_format\":\"" + audioFormat
        + "\",\"audio_speed\":\"1.0\",\"audio_volume\":\"0\",\"text_paragraph_pause_time\":\"0\"}";

    String ttsFileUrlJsonStr = httpRequestUtils.createHttpsPostByjson(url, params);
    String ttsFileUrl = JSONObject.parseObject(ttsFileUrlJsonStr).getString("audio_file_url");
    log.info(ttsFileUrl);
    var response = httpRequestUtils.httpGet(ttsFileUrl);
    if (response.isSuccessful()) {
      // Convert the response body to a byte array
      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      Objects.requireNonNull(response.body()).byteStream().transferTo(byteArrayOutputStream);

      // You can now work with the byte array, such as saving it to a file or processing it further
      return byteArrayOutputStream.toByteArray();
    } else {
      throw new IOException("Failed to download the file, response code: " + response.code());
    }
  }

  public static String bytesToHex(byte[] byteArray) {
    StringBuilder hexString = new StringBuilder();
    for (byte b : byteArray) {
      hexString.append(String.format("%02X", b)); // Convert byte to hex (uppercase)
    }
    return hexString.toString();
  }

  // 仅测试使用
  public static void saveHexToFile(byte[] audio) {
    // Convert byte array to hex string
    String hexAudio = bytesToHex(audio);

    // Write the hex string to a file
    try (FileOutputStream fos = new FileOutputStream("audio_hex.txt")) {
      fos.write(hexAudio.getBytes(StandardCharsets.UTF_8));
      log.info("Hex data has been successfully written to audio_hex.txt");
    } catch (IOException e) {
      log.error("Error writing hex data to file: " + e.getMessage());
    }
  }
}
