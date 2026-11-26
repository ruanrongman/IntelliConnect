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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.websocket.server.ServerEndpointConfig;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import top.rslly.iot.config.RodakDeviceSecurity;
import top.rslly.iot.services.rodak.RodakConversation;
import top.rslly.iot.services.rodak.RodakDescriptor;
import top.rslly.iot.services.rodak.RodakDescriptor.VoiceFormat;
import top.rslly.iot.services.rodak.RodakPairingService.Identity;
import top.rslly.iot.utility.SpringBeanUtils;
import top.rslly.iot.utility.ai.chain.Router.TurnStage;
import top.rslly.iot.utility.ai.tools.MusicPlayer.Track;
import top.rslly.iot.utility.ai.tools.ToolPrefix;
import top.rslly.iot.utility.ai.voice.ASR.StreamingAsrSession;
import top.rslly.iot.utility.ai.voice.AudioUtils;
import top.rslly.iot.utility.ai.voice.OpusEncoderUtils;
import top.rslly.iot.utility.ai.voice.SlieroVadListener;
import top.rslly.iot.utility.ai.voice.concentus.OpusDecoder;
import top.rslly.iot.utility.ai.voice.concentus.OpusException;
import top.rslly.iot.utility.ai.voice.concentus.OpusSignal;
import ws.schild.jave.process.ffmpeg.DefaultFFMPEGLocator;

@Component
@ServerEndpoint(value = RodakDescriptor.VOICE,
    configurator = RodakRealtimeVoiceWebSocket.Handshake.class)
public class RodakRealtimeVoiceWebSocket {
  static final int SAMPLE_RATE = RodakDescriptor.VOICE_UPLINK.sampleRate();
  private static final int CONTROL_LIMIT = 65536;
  private static final int MAX_CAPTURE_BYTES = SAMPLE_RATE * 2 * 20;
  private static final int VAD_WINDOW = 512 * 2;
  private static final int BARGE_IN_PREROLL_MS = 300;
  private static final int BARGE_IN_PREROLL_FRAMES =
      BARGE_IN_PREROLL_MS / RodakDescriptor.VOICE_UPLINK.frameDurationMs();
  private static final long ASR_TIMEOUT = TimeUnit.SECONDS.toNanos(20);
  private static final long ROUTER_TIMEOUT = TimeUnit.SECONDS.toNanos(90);
  private static final long FIRST_AUDIO_TIMEOUT = TimeUnit.SECONDS.toNanos(20);
  private static final long ACCEPTED_TURN_TIMEOUT = TimeUnit.SECONDS.toNanos(120);
  private static final long AUDIO_GAP_TIMEOUT = TimeUnit.SECONDS.toNanos(20);
  private static final int MAX_OUTPUT_TEXT_LENGTH = 4096;
  private static final long PROGRESS_INTERVAL = TimeUnit.MILLISECONDS.toNanos(500);
  private static final String DEVICE_VAD = "device-authoritative";
  private static final Set<String> KNOWN_FEATURES =
      Set.of("mcp", "device_vad_epoch", "announce", "announceSeq", "turn_accepted",
          "turn_progress");
  private static final Set<String> KNOWN_VAD_STRATEGIES =
      Set.of(RodakDescriptor.SERVER_VAD, RodakDescriptor.HYBRID_VAD, DEVICE_VAD);
  private static final ObjectMapper JSON = new ObjectMapper();
  private static final Logger LOG = LoggerFactory.getLogger(RodakRealtimeVoiceWebSocket.class);

  private Session socket;
  private RodakVoiceRuntime runtime;
  private Identity device;
  private String sessionId;
  private long generation;
  private long inputSequence;
  private long outputSequence;
  private long playbackEpoch;
  private long lastOutputEpoch;
  private long vadSequence;
  private long vadPlaybackEpoch;
  private long vadCaptureEpoch;
  private long deviceBoundary;
  private String vadStrategy;
  private long lastActivity;
  private VoiceFormat downlink;
  private boolean inputOpen;
  private boolean announceSession;
  private boolean turnAcceptedEnabled;
  private boolean turnProgressEnabled;
  private long acceptedTurnCount;
  private boolean closed;
  private OpusDecoder decoder;
  private SlieroVadListener serverVad;
  private final byte[] vadWindow = new byte[VAD_WINDOW];
  private final ArrayDeque<UplinkFrame> playbackUplink = new ArrayDeque<>();
  private int vadOffset;
  private Turn current;

  public static class Handshake extends ServerEndpointConfig.Configurator {
    @Override
    public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request,
        HandshakeResponse response) {
      for (String name : List.of("Authorization", "Device-Id", "Client-Id", "Protocol-Version")) {
        request.getHeaders().entrySet().stream()
            .filter(header -> name.equalsIgnoreCase(header.getKey()))
            .findFirst().filter(header -> !header.getValue().isEmpty())
            .ifPresent(
                header -> config.getUserProperties().put(name, header.getValue().getFirst()));
      }
    }
  }

  @OnOpen
  public void onOpen(Session session) {
    socket = session;
    runtime = SpringBeanUtils.getBean(RodakVoiceRuntime.class);
    try {
      var headers = session.getUserProperties();
      device = runtime
          .authenticate(RodakDeviceSecurity.bearer((String) headers.remove("Authorization")));
      if (!RodakDeviceSecurity.deviceMatches(device, (String) headers.get("Device-Id"))) {
        throw new SecurityException("device mismatch");
      }
      socket.setMaxBinaryMessageBufferSize(Rav1Frame.MAX_PAYLOAD + 12);
      socket.setMaxTextMessageBufferSize(CONTROL_LIMIT);
      socket.setMaxIdleTimeout(120_000);
      socket.getUserProperties().put("org.apache.tomcat.websocket.BLOCKING_SEND_TIMEOUT", 2500L);
      decoder = new OpusDecoder(SAMPLE_RATE, 1);
      lastActivity = System.nanoTime();
      runtime.connect(device, this);
    } catch (SecurityException e) {
      close(1008, "unauthorized");
    } catch (Exception e) {
      close(1011, "voice unavailable");
    }
  }

  @OnMessage
  public synchronized void onText(String text) {
    if (closed || !runtime.registered(device, this)) {
      return;
    }
    lastActivity = System.nanoTime();
    try {
      require(text.getBytes(StandardCharsets.UTF_8).length <= CONTROL_LIMIT);
      JsonNode event = JSON.reader()
          .with(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
          .readTree(text);
      require(event != null && event.isObject());
      String type = string(event, "event");
      if ("session.open".equals(type)) {
        negotiate(event);
        return;
      }
      require(sessionId != null && sessionId.equals(string(event, "sessionId")));
      if (announceSession && "playback.abort".equals(type)) {
        fields(event, "reason", "playbackEpoch");
        string(event, "reason");
        if (matchesPlayback(event)) {
          cancelTurn("interrupted");
          endSession("announce-interrupted");
        }
        return;
      }
      require(!announceSession || "session.end".equals(type));
      switch (type) {
        case "input.start" -> {
          fields(event, "mode");
          if (event.has("mode")) {
            require(Set.of("manual-stop", "auto-stop", "realtime").contains(string(event, "mode")));
          }
          inputOpen = true;
          if (current == null || !current.capturing) {
            interruptTurn("input.start");
            playbackUplink.clear();
            startTurn();
          }
        }
        case "wake.detected" -> {
          fields(event, "text");
          string(event, "text");
          inputOpen = false;
          playbackUplink.clear();
          interruptTurn("wake.detected");
        }
        case "input.stop" -> {
          fields(event, "reason");
          if (event.has("reason"))
            string(event, "reason");
          inputOpen = false;
          playbackUplink.clear();
          finishInput();
        }
        case "vad" -> deviceVad(event);
        case "playback.abort" -> {
          fields(event, "reason", "playbackEpoch");
          String reason = string(event, "reason");
          long epoch = playbackEpoch(event);
          if ("vad_detected".equals(reason)) {
            if (canCaptureBargeIn(epoch == 0 ? lastOutputEpoch : epoch)
                || canCaptureProcessingInterrupt(epoch)) {
              startBargeIn("playback.abort");
            } else if (!inputOpen && matchesPlayback(event) && current != null && current.playing) {
              interruptTurn("playback.abort");
            }
          } else if ((epoch == 0 || epoch == playbackEpoch) && current != null
              && !current.capturing) {
            interruptTurn("playback.abort");
            playbackUplink.clear();
          }
        }
        case "session.end" -> {
          fields(event, "reason");
          if (event.has("reason"))
            string(event, "reason");
          close(1000, "session ended");
        }
        default -> throw new IllegalArgumentException("invalid event");
      }
    } catch (IllegalArgumentException | com.fasterxml.jackson.core.JsonProcessingException e) {
      protocolError();
    } catch (Exception e) {
      close(1011, "voice unavailable");
    }
  }

  @OnMessage
  public synchronized void onBinary(ByteBuffer bytes) {
    if (closed || !runtime.registered(device, this)) {
      return;
    }
    lastActivity = System.nanoTime();
    try {
      require(sessionId != null && inputOpen && bytes.remaining() <= Rav1Frame.MAX_PAYLOAD + 12);
      byte[] encoded = new byte[bytes.remaining()];
      bytes.get(encoded);
      Rav1Frame frame = Rav1Frame.decode(encoded, inputSequence);
      inputSequence = frame.sequence();
      int samples = SAMPLE_RATE * RodakDescriptor.VOICE_UPLINK.frameDurationMs() / 1000;
      byte[] pcm = new byte[samples * Short.BYTES];
      require(
          decoder.decode(frame.payload(), 0, frame.payload().length, pcm, 0, samples,
              false) == samples);
      if (current != null && !current.capturing) {
        if (current.playing) {
          retainPlaybackUplink(pcm);
        } else if (canCaptureProcessingInterrupt(0)) {
          retainPlaybackUplink(pcm);
          if (serverVad != null && detectSpeech(pcm).started()) {
            startBargeIn("processing.vad");
          }
        }
        return;
      }
      VadBoundary boundary = detectSpeech(pcm);
      if (current == null && boundary.started()) {
        playbackUplink.clear();
        startTurn();
      }
      if (current == null) {
        return;
      }
      Turn turn = current;
      appendAudio(turn, pcm);
      boolean awaitingDeviceEnd = deviceBoundary != 0
          && System.nanoTime() - deviceBoundary < TimeUnit.SECONDS.toNanos(3);
      turn.serverEndPending |= boundary.ended();
      if ((!awaitingDeviceEnd && turn.serverEndPending) || turn.pcm.size() >= MAX_CAPTURE_BYTES) {
        finishInput();
      }
    } catch (IllegalArgumentException | OpusException e) {
      protocolError();
    } catch (Exception e) {
      close(1011, "voice unavailable");
    }
  }

  private void retainPlaybackUplink(byte[] pcm) {
    if (playbackUplink.size() == BARGE_IN_PREROLL_FRAMES)
      playbackUplink.removeFirst();
    playbackUplink.addLast(new UplinkFrame(runtime.nanoTime(), pcm));
  }

  private VadBoundary detectSpeech(byte[] pcm) {
    if (serverVad == null)
      return new VadBoundary(false, false);
    boolean started = false;
    boolean ended = false;
    for (int offset = 0; offset < pcm.length;) {
      int count = Math.min(pcm.length - offset, VAD_WINDOW - vadOffset);
      System.arraycopy(pcm, offset, vadWindow, vadOffset, count);
      offset += count;
      vadOffset += count;
      if (vadOffset == VAD_WINDOW) {
        var boundary = serverVad.listen(vadWindow);
        vadOffset = 0;
        if (boundary != null) {
          started |= boundary.containsKey("start");
          ended |= boundary.containsKey("end");
        }
      }
    }
    return new VadBoundary(started, ended);
  }

  private void appendAudio(Turn turn, byte[] pcm) {
    turn.pcm.writeBytes(pcm);
    if (turn.asr != null && !turn.asr.sendPcm(pcm)) {
      turn.asr.cancel();
      turn.asr = null;
    }
  }

  private void negotiate(JsonNode event) throws IOException {
    require(sessionId == null);
    for (var names = event.fieldNames(); names.hasNext();) {
      require(Set.of("event", "protocol", "protocolVersion", "generation", "uplink",
          "downlink", "features", "vadStrategies", "preferredVadStrategy").contains(names.next()));
    }
    require("rodak-realtime-voice".equals(string(event, "protocol")));
    require(number(event, "protocolVersion", 1) == 1);
    generation = number(event, "generation", 0xffffffffL);
    require(generation > 0);
    require(RodakDescriptor.VOICE_UPLINK.equals(audioFormat(event.path("uplink"))));
    downlink = audioFormat(event.path("downlink"));
    JsonNode features = event.path("features");
    require(features.isObject() && features.path("mcp").isBoolean()
        && !features.path("mcp").booleanValue());
    features.fieldNames().forEachRemaining(key -> require(KNOWN_FEATURES.contains(key)));
    number(features, "device_vad_epoch", 1);
    announceSession = features.has("announce");
    require(announceSession == features.has("announceSeq"));
    require(!announceSession
        || (features.path("announce").isBoolean() && features.path("announce").booleanValue()));
    long announceSeq = announceSession ? number(features, "announceSeq", 0xffffffffL) : 0;
    require(!announceSession || announceSeq > 0);
    boolean wantsAccepted = features.has("turn_accepted")
        && number(features, "turn_accepted", 1) == 1;
    boolean wantsProgress = features.has("turn_progress")
        && number(features, "turn_progress", 1) == 1;
    require(!wantsProgress || wantsAccepted);
    turnAcceptedEnabled = wantsAccepted && !announceSession;
    turnProgressEnabled = wantsProgress && turnAcceptedEnabled;
    if (turnAcceptedEnabled) {
      socket.setMaxIdleTimeout(150_000);
    }

    JsonNode strategies = event.path("vadStrategies");
    String preferred = string(event, "preferredVadStrategy");
    var supported = new java.util.HashSet<String>();
    require(strategies.isArray());
    for (JsonNode item : strategies) {
      require(item.isTextual() && KNOWN_VAD_STRATEGIES.contains(item.asText())
          && supported.add(item.asText()));
    }
    require(supported.contains(preferred));
    String configured = SpringBeanUtils.getBean(RodakDescriptor.class).preferredVadStrategy();
    if (supported.contains(configured)) {
      vadStrategy = configured;
    } else if (supported.contains(RodakDescriptor.SERVER_VAD)) {
      vadStrategy = RodakDescriptor.SERVER_VAD;
    } else {
      vadStrategy = DEVICE_VAD;
    }
    require(supported.contains(vadStrategy));
    if (!announceSession && !DEVICE_VAD.equals(vadStrategy)) {
      serverVad = new SlieroVadListener();
      serverVad.init();
      if (serverVad.isStop()) {
        close(1011, "server VAD unavailable");
        return;
      }
    }
    sessionId = UUID.randomUUID().toString();
    send(Map.of("event", "session.ready", "sessionId", sessionId, "generation", generation,
        "protocol", "rodak-realtime-voice", "protocolVersion", 1, "transport", "websocket",
        "uplink", RodakDescriptor.VOICE_UPLINK.wire(), "downlink", downlink.wire(),
        "vadStrategy", vadStrategy));
    LOG.debug("Rodak voice ready sessionId={} generation={} vad={} downlink={}",
        sessionId, generation, vadStrategy, downlink);
    if (announceSession) {
      Track track = runtime.claimMusic(device, announceSeq);
      if (track == null) {
        endSession("announce-expired");
        return;
      }
      Turn turn = new Turn(++playbackEpoch, runtime.nanoTime());
      turn.capturing = false;
      current = turn;
      turn.work = runtime.submitMusic(() -> playAnnouncement(turn, track, announceSeq));
    }
  }

  private void deviceVad(JsonNode event) throws IOException {
    fields(event, "state", "source", "sequence", "triggerMs", "playbackEpoch");
    String state = string(event, "state");
    require("start".equals(state) || "end".equals(state));
    string(event, "source");
    long sequence = number(event, "sequence", 0xffffffffL);
    require(sequence > 0);
    number(event, "triggerMs", 9007199254740991L);
    long epoch = playbackEpoch(event);
    if (RodakDescriptor.SERVER_VAD.equals(vadStrategy)) {
      return;
    }
    if ("start".equals(state)) {
      boolean processingInterrupt = canCaptureProcessingInterrupt(epoch);
      boolean playbackInterrupt = canCaptureBargeIn(epoch == 0 ? lastOutputEpoch : epoch);
      if (sequence <= vadSequence || !inputOpen
          || (!processingInterrupt && epoch != 0 && epoch != playbackEpoch)
          || (current != null && !current.capturing && !current.outputStarted
              && !processingInterrupt)) {
        return;
      }
      if (processingInterrupt || playbackInterrupt) {
        startBargeIn("vad.start");
      } else if (current == null) {
        playbackUplink.clear();
        startTurn();
      }
      vadSequence = sequence;
      vadPlaybackEpoch = epoch;
      vadCaptureEpoch = current.epoch;
      deviceBoundary = System.nanoTime();
      current.serverEndPending = false;
    } else if (current != null && current.capturing && current.epoch == vadCaptureEpoch
        && sequence == vadSequence && epoch == vadPlaybackEpoch) {
      finishInput();
    }
  }

  private boolean canCaptureBargeIn(long epoch) {
    return inputOpen && epoch != 0 && epoch == lastOutputEpoch && epoch == playbackEpoch
        && (current == null || current.playing || current.outputStarted);
  }

  private boolean canCaptureProcessingInterrupt(long epoch) {
    return turnAcceptedEnabled && inputOpen && current != null && current.id != 0
        && !current.outputStarted && epoch == 0;
  }

  private void startBargeIn(String event) throws IOException {
    interruptTurn(event);
    startTurn();
    // Keep the words spoken while the device was confirming the interruption.
    long threshold = runtime.nanoTime() - TimeUnit.MILLISECONDS.toNanos(BARGE_IN_PREROLL_MS);
    while (!playbackUplink.isEmpty()) {
      UplinkFrame frame = playbackUplink.removeFirst();
      if (frame.receivedAt() - threshold >= 0) {
        appendAudio(current, frame.pcm());
        detectSpeech(frame.pcm());
      }
    }
    if (serverVad != null)
      serverVad.confirmSpeechStarted();
  }

  private boolean matchesPlayback(JsonNode event) {
    long epoch = playbackEpoch(event);
    return epoch == 0 || epoch == playbackEpoch;
  }

  private static long playbackEpoch(JsonNode event) {
    if (!event.has("playbackEpoch")) {
      return 0;
    }
    long epoch = number(event, "playbackEpoch", 9007199254740991L);
    require(epoch > 0);
    return epoch;
  }

  private void startTurn() {
    deviceBoundary = 0;
    Turn turn = new Turn(++playbackEpoch, runtime.nanoTime());
    current = turn;
    turn.provider = runtime.asrProvider(device.productId());
    turn.asr = runtime.startAsr(turn.provider, sessionId + "-" + turn.epoch);
  }

  private void finishInput() throws IOException {
    if (current == null || !current.capturing) {
      return;
    }
    Turn turn = current;
    turn.capturing = false;
    if (turn.pcm.size() == 0) {
      cancelTurn("empty-input");
      return;
    }
    try {
      if (turnAcceptedEnabled)
        armDeadline(turn, runtime.nanoTime() + ASR_TIMEOUT);
      turn.work = runtime.submit(() -> respond(turn));
    } catch (RejectedExecutionException e) {
      cancelTurn("busy");
      sendError("voice-busy", "voice workers are busy", true);
    }
  }

  private synchronized boolean acceptTurn(Turn turn) throws IOException {
    if (!canRespond(turn))
      return false;
    if (turnAcceptedEnabled) {
      long now = runtime.nanoTime();
      long id = acceptedTurnCount + 1;
      send(Map.of("event", "turn.accepted", "sessionId", sessionId, "turnId", id));
      acceptedTurnCount = id;
      turn.id = id;
      turn.responseDeadline = now + ACCEPTED_TURN_TIMEOUT;
      resetServerVad();
      armDeadline(turn, now + ROUTER_TIMEOUT);
    }
    return true;
  }

  private synchronized boolean prepareAudio(Turn turn) {
    if (!canRespond(turn))
      return false;
    if (turn.id != 0) {
      armDeadline(turn,
          Math.min(turn.responseDeadline, runtime.nanoTime() + FIRST_AUDIO_TIMEOUT));
      reportProgress(turn, TurnStage.COMPOSING);
    }
    return true;
  }

  private synchronized void reportProgress(Turn turn, TurnStage stage) {
    if (!canRespond(turn))
      throw new CancellationException();
    if (!turnProgressEnabled || turn.id == 0 || turn.outputStarted)
      return;
    turn.pendingStage = stage == turn.lastStage ? null : stage;
    if (turn.pendingStage == null)
      return;
    long remaining = turn.lastProgressAt + PROGRESS_INTERVAL - runtime.nanoTime();
    if (stage == TurnStage.TOOL || turn.progressSequence == 0 || remaining <= 0) {
      clearPendingProgressTask(turn);
      flushProgress(turn);
      if (!ownsTurn(turn))
        throw new CancellationException();
    } else if (turn.progressTask == null) {
      long lastProgress = turn.lastProgressAt;
      turn.progressTask =
          runtime.scheduleVoiceTimer(() -> flushScheduledProgress(turn, lastProgress), remaining);
    }
  }

  private synchronized void flushScheduledProgress(Turn turn, long lastProgress) {
    if (turn.progressTask == null || turn.lastProgressAt != lastProgress)
      return;
    turn.progressTask = null;
    flushProgress(turn);
  }

  private void flushProgress(Turn turn) {
    TurnStage stage = turn.pendingStage;
    turn.pendingStage = null;
    if (stage == null || turn.outputStarted || !canRespond(turn))
      return;
    long sequence = turn.progressSequence + 1;
    try {
      send(Map.of("event", "turn.progress", "sessionId", sessionId, "turnId", turn.id,
          "seq", sequence, "stage", stage.name().toLowerCase(Locale.ROOT)));
      turn.progressSequence = sequence;
      turn.lastStage = stage;
      turn.lastProgressAt = runtime.nanoTime();
    } catch (IOException e) {
      close(1011, "send failed");
    }
  }

  private void clearPendingProgressTask(Turn turn) {
    if (turn.progressTask != null) {
      turn.progressTask.cancel(false);
      turn.progressTask = null;
    }
  }

  private void armDeadline(Turn turn, long deadline) {
    clearDeadline(turn);
    turn.deadlineTask = runtime.scheduleVoiceTimer(() -> checkTurnDeadline(turn, deadline),
        Math.max(0, deadline - runtime.nanoTime()));
    turn.deadline = deadline;
  }

  private void clearDeadline(Turn turn) {
    if (turn.deadlineTask != null) {
      turn.deadlineTask.cancel(false);
      turn.deadlineTask = null;
    }
  }

  private synchronized void checkTurnDeadline(Turn turn, long deadline) {
    if (!ownsTurn(turn) || turn.deadlineTask == null || turn.deadline != deadline)
      return;
    long effectiveDeadline =
        turn.playing ? turn.lastAudioAt + AUDIO_GAP_TIMEOUT : turn.deadline;
    if (effectiveDeadline - runtime.nanoTime() > 0) {
      armDeadline(turn, effectiveDeadline);
    } else {
      expireTurn(turn);
    }
  }

  private boolean ownsTurn(Turn turn) {
    return current == turn && !closed && runtime.registered(device, this);
  }

  private boolean canRespond(Turn turn) {
    if (!ownsTurn(turn) || !java.time.Instant.now().isBefore(device.expiresAt()))
      return false;
    if (turn.deadlineTask != null) {
      long deadline = turn.playing ? turn.lastAudioAt + AUDIO_GAP_TIMEOUT : turn.deadline;
      if (deadline - runtime.nanoTime() <= 0) {
        expireTurn(turn);
        return false;
      }
    }
    return true;
  }

  private void expireTurn(Turn turn) {
    boolean playing = turn.playing;
    if (playing) {
      LOG.warn("Rodak audio stalled sessionId={} epoch={} packets={} gapMs={}",
          sessionId, turn.epoch, turn.outputPackets,
          TimeUnit.NANOSECONDS.toMillis(runtime.nanoTime() - turn.lastAudioAt));
    }
    try {
      cancelTurn("timeout");
      if (!playing) {
        sendError("voice-turn-timeout", "voice turn timed out", false);
        close(1000, "voice turn timed out");
      }
    } catch (IOException e) {
      close(1011, "send failed");
    }
  }

  private void sendTurnError(Turn turn, String code, String message) throws IOException {
    clearDeadline(turn);
    clearPendingProgressTask(turn);
    sendError(code, message, true);
    turn.errorSent = true;
  }

  private void respond(Turn turn) {
    synchronized (this) {
      turn.worker = Thread.currentThread();
      if (!canRespond(turn))
        return;
    }
    String streamId = "rodak:" + sessionId + ":" + turn.epoch;
    String chatId = RodakConversation.chatId(device.productId(), device.id(), device.version());
    String stage = "asr";
    boolean goodbye = false;
    long started = System.nanoTime();
    try {
      String text = runtime.transcribe(turn.provider, turn.asr, turn.pcm);
      if (!stillCurrent(turn))
        return;
      if (text == null || text.isBlank()) {
        LOG.info("Rodak voice ASR empty sessionId={} epoch={} pcmBytes={} elapsedMs={}",
            sessionId, turn.epoch, turn.pcm.size(), elapsedMillis(started));
        return;
      }
      if (text.length() > 4096)
        text = text.substring(0, 4096);
      LOG.info(
          "Rodak voice ASR complete sessionId={} epoch={} pcmBytes={} textChars={} elapsedMs={}",
          sessionId, turn.epoch, turn.pcm.size(), text.length(), elapsedMillis(started));
      stage = "router";
      started = System.nanoTime();
      if (!acceptTurn(turn))
        return;
      var audio = startReplyAudio(turn, streamId);
      var reply = runtime.reply(text, streamId, chatId, device,
          turnAcceptedEnabled ? step -> reportProgress(turn, step) : null, audio::acceptText);
      if (!stillCurrent(turn))
        return;
      if (reply.track() != null) {
        synchronized (this) {
          if (canRespond(turn)) {
            turn.work = null;
            audio.close();
            if (turn.playing)
              stopOutput(turn, "interrupted");
            endSession("music-announce");
            runtime.announceMusic(device, reply.track());
          }
        }
        return;
      }
      String answer = reply.text();
      if (answer == null || answer.isBlank()) {
        LOG.warn("Rodak voice Router returned empty reply sessionId={} epoch={} elapsedMs={}",
            sessionId, turn.epoch, elapsedMillis(started));
        answer = "";
      }
      goodbye = answer.startsWith(ToolPrefix.GOODBYE.getPrefix());
      String spoken = runtime.speechText(answer);
      stage = "tts";
      started = System.nanoTime();
      audio.finish(spoken, answer);
      boolean emitted = audio.await();
      synchronized (this) {
        if (canRespond(turn)) {
          LOG.info(
              "Rodak voice TTS finished sessionId={} epoch={} success={} packets={} elapsedMs={}",
              sessionId, turn.epoch, emitted, turn.outputPackets, elapsedMillis(started));
          if (turn.playing)
            stopOutput(turn, emitted ? "complete" : "error");
          else if (!emitted && !goodbye && !spoken.isBlank())
            sendTurnError(turn, "tts-failed", "speech synthesis failed");
        }
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (CancellationException ignored) {
      // The session or turn was superseded.
    } catch (Exception e) {
      LOG.warn("Rodak voice stage failed sessionId={} epoch={} stage={} errorType={} elapsedMs={}",
          sessionId, turn.epoch, stage, e.getClass().getSimpleName(), elapsedMillis(started));
      synchronized (this) {
        if (canRespond(turn)) {
          try {
            if (turn.playing)
              stopOutput(turn, "error");
            else if (!goodbye)
              sendTurnError(turn, "voice-runtime-error", "voice turn failed");
          } catch (IOException io) {
            close(1011, "send failed");
          }
        }
      }
    } finally {
      runtime.releaseReplyQueue(streamId);
      synchronized (this) {
        if (current == turn) {
          try {
            if (canRespond(turn)) {
              turn.work = null;
              if (goodbye) {
                endSession("goodbye-intent");
              } else if (turn.id != 0 && !turn.outputStarted && !turn.errorSent) {
                sendError("voice-turn-no-reply", "voice turn produced no playable reply", false);
                close(1000, "voice turn produced no reply");
              }
            }
          } catch (IOException e) {
            close(1011, "send failed");
          } finally {
            if (current == turn) {
              clearTurn(turn);
            }
          }
        }
      }
    }
  }

  private synchronized RodakReplyAudio startReplyAudio(Turn turn, String streamId) {
    if (!canRespond(turn))
      throw new CancellationException();
    long[] nextPacket = {0};
    turn.audio = new RodakReplyAudio(() -> prepareAudio(turn),
        (text, packets) -> runtime.synthesize(streamId, text, device.productId(),
            downlink.sampleRate(), downlink.frameDurationMs(), packets),
        (text, packet) -> output(turn, text, packet, nextPacket));
    return turn.audio;
  }

  private void playAnnouncement(Turn turn, Track track, long announceSeq) {
    String reason = "announce-failed";
    Process process = null;
    try {
      int rate = downlink.sampleRate();
      int frameDurationMs = downlink.frameDurationMs();
      process = new ProcessBuilder(MusicFfmpeg.LOCATOR.getExecutablePath(), "-nostdin",
          "-v", "error", "-f", "mp3", "-i", track.mp3File().toAbsolutePath().toString(),
          "-vn", "-f", "s16le", "-ac", "1", "-ar", Integer.toString(rate), "pipe:1")
              .redirectError(ProcessBuilder.Redirect.DISCARD).start();
      synchronized (this) {
        if (closed || current != turn) {
          return;
        }
        turn.transcoder = process;
      }
      var encoder = new OpusEncoderUtils(rate, 1, frameDurationMs);
      encoder.getEncoder().setBitrate(48_000);
      encoder.getEncoder().setSignalType(OpusSignal.OPUS_SIGNAL_MUSIC);
      int frameBytes = rate * frameDurationMs * Short.BYTES / 1000;
      long[] nextPacket = {0};
      try (var pcm = process.getInputStream()) {
        byte[] frame;
        while ((frame = pcm.readNBytes(frameBytes)).length != 0) {
          for (byte[] packet : encoder.encodePcmToOpus(frame, frame.length < frameBytes)) {
            if (packet.length == 0) {
              throw new IOException("music Opus encoding failed");
            }
            output(turn, track.title(), packet, nextPacket);
          }
        }
      }
      if (process.waitFor() == 0 && turn.outputPackets > 0) {
        reason = "announce-finished";
      }
    } catch (CancellationException ignored) {
      // Playback was cancelled when the connection ended.
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (Exception e) {
      LOG.warn("Rodak music playback failed seq={} errorType={}", announceSeq,
          e.getClass().getSimpleName());
    } finally {
      if (process != null) {
        process.destroyForcibly();
      }
      synchronized (this) {
        if (current == turn && !closed && runtime.registered(device, this)) {
          turn.work = null;
          try {
            if (turn.playing) {
              stopOutput(turn, "announce-finished".equals(reason) ? "complete" : "error");
            }
            endSession(reason);
          } catch (IOException e) {
            close(1011, "send failed");
          }
        }
      }
    }
  }

  private static long elapsedMillis(long started) {
    return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
  }

  private boolean stillCurrent(Turn turn) {
    boolean authorized = runtime.isCurrent(device);
    synchronized (this) {
      if (!authorized && !closed)
        close(1008, "device authorization ended");
      return authorized && canRespond(turn);
    }
  }

  private void output(Turn turn, String text, byte[] packet, long[] nextPacket) {
    try {
      boolean prebuffering = !announceSession && turn.outputPackets < AudioUtils.PRE_BUFFER_COUNT;
      long scheduledAt = nextPacket[0];
      long delay = scheduledAt - System.nanoTime();
      if (!prebuffering && scheduledAt != 0 && delay > 0)
        TimeUnit.NANOSECONDS.sleep(delay);
      synchronized (this) {
        if (Thread.currentThread().isInterrupted() || !canRespond(turn)) {
          throw new CancellationException();
        }
        if (!turn.playing) {
          playbackUplink.clear();
          clearPendingProgressTask(turn);
          turn.pendingStage = null;
          clearDeadline(turn);
          String displayText = text.substring(0, Math.min(text.length(), MAX_OUTPUT_TEXT_LENGTH));
          send(Map.of("event", "output.start", "sessionId", sessionId,
              "playbackEpoch", turn.epoch, "text", displayText));
          turn.outputStarted = true;
          lastOutputEpoch = turn.epoch;
          turn.playing = true;
        }
        long sendStartedAt = System.nanoTime();
        socket.getBasicRemote()
            .sendBinary(ByteBuffer.wrap(new Rav1Frame(++outputSequence, packet).encode()));
        if (!announceSession) {
          turn.lastAudioAt = runtime.nanoTime();
          if (turn.deadlineTask == null)
            armDeadline(turn, turn.lastAudioAt + AUDIO_GAP_TIMEOUT);
        }
        if (++turn.outputPackets == 1) {
          LOG.info("Rodak voice first audio sent sessionId={} epoch={} sequence={} bytes={}",
              sessionId, turn.epoch, outputSequence, packet.length);
        }
        long frameNanos = TimeUnit.MILLISECONDS.toNanos(downlink.frameDurationMs());
        if (prebuffering || scheduledAt == 0 || sendStartedAt - scheduledAt >= frameNanos) {
          scheduledAt = sendStartedAt;
        }
        nextPacket[0] = scheduledAt + frameNanos;
        lastActivity = System.nanoTime();
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new CancellationException();
    } catch (IOException e) {
      LOG.warn("Rodak voice output send failed sessionId={} epoch={} packets={} errorType={}",
          sessionId, turn.epoch, turn.outputPackets, e.getClass().getSimpleName());
      close(1011, "send failed");
      throw new CancellationException();
    }
  }

  private void stopOutput(Turn turn, String reason) throws IOException {
    clearDeadline(turn);
    turn.playing = false;
    send(Map.of("event", "output.stop", "sessionId", sessionId, "reason", reason,
        "playbackEpoch", turn.epoch));
    LOG.info("Rodak voice output stopped sessionId={} epoch={} reason={} packets={}",
        sessionId, turn.epoch, reason, turn.outputPackets);
  }

  private void interruptTurn(String event) throws IOException {
    if (current != null && !current.capturing) {
      LOG.info("Rodak voice interruption received sessionId={} epoch={} event={}",
          sessionId, current.epoch, event);
    }
    cancelTurn("interrupted");
  }

  private void cancelTurn(String reason) throws IOException {
    Turn turn = current;
    if (turn == null)
      return;
    clearTurn(turn);
    if (turn.work != null && turn.worker != Thread.currentThread())
      runtime.cancel(turn.work);
    if (turn.transcoder != null)
      turn.transcoder.destroyForcibly();
    if (turn.asr != null)
      turn.asr.cancel();
    if (turn.playing && !closed)
      stopOutput(turn, reason);
  }

  private void clearTurn(Turn turn) {
    current = null;
    if (turn.audio != null)
      turn.audio.close();
    clearDeadline(turn);
    clearPendingProgressTask(turn);
    turn.pendingStage = null;
    deviceBoundary = 0;
    resetServerVad();
  }

  private void resetServerVad() {
    if (serverVad != null) {
      serverVad.reset();
      vadOffset = 0;
    }
  }

  void checkConnection(Identity identity) {
    boolean valid = runtime.isCurrent(identity);
    synchronized (this) {
      if (closed)
        return;
      if (!valid) {
        close(1008, "device authorization ended");
      } else if (sessionId == null
          && System.nanoTime() - lastActivity > TimeUnit.SECONDS.toNanos(10)) {
        close(1002, "session.open timeout");
      } else if (announceSession && current != null
          && System.nanoTime() - lastActivity > TimeUnit.SECONDS.toNanos(20)) {
        close(1011, "announce stalled");
      } else if (!announceSession && current != null
          && !current.playing && (!turnAcceptedEnabled || current.capturing)
          && runtime.nanoTime() - current.startedAt > ROUTER_TIMEOUT) {
        try {
          cancelTurn("timeout");
          sendError("turn-timeout", "voice turn timed out", true);
        } catch (IOException e) {
          close(1011, "send failed");
        }
      }
    }
  }

  private static VoiceFormat audioFormat(JsonNode value) {
    require(value.isObject() && value.size() == 4 && "opus".equals(string(value, "codec"))
        && number(value, "channels", 1) == 1);
    return new VoiceFormat((int) number(value, "sampleRateHz", 48000),
        (int) number(value, "frameDurationMs", 60));
  }

  private static void fields(JsonNode value, String... allowed) {
    Set<String> fields = Set.of(allowed);
    for (var names = value.fieldNames(); names.hasNext();) {
      String name = names.next();
      require("event".equals(name) || "sessionId".equals(name) || fields.contains(name));
    }
  }

  private static long number(JsonNode node, String key, long max) {
    JsonNode value = node.path(key);
    require(value.isIntegralNumber() && value.canConvertToLong()
        && value.longValue() >= 0 && value.longValue() <= max);
    return value.longValue();
  }

  private static String string(JsonNode node, String key) {
    JsonNode value = node.path(key);
    require(value.isTextual() && !value.asText().isBlank());
    return value.asText();
  }

  private static void require(boolean condition) {
    if (!condition)
      throw new IllegalArgumentException("invalid canonical message");
  }

  private void send(Object value) throws IOException {
    socket.getBasicRemote().sendText(JSON.writeValueAsString(value));
  }

  private void endSession(String reason) throws IOException {
    send(Map.of("event", "session.end", "sessionId", sessionId, "reason", reason));
    close(1000, reason);
  }

  private void sendError(String code, String message, boolean retryable) throws IOException {
    var error = JSON.createObjectNode().put("event", "error").put("code", code)
        .put("message", message).put("retryable", retryable);
    if (sessionId != null)
      error.put("sessionId", sessionId);
    send(error);
  }

  private void protocolError() {
    try {
      sendError("invalid-canonical-event", "invalid canonical message", false);
    } catch (IOException ignored) {
      // The socket may already have closed.
    } finally {
      close(1002, "invalid canonical message");
    }
  }

  synchronized void close(int code, String reason) {
    if (closed)
      return;
    closed = true;
    playbackUplink.clear();
    try {
      cancelTurn(reason);
      if (serverVad != null)
        serverVad.destroy();
      if (socket != null && socket.isOpen())
        socket.close(new CloseReason(() -> code, reason));
    } catch (IOException ignored) {
      // Resources and the registry entry are released even if the peer is gone.
    } finally {
      if (device != null)
        runtime.disconnect(device, this);
      LOG.info("Rodak voice closed sessionId={} generation={} code={} reason={}", sessionId,
          generation, code, reason);
    }
  }

  @OnClose
  public void onClose(Session session, CloseReason reason) {
    close(reason.getCloseCode().getCode(), "peer closed");
  }

  @OnError
  public void onError(Session session, Throwable error) {
    close(1011, "transport error");
  }

  private record UplinkFrame(long receivedAt, byte[] pcm) {}

  private record VadBoundary(boolean started, boolean ended) {}

  private static final class Turn {
    final long epoch;
    final long startedAt;
    final ByteArrayOutputStream pcm = new ByteArrayOutputStream();
    String provider;
    StreamingAsrSession asr;
    Future<?> work;
    RodakReplyAudio audio;
    Thread worker;
    Future<?> deadlineTask;
    Future<?> progressTask;
    long id;
    long deadline;
    long responseDeadline;
    long lastAudioAt;
    long lastProgressAt;
    long progressSequence;
    TurnStage lastStage;
    TurnStage pendingStage;
    Process transcoder;
    boolean capturing = true;
    boolean playing;
    boolean outputStarted;
    boolean errorSent;
    boolean serverEndPending;
    int outputPackets;

    Turn(long epoch, long startedAt) {
      this.epoch = epoch;
      this.startedAt = startedAt;
    }
  }

  private static final class MusicFfmpeg {
    static final DefaultFFMPEGLocator LOCATOR = new DefaultFFMPEGLocator();
  }
}
