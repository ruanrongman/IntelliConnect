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
package top.rslly.iot.utility.ai.voice.TTS;

import io.github.whitemagic2014.tts.TTS;
import io.github.whitemagic2014.tts.TTSVoice;
import io.github.whitemagic2014.tts.bean.Voice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.rslly.iot.utility.ai.voice.AudioUtils;
import top.rslly.iot.utility.ai.voice.OpusEncoderUtils;

import javax.websocket.Session;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

@Slf4j
@Component
public class EdgeTTs implements TtsService {
  private static final String myVoice = "zh-CN-XiaoyiNeural";

  @Override
  public void websocketAudioSync(String text, Session session, String chatId, String voice) {
    // Only used for WebSocket audio sending.
    final BlockingQueue<byte[]> audioQueue = new LinkedBlockingQueue<>();
    // End-of-stream marker: an empty byte array.
    final byte[] EOS = new byte[0];
    final OpusEncoderUtils encoder = new OpusEncoderUtils(16000, 1, 60);
    String fullPath = null;

    try {
      String voiceName = myVoice;
      if (voice != null && !voice.isBlank()) {
        voiceName = voice;
      }
      // 获取中文语音
      String finalVoiceName = voiceName;
      Voice voiceObj = TTSVoice.provides().stream()
          .filter(v -> v.getShortName().equals(finalVoiceName))
          .collect(Collectors.toList()).get(0);

      TTS ttsEngine = new TTS(voiceObj, text);
      // 执行TTS转换获取音频文件
      String outputPath = System.getProperty("java.io.tmpdir");
      String audioFilePath = ttsEngine.findHeadHook()
          .storage(outputPath)
          .isRateLimited(true)
          .overwrite(false)
          .formatMp3()
          .trans();

      // 使用 Paths.get 来正确拼接路径，解决缺少分隔符的问题
      fullPath = Paths.get(outputPath, audioFilePath).toString();

      // 1. 将MP3转换为PCM (已经设置为16kHz采样率和单声道)
      byte[] pcmData = AudioUtils.convertMp3ToPcm(fullPath);
      List<byte[]> packets = encoder.encodePcmToOpus(pcmData, false);
      for (byte[] packet : packets) {
        audioQueue.offer(packet);
      }
      packets = encoder.encodePcmToOpus(new byte[0], true);
      for (byte[] packet : packets) {
        audioQueue.offer(packet);
      }
      // Signal end-of-stream by adding an empty array.
      audioQueue.offer(EOS);

      AudioUtils.asyncSendAudioQueue(chatId, session, audioQueue);

    } catch (Exception e) {
      log.error("websocketAudio error for chatId: {}", chatId, e);
    } finally {
      if (fullPath != null) {
        try {
          Files.deleteIfExists(Paths.get(fullPath));
          log.debug("Successfully deleted temporary file: {}", fullPath);
        } catch (Exception e) {
          log.warn("Failed to delete temporary file: {}", fullPath, e);
        }
      }
    }
  }

  @Override
  public void asyncSynthesizeAndSaveAudio(String text, String chatId) {
    throw new UnsupportedOperationException();
  }
}
