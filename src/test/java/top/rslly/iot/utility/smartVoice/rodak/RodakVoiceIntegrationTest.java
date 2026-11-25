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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.net.http.WebSocketHandshakeException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
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
  private static final Path FIXTURES = Path.of("rodak", "rodakos-main", "contracts",
      "rodak-realtime-voice", "v1", "examples");
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
  @Autowired
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
    when(router.response(eq("语音输入"), eq("语音输入"), anyString(),
        eq("rodak:7:voice-device:3"), eq(7))).thenReturn("语音回复");
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
    when(router.response(eq("语音输入"), eq("语音输入"), anyString(),
        eq("rodak:7:voice-device:3"), eq(7)))
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
    verify(router, times(3)).response(eq("语音输入"), eq("语音输入"), anyString(),
        eq("rodak:7:voice-device:3"), eq(7));

    Client replacement = new Client();
    connect(DEVICE, "test-device-token", replacement);
    assertEquals(4001, first.next());
    runtime.revoke(new Revoked(IDENTITY.id(), DEVICE, IDENTITY.version()));
    assertEquals(1008, replacement.next());
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void goodbyeEndsTheSessionEvenIfFarewellAudioIsUnavailable(boolean audioAvailable)
      throws Exception {
    String farewell = "下次见！";
    when(router.response(eq("语音输入"), eq("语音输入"), anyString(),
        eq("rodak:7:voice-device:3"), eq(7))).thenReturn(ToolPrefix.GOODBYE.getPrefix() + farewell);
    if (!audioAvailable) {
      when(tts.streamTextAudio(anyString(), eq(farewell), eq(7), eq(16000), eq(60), any()))
          .thenReturn(false);
    }
    Client client = new Client();
    WebSocket socket = connect(DEVICE, "test-device-token", client);
    socket.sendText(sessionOpen(16000, 60).toString(), true).join();
    var ready = JSON.readTree(assertInstanceOf(String.class, client.next()));
    String session = ready.path("sessionId").asText();
    send(socket, Map.of("event", "input.start", "sessionId", session, "mode", "manual-stop"));
    socket.sendBinary(ByteBuffer.wrap(new Rav1Frame(1, opus(16000)).encode()), true).join();
    send(socket, Map.of("event", "input.stop", "sessionId", session));

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
    when(router.response(eq("语音输入"), eq("语音输入"), anyString(),
        eq("rodak:7:voice-device:3"), eq(7))).thenAnswer(call -> {
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
    Client conversation = new Client();
    WebSocket socket = connect(DEVICE, "test-device-token", conversation);
    socket.sendText(sessionOpen.toString(), true).join();
    var ready = JSON.readTree(assertInstanceOf(String.class, conversation.next()));
    String sessionId = ready.path("sessionId").asText();
    send(socket, Map.of("event", "input.start", "sessionId", sessionId, "mode", "manual-stop"));
    socket.sendBinary(ByteBuffer.wrap(new Rav1Frame(1, opus(16000)).encode()), true).join();
    send(socket, Map.of("event", "input.stop", "sessionId", sessionId));
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
    socket.sendText(Files.readString(FIXTURES.resolve("invalid/session-open.generation-zero.json")),
        true).join();
    var error = JSON.readTree(assertInstanceOf(String.class, client.next()));
    assertEquals("invalid-canonical-event", error.path("code").asText());
    assertEquals(1002, client.next());

    byte[] valid = HexFormat.of().parseHex(JSON.readTree(Files.readString(
        FIXTURES.resolve("valid/rav1-frame.opus.json"))).asText());
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

  @Test
  void serverVadEndsRealtimeSpeechAndKeepsTheUplinkOpenAfterPlaybackAbort() throws Exception {
    var voice = (Map<?, ?>) descriptor.bootstrap("http://localhost").get("realtimeVoice");
    assertEquals(List.of("server-authoritative"), voice.get("vadStrategies"));
    assertEquals("server-authoritative", voice.get("preferredVadStrategy"));
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
    var second = JSON.readTree(assertInstanceOf(String.class, client.next()));
    assertEquals("output.start", second.path("event").asText());
    assertEquals(epoch + 1, second.path("playbackEpoch").asLong());
    assertInstanceOf(byte[].class, client.next());
    assertEquals("output.stop",
        JSON.readTree(assertInstanceOf(String.class, client.next())).path("event").asText());
    socket.sendClose(1000, "done").join();
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
    var open = (ObjectNode) JSON.readTree(
        Files.readString(FIXTURES.resolve("valid/session-open.wake.json")));
    open.withObject("/downlink").put("sampleRateHz", downlinkRate)
        .put("frameDurationMs", downlinkDuration);
    return open;
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

  private static final class Client implements WebSocket.Listener {
    private final BlockingQueue<Object> messages = new LinkedBlockingQueue<>();
    private final StringBuilder text = new StringBuilder();
    private final List<byte[]> binary = new ArrayList<>();

    Object next() throws InterruptedException {
      Object message = messages.poll(10, TimeUnit.SECONDS);
      assertNotNull(message, "expected a WebSocket frame");
      return message;
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
