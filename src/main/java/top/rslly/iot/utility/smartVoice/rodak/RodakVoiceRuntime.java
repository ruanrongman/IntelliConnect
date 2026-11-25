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

import jakarta.annotation.PreDestroy;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import top.rslly.iot.dao.ProductAsrRepository;
import top.rslly.iot.services.rodak.RodakMqttBridge;
import top.rslly.iot.services.rodak.RodakPairingService;
import top.rslly.iot.services.rodak.RodakPairingService.Identity;
import top.rslly.iot.services.rodak.RodakPairingService.Revoked;
import top.rslly.iot.utility.ai.chain.Router;
import top.rslly.iot.utility.ai.tools.MusicPlayer.Track;
import top.rslly.iot.utility.ai.tools.ToolPrefix;
import top.rslly.iot.utility.ai.voice.ASR.AsrServiceFactory;
import top.rslly.iot.utility.ai.voice.ASR.StreamingAsrSession;
import top.rslly.iot.utility.ai.voice.TTS.TtsServiceFactory;

@Component
public class RodakVoiceRuntime {
  private static final Logger LOG = LoggerFactory.getLogger(RodakVoiceRuntime.class);
  private static final int ANNOUNCE_TIMEOUT_SECONDS = 30;
  private static final int MAX_BUSY_RETRIES = 2;
  private final RodakPairingService devices;
  private final ProductAsrRepository productAsr;
  private final AsrServiceFactory asr;
  private final Router router;
  private final TtsServiceFactory tts;
  private final ObjectProvider<RodakMqttBridge> mqtt;
  private final boolean skipToolPrefix;
  private final Map<String, Connection> connections = new ConcurrentHashMap<>();
  private final Map<String, MusicTurn> musicTurns = new ConcurrentHashMap<>();
  private final Map<String, MusicAnnouncement> musicAnnouncements = new ConcurrentHashMap<>();
  private final AtomicLong announceSequence =
      new AtomicLong(ThreadLocalRandom.current().nextLong(0xffffffffL));
  private final ScheduledExecutorService announceCommands =
      Executors.newSingleThreadScheduledExecutor(
          Thread.ofPlatform().daemon().name("rodak-announce").factory());
  private final ExecutorService musicWorkers = Executors.newVirtualThreadPerTaskExecutor();
  private final ThreadPoolExecutor workers = new ThreadPoolExecutor(4, 4, 0, TimeUnit.SECONDS,
      new ArrayBlockingQueue<>(8), Thread.ofPlatform().daemon().name("rodak-voice-", 1).factory());

  public RodakVoiceRuntime(RodakPairingService devices, ProductAsrRepository productAsr,
      AsrServiceFactory asr, Router router, TtsServiceFactory tts,
      ObjectProvider<RodakMqttBridge> mqtt,
      @Value("${ai.tts.skip-tool-prefix:true}") boolean skipToolPrefix) {
    this.devices = devices;
    this.productAsr = productAsr;
    this.asr = asr;
    this.router = router;
    this.tts = tts;
    this.mqtt = mqtt;
    this.skipToolPrefix = skipToolPrefix;
  }

  Identity authenticate(String token) {
    return devices.authenticate(token);
  }

  boolean isCurrent(Identity identity) {
    return devices.isCurrent(identity);
  }

  void connect(Identity identity, RodakRealtimeVoiceWebSocket socket) {
    Connection prior;
    synchronized (connections) {
      if (connections.size() >= 32 && !connections.containsKey(identity.deviceKey())) {
        throw new IllegalStateException("Rodak voice connection limit reached");
      }
      prior = connections.put(identity.deviceKey(), new Connection(identity, socket));
    }
    if (prior != null) {
      prior.socket().close(4001, "connection replaced");
    }
    if (!isCurrent(identity)) {
      socket.close(1008, "device authorization ended");
    } else {
      devices.seen(identity);
    }
  }

  boolean registered(Identity identity, RodakRealtimeVoiceWebSocket socket) {
    Connection connection = connections.get(identity.deviceKey());
    return connection != null && connection.socket() == socket;
  }

  public boolean isConnected(String deviceKey) {
    return connections.containsKey(deviceKey);
  }

  void disconnect(Identity identity, RodakRealtimeVoiceWebSocket socket) {
    connections.computeIfPresent(identity.deviceKey(),
        (key, existing) -> existing.socket() == socket ? null : existing);
  }

  @EventListener
  public void revoke(Revoked event) {
    musicAnnouncements.computeIfPresent(event.key(),
        (key, announcement) -> announcement.identity().version() == event.version() ? null
            : announcement);
    Connection connection = connections.get(event.key());
    if (connection != null && connection.identity().version() == event.version()) {
      connection.socket().close(1008, "device unbound");
    }
  }

  @Scheduled(fixedDelay = 5000)
  public void checkConnections() {
    Instant now = Instant.now();
    musicAnnouncements.values().removeIf(announcement -> !now.isBefore(announcement.expiresAt()));
    connections.values()
        .forEach(connection -> connection.socket().checkConnection(connection.identity()));
  }

  @PreDestroy
  void shutdown() {
    connections.values().forEach(connection -> connection.socket().close(1001, "server stopping"));
    workers.shutdownNow();
    announceCommands.shutdownNow();
    musicWorkers.shutdownNow();
    musicAnnouncements.clear();
  }

  String asrProvider(int productId) {
    return productAsr.findAllByProductId(productId).stream().findFirst()
        .map(config -> config.getAsrName()).orElse(null);
  }

  StreamingAsrSession startAsr(String provider, String turn) {
    var streaming = asr.getStreamingService(provider);
    if (streaming == null)
      return null;
    try {
      return streaming.startStreaming(turn);
    } catch (RuntimeException e) {
      return null;
    }
  }

  String transcribe(String provider, StreamingAsrSession streaming, ByteArrayOutputStream pcm)
      throws InterruptedException, IOException {
    if (streaming != null) {
      try {
        return streaming.finish().get(12, TimeUnit.SECONDS);
      } catch (ExecutionException | TimeoutException e) {
        // Reuse the bounded PCM captured during streaming.
      } finally {
        streaming.cancel();
      }
    }
    var file = Files.createTempFile("rodak-audio-", ".pcm");
    try {
      try (var output = Files.newOutputStream(file)) {
        pcm.writeTo(output);
      }
      return asr.getService(provider).getTextRealtime(file.toFile(),
          RodakRealtimeVoiceWebSocket.SAMPLE_RATE, "pcm");
    } finally {
      Files.deleteIfExists(file);
    }
  }

  VoiceReply reply(String text, String streamId, String conversationId, Identity identity) {
    var turn = new MusicTurn(identity);
    musicTurns.put(streamId, turn);
    try {
      String answer = router.response(text, text, streamId, conversationId, identity.productId());
      return new VoiceReply(answer, turn.track);
    } finally {
      musicTurns.remove(streamId, turn);
    }
  }

  public boolean hasMusicTurn(String streamId) {
    return musicTurns.containsKey(streamId);
  }

  public String prepareMusic(String streamId, Track track) {
    MusicTurn turn = musicTurns.get(streamId);
    RodakMqttBridge bridge = mqtt.getIfAvailable();
    if (turn == null || bridge == null || !bridge.canAnnounce(turn.identity)
        || musicAnnouncements.containsKey(turn.identity.deviceKey())) {
      return "设备暂时无法接收音乐播报。";
    }
    turn.track = track;
    return (track.isRandom() ? "为你随机播放《" : "正在准备播放《") + track.title() + "》。";
  }

  boolean announceMusic(Identity identity, Track track) {
    var announcement = new MusicAnnouncement(identity, track, nextAnnounceSequence(),
        Instant.now().plusSeconds(ANNOUNCE_TIMEOUT_SECONDS), 0);
    if (musicAnnouncements.putIfAbsent(identity.deviceKey(), announcement) != null) {
      return false;
    }
    // Firmware releases audio focus asynchronously after the conversation ends.
    announceCommands.schedule(() -> dispatchMusic(announcement), 750, TimeUnit.MILLISECONDS);
    return true;
  }

  private void dispatchMusic(MusicAnnouncement announcement) {
    Identity identity = announcement.identity();
    String key = identity.deviceKey();
    if (musicAnnouncements.get(key) != announcement) {
      return;
    }
    try {
      RodakMqttBridge bridge = mqtt.getIfAvailable();
      if (bridge != null && bridge.announce(identity, announcement.sequence(),
          announcement.expiresAt(), error -> rejectedMusic(announcement, error))) {
        LOG.info("Rodak music announcement sent device={} seq={}", key, announcement.sequence());
        return;
      }
    } catch (RuntimeException error) {
      LOG.warn("Rodak music announcement dispatch failed device={}", key, error);
    }
    musicAnnouncements.remove(key, announcement);
  }

  private void rejectedMusic(MusicAnnouncement announcement, String error) {
    String key = announcement.identity().deviceKey();
    if ("busy".equals(error) && announcement.retries() < MAX_BUSY_RETRIES
        && Instant.now().plusSeconds(1).isBefore(announcement.expiresAt())) {
      var retry = new MusicAnnouncement(announcement.identity(), announcement.track(),
          nextAnnounceSequence(), announcement.expiresAt(), announcement.retries() + 1);
      if (musicAnnouncements.replace(key, announcement, retry)) {
        announceCommands.schedule(() -> dispatchMusic(retry), 1, TimeUnit.SECONDS);
        return;
      }
    }
    if (musicAnnouncements.remove(key, announcement)) {
      LOG.info("Rodak music announcement rejected device={} seq={} reason={}", key,
          announcement.sequence(), error);
    }
  }

  private long nextAnnounceSequence() {
    return announceSequence.updateAndGet(sequence -> sequence == 0xffffffffL ? 1 : sequence + 1);
  }

  Track claimMusic(Identity identity, long sequence) {
    MusicAnnouncement announcement = musicAnnouncements.get(identity.deviceKey());
    if (announcement == null || announcement.sequence() != sequence
        || !announcement.identity().id().equals(identity.id())
        || announcement.identity().productId() != identity.productId()
        || announcement.identity().version() != identity.version()
        || !Instant.now().isBefore(announcement.expiresAt()) || !isCurrent(identity)) {
      return null;
    }
    return musicAnnouncements.remove(identity.deviceKey(), announcement) ? announcement.track()
        : null;
  }

  Future<?> submitMusic(Runnable playback) {
    return musicWorkers.submit(playback);
  }

  String speechText(String reply) {
    return skipToolPrefix ? ToolPrefix.stripLeadingPrefix(reply) : reply;
  }

  void releaseReplyQueue(String streamId) {
    Router.queueMap.remove(streamId);
  }

  boolean synthesize(String streamId, String text, int productId, int sampleRate, int duration,
      Consumer<byte[]> packet) {
    return tts.streamTextAudio(streamId, text, productId, sampleRate, duration, packet);
  }

  Future<?> submit(Runnable turn) {
    return workers.submit(turn);
  }

  void cancel(Future<?> future) {
    future.cancel(true);
    if (future instanceof Runnable task)
      workers.remove(task);
  }

  private record Connection(Identity identity, RodakRealtimeVoiceWebSocket socket) {}

  record VoiceReply(String text, Track track) {}

  private static final class MusicTurn {
    private final Identity identity;
    private volatile Track track;

    private MusicTurn(Identity identity) {
      this.identity = identity;
    }
  }

  private record MusicAnnouncement(Identity identity, Track track, long sequence, Instant expiresAt,
      int retries) {}
}
