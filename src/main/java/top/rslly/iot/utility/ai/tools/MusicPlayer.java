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

import lombok.extern.slf4j.Slf4j;
import top.rslly.iot.utility.ai.voice.AudioUtils;
import top.rslly.iot.utility.ai.voice.OpusEncoderUtils;
import top.rslly.iot.utility.ai.voice.TTS.TtsServiceFactory;
import top.rslly.iot.utility.smartVoice.XiaoZhiWebsocket;

import jakarta.websocket.Session;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Stream;

/**
 * 音乐播放器 - 用于小智设备播放本地音乐 支持从本地文件夹读取MP3文件并播放
 */
@Slf4j
public class MusicPlayer {

  // Opus帧时长（毫秒）
  private static final int FRAME_DURATION_MS = 60;

  private final String chatId;
  private final int productId;
  private final String song;
  private final String artist;
  private final String musicPath;
  private final TtsServiceFactory ttsServiceFactory;

  // 播放中断标志
  private volatile boolean interrupted = false;

  public MusicPlayer(String chatId, int productId, String song, String artist,
      String musicPath, TtsServiceFactory ttsServiceFactory) {
    this.chatId = chatId;
    this.productId = productId;
    this.song = song;
    this.artist = artist;
    this.musicPath = musicPath;
    this.ttsServiceFactory = ttsServiceFactory;
  }

  /**
   * 检查会话是否有效
   */
  private boolean isSessionValid(Session session) {
    return session != null && session.isOpen() && !interrupted;
  }

  /**
   * 安全发送文本消息
   */
  private void safeSendText(Session session, String message) {
    if (!isSessionValid(session)) {
      return;
    }
    try {
      session.getBasicRemote().sendText(message);
    } catch (IllegalStateException | IOException e) {
      log.debug("发送文本消息失败（客户端可能已断开）: {}", e.getMessage());
      interrupted = true;
    }
  }

  /**
   * 播放音乐
   */
  public void play() {
    try {
      // 1. 查找匹配的MP3文件
      FindResult result = findMp3File();
      if (result.mp3File == null) {
        log.info("音乐文件夹为空");
        return;
      }

      log.info("找到音乐文件: {}", result.mp3File);

      // 2. 播放音频
      playAudio(result.mp3File, result.isRandom);

    } catch (Exception e) {
      log.error("播放音乐时发生错误", e);
    }
  }

  /**
   * 查找结果
   */
  private record FindResult(Path mp3File, boolean isRandom) {}

  /**
   * 在音乐文件夹中查找匹配的MP3文件，找不到时随机选择一首
   */
  private FindResult findMp3File() {
    Path musicDir = Path.of(musicPath);
    if (!Files.exists(musicDir) || !Files.isDirectory(musicDir)) {
      log.warn("音乐文件夹不存在: {}", musicPath);
      return new FindResult(null, false);
    }

    String songLower = song.toLowerCase();
    String artistLower = (artist != null && !artist.isEmpty()) ? artist.toLowerCase() : null;

    try (Stream<Path> paths = Files.walk(musicDir)) {
      // 查找所有MP3文件
      List<Path> mp3Files = paths
          .filter(Files::isRegularFile)
          .filter(p -> p.toString().toLowerCase().endsWith(".mp3"))
          .toList();

      if (mp3Files.isEmpty()) {
        log.warn("音乐文件夹中没有MP3文件: {}", musicPath);
        return new FindResult(null, false);
      }

      // 计算每个文件的匹配得分
      List<MatchResult> matchResults = new ArrayList<>();
      for (Path mp3 : mp3Files) {
        String fileName = mp3.getFileName().toString().toLowerCase();
        int score = calculateMatchScore(fileName, songLower, artistLower);
        matchResults.add(new MatchResult(mp3, score));
      }

      // 按得分排序，取最高分
      matchResults.sort((a, b) -> Integer.compare(b.score, a.score));
      MatchResult best = matchResults.get(0);

      if (best.score > 0) {
        return new FindResult(best.file, false);
      }

      // 得分为0，随机选择
      log.info("未找到匹配的音乐文件: {}，随机播放一首", song);
      Random random = new Random();
      Path randomFile = mp3Files.get(random.nextInt(mp3Files.size()));
      return new FindResult(randomFile, true);

    } catch (IOException e) {
      log.error("遍历音乐文件夹失败: {}", musicPath, e);
      return new FindResult(null, false);
    }
  }

  /**
   * 计算匹配得分
   */
  private int calculateMatchScore(String fileName, String songLower, String artistLower) {
    int score = 0;
    // 移除.mp3后缀
    String nameWithoutExt = fileName.replace(".mp3", "");

    // 1. 歌曲名完整匹配（最高分）
    if (nameWithoutExt.contains(songLower)) {
      score += 100;
    }

    // 2. 歌曲名单词部分匹配（适用于英文和用分隔符分割的文件名）
    String[] songWords = songLower.split("[\\s\\-_]+");
    for (String word : songWords) {
      if (word.length() > 1 && nameWithoutExt.contains(word)) {
        score += 20;
      }
    }

    // 3. 中文子串匹配（适用于中文歌曲名）
    // 例如 "生日歌" 可以匹配 "生日快乐.mp3"
    if (containsChinese(songLower)) {
      int chineseScore = calculateChineseMatchScore(nameWithoutExt, songLower);
      score += chineseScore;
    }

    // 4. 歌手匹配
    if (artistLower != null && !artistLower.isEmpty()) {
      // 歌手名完整匹配
      if (nameWithoutExt.contains(artistLower)) {
        score += 50;
      }

      // 歌手单词部分匹配（英文）
      String[] artistWords = artistLower.split("[\\s\\-_]+");
      for (String word : artistWords) {
        if (word.length() > 1 && nameWithoutExt.contains(word)) {
          score += 10;
        }
      }

      // 歌手中文子串匹配
      if (containsChinese(artistLower)) {
        int artistChineseScore = calculateChineseMatchScore(nameWithoutExt, artistLower);
        score += artistChineseScore / 2; // 歌手匹配权重较低
      }
    }

    return score;
  }

  /**
   * 判断字符串是否包含中文字符
   */
  private boolean containsChinese(String str) {
    if (str == null || str.isEmpty()) {
      return false;
    }
    for (char c : str.toCharArray()) {
      if (c >= '\u4e00' && c <= '\u9fa5') {
        return true;
      }
    }
    return false;
  }

  /**
   * 计算中文子串匹配得分 从最长子串开始匹配，匹配到的子串越长得分越高 例如：搜索"生日歌"，文件名"生日快乐" - "生日歌"(3字) 不匹配 - "生日"(2字) 匹配，得分 = 2 *
   * 15 = 30
   */
  private int calculateChineseMatchScore(String fileName, String searchTerm) {
    int maxScore = 0;
    int searchLen = searchTerm.length();

    // 从最长子串开始尝试匹配
    for (int len = searchLen; len >= 2; len--) {
      for (int i = 0; i <= searchLen - len; i++) {
        String sub = searchTerm.substring(i, i + len);
        if (fileName.contains(sub)) {
          // 计算得分：匹配长度 * 15，最长匹配额外加分
          int currentScore = len * 15;
          if (len == searchLen) {
            currentScore += 20; // 完整匹配额外加分
          }
          maxScore = Math.max(maxScore, currentScore);
        }
      }
    }

    // 单字符匹配（权重较低，避免误匹配）
    if (maxScore == 0) {
      int singleCharMatches = 0;
      for (char c : searchTerm.toCharArray()) {
        if (c >= '\u4e00' && c <= '\u9fa5' && fileName.indexOf(c) >= 0) {
          singleCharMatches++;
        }
      }
      // 至少匹配2个单字才给分
      if (singleCharMatches >= 2) {
        maxScore = singleCharMatches * 5;
      }
    }

    return maxScore;
  }

  /**
   * 匹配结果
   */
  private record MatchResult(Path file, int score) {}

  /**
   * 播放音频文件
   */
  private void playAudio(Path audioPath, boolean isRandom) {
    if (!Files.exists(audioPath)) {
      log.error("音频文件不存在: {}", audioPath);
      return;
    }

    Session session = XiaoZhiWebsocket.clients.get(chatId);
    if (!isSessionValid(session)) {
      log.debug("WebSocket会话不存在或已关闭: {}", chatId);
      return;
    }

    try {
      // 读取音频文件并转换为PCM格式
      byte[] audioData = AudioUtils.convertMp3ToPcm(audioPath.toAbsolutePath().toString());
      if (audioData == null || audioData.length == 0) {
        log.warn("音频数据为空");
        return;
      }

      // 发送开始播放消息
      safeSendText(session, "{\"type\":\"tts\",\"state\":\"start\"}");

      // 发送播放信息
      String fileName = audioPath.getFileName().toString().replace(".mp3", "").replace(".MP3", "");
      String songInfo;
      if (isRandom) {
        songInfo = "为您随机播放：" + fileName;
      } else {
        songInfo = "正在播放：" + fileName;
      }
      safeSendText(session,
          "{\"type\": \"tts\", \"state\": \"sentence_start\", \"text\": \"" + songInfo + "\"}");

      // 直接播放音频
      playAudioDirectly(audioData, session);

      // 发送停止消息
      safeSendText(session, "{\"type\":\"tts\",\"state\":\"stop\"}");

    } catch (Exception e) {
      // 忽略客户端断开导致的异常
      if (isClientDisconnectedException(e)) {
        log.debug("客户端已断开连接: {}", chatId);
      } else {
        log.error("处理音频时发生错误 - chatId: {}", chatId, e);
      }
    }
  }

  /**
   * 判断是否为客户端断开导致的异常
   */
  private boolean isClientDisconnectedException(Exception e) {
    String message = e.getMessage();
    if (message == null) {
      message = "";
    }
    return message.contains("closed") ||
        message.contains("CLOSED") ||
        message.contains("invalid state") ||
        e instanceof IllegalStateException;
  }

  /**
   * 直接播放音频
   */
  private void playAudioDirectly(byte[] audioData, Session session) {
    if (!isSessionValid(session)) {
      return;
    }

    BlockingQueue<byte[]> audioQueue = new LinkedBlockingQueue<>();
    OpusEncoderUtils encoder = new OpusEncoderUtils(16000, 1, FRAME_DURATION_MS);

    // 将PCM数据编码为Opus并放入队列
    List<byte[]> opusPackets = encoder.encodePcmToOpus(audioData, true);
    for (byte[] packet : opusPackets) {
      audioQueue.offer(packet);
    }
    // 结束标记
    audioQueue.offer(new byte[0]);

    // 使用AudioUtils异步发送
    AudioUtils.asyncSendAudioQueue(chatId, session, audioQueue);
  }
}
