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
package top.rslly.iot.utility.smartVoice.rodak;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.net.http.WebSocketHandshakeException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.websocket.servlet.WebSocketServletAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;
import top.rslly.iot.config.RodakDeviceSecurity;
import top.rslly.iot.controllers.RodakDataController;
import top.rslly.iot.dao.ProductAsrRepository;
import top.rslly.iot.services.rodak.RodakDataService;
import top.rslly.iot.services.rodak.RodakDescriptor;
import top.rslly.iot.services.rodak.RodakMqttBridge;
import top.rslly.iot.services.rodak.RodakPairingService;
import top.rslly.iot.services.rodak.RodakPairingService.Identity;
import top.rslly.iot.services.rodak.RodakPairingService.Revoked;
import top.rslly.iot.services.rodak.RodakTokenService;
import top.rslly.iot.utility.SpringBeanUtils;
import top.rslly.iot.utility.ai.LlmDiyUtility;
import top.rslly.iot.utility.ai.chain.Router;
import top.rslly.iot.utility.ai.chain.Router.TurnStage;
import top.rslly.iot.utility.ai.llm.LLM;
import top.rslly.iot.utility.ai.prompts.MusicToolPrompt;
import top.rslly.iot.utility.ai.tools.MusicTool;
import top.rslly.iot.utility.ai.tools.ToolPrefix;
import top.rslly.iot.utility.ai.voice.ASR.AsrServiceFactory;
import top.rslly.iot.utility.ai.voice.ASR.StreamingAsrService;
import top.rslly.iot.utility.ai.voice.ASR.StreamingAsrSession;
import top.rslly.iot.utility.ai.voice.OpusEncoderUtils;
import top.rslly.iot.utility.ai.voice.TTS.TtsServiceFactory;
import top.rslly.iot.utility.ai.voice.concentus.OpusDecoder;
import ws.schild.jave.process.ffmpeg.DefaultFFMPEGLocator;

@SpringBootTest(classes = RodakVoiceIntegrationTest.Config.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "spring.config.location=optional:classpath:/rodak-voice-test.yaml")
@ActiveProfiles("rodak-voice-test")
class RodakVoiceIntegrationTest {
  private static final ObjectMapper JSON = new ObjectMapper();
  private static final String DEVICE = "aa:bb:cc:dd:ee:ff";
  private static final Identity IDENTITY = new Identity("voice-device", DEVICE, 7, 3,
      Instant.now().plusSeconds(1200));
  private static ApplicationContext previousContext;
  @LocalServerPort
  int port;
  @Autowired
  ApplicationContext context;
  @MockitoBean
  RodakPairingService devices;
  @MockitoBean
  RodakDataService data;
  @MockitoBean
  RodakMqttBridge mqtt;
  @MockitoSpyBean
  RodakVoiceRuntime runtime;
  @MockitoBean
  AsrServiceFactory asr;
  @MockitoBean
  Router router;
  @MockitoBean
  TtsServiceFactory tts;
  @MockitoBean
  ProductAsrRepository productAsr;
  @MockitoBean
  RodakTokenService tokens;
  @MockitoSpyBean
  RodakDescriptor descriptor;

  @BeforeEach
  void configureProviders() {
    if (SpringBeanUtils.getApplicationContext() != context) {
      previousContext = SpringBeanUtils.getApplicationContext();
    }
    SpringBeanUtils.setApplicationContext(context);
    reset(devices, router, asr, tts);
    when(devices.authenticate("test-device-token")).thenReturn(IDENTITY);
    when(devices.authenticate("wrong-token")).thenThrow(new SecurityException());
    when(devices.authenticate(null)).thenThrow(new SecurityException());
    when(devices.isCurrent(IDENTITY)).thenReturn(true);
    var provider = mock(StreamingAsrService.class);
    when(asr.getStreamingService(null)).thenReturn(provider);
    when(provider.startStreaming(anyString())).thenAnswer(call -> {
      var recognition = mock(StreamingAsrSession.class);
      when(recognition.sendPcm(any(byte[].class))).thenReturn(true);
      when(recognition.finish()).thenReturn(CompletableFuture.completedFuture("语音输入"));
      return recognition;
    });
    when(router.responseWithSpeech(eq("语音输入"), eq("语音输入"), anyString(),
        eq("rodak:7:voice-device:3"), eq(7), nullable(Consumer.class), any())).thenReturn("语音回复");
    byte[] packet = opus(16000);
    when(tts.streamTextAudio(anyString(), anyString(), eq(7), eq(16000), eq(60), any()))
        .thenAnswer(call -> {
          Consumer<byte[]> output = call.getArgument(5);
          output.accept(packet);
          return true;
        });
  }

  @AfterAll
  static void clearContext() {
    SpringBeanUtils.setApplicationContext(previousContext);
  }

  @Test
  void aiotRoutesUseDeviceAuthenticationAndPreserveTheWireEnvelope() throws Exception {
    try (var client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build()) {
      URI heartbeat = URI.create("http://127.0.0.1:" + port + "/api/v1/aiot/devices/heartbeat");
      URI ota = URI.create("http://127.0.0.1:" + port + "/api/v1/ota/check");
      var unauthenticated = client.send(
          HttpRequest.newBuilder(heartbeat).POST(HttpRequest.BodyPublishers.noBody()).build(),
          HttpResponse.BodyHandlers.ofString());
      assertEquals(401, unauthenticated.statusCode());
      assertEquals(401, JSON.readTree(unauthenticated.body()).path("code").asInt());
      var otaCheck = client.send(HttpRequest.newBuilder(ota)
          .header("Authorization", "Bearer test-device-token").GET().build(),
          HttpResponse.BodyHandlers.ofString());
      assertEquals(200, otaCheck.statusCode());
      assertEquals(JSON.readTree("{\"upgradeAvailable\":false}"),
          JSON.readTree(otaCheck.body()).path("data"));

      URI telemetry = heartbeat.resolve("telemetry");
      var report = client.send(HttpRequest.newBuilder(telemetry)
          .header("Authorization", "Bearer test-device-token")
          .POST(HttpRequest.BodyPublishers.ofString("{\"battery\":82}")).build(),
          HttpResponse.BodyHandlers.ofString());
      assertEquals(200, report.statusCode());
      assertTrue(JSON.readTree(report.body()).path("data").booleanValue());
      verify(data).telemetry(IDENTITY, JSON.readTree("{\"battery\":82}"));
    }
  }

  @Test
  void rejectsMissingHeadersAndWrongDeviceBeforeUpgrade() {
    var failure = assertThrows(CompletionException.class,
        () -> HttpClient.newHttpClient().newWebSocketBuilder()
            .buildAsync(endpoint(), new Client()).join());
    assertEquals(400, assertInstanceOf(WebSocketHandshakeException.class,
        failure.getCause()).getResponse().statusCode());
    var mismatch = assertThrows(CompletionException.class,
        () -> connect("aa:bb:cc:dd:ee:00", "test-device-token", new Client()));
    assertEquals(401, assertInstanceOf(WebSocketHandshakeException.class,
        mismatch.getCause()).getResponse().statusCode());
    var invalidToken = assertThrows(CompletionException.class,
        () -> connect(DEVICE, "wrong-token", new Client()));
    assertEquals(401, assertInstanceOf(WebSocketHandshakeException.class,
        invalidToken.getCause()).getResponse().statusCode());
  }

  @Test
  void exchangesThreeTurnsAndClosesOnReplacementAndUnbind() throws Exception {
    String weather = "北京今天晴。";
    String control = "灯已打开。";
    List<String> replies = List.of("语音回复", ToolPrefix.WEATHER.getPrefix() + weather,
        ToolPrefix.CONTROL.getPrefix() + control);
    List<String> spokenReplies = List.of("语音回复", weather, control);
    when(router.responseWithSpeech(eq("语音输入"), eq("语音输入"), anyString(),
        eq("rodak:7:voice-device:3"), eq(7), nullable(Consumer.class), any()))
            .thenReturn(replies.get(0), replies.get(1), replies.get(2));
    Client first = new Client();
    WebSocket socket = connect(DEVICE, "test-device-token", first);
    var open = sessionOpen(24000, 20);
    byte[] replyPacket = new OpusEncoderUtils(24000, 1, 20)
        .encodePcmToOpus(new byte[960], true).getFirst();
    when(tts.streamTextAudio(anyString(), anyString(), eq(7), eq(24000), eq(20), any()))
        .thenAnswer(call -> {
          Consumer<byte[]> output = call.getArgument(5);
          output.accept(replyPacket);
          return true;
        });
    // Manual input exercises the original firmware format and uses an explicit input.stop.
    open.put("preferredVadStrategy", "device-authoritative");
    socket.sendText(open.toString(), true).join();
    var ready = JSON.readTree(assertInstanceOf(String.class, first.next()));
    assertEquals("session.ready", ready.path("event").asText());
    assertEquals("server-authoritative", ready.path("vadStrategy").asText());
    assertEquals(7, ready.path("generation").asInt());
    assertEquals(open.path("uplink"), ready.path("uplink"));
    assertEquals(open.path("downlink"), ready.path("downlink"));
    String session = ready.path("sessionId").asText();
    byte[] input = opus(16000);
    for (int round = 1; round <= 3; round++) {
      send(socket, Map.of("event", "input.start", "sessionId", session, "mode", "manual-stop"));
      socket.sendBinary(ByteBuffer.wrap(new Rav1Frame(round, input).encode()), true).join();
      send(socket, Map.of("event", "input.stop", "sessionId", session));
      var start = JSON.readTree(assertInstanceOf(String.class, first.next()));
      assertEquals("output.start", start.path("event").asText());
      assertEquals(round, start.path("playbackEpoch").asLong());
      assertEquals(replies.get(round - 1), start.path("text").asText());
      var audio = Rav1Frame.decode(assertInstanceOf(byte[].class, first.next()), round - 1);
      assertEquals(round, audio.sequence());
      var decoder = new OpusDecoder(24000, 1);
      assertEquals(480,
          decoder.decode(audio.payload(), 0, audio.payload().length, new byte[960], 0, 480,
              false));
      var stop = JSON.readTree(assertInstanceOf(String.class, first.next()));
      assertEquals("output.stop", stop.path("event").asText());
      assertEquals(round, stop.path("playbackEpoch").asLong());
      verify(tts).streamTextAudio(anyString(), eq(spokenReplies.get(round - 1)), eq(7),
          eq(24000), eq(20), any());
    }
    verify(router, times(3)).responseWithSpeech(eq("语音输入"), eq("语音输入"), anyString(),
        eq("rodak:7:voice-device:3"), eq(7), nullable(Consumer.class), any());

    Client replacement = new Client();
    connect(DEVICE, "test-device-token", replacement);
    assertEquals(4001, first.next());
    runtime.revoke(new Revoked(IDENTITY.id(), DEVICE, IDENTITY.version()));
    assertEquals(1008, replacement.next());
  }

  @ParameterizedTest
  @CsvSource({"false, true", "false, false", "true, true", "true, false"})
  void goodbyeEndsTheSessionEvenIfFarewellAudioIsUnavailable(boolean negotiated,
      boolean audioAvailable)
      throws Exception {
    String farewell = "下次见！";
    when(router.responseWithSpeech(eq("语音输入"), eq("语音输入"), anyString(),
        eq("rodak:7:voice-device:3"), eq(7), nullable(Consumer.class), any()))
            .thenReturn(ToolPrefix.GOODBYE.getPrefix() + farewell);
    if (!audioAvailable) {
      when(tts.streamTextAudio(anyString(), eq(farewell), eq(7), eq(16000), eq(60), any()))
          .thenReturn(false);
    }
    Client client = new Client();
    WebSocket socket = connect(DEVICE, "test-device-token", client);
    var open = sessionOpen(16000, 60);
    if (negotiated)
      open.withObject("/features").put("turn_accepted", 1);
    socket.sendText(open.toString(), true).join();
    var ready = JSON.readTree(assertInstanceOf(String.class, client.next()));
    String session = ready.path("sessionId").asText();
    send(socket, Map.of("event", "input.start", "sessionId", session, "mode", "manual-stop"));
    socket.sendBinary(ByteBuffer.wrap(new Rav1Frame(1, opus(16000)).encode()), true).join();
    send(socket, Map.of("event", "input.stop", "sessionId", session));

    if (negotiated)
      assertAccepted(client, session, 1);
    if (audioAvailable) {
      var start = JSON.readTree(assertInstanceOf(String.class, client.next()));
      assertEquals("output.start", start.path("event").asText());
      assertEquals(1,
          Rav1Frame.decode(assertInstanceOf(byte[].class, client.next()), 0).sequence());
      var stop = JSON.readTree(assertInstanceOf(String.class, client.next()));
      assertEquals("output.stop", stop.path("event").asText());
      assertEquals("complete", stop.path("reason").asText());
    }
    var end = JSON.readTree(assertInstanceOf(String.class, client.next()));
    assertEquals("session.end", end.path("event").asText());
    assertEquals(session, end.path("sessionId").asText());
    assertEquals("goodbye-intent", end.path("reason").asText());
    assertEquals(1000, client.next());
    verify(tts).streamTextAudio(anyString(), eq(farewell), eq(7), eq(16000), eq(60), any());
  }

  @Test
  void musicToolUsesTheExistingChatIdAndStreamsAnAnnouncementOnce(@TempDir Path musicDir)
      throws Exception {
    var encoder = new ProcessBuilder(new DefaultFFMPEGLocator().getExecutablePath(), "-v", "error",
        "-f", "lavfi", "-i", "sine=frequency=440", "-t", "0.18",
        musicDir.resolve("测试音乐.mp3").toString())
            .redirectError(ProcessBuilder.Redirect.INHERIT).start();
    try {
      assertTrue(encoder.waitFor(10, TimeUnit.SECONDS));
      assertEquals(0, encoder.exitValue());
    } finally {
      encoder.destroyForcibly();
    }
    var music = new MusicTool();
    music.setMusicPath(musicDir.toString());
    music.setRodakVoice(context.getBeanProvider(RodakVoiceRuntime.class));
    var prompt = mock(MusicToolPrompt.class);
    when(prompt.getMusicTool()).thenReturn("Select music");
    music.setMusicToolPrompt(prompt);
    var llmLookup = mock(LlmDiyUtility.class);
    var llm = mock(LLM.class);
    when(llmLookup.getDiyLlm(eq(7), any(), eq("3"))).thenReturn(llm);
    when(llm.jsonChat(eq("播放测试音乐"), anyList(), eq(true)))
        .thenReturn(com.alibaba.fastjson.JSON.parseObject(
            "{\"action\":{\"code\":200,\"answer\":\"好的\",\"music\":\"测试音乐\"}}"));
    music.setLlmDiyUtility(llmLookup);
    when(router.responseWithSpeech(eq("语音输入"), eq("语音输入"), anyString(),
        eq("rodak:7:voice-device:3"), eq(7), nullable(Consumer.class), any())).thenAnswer(call -> {
          String streamId = call.getArgument(2);
          var result = music.run("播放测试音乐", Map.of("chatId", streamId, "productId", 7));
          assertEquals("正在准备播放《测试音乐》。", result.get("answer"));
          assertEquals("", result.get("url"));
          return ToolPrefix.MUSIC.getPrefix() + result.get("answer");
        });
    when(mqtt.canAnnounce(IDENTITY)).thenReturn(true);
    var sequences = new LinkedBlockingQueue<Long>();
    var busy = new AtomicBoolean(true);
    when(mqtt.announce(eq(IDENTITY), anyLong(), any(Instant.class), any())).thenAnswer(call -> {
      long sequence = call.getArgument(1);
      if (busy.getAndSet(false)) {
        Consumer<String> rejected = call.getArgument(3);
        rejected.accept("busy");
      }
      sequences.add(sequence);
      return true;
    });

    var sessionOpen = sessionOpen(16000, 60);
    sessionOpen.withObject("/features").put("turn_accepted", 1);
    Client conversation = new Client();
    WebSocket socket = connect(DEVICE, "test-device-token", conversation);
    socket.sendText(sessionOpen.toString(), true).join();
    var ready = JSON.readTree(assertInstanceOf(String.class, conversation.next()));
    String sessionId = ready.path("sessionId").asText();
    send(socket, Map.of("event", "input.start", "sessionId", sessionId, "mode", "manual-stop"));
    socket.sendBinary(ByteBuffer.wrap(new Rav1Frame(1, opus(16000)).encode()), true).join();
    send(socket, Map.of("event", "input.stop", "sessionId", sessionId));
    assertAccepted(conversation, sessionId, 1);
    assertEquals("music-announce",
        JSON.readTree(assertInstanceOf(String.class, conversation.next())).path("reason").asText());
    assertEquals(1000, conversation.next());
    Long busySequence = sequences.poll(5, TimeUnit.SECONDS);
    assertNotNull(busySequence);
    Long sequence = sequences.poll(5, TimeUnit.SECONDS);
    assertNotNull(sequence);
    assertNotEquals(busySequence, sequence);

    var announceOpen = sessionOpen(48000, 20);
    announceOpen.withObject("/features").put("announce", true).put("announceSeq", sequence);
    Client playback = new Client();
    connect(DEVICE, "test-device-token", playback).sendText(announceOpen.toString(), true).join();
    var playbackReady = JSON.readTree(assertInstanceOf(String.class, playback.next()));
    assertEquals("session.ready", playbackReady.path("event").asText());
    assertEquals(announceOpen.path("downlink"), playbackReady.path("downlink"));
    var start = JSON.readTree(assertInstanceOf(String.class, playback.next()));
    assertEquals("output.start", start.path("event").asText());
    assertEquals("测试音乐", start.path("text").asText());
    long packets = 0;
    var decoder = new OpusDecoder(48000, 1);
    Object frame = playback.next();
    while (frame instanceof byte[] bytes) {
      var audio = Rav1Frame.decode(bytes, packets);
      assertEquals(++packets, audio.sequence());
      assertEquals(960, decoder.decode(audio.payload(), 0, audio.payload().length, new byte[1920],
          0, 960, false));
      frame = playback.next();
    }
    assertTrue(packets >= 9);
    var stop = JSON.readTree(assertInstanceOf(String.class, frame));
    assertEquals("output.stop", stop.path("event").asText());
    assertEquals("complete", stop.path("reason").asText());
    var end = JSON.readTree(assertInstanceOf(String.class, playback.next()));
    assertEquals("announce-finished", end.path("reason").asText());
    assertEquals(1000, playback.next());
    verifyNoInteractions(tts);

    Client replay = new Client();
    connect(DEVICE, "test-device-token", replay).sendText(announceOpen.toString(), true).join();
    assertEquals("session.ready",
        JSON.readTree(assertInstanceOf(String.class, replay.next())).path("event").asText());
    assertEquals("announce-expired",
        JSON.readTree(assertInstanceOf(String.class, replay.next())).path("reason").asText());
    assertEquals(1000, replay.next());
  }

  @Test
  void rejectsInvalidControlAndDuplicateSequence() throws Exception {
    Client client = new Client();
    WebSocket socket = connect(DEVICE, "test-device-token", client);
    socket.sendText(fixture("session-open.generation-zero.json").toString(), true).join();
    var error = JSON.readTree(assertInstanceOf(String.class, client.next()));
    assertEquals("invalid-canonical-event", error.path("code").asText());
    assertEquals(1002, client.next());

    byte[] valid = HexFormat.of().parseHex(fixture("rav1-frame.opus.json").asText());
    long sequence = Rav1Frame.decode(valid, 0).sequence();
    assertEquals(1, sequence);
    assertThrows(IllegalArgumentException.class, () -> Rav1Frame.decode(valid, sequence));
    assertThrows(IllegalArgumentException.class, () -> Rav1Frame.decode(new byte[12], 0));
  }

  @Test
  void manualStopFinishesARequestAndAbortRejectsLatePackets() throws Exception {
    byte[] packet = opus(16000);
    var providerInterrupted = new CountDownLatch(1);
    var latePacket = new CompletableFuture<Consumer<byte[]>>();
    when(tts.streamTextAudio(anyString(), eq("语音回复"), eq(7), eq(16000), eq(60), any()))
        .thenAnswer(call -> {
          Consumer<byte[]> output = call.getArgument(5);
          output.accept(packet);
          latePacket.complete(output);
          try {
            new CountDownLatch(1).await(10, TimeUnit.SECONDS);
          } catch (InterruptedException e) {
            providerInterrupted.countDown();
            Thread.currentThread().interrupt();
          }
          return true;
        });
    Client client = new Client();
    WebSocket socket = connect(DEVICE, "test-device-token", client);
    var open = sessionOpen(16000, 60);
    socket.sendText(open.toString(), true).join();
    var ready = JSON.readTree(assertInstanceOf(String.class, client.next()));
    String session = ready.path("sessionId").asText();
    send(socket, Map.of("event", "wake.detected", "sessionId", session, "text", "你好"));
    send(socket, Map.of("event", "input.start", "sessionId", session, "mode", "manual-stop"));
    socket.sendBinary(ByteBuffer.wrap(new Rav1Frame(1, opus(16000)).encode()), true).join();
    send(socket, Map.of("event", "input.stop", "sessionId", session));
    var start = JSON.readTree(assertInstanceOf(String.class, client.next()));
    assertEquals("output.start", start.path("event").asText());
    assertInstanceOf(byte[].class, client.next());
    send(socket, Map.of("event", "playback.abort", "sessionId", session, "reason", "user",
        "playbackEpoch", start.path("playbackEpoch").asLong()));
    var stop = JSON.readTree(assertInstanceOf(String.class, client.next()));
    assertEquals("output.stop", stop.path("event").asText());
    assertEquals("interrupted", stop.path("reason").asText());
    assertTrue(providerInterrupted.await(5, TimeUnit.SECONDS));
    assertThrows(java.util.concurrent.CancellationException.class,
        () -> latePacket.get(5, TimeUnit.SECONDS).accept(packet));
    socket.sendClose(1000, "done").join();
    assertEquals(1000, client.next());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void serverVadEndsRealtimeSpeechAndKeepsTheUplinkOpenAfterPlaybackAbort(boolean negotiated)
      throws Exception {
    var voice = (Map<?, ?>) descriptor.bootstrap("http://localhost").get("realtimeVoice");
    assertEquals(List.of("server-authoritative"), voice.get("vadStrategies"));
    assertEquals("server-authoritative", voice.get("preferredVadStrategy"));
    assertFalse(assertInstanceOf(List.class, voice.get("events")).contains("turn.accepted"));
    assertFalse(assertInstanceOf(List.class, voice.get("events")).contains("turn.progress"));
    byte[] packet = opus(16000);
    var invocation = new java.util.concurrent.atomic.AtomicInteger();
    when(tts.streamTextAudio(anyString(), eq("语音回复"), eq(7), eq(16000), eq(60), any()))
        .thenAnswer(call -> {
          Consumer<byte[]> output = call.getArgument(5);
          output.accept(packet);
          if (invocation.incrementAndGet() == 1) {
            try {
              new CountDownLatch(1).await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }
          }
          return true;
        });
    Client client = new Client();
    WebSocket socket = connect(DEVICE, "test-device-token", client);
    var open = sessionOpen(16000, 60);
    if (negotiated)
      open.withObject("/features").put("turn_accepted", 1);
    socket.sendText(open.toString(), true).join();
    var ready = JSON.readTree(assertInstanceOf(String.class, client.next()));
    assertEquals("server-authoritative", ready.path("vadStrategy").asText());
    String session = ready.path("sessionId").asText();
    send(socket, Map.of("event", "input.start", "sessionId", session, "mode", "realtime"));
    send(socket, vad(session, "start", 1, 0));
    socket.sendBinary(ByteBuffer.wrap(new Rav1Frame(1, packet).encode()), true).join();
    send(socket, vad(session, "end", 1, 0));
    assertNull(client.messages.poll(250, TimeUnit.MILLISECONDS));
    long sequence = sendSpeechAndSilence(socket, 1);
    if (negotiated)
      assertAccepted(client, session, 1);
    var first = JSON.readTree(assertInstanceOf(String.class, client.next()));
    assertEquals("output.start", first.path("event").asText());
    assertInstanceOf(byte[].class, client.next());
    long epoch = first.path("playbackEpoch").asLong();
    send(socket, vad(session, "start", 2, epoch));
    assertNull(client.messages.poll(250, TimeUnit.MILLISECONDS));
    send(socket, Map.of("event", "playback.abort", "sessionId", session, "reason", "vad",
        "playbackEpoch", epoch));
    var interrupted = JSON.readTree(assertInstanceOf(String.class, client.next()));
    assertEquals("interrupted", interrupted.path("reason").asText());
    sendSpeechAndSilence(socket, sequence);
    if (negotiated)
      assertAccepted(client, session, 2);
    var second = JSON.readTree(assertInstanceOf(String.class, client.next()));
    assertEquals("output.start", second.path("event").asText());
    assertEquals(epoch + 1, second.path("playbackEpoch").asLong());
    assertInstanceOf(byte[].class, client.next());
    assertEquals("output.stop",
        JSON.readTree(assertInstanceOf(String.class, client.next())).path("event").asText());
    socket.sendClose(1000, "done").join();
    assertEquals(1000, client.next());
  }

  @ParameterizedTest
  @CsvSource({"server-authoritative, false", "server-authoritative, true",
      "hybrid-fallback, false"})
  void realtimeBargeInCapturesTheExistingUplinkAndKeepsItsOnset(String strategy,
      boolean stopSentBeforeAbort) throws Exception {
    doReturn(strategy).when(descriptor).preferredVadStrategy();
    controlVoiceTime();
    var recognizedBytes = new LinkedBlockingQueue<Integer>();
    doAnswer(call -> {
      java.io.ByteArrayOutputStream captured = call.getArgument(2);
      recognizedBytes.add(captured.size());
      return "语音输入";
    }).when(runtime).transcribe(nullable(String.class), any(), any());
    byte[] packet = opus(16000);
    var firstReply = new AtomicBoolean(true);
    var releaseFirstReply = new CompletableFuture<Void>();
    when(tts.streamTextAudio(anyString(), anyString(), eq(7), eq(16000), eq(60), any()))
        .thenAnswer(call -> {
          Consumer<byte[]> output = call.getArgument(5);
          output.accept(packet);
          if (firstReply.getAndSet(false)) {
            releaseFirstReply.orTimeout(10, TimeUnit.SECONDS).join();
            if (!stopSentBeforeAbort) {
              assertThrows(java.util.concurrent.CancellationException.class,
                  () -> output.accept(packet));
            }
          }
          return true;
        });
    var client = new Client();
    var socket = connect(DEVICE, "test-device-token", client);
    var open = sessionOpen(16000, 60);
    open.withObject("/features").put("turn_accepted", 1);
    String session = negotiate(socket, client, open);
    send(socket, Map.of("event", "input.start", "sessionId", session, "mode", "realtime"));
    long sequence = sendSpeechAndSilence(socket, 0);
    assertAccepted(client, session, 1);
    long epoch = client.event("output.start").path("playbackEpoch").asLong();
    assertInstanceOf(byte[].class, client.next());
    assertNotNull(recognizedBytes.poll(5, TimeUnit.SECONDS));
    for (int i = 0; i < 8; i++) {
      socket.sendBinary(ByteBuffer.wrap(new Rav1Frame(++sequence, packet).encode()), true).join();
    }

    // A stale abort cannot stop the latest output or consume its buffered input.
    send(socket, Map.of("event", "playback.abort", "sessionId", session,
        "reason", "vad_detected", "playbackEpoch", epoch + 1));
    assertNull(client.messages.poll(100, TimeUnit.MILLISECONDS));
    if (stopSentBeforeAbort) {
      releaseFirstReply.complete(null);
      assertEquals("complete", client.event("output.stop").path("reason").asText());
    }

    boolean hybrid = "hybrid-fallback".equals(strategy);
    if (hybrid) {
      send(socket, vad(session, "start", 1, epoch));
    }
    var abort = Map.<String, Object>of("event", "playback.abort", "sessionId", session,
        "reason", "vad_detected", "playbackEpoch", epoch);
    send(socket, abort);
    if (!stopSentBeforeAbort) {
      assertEquals("interrupted", client.event("output.stop").path("reason").asText());
      releaseFirstReply.complete(null);
    }
    send(socket, abort);
    if (hybrid) {
      send(socket, vad(session, "end", 1, epoch));
    } else {
      for (int i = 0; i < 17; i++) {
        socket.sendBinary(ByteBuffer.wrap(new Rav1Frame(++sequence, packet).encode()), true).join();
      }
    }
    assertAccepted(client, session, 2);
    assertCompleteReply(client, 2);
    Integer captured = recognizedBytes.poll(5, TimeUnit.SECONDS);
    assertNotNull(captured);
    int frameBytes = 16000 * 60 / 1000 * Short.BYTES;
    if (hybrid) {
      assertEquals(5 * frameBytes, captured);
    } else {
      assertTrue(captured > 5 * frameBytes);
    }
    socket.sendClose(1000, "done").join();
    assertEquals(1000, client.next());
  }

  @ParameterizedTest
  @ValueSource(strings = {"thinking", "tool"})
  void incomingRealtimeSpeechInterruptsThinkingAndToolStages(String stage) throws Exception {
    doReturn(RodakDescriptor.SERVER_VAD).when(descriptor).preferredVadStrategy();
    controlVoiceTime();
    var routerCalls = new java.util.concurrent.atomic.AtomicInteger();
    var firstStageStarted = new CountDownLatch(1);
    var invocationInterrupted = new CountDownLatch(1);
    when(router.responseWithSpeech(anyString(), anyString(), anyString(), anyString(), eq(7),
        nullable(Consumer.class), any())).thenAnswer(call -> {
          if (routerCalls.getAndIncrement() == 0) {
            Consumer<TurnStage> progress = call.getArgument(5);
            progress.accept(TurnStage.THINKING);
            if ("tool".equals(stage)) {
              progress.accept(TurnStage.TOOL);
            }
            firstStageStarted.countDown();
            try {
              new CountDownLatch(1).await();
            } catch (InterruptedException e) {
              invocationInterrupted.countDown();
              throw new java.util.concurrent.CancellationException();
            }
            return "stale reply";
          }
          return "语音回复";
        });

    var client = new Client();
    var socket = connect(DEVICE, "test-device-token", client);
    var open = sessionOpen(16000, 60);
    open.withObject("/features").put("turn_accepted", 1).put("turn_progress", 1);
    String session = negotiate(socket, client, open);
    send(socket, Map.of("event", "input.start", "sessionId", session, "mode", "realtime"));
    long sequence = sendSpeechAndSilence(socket, 0);
    assertAccepted(client, session, 1);
    assertProgress(client, session, 1, 1, "thinking");
    if ("tool".equals(stage))
      assertProgress(client, session, 1, 2, "tool");
    assertTrue(firstStageStarted.await(5, TimeUnit.SECONDS));

    sendSpeechAndSilence(socket, sequence);
    assertAccepted(client, session, 2);
    assertTrue(invocationInterrupted.await(5, TimeUnit.SECONDS));
    assertProgress(client, session, 2, 1, "composing");
    assertCompleteReply(client, 1);
    verify(router, times(2)).responseWithSpeech(anyString(), anyString(), anyString(),
        anyString(), eq(7), nullable(Consumer.class), any());
    socket.sendClose(1000, "done").join();
    assertEquals(1000, client.next());
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1})
  void zeroFlagsAndAcceptanceOnlyDoNotSendProgress(int acceptance) throws Exception {
    var open = sessionOpen(16000, 60);
    open.withObject("/features").put("turn_accepted", acceptance).put("turn_progress", 0);
    var client = new Client();
    var socket = connect(DEVICE, "test-device-token", client);
    String session = negotiate(socket, client, open);
    sendUtterance(socket, session, 1);
    if (acceptance == 1)
      assertAccepted(client, session, 1);
    assertCompleteReply(client, 1);
  }

  @ParameterizedTest
  @ValueSource(strings = {"session-open.progress-without-accepted.json",
      "session-open.turn-accepted-two.json", "boolean-progress", "unknown-feature"})
  void rejectsInvalidTurnNegotiation(String example) throws Exception {
    ObjectNode open;
    if (example.endsWith(".json")) {
      open = (ObjectNode) fixture(example);
    } else {
      open = progressOpen();
      var features = open.withObject("/features");
      if (example.equals("boolean-progress")) {
        features.put("turn_progress", true);
      } else {
        features.put("unexpected_flag", 1);
      }
    }
    var client = new Client();
    connect(DEVICE, "test-device-token", client).sendText(open.toString(), true).join();
    assertEquals("invalid-canonical-event", client.event("error").path("code").asText());
    assertEquals(1002, client.next());
  }

  @Test
  void mergesProgressAndRestartsWireCountersOnEachSession() throws Exception {
    FakeTimers timers = controlVoiceTime();
    var replyGates = new LinkedBlockingQueue<CompletableFuture<String>>();
    when(router.responseWithSpeech(anyString(), anyString(), anyString(), anyString(), eq(7),
        nullable(Consumer.class), any())).thenAnswer(call -> {
          Consumer<TurnStage> progress = call.getArgument(5);
          progress.accept(TurnStage.THINKING);
          progress.accept(TurnStage.TOOL);
          progress.accept(TurnStage.THINKING);
          progress.accept(TurnStage.COMPOSING);
          var gate = new CompletableFuture<String>();
          replyGates.add(gate);
          return gate.orTimeout(10, TimeUnit.SECONDS).join();
        });

    var client = new Client();
    var socket = connect(DEVICE, "test-device-token", client);
    String session = negotiate(socket, client, progressOpen());
    assertProgressTurn(timers, replyGates, client, socket, session, 1);
    assertProgressTurn(timers, replyGates, client, socket, session, 2);

    var replacement = new Client();
    var replacementSocket = connect(DEVICE, "test-device-token", replacement);
    String replacementSession = negotiate(replacementSocket, replacement, progressOpen());
    assertNotEquals(session, replacementSession);
    assertEquals(4001, client.next());
    assertProgressTurn(timers, replyGates, replacement, replacementSocket, replacementSession, 1);
  }

  @Test
  void emptyRecognitionDoesNotConsumeTurnIdAndEmptyAnswerEndsAcceptedTurn() throws Exception {
    doReturn("  ", "语音输入").when(runtime).transcribe(nullable(String.class), any(), any());
    when(router.responseWithSpeech(anyString(), anyString(), anyString(), anyString(), eq(7),
        nullable(Consumer.class), any())).thenReturn("");
    var client = new Client();
    var socket = connect(DEVICE, "test-device-token", client);
    String session = negotiate(socket, client, progressOpen());
    sendUtterance(socket, session, 1);
    verify(runtime, timeout(5000)).transcribe(nullable(String.class), any(), any());
    sendUtterance(socket, session, 2);
    assertAccepted(client, session, 1);
    var error = client.event("error");
    assertEquals("voice-turn-no-reply", error.path("code").asText());
    assertFalse(error.path("retryable").asBoolean());
    assertEquals(1000, client.next());
    verifyNoInteractions(tts);
  }

  @Test
  void routerTimeoutClosesOnceAndDropsLateProgress() throws Exception {
    FakeTimers timers = controlVoiceTime();
    var progressHook = new CompletableFuture<Consumer<TurnStage>>();
    var reply = new CompletableFuture<String>();
    when(router.responseWithSpeech(anyString(), anyString(), anyString(), anyString(), eq(7),
        nullable(Consumer.class), any())).thenAnswer(call -> {
          Consumer<TurnStage> progress = call.getArgument(5);
          progress.accept(TurnStage.THINKING);
          progress.accept(TurnStage.TOOL);
          progressHook.complete(progress);
          return reply.orTimeout(10, TimeUnit.SECONDS).join();
        });
    var client = new Client();
    var socket = connect(DEVICE, "test-device-token", client);
    String session = negotiate(socket, client, progressOpen());
    sendUtterance(socket, session, 1);
    assertAccepted(client, session, 1);
    assertProgress(client, session, 1, 1, "thinking");
    assertProgress(client, session, 1, 2, "tool");
    var progress = progressHook.get(5, TimeUnit.SECONDS);
    timers.advance(90_000);
    var error = client.event("error");
    assertEquals(session, error.path("sessionId").asText());
    assertEquals("voice-turn-timeout", error.path("code").asText());
    assertFalse(error.path("retryable").asBoolean());
    assertEquals(1000, client.next());
    assertThrows(java.util.concurrent.CancellationException.class,
        () -> progress.accept(TurnStage.COMPOSING));
    reply.complete("迟到的回答");
    verifyNoInteractions(tts);
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void preOutputStageTimeoutClosesOnce(boolean waitingForTts) throws Exception {
    FakeTimers timers = controlVoiceTime();
    var entered = new CountDownLatch(1);
    var release = new CompletableFuture<Void>();
    if (waitingForTts) {
      when(tts.streamTextAudio(anyString(), anyString(), eq(7), eq(16000), eq(60), any()))
          .thenAnswer(call -> {
            entered.countDown();
            release.orTimeout(10, TimeUnit.SECONDS).join();
            Consumer<byte[]> output = call.getArgument(5);
            output.accept(opus(16000));
            return true;
          });
    } else {
      doAnswer(call -> {
        entered.countDown();
        release.orTimeout(10, TimeUnit.SECONDS).join();
        return "迟到的识别结果";
      }).when(runtime).transcribe(nullable(String.class), any(), any());
    }
    var client = new Client();
    var socket = connect(DEVICE, "test-device-token", client);
    var open = sessionOpen(16000, 60);
    open.withObject("/features").put("turn_accepted", 1);
    String session = negotiate(socket, client, open);
    sendUtterance(socket, session, 1);
    if (waitingForTts)
      assertAccepted(client, session, 1);
    assertTrue(entered.await(5, TimeUnit.SECONDS));
    timers.advance(20_000);
    var error = client.event("error");
    assertEquals("voice-turn-timeout", error.path("code").asText());
    assertFalse(error.path("retryable").asBoolean());
    release.complete(null);
    assertEquals(1000, client.next());
    if (!waitingForTts)
      verifyNoInteractions(router, tts);
  }

  @Test
  void abortBeforeOutputDropsPendingProgressAndDoesNotAffectTheNextTurn() throws Exception {
    FakeTimers timers = controlVoiceTime();
    var reply = new CompletableFuture<String>();
    var progressHook = new CompletableFuture<Consumer<TurnStage>>();
    when(router.responseWithSpeech(anyString(), anyString(), anyString(), anyString(), eq(7),
        nullable(Consumer.class), any())).thenAnswer(call -> {
          Consumer<TurnStage> progress = call.getArgument(5);
          progress.accept(TurnStage.THINKING);
          progress.accept(TurnStage.TOOL);
          progress.accept(TurnStage.COMPOSING);
          progressHook.complete(progress);
          return reply.orTimeout(10, TimeUnit.SECONDS).join();
        }).thenReturn("语音回复");
    var client = new Client();
    var socket = connect(DEVICE, "test-device-token", client);
    String session = negotiate(socket, client, progressOpen());
    sendUtterance(socket, session, 1);
    assertAccepted(client, session, 1);
    assertProgress(client, session, 1, 1, "thinking");
    assertProgress(client, session, 1, 2, "tool");
    var progress = progressHook.get(5, TimeUnit.SECONDS);
    send(socket, Map.of("event", "playback.abort", "sessionId", session, "reason", "user"));
    sendUtterance(socket, session, 2);
    assertAccepted(client, session, 2);
    assertProgress(client, session, 2, 1, "composing");
    assertCompleteReply(client, 1);
    timers.advance(120_000);
    assertThrows(java.util.concurrent.CancellationException.class,
        () -> progress.accept(TurnStage.COMPOSING));
    reply.complete("迟到的回答");
    verify(tts, times(1)).streamTextAudio(anyString(), anyString(), eq(7), eq(16000), eq(60),
        any());
  }

  @Test
  void audioDeadlineTracksTheMostRecentPacketAndStopsStalledPlayback() throws Exception {
    FakeTimers timers = controlVoiceTime();
    var sendSecond = new CompletableFuture<Void>();
    var sendLate = new CompletableFuture<Void>();
    var firstSent = new CountDownLatch(1);
    var secondSent = new CountDownLatch(1);
    byte[] packet = opus(16000);
    when(tts.streamTextAudio(anyString(), anyString(), eq(7), eq(16000), eq(60), any()))
        .thenAnswer(call -> {
          Consumer<byte[]> output = call.getArgument(5);
          output.accept(packet);
          firstSent.countDown();
          sendSecond.orTimeout(10, TimeUnit.SECONDS).join();
          output.accept(packet);
          secondSent.countDown();
          sendLate.orTimeout(10, TimeUnit.SECONDS).join();
          output.accept(packet);
          return true;
        });
    var client = new Client();
    var socket = connect(DEVICE, "test-device-token", client);
    var open = sessionOpen(16000, 60);
    open.withObject("/features").put("turn_accepted", 1);
    String session = negotiate(socket, client, open);
    sendUtterance(socket, session, 1);
    assertAccepted(client, session, 1);
    client.event("output.start");
    assertInstanceOf(byte[].class, client.next());
    assertTrue(firstSent.await(5, TimeUnit.SECONDS));
    timers.advance(19_000);
    sendSecond.complete(null);
    assertInstanceOf(byte[].class, client.next());
    assertTrue(secondSent.await(5, TimeUnit.SECONDS));
    timers.advance(1000);
    assertTrue(client.messages.isEmpty());
    timers.advance(19_000);
    assertEquals("timeout", client.event("output.stop").path("reason").asText());
    sendLate.complete(null);
    send(socket, Map.of("event", "session.end", "sessionId", session));
    assertEquals(1000, client.next());
  }

  @Test
  void enabledHybridUsesDeviceBoundariesAndFallsBackToServerForAnOlderOffer() throws Exception {
    doReturn("hybrid-fallback").when(descriptor).preferredVadStrategy();
    var voice = (Map<?, ?>) descriptor.bootstrap("http://localhost").get("realtimeVoice");
    assertEquals(List.of("server-authoritative", "hybrid-fallback"), voice.get("vadStrategies"));
    assertEquals("hybrid-fallback", voice.get("preferredVadStrategy"));

    Client hybrid = new Client();
    WebSocket socket = connect(DEVICE, "test-device-token", hybrid);
    var open = sessionOpen(16000, 60);
    socket.sendText(open.toString(), true).join();
    var ready = JSON.readTree(assertInstanceOf(String.class, hybrid.next()));
    assertEquals("hybrid-fallback", ready.path("vadStrategy").asText());
    String session = ready.path("sessionId").asText();
    send(socket, Map.of("event", "input.start", "sessionId", session, "mode", "realtime"));
    send(socket, vad(session, "start", 1, 0));
    socket.sendBinary(ByteBuffer.wrap(new Rav1Frame(1, opus(16000)).encode()), true).join();
    send(socket, vad(session, "end", 1, 0));
    assertEquals("output.start",
        JSON.readTree(assertInstanceOf(String.class, hybrid.next())).path("event").asText());
    assertInstanceOf(byte[].class, hybrid.next());
    assertEquals("output.stop",
        JSON.readTree(assertInstanceOf(String.class, hybrid.next())).path("event").asText());
    socket.sendClose(1000, "done").join();
    assertEquals(1000, hybrid.next());

    Client serverOnly = new Client();
    WebSocket serverSocket = connect(DEVICE, "test-device-token", serverOnly);
    open.putArray("vadStrategies").add("server-authoritative");
    open.put("preferredVadStrategy", "server-authoritative");
    serverSocket.sendText(open.toString(), true).join();
    var serverReady = JSON.readTree(assertInstanceOf(String.class, serverOnly.next()));
    assertEquals("server-authoritative", serverReady.path("vadStrategy").asText());
    serverSocket.sendClose(1000, "done").join();
    assertEquals(1000, serverOnly.next());
  }

  private static long sendSpeechAndSilence(WebSocket socket, long sequence) throws Exception {
    byte[] speech;
    try (var sample = RodakVoiceIntegrationTest.class.getResourceAsStream("/audio/rtasr.pcm")) {
      assertNotNull(sample);
      speech = sample.readAllBytes();
    }
    byte[] speechAndSilence = java.util.Arrays.copyOf(speech, speech.length + 32000);
    var encoder = new OpusEncoderUtils(16000, 1, 60);
    for (byte[] frame : encoder.encodePcmToOpus(speechAndSilence, true)) {
      socket.sendBinary(ByteBuffer.wrap(new Rav1Frame(++sequence, frame).encode()), true).join();
    }
    return sequence;
  }

  private static Map<String, Object> vad(String session, String state, int sequence, long epoch) {
    var message = new java.util.HashMap<String, Object>(Map.of("event", "vad", "sessionId", session,
        "state", state, "source", "esp-sr", "sequence", sequence, "triggerMs", 60));
    if (epoch != 0) {
      message.put("playbackEpoch", epoch);
    }
    return message;
  }

  @Test
  void rejectsUplinkFormatsTheFirmwareDoesNotSupport() throws Exception {
    var open = sessionOpen(24000, 20);
    open.withObject("/uplink").put("sampleRateHz", 24000).put("frameDurationMs", 20);
    var client = new Client();
    connect(DEVICE, "test-device-token", client).sendText(open.toString(), true).join();
    assertEquals("invalid-canonical-event",
        JSON.readTree(assertInstanceOf(String.class, client.next())).path("code").asText());
    assertEquals(1002, client.next());
  }

  private static ObjectNode sessionOpen(int downlinkRate, int downlinkDuration) throws Exception {
    var open = (ObjectNode) fixture("session-open.wake.json");
    open.withObject("/downlink").put("sampleRateHz", downlinkRate)
        .put("frameDurationMs", downlinkDuration);
    return open;
  }

  private static ObjectNode progressOpen() throws Exception {
    var open = (ObjectNode) fixture("session-open.turn-progress.json");
    open.withObject("/downlink").put("sampleRateHz", 16000);
    return open;
  }

  private FakeTimers controlVoiceTime() {
    var timers = new FakeTimers();
    doAnswer(call -> timers.now()).when(runtime).nanoTime();
    doAnswer(call -> timers.schedule(call.getArgument(0), call.getArgument(1)))
        .when(runtime).scheduleVoiceTimer(any(), anyLong());
    return timers;
  }

  private static String negotiate(WebSocket socket, Client client, JsonNode open)
      throws Exception {
    socket.sendText(open.toString(), true).join();
    return client.event("session.ready").path("sessionId").asText();
  }

  private static void sendUtterance(WebSocket socket, String session, int sequence)
      throws Exception {
    send(socket, Map.of("event", "input.start", "sessionId", session, "mode", "manual-stop"));
    socket.sendBinary(ByteBuffer.wrap(new Rav1Frame(sequence, opus(16000)).encode()), true)
        .join();
    send(socket, Map.of("event", "input.stop", "sessionId", session));
  }

  private static void assertAccepted(Client client, String session, long turn) throws Exception {
    var accepted = client.event("turn.accepted");
    assertEquals(3, accepted.size());
    assertEquals(session, accepted.path("sessionId").asText());
    assertEquals(turn, accepted.path("turnId").asLong());
  }

  private static void assertProgress(Client client, String session, long turn, long sequence,
      String stage) throws Exception {
    var progress = client.event("turn.progress");
    assertEquals(5, progress.size());
    assertEquals(session, progress.path("sessionId").asText());
    assertEquals(turn, progress.path("turnId").asLong());
    assertEquals(sequence, progress.path("seq").asLong());
    assertEquals(stage, progress.path("stage").asText());
  }

  private static void assertProgressTurn(FakeTimers timers,
      BlockingQueue<CompletableFuture<String>> replyGates, Client client, WebSocket socket,
      String session, int turn) throws Exception {
    sendUtterance(socket, session, turn);
    assertAccepted(client, session, turn);
    assertProgress(client, session, turn, 1, "thinking");
    assertProgress(client, session, turn, 2, "tool");
    var gate = replyGates.poll(5, TimeUnit.SECONDS);
    assertNotNull(gate);
    timers.advance(499);
    assertTrue(client.messages.isEmpty());
    timers.advance(1);
    assertProgress(client, session, turn, 3, "composing");
    gate.complete("语音回复");
    assertCompleteReply(client, turn);
  }

  private static void assertCompleteReply(Client client, long sequence) throws Exception {
    client.event("output.start");
    assertEquals(sequence,
        Rav1Frame.decode(assertInstanceOf(byte[].class, client.next()), sequence - 1).sequence());
    assertEquals("complete", client.event("output.stop").path("reason").asText());
  }

  @Test
  void streamsFirstSentenceBeforeRouterReturnsAndKeepsOneOrderedPlaybackEpoch() throws Exception {
    var routerMayFinish = new CountDownLatch(1);
    var firstMayFinish = new CountDownLatch(1);
    var synthesis = new LinkedBlockingQueue<String>();
    byte[] first = {1};
    byte[] second = {2};
    byte[] third = {3};
    byte[] tail = {4};
    when(router.responseWithSpeech(anyString(), anyString(), anyString(), anyString(), eq(7),
        any(), any())).thenAnswer(call -> {
          Consumer<String> text = call.getArgument(6);
          text.accept("%工");
          text.accept("具：weatherTool。第一");
          text.accept("句。第二句。第三句。");
          assertTrue(routerMayFinish.await(5, TimeUnit.SECONDS));
          text.accept("末尾");
          return "第一句。第二句。第三句。末尾";
        });
    when(tts.streamTextAudio(anyString(), anyString(), eq(7), eq(16000), eq(60), any()))
        .thenAnswer(call -> {
          String text = call.getArgument(1);
          Consumer<byte[]> audio = call.getArgument(5);
          synthesis.add(text);
          switch (text) {
            case "第一句。" -> {
              audio.accept(first);
              assertTrue(firstMayFinish.await(5, TimeUnit.SECONDS));
            }
            case "第二句。" -> audio.accept(second);
            case "第三句。" -> audio.accept(third);
            case "末尾" -> audio.accept(tail);
            default -> fail("Unexpected spoken text " + text);
          }
          return true;
        });

    var client = new Client();
    var socket = connect(DEVICE, "test-device-token", client);
    var open = sessionOpen(16000, 60);
    open.withObject("/features").put("turn_accepted", 1);
    String session = negotiate(socket, client, open);
    try {
      sendUtterance(socket, session, 1);
      assertAccepted(client, session, 1);
      var start = client.event("output.start");
      assertEquals("第一句。", start.path("text").asText());
      long epoch = start.path("playbackEpoch").asLong();
      assertArrayEquals(first, Rav1Frame.decode(assertInstanceOf(byte[].class, client.next()), 0)
          .payload());
      assertEquals(java.util.Set.of("第一句。", "第二句。"),
          java.util.Set.of(synthesis.poll(5, TimeUnit.SECONDS),
              synthesis.poll(5, TimeUnit.SECONDS)));
      assertNull(synthesis.poll(100, TimeUnit.MILLISECONDS));
      firstMayFinish.countDown();
      assertArrayEquals(second, Rav1Frame.decode(assertInstanceOf(byte[].class, client.next()), 1)
          .payload());
      assertArrayEquals(third, Rav1Frame.decode(assertInstanceOf(byte[].class, client.next()), 2)
          .payload());
      routerMayFinish.countDown();
      assertArrayEquals(tail, Rav1Frame.decode(assertInstanceOf(byte[].class, client.next()), 3)
          .payload());
      assertEquals(epoch, client.event("output.stop").path("playbackEpoch").asLong());
    } finally {
      firstMayFinish.countDown();
      routerMayFinish.countDown();
      socket.sendClose(1000, "done").join();
    }
  }

  @Test
  void abortCancelsLiveTextAndPrefetchedAudio() throws Exception {
    var modelStopped = new CountDownLatch(1);
    var synthesisStopped = new CountDownLatch(1);
    var prefetched = new CountDownLatch(1);
    var textSink = new CompletableFuture<Consumer<String>>();
    when(router.responseWithSpeech(anyString(), anyString(), anyString(), anyString(), eq(7),
        any(), any())).thenAnswer(call -> {
          Consumer<String> text = call.getArgument(6);
          textSink.complete(text);
          text.accept("第一句。第二句。");
          try {
            new CountDownLatch(1).await(5, TimeUnit.SECONDS);
          } catch (InterruptedException e) {
            modelStopped.countDown();
            Thread.currentThread().interrupt();
          }
          return "第一句。第二句。";
        });
    when(tts.streamTextAudio(anyString(), anyString(), eq(7), eq(16000), eq(60), any()))
        .thenAnswer(call -> {
          String text = call.getArgument(1);
          Consumer<byte[]> audio = call.getArgument(5);
          if (text.equals("第一句。")) {
            audio.accept(new byte[] {1});
            new CountDownLatch(1).await(5, TimeUnit.SECONDS);
          } else if (text.equals("第二句。")) {
            prefetched.countDown();
            try {
              new CountDownLatch(1).await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
              synthesisStopped.countDown();
              assertThrows(java.util.concurrent.CancellationException.class,
                  () -> audio.accept(new byte[] {2}));
            }
          }
          return true;
        });
    var client = new Client();
    var socket = connect(DEVICE, "test-device-token", client);
    var open = sessionOpen(16000, 60);
    open.withObject("/features").put("turn_accepted", 1);
    String session = negotiate(socket, client, open);
    sendUtterance(socket, session, 1);
    assertAccepted(client, session, 1);
    long epoch = client.event("output.start").path("playbackEpoch").asLong();
    assertInstanceOf(byte[].class, client.next());
    assertTrue(prefetched.await(5, TimeUnit.SECONDS));

    send(socket, Map.of("event", "playback.abort", "sessionId", session,
        "reason", "user", "playbackEpoch", epoch));
    assertEquals("interrupted", client.event("output.stop").path("reason").asText());
    assertTrue(modelStopped.await(5, TimeUnit.SECONDS));
    assertTrue(synthesisStopped.await(5, TimeUnit.SECONDS));
    textSink.get(5, TimeUnit.SECONDS).accept("迟到的第三句。");
    send(socket, Map.of("event", "session.end", "sessionId", session));
    assertEquals(1000, client.next());
    verify(tts, times(2)).streamTextAudio(anyString(), anyString(), eq(7), eq(16000),
        eq(60), any());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void longReplyPlaysThroughTheTailWithBoundedSynthesisSegments(boolean streaming)
      throws Exception {
    String reply = ("语".repeat(178) + "，" + "𠀀".repeat(20) + "音".repeat(60)).repeat(18) + "尾段";
    when(router.responseWithSpeech(anyString(), anyString(), anyString(), anyString(), eq(7),
        any(), any())).thenAnswer(call -> {
          if (streaming) {
            Consumer<String> text = call.getArgument(6);
            for (int offset = 0; offset < reply.length(); offset += 37) {
              text.accept(reply.substring(offset, Math.min(reply.length(), offset + 37)));
            }
          }
          return reply;
        });
    when(tts.streamTextAudio(anyString(), anyString(), eq(7), eq(16000), eq(60), any()))
        .thenAnswer(call -> {
          String segment = call.getArgument(1);
          assertTrue(segment.codePointCount(0, segment.length()) <= 240);
          Consumer<byte[]> audio = call.getArgument(5);
          audio.accept(segment.getBytes(java.nio.charset.StandardCharsets.UTF_8));
          return true;
        });
    var client = new Client();
    var socket = connect(DEVICE, "test-device-token", client);
    var open = sessionOpen(16000, 60);
    open.withObject("/features").put("turn_accepted", 1);
    String session = negotiate(socket, client, open);
    sendUtterance(socket, session, 1);
    assertAccepted(client, session, 1);
    client.event("output.start");
    var played = new StringBuilder();
    long sequence = 0;
    Object result = client.next();
    while (result instanceof byte[] bytes) {
      var frame = Rav1Frame.decode(bytes, sequence++);
      played.append(new String(frame.payload(), java.nio.charset.StandardCharsets.UTF_8));
      result = client.next();
    }
    assertEquals(reply, played.toString());
    var stop = JSON.readTree(assertInstanceOf(String.class, result));
    assertEquals("output.stop", stop.path("event").asText());
    assertEquals("complete", stop.path("reason").asText());
    socket.sendClose(1000, "done").join();
  }

  @Test
  void prefetchedAudioDrainsEvenWhenTheProviderFailsAfterProducingFrames() throws Exception {
    var finishFirst = new CountDownLatch(1);
    var secondProduced = new CountDownLatch(1);
    int secondFrames = 40;
    when(router.responseWithSpeech(anyString(), anyString(), anyString(), anyString(), eq(7),
        any(), any())).thenAnswer(call -> {
          Consumer<String> text = call.getArgument(6);
          text.accept("第一句。第二句。");
          return "第一句。第二句。";
        });
    when(tts.streamTextAudio(anyString(), anyString(), eq(7), eq(16000), eq(60), any()))
        .thenAnswer(call -> {
          Consumer<byte[]> audio = call.getArgument(5);
          if (call.getArgument(1).equals("第一句。")) {
            audio.accept(new byte[] {1});
            assertTrue(finishFirst.await(5, TimeUnit.SECONDS));
            return true;
          }
          for (int i = 0; i < secondFrames; i++) {
            audio.accept(new byte[] {(byte) (i + 2)});
          }
          secondProduced.countDown();
          return false;
        });

    var client = new Client();
    var socket = connect(DEVICE, "test-device-token", client);
    var open = sessionOpen(16000, 60);
    open.withObject("/features").put("turn_accepted", 1);
    String session = negotiate(socket, client, open);
    try {
      sendUtterance(socket, session, 1);
      assertAccepted(client, session, 1);
      client.event("output.start");
      assertArrayEquals(new byte[] {1},
          Rav1Frame.decode(assertInstanceOf(byte[].class, client.next()), 0).payload());
      assertTrue(secondProduced.await(5, TimeUnit.SECONDS));
      finishFirst.countDown();
      for (int i = 1; i <= secondFrames; i++) {
        var frame = Rav1Frame.decode(assertInstanceOf(byte[].class, client.next()), i);
        assertArrayEquals(new byte[] {(byte) (i + 1)}, frame.payload());
      }
      assertEquals("error", client.event("output.stop").path("reason").asText());
      send(socket, Map.of("event", "session.end", "sessionId", session));
      assertEquals(1000, client.next());
    } finally {
      finishFirst.countDown();
      socket.abort();
    }
  }

  @Test
  void legacyReplyKeepsPlayingPastTheTurnDeadline() throws Exception {
    FakeTimers timers = controlVoiceTime();
    var firstPacket = new CompletableFuture<Consumer<byte[]>>();
    var releaseFirst = new CountDownLatch(1);
    var releaseLast = new CountDownLatch(1);
    byte[] packet = opus(16000);
    when(tts.streamTextAudio(anyString(), anyString(), eq(7), eq(16000), eq(60), any()))
        .thenAnswer(call -> {
          Consumer<byte[]> audio = call.getArgument(5);
          firstPacket.complete(audio);
          assertTrue(releaseFirst.await(5, TimeUnit.SECONDS));
          audio.accept(packet);
          assertTrue(releaseLast.await(5, TimeUnit.SECONDS));
          audio.accept(packet);
          return true;
        });
    var client = new Client();
    var socket = connect(DEVICE, "test-device-token", client);
    String session = negotiate(socket, client, sessionOpen(16000, 60));
    try {
      sendUtterance(socket, session, 1);
      firstPacket.get(5, TimeUnit.SECONDS);
      timers.advance(89_000);
      releaseFirst.countDown();
      client.event("output.start");
      assertInstanceOf(byte[].class, client.next());
      timers.advance(2_000);
      runtime.checkConnections();
      releaseLast.countDown();
      assertEquals(2,
          Rav1Frame.decode(assertInstanceOf(byte[].class, client.next()), 1).sequence());
      assertEquals("complete", client.event("output.stop").path("reason").asText());
    } finally {
      releaseFirst.countDown();
      releaseLast.countDown();
      socket.sendClose(1000, "done").join();
    }
  }

  private static JsonNode fixture(String name) throws Exception {
    try (var input = RodakVoiceIntegrationTest.class.getResourceAsStream("/rodak-voice/" + name)) {
      return JSON.readTree(input);
    }
  }

  private URI endpoint() {
    return URI.create("ws://127.0.0.1:" + port + "/api/v1/aiot/devices/realtime-voice");
  }

  private WebSocket connect(String device, String bearer, Client client) {
    return HttpClient.newHttpClient().newWebSocketBuilder().header("Device-Id", device)
        .header("Client-Id", "rodak-test")
        .header("Protocol-Version", "1").header("Authorization", "Bearer " + bearer)
        .buildAsync(endpoint(), client).join();
  }

  private static byte[] opus(int rate) {
    return new OpusEncoderUtils(rate, 1, 60).encodePcmToOpus(new byte[rate * 120 / 1000], true)
        .getFirst();
  }

  private static void send(WebSocket socket, Map<String, Object> event) throws Exception {
    socket.sendText(JSON.writeValueAsString(event), true).join();
  }

  private static final class FakeTimers {
    private final PriorityQueue<Timer> timers =
        new PriorityQueue<>(Comparator.comparingLong(Timer::at));
    private long now = TimeUnit.SECONDS.toNanos(1);

    synchronized long now() {
      return now;
    }

    synchronized Future<?> schedule(Runnable task, long delay) {
      var future = new FutureTask<Void>(task, null);
      timers.add(new Timer(now + delay, future));
      return future;
    }

    void advance(long millis) {
      synchronized (this) {
        now += TimeUnit.MILLISECONDS.toNanos(millis);
      }
      while (true) {
        Timer timer;
        synchronized (this) {
          if (timers.isEmpty() || timers.peek().at() > now)
            return;
          timer = timers.remove();
        }
        timer.task().run();
      }
    }

    private record Timer(long at, FutureTask<Void> task) {}
  }

  private static final class Client implements WebSocket.Listener {
    private final BlockingQueue<Object> messages = new LinkedBlockingQueue<>();
    private final StringBuilder text = new StringBuilder();
    private final List<byte[]> binary = new ArrayList<>();

    Object next() throws InterruptedException {
      Object message = messages.poll(10, TimeUnit.SECONDS);
      assertNotNull(message, "expected a WebSocket frame");
      return message;
    }

    JsonNode event(String name) throws Exception {
      var event = JSON.readTree(assertInstanceOf(String.class, next()));
      assertEquals(name, event.path("event").asText());
      return event;
    }

    @Override
    public java.util.concurrent.CompletionStage<?> onText(WebSocket socket,
        CharSequence fragment, boolean last) {
      text.append(fragment);
      if (last) {
        messages.add(text.toString());
        text.setLength(0);
      }
      socket.request(1);
      return null;
    }

    @Override
    public java.util.concurrent.CompletionStage<?> onBinary(WebSocket socket, ByteBuffer data,
        boolean last) {
      byte[] bytes = new byte[data.remaining()];
      data.get(bytes);
      binary.add(bytes);
      if (last) {
        var output = new java.io.ByteArrayOutputStream();
        binary.forEach(output::writeBytes);
        messages.add(output.toByteArray());
        binary.clear();
      }
      socket.request(1);
      return null;
    }

    @Override
    public java.util.concurrent.CompletionStage<?> onClose(WebSocket socket, int status,
        String reason) {
      messages.add(status);
      return null;
    }
  }

  @Configuration(proxyBeanMethods = false)
  @Profile("rodak-voice-test")
  @ImportAutoConfiguration({ServletWebServerFactoryAutoConfiguration.class,
      DispatcherServletAutoConfiguration.class, WebMvcAutoConfiguration.class,
      WebSocketServletAutoConfiguration.class, JacksonAutoConfiguration.class,
      HttpMessageConvertersAutoConfiguration.class, SecurityAutoConfiguration.class,
      SecurityFilterAutoConfiguration.class})
  @Import({RodakDeviceSecurity.class, RodakVoiceRuntime.class, RodakRealtimeVoiceWebSocket.class,
      RodakDataController.class, RodakDescriptor.class})
  static class Config {
    @Bean
    ServerEndpointExporter endpoints() {
      return new ServerEndpointExporter();
    }
  }
}
