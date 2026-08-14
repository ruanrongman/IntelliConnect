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
package top.rslly.iot.utility.smartVoice;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import top.rslly.iot.utility.ai.voice.ASR.AsrServiceFactory;
import top.rslly.iot.utility.ai.voice.ASR.StreamingAsrService;
import top.rslly.iot.utility.ai.voice.ASR.StreamingAsrSession;

class XiaoZhiStreamingAsrTest {

  private final String chatId = "streaming-asr-test";
  private TestXiaoZhiUtil xiaoZhiUtil;
  private FakeStreamingSession streamingSession;

  @BeforeEach
  void setUp() {
    xiaoZhiUtil = new TestXiaoZhiUtil();
    streamingSession = new FakeStreamingSession();
    StreamingAsrService streamingService = sessionId -> streamingSession;
    AsrServiceFactory factory = new AsrServiceFactory() {
      @Override
      public StreamingAsrService getStreamingService(String provider) {
        return streamingService;
      }
    };
    ReflectionTestUtils.setField(xiaoZhiUtil, "asrServiceFactory", factory);
    XiaoZhiWebsocket.initSendContext(chatId);
  }

  @AfterEach
  void tearDown() {
    xiaoZhiUtil.cancelStreamingAsr(chatId);
    xiaoZhiUtil.shutdownExecutors();
    XiaoZhiWebsocket.closeSendContext(chatId);
  }

  @Test
  void finalStreamingTextUsesExistingAudioPipeline() throws Exception {
    long inputRound = XiaoZhiWebsocket.beginInputRound(chatId);
    assertTrue(xiaoZhiUtil.beginStreamingAsr(chatId, 1, inputRound));
    byte[] pcm = {1, 2, 3};
    assertTrue(xiaoZhiUtil.sendStreamingPcm(chatId, inputRound, pcm));
    assertArrayEquals(pcm, streamingSession.audioChunks.getFirst());

    assertTrue(xiaoZhiUtil.finishStreamingAsr(audioFrames(21), chatId, 1, false, inputRound));
    streamingSession.result.complete("最终文本");

    assertTrue(xiaoZhiUtil.invoked.await(2, TimeUnit.SECONDS));
    assertEquals("最终文本", xiaoZhiUtil.detectedText.get());
    assertEquals(21, xiaoZhiUtil.audioFrameCount);
  }

  @Test
  void duplicateFinishDoesNotCancelPendingFinalResult() throws Exception {
    long inputRound = XiaoZhiWebsocket.beginInputRound(chatId);
    assertTrue(xiaoZhiUtil.beginStreamingAsr(chatId, 1, inputRound));
    assertTrue(xiaoZhiUtil.finishStreamingAsr(audioFrames(21), chatId, 1, false, inputRound));

    assertTrue(xiaoZhiUtil.finishStreamingAsr(List.of(), chatId, 1, false, inputRound));
    streamingSession.result.complete("最终文本");

    assertFalse(streamingSession.cancelled);
    assertTrue(xiaoZhiUtil.invoked.await(2, TimeUnit.SECONDS));
    assertEquals("最终文本", xiaoZhiUtil.detectedText.get());
  }

  @Test
  void failedStreamingResultFallsBackToBatchAsr() throws Exception {
    long inputRound = XiaoZhiWebsocket.beginInputRound(chatId);
    assertTrue(xiaoZhiUtil.beginStreamingAsr(chatId, 1, inputRound));
    assertTrue(xiaoZhiUtil.finishStreamingAsr(audioFrames(21), chatId, 1, false, inputRound));

    streamingSession.result.completeExceptionally(new IllegalStateException("connection failed"));

    assertTrue(xiaoZhiUtil.invoked.await(2, TimeUnit.SECONDS));
    assertNull(xiaoZhiUtil.detectedText.get());
  }

  @Test
  void synchronousFinishFailureFallsBackToBatchAsr() throws Exception {
    long inputRound = XiaoZhiWebsocket.beginInputRound(chatId);
    assertTrue(xiaoZhiUtil.beginStreamingAsr(chatId, 1, inputRound));
    streamingSession.finishFailure = new IllegalStateException("finish failed");

    assertTrue(xiaoZhiUtil.finishStreamingAsr(audioFrames(21), chatId, 1, false, inputRound));

    assertTrue(streamingSession.cancelled);
    assertTrue(xiaoZhiUtil.invoked.await(2, TimeUnit.SECONDS));
    assertNull(xiaoZhiUtil.detectedText.get());
  }

  @Test
  void staleStreamingResultIsDiscarded() throws Exception {
    long inputRound = XiaoZhiWebsocket.beginInputRound(chatId);
    assertTrue(xiaoZhiUtil.beginStreamingAsr(chatId, 1, inputRound));
    assertTrue(xiaoZhiUtil.finishStreamingAsr(audioFrames(21), chatId, 1, false, inputRound));
    XiaoZhiWebsocket.beginInputRound(chatId);

    streamingSession.result.complete("过期文本");

    assertFalse(xiaoZhiUtil.invoked.await(300, TimeUnit.MILLISECONDS));
  }

  @Test
  void shortAudioCancelsStreamAndKeepsShortAudioPath() throws Exception {
    long inputRound = XiaoZhiWebsocket.beginInputRound(chatId);
    assertTrue(xiaoZhiUtil.beginStreamingAsr(chatId, 1, inputRound));

    assertTrue(xiaoZhiUtil.finishStreamingAsr(audioFrames(2), chatId, 1, false, inputRound));

    assertTrue(streamingSession.cancelled);
    assertTrue(xiaoZhiUtil.invoked.await(2, TimeUnit.SECONDS));
    assertEquals(2, xiaoZhiUtil.audioFrameCount);
    assertNull(xiaoZhiUtil.detectedText.get());
  }

  @Test
  void cancelStreamingAsrReleasesActiveSession() {
    long inputRound = XiaoZhiWebsocket.beginInputRound(chatId);
    assertTrue(xiaoZhiUtil.beginStreamingAsr(chatId, 1, inputRound));

    xiaoZhiUtil.cancelStreamingAsr(chatId);

    assertTrue(streamingSession.cancelled);
    assertFalse(xiaoZhiUtil.hasStreamingAsr(chatId, inputRound));
  }

  @Test
  void cancellationDoesNotTriggerBatchFallback() throws Exception {
    long inputRound = XiaoZhiWebsocket.beginInputRound(chatId);
    assertTrue(xiaoZhiUtil.beginStreamingAsr(chatId, 1, inputRound));
    assertTrue(xiaoZhiUtil.finishStreamingAsr(audioFrames(21), chatId, 1, false, inputRound));

    xiaoZhiUtil.cancelStreamingAsr(chatId);

    assertTrue(streamingSession.cancelled);
    assertFalse(xiaoZhiUtil.invoked.await(300, TimeUnit.MILLISECONDS));
  }

  private List<byte[]> audioFrames(int count) {
    List<byte[]> frames = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      frames.add(new byte[] {(byte) i});
    }
    return frames;
  }

  private static final class FakeStreamingSession implements StreamingAsrSession {
    private final List<byte[]> audioChunks = new ArrayList<>();
    private final CompletableFuture<String> result = new CompletableFuture<>();
    private boolean cancelled;
    private RuntimeException finishFailure;

    @Override
    public boolean sendPcm(byte[] pcmData) {
      audioChunks.add(pcmData.clone());
      return true;
    }

    @Override
    public CompletableFuture<String> finish() {
      if (finishFailure != null) {
        throw finishFailure;
      }
      return result;
    }

    @Override
    public void cancel() {
      cancelled = true;
      result.cancel(false);
    }
  }

  private static final class TestXiaoZhiUtil extends XiaoZhiUtil {
    private final CountDownLatch invoked = new CountDownLatch(1);
    private final AtomicReference<String> detectedText = new AtomicReference<>();
    private volatile int audioFrameCount;

    @Override
    public void dealWithAudio(List<byte[]> audioList, String chatId, int productId,
        boolean isManual, long inputRound, String... detect) {
      audioFrameCount = audioList.size();
      detectedText.set(detect.length == 0 ? null : detect[0]);
      invoked.countDown();
    }
  }
}
