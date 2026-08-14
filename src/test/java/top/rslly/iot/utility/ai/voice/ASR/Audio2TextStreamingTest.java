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
package top.rslly.iot.utility.ai.voice.ASR;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionResult;
import com.alibaba.dashscope.audio.asr.recognition.timestamp.Sentence;
import com.alibaba.dashscope.common.ResultCallback;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class Audio2TextStreamingTest {

  @Test
  void streamingParamUsesLatestQwenAudioPcmModel() {
    Audio2Text audio2Text = new Audio2Text();
    audio2Text.setApiKey("test-key");

    RecognitionParam param = audio2Text.buildStreamingParam();

    assertEquals("qwen-audio-3.0-asr-flash-streaming", param.getModel());
    assertEquals("pcm", param.getFormat());
    assertEquals(16000, param.getSampleRate());
    assertEquals("test-key", param.getApiKey());
  }

  @Test
  void sendsPcmInOrderAndCompletesAfterAllFinalSentences() throws Exception {
    FakeStreamingRecognizer recognizer = new FakeStreamingRecognizer();
    Audio2Text audio2Text = createClient(recognizer);
    StreamingAsrSession session = audio2Text.startStreaming("chat-round-1");
    assertTrue(recognizer.started.await(1, TimeUnit.SECONDS));

    assertTrue(session.sendPcm(new byte[] {1, 2, 3}));
    assertTrue(session.sendPcm(new byte[] {4, 5}));
    var finalResult = session.finish();
    assertTrue(recognizer.stopped.await(1, TimeUnit.SECONDS));

    recognizer.emit("临时结果", false);
    assertFalse(finalResult.isDone());
    recognizer.emit("第一句。", true);
    recognizer.emit("第二句。", true);
    assertFalse(finalResult.isDone());
    recognizer.complete();

    assertEquals("第一句。第二句。", finalResult.get(1, TimeUnit.SECONDS));
    assertEquals(2, recognizer.frames.size());
    assertArrayEquals(new byte[] {1, 2, 3}, recognizer.frames.get(0));
    assertArrayEquals(new byte[] {4, 5}, recognizer.frames.get(1));
    assertTrue(recognizer.closed.get());
  }

  @Test
  void callbackErrorFailsAndClosesSession() throws Exception {
    FakeStreamingRecognizer recognizer = new FakeStreamingRecognizer();
    Audio2Text audio2Text = createClient(recognizer);
    StreamingAsrSession session = audio2Text.startStreaming("chat-round-error");
    assertTrue(recognizer.started.await(1, TimeUnit.SECONDS));

    recognizer.error(new IOException("connection lost"));

    ExecutionException error = assertThrows(
        ExecutionException.class, () -> session.finish().get(1, TimeUnit.SECONDS));
    assertInstanceOf(IOException.class, error.getCause());
    assertTrue(recognizer.closed.get());
  }

  @Test
  void missingCompletionTimesOutAndClosesSession() throws Exception {
    FakeStreamingRecognizer recognizer = new FakeStreamingRecognizer();
    Audio2Text audio2Text = createClient(recognizer);
    audio2Text.setDashscopeStreamingFinalTimeoutMs(50);
    StreamingAsrSession session = audio2Text.startStreaming("chat-round-timeout");
    assertTrue(recognizer.started.await(1, TimeUnit.SECONDS));

    var finalResult = session.finish();
    assertTrue(recognizer.stopped.await(1, TimeUnit.SECONDS));

    ExecutionException error = assertThrows(
        ExecutionException.class, () -> finalResult.get(1, TimeUnit.SECONDS));
    assertInstanceOf(TimeoutException.class, error.getCause());
    assertTrue(recognizer.closed.get());
  }

  private Audio2Text createClient(FakeStreamingRecognizer recognizer) {
    Audio2Text audio2Text = new Audio2Text();
    audio2Text.setApiKey("test-key");
    audio2Text.setDashscopeAcquireTimeoutMs(1000);
    audio2Text.setDashscopeStreamingFinalTimeoutMs(1000);
    audio2Text.setStreamingRecognizerFactory(() -> recognizer);
    return audio2Text;
  }

  private static final class FakeStreamingRecognizer
      implements Audio2Text.StreamingRecognizer {
    private final CountDownLatch started = new CountDownLatch(1);
    private final CountDownLatch stopped = new CountDownLatch(1);
    private final List<byte[]> frames = new CopyOnWriteArrayList<>();
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private volatile ResultCallback<RecognitionResult> callback;

    @Override
    public void start(
        RecognitionParam param, ResultCallback<RecognitionResult> callback) {
      this.callback = callback;
      started.countDown();
    }

    @Override
    public void sendAudioFrame(ByteBuffer pcmData) {
      ByteBuffer copy = pcmData.asReadOnlyBuffer();
      byte[] bytes = new byte[copy.remaining()];
      copy.get(bytes);
      frames.add(bytes);
    }

    @Override
    public void stop() {
      stopped.countDown();
    }

    @Override
    public void close() {
      closed.set(true);
    }

    private void emit(String text, boolean sentenceEnd) {
      Sentence sentence = new Sentence();
      sentence.setText(text);
      sentence.setEndTime(sentenceEnd ? 100L : null);
      RecognitionResult result = new RecognitionResult();
      result.setSentence(sentence);
      callback.onEvent(result);
    }

    private void complete() {
      callback.onComplete();
    }

    private void error(Exception error) {
      callback.onError(error);
    }
  }
}
