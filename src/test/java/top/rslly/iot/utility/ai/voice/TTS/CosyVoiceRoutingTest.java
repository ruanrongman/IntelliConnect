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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesisAudioFormat;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesisParam;
import com.alibaba.dashscope.audio.ttsv2.SpeechSynthesizer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;
import top.rslly.iot.models.ProductRoleEntity;
import top.rslly.iot.services.agent.ProductRoleServiceImpl;
import top.rslly.iot.services.agent.ProductVoiceDiyServiceImpl;
import top.rslly.iot.utility.ai.voice.VoiceTimbre;

class CosyVoiceRoutingTest {
  private Text2audio service;
  private Object originalParam;

  @BeforeEach
  void setUp() {
    originalParam = ReflectionTestUtils.getField(Text2audio.class, "param");
    service = new Text2audio();
    service.setApiKey("test-key-not-used-for-network");
  }

  @AfterEach
  void restoreParameters() {
    ReflectionTestUtils.setField(Text2audio.class, "param", originalParam);
  }

  static Stream<Arguments> routes() {
    return Stream.of(
        Arguments.of("longxiaochun", "cosyvoice-v3-flash", "longxiaochun_v3"),
        Arguments.of("longxiaoxia", "cosyvoice-v3-flash", "longxiaoxia_v3"),
        Arguments.of("longxiaocheng", "cosyvoice-v3-flash", "longsanshu_v3"),
        Arguments.of("longxiaobai", "cosyvoice-v3-flash", "longxing_v3"),
        Arguments.of("longshu", "cosyvoice-v3-flash", "longshu_v3"),
        Arguments.of("longtong", "cosyvoice-v3-flash", "longhuhu_v3"),
        Arguments.of("cosy_v3_flash_longxiaochun_v3", "cosyvoice-v3-flash", "longxiaochun_v3"),
        Arguments.of("cosy_v3_flash_longanyang", "cosyvoice-v3-flash", "longanyang"),
        Arguments.of("cosy_v2_longxiaochun_v2", "cosyvoice-v2", "longxiaochun_v2"),
        Arguments.of("cosy_v2_longyingxiao", "cosyvoice-v2", "longyingxiao"),
        Arguments.of("longwan", "cosyvoice-v3-flash", "longwan_v3"),
        Arguments.of("longwan_v3", "cosyvoice-v3-flash", "longwan_v3"),
        Arguments.of("longanyang", "cosyvoice-v3-flash", "longanyang"),
        Arguments.of("longanhuan", "cosyvoice-v3-flash", "longanhuan"),
        Arguments.of("unknown-legacy-voice", "cosyvoice-v3-flash", "longxiaochun_v3"),
        Arguments.of("  longxiaochun  ", "cosyvoice-v3-flash", "longxiaochun_v3"),
        Arguments.of("  cosy_v2_longxiaochun_v2  ", "cosyvoice-v2", "longxiaochun_v2"),
        Arguments.of(null, "cosyvoice-v3-flash", "longxiaochun_v3"));
  }

  @ParameterizedTest
  @MethodSource("routes")
  void synchronousAndStreamingRequestsUseSameRoute(String input, String model, String voice) {
    List<SpeechSynthesisParam> requests = new ArrayList<>();
    byte[] mp3 = {1, 2, 3};
    try (var mocked = mockConstruction(SpeechSynthesizer.class, (synthesizer, context) -> {
      requests.add((SpeechSynthesisParam) context.arguments().get(0));
      Object callback = context.arguments().get(1);
      when(synthesizer.call(anyString())).thenAnswer(invocation -> {
        if (callback instanceof Text2audio.ReactCallback streaming) {
          streaming.acceptPcm(new byte[1920]);
          streaming.onComplete();
        }
        return ByteBuffer.wrap(mp3);
      });
    })) {
      assertArrayEquals(mp3, Text2audio.synthesizeAndSaveAudio("测试", input).array());
      List<byte[]> chunks = new ArrayList<>();
      assertTrue(service.streamTextAudio("routing-test", "测试", 0.8f, 1.2f, input, chunks::add));
      assertFalse(chunks.isEmpty());
      assertEquals(2, mocked.constructed().size());
    }
    for (SpeechSynthesisParam request : requests) {
      assertEquals(model, request.getModel());
      assertEquals(voice, request.getVoice());
    }
    assertEquals(SpeechSynthesisAudioFormat.MP3_16000HZ_MONO_128KBPS, requests.get(0).getFormat());
    assertEquals(SpeechSynthesisAudioFormat.PCM_16000HZ_MONO_16BIT, requests.get(1).getFormat());
    assertEquals(0.8f, requests.get(1).getPitchRate());
    assertEquals(1.2f, requests.get(1).getSpeechRate());
  }

  @Test
  void allDefaultEntrypointsUseV3Flash() {
    List<SpeechSynthesisParam> requests = new ArrayList<>();
    try (var mocked = mockConstruction(SpeechSynthesizer.class, (synthesizer, context) -> {
      requests.add((SpeechSynthesisParam) context.arguments().get(0));
      when(synthesizer.call(anyString())).thenReturn(ByteBuffer.wrap(new byte[] {1}));
    })) {
      Text2audio.synthesizeAndSaveAudio("测试");
      service.asyncSynthesizeAndSaveAudio("测试", "routing-test");
      assertEquals(2, mocked.constructed().size());
    }
    requests.add((SpeechSynthesisParam) ReflectionTestUtils.getField(Text2audio.class, "param"));
    requests.forEach(request -> {
      assertEquals("cosyvoice-v3-flash", request.getModel());
      assertEquals("longxiaochun_v3", request.getVoice());
    });
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "\t"})
  void blankVoiceDefaultsToV3Flash(String voice) {
    assertEquals(new Text2audio.ResolvedVoice("cosyvoice-v3-flash", "longxiaochun_v3"),
        Text2audio.resolveVoice(voice));
  }

  @Test
  void entireNewCatalogResolvesToFlash() {
    var voices = VoiceTimbre.stream()
        .filter(v -> v.getTimbre().startsWith("cosy_v3_flash_")).toList();
    assertEquals(88, voices.size());
    voices.forEach(v -> {
      var route = Text2audio.resolveVoice(v.getTimbre());
      assertEquals("cosyvoice-v3-flash", route.model());
      assertEquals(v.getTimbre().substring("cosy_v3_flash_".length()), route.voice());
    });
  }

  @ParameterizedTest
  @CsvSource({"longxiaochun,longxiaochun_v3", "longxiaoxia,longxiaoxia_v3",
      "longxiaocheng,longsanshu_v3", "longxiaobai,longxing_v3", "longshu,longshu_v3",
      "longtong,longhuhu_v3"})
  void legacyAliasesShareNewCacheFingerprintAndInvalidateOldCache(String legacy, String target) {
    var roleService = mock(ProductRoleServiceImpl.class);
    var voiceService = mock(ProductVoiceDiyServiceImpl.class);
    var factory = new TtsServiceFactory();
    ReflectionTestUtils.setField(factory, "productRoleService", roleService);
    ReflectionTestUtils.setField(factory, "productVoiceDiyService", voiceService);
    when(voiceService.findAllByProductId(7)).thenReturn(List.of());
    var role = new ProductRoleEntity();
    role.setVoice(legacy);
    when(roleService.findAllByProductId(7)).thenReturn(List.of(role));
    String upgraded = factory.getCacheFingerprint(7);
    assertEquals("dashscope|cosyvoice-v3-flash|" + target + "|1.0|1.0", upgraded);
    assertNotEquals("dashscope|" + legacy + "|1.0|1.0", upgraded);
    role.setVoice("cosy_v3_flash_" + target);
    assertEquals(upgraded, factory.getCacheFingerprint(7));
    role.setVoice(null);
    assertEquals("dashscope|cosyvoice-v3-flash|longxiaochun_v3|1.0|1.0",
        factory.getCacheFingerprint(7));
    role.setVoice("cosy_v2_longxiaochun_v2");
    assertEquals("dashscope|cosyvoice-v2|longxiaochun_v2|1.0|1.0", factory.getCacheFingerprint(7));
    role.setVoice("edge-zh-CN-XiaoxiaoNeural");
    assertEquals("edge|zh-CN-XiaoxiaoNeural|1.0|1.0", factory.getCacheFingerprint(7));
    role.setVoice("minimax-Chinese (Mandarin)_Warm_Bestie");
    assertEquals("minimax|Chinese (Mandarin)_Warm_Bestie|1.0|1.0", factory.getCacheFingerprint(7));
  }
}
