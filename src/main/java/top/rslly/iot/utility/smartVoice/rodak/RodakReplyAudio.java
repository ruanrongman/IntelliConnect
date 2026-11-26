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

import java.util.ArrayDeque;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import top.rslly.iot.utility.ai.tools.ToolPrefix;
import top.rslly.iot.utility.ai.voice.AudioUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class RodakReplyAudio implements AutoCloseable {
  private static final int MAX_SEGMENT_CODE_POINTS = 240;
  private static final int MAX_IN_FLIGHT = 2;
  // Two minutes of audio at the shortest supported frame duration (5 ms).
  private static final int MAX_BUFFERED_FRAMES = 24_000;
  private static final byte[] END_OF_AUDIO = new byte[0];
  private static final Logger LOG = LoggerFactory.getLogger(RodakReplyAudio.class);
  private final StringBuilder pendingText = new StringBuilder();
  private final StringBuilder queuedText = new StringBuilder();
  private final ArrayDeque<Speech> ready = new ArrayDeque<>();
  private final ArrayDeque<Sentence> inFlight = new ArrayDeque<>();
  private final BooleanSupplier prepare;
  private final BiFunction<String, Consumer<byte[]>, Boolean> synthesize;
  private final BiConsumer<String, byte[]> send;
  private FutureTask<Boolean> playback;
  private int scanOffset;
  private int scannedCodePoints;
  private boolean prepared;
  private boolean finished;
  private volatile boolean closed;

  RodakReplyAudio(BooleanSupplier prepare, BiFunction<String, Consumer<byte[]>, Boolean> synthesize,
      BiConsumer<String, byte[]> send) {
    this.prepare = prepare;
    this.synthesize = synthesize;
    this.send = send;
  }

  synchronized void acceptText(String delta) {
    if (closed || finished || delta == null || delta.isEmpty()) {
      return;
    }
    pendingText.append(delta);
    while (scanOffset < pendingText.length()) {
      int ch = pendingText.codePointAt(scanOffset);
      int next = scanOffset + Character.charCount(ch);
      if (next == pendingText.length()
          && (ch == '.' || (ch <= Character.MAX_VALUE && Character.isHighSurrogate((char) ch)))) {
        break;
      }
      boolean boundary = "。？！；：!?;\n".indexOf(ch) >= 0
          || (ch == '.' && Character.isWhitespace(pendingText.charAt(next)));
      scanOffset = next;
      scannedCodePoints++;
      if (!boundary && scannedCodePoints < MAX_SEGMENT_CODE_POINTS) {
        continue;
      }
      String segment = pendingText.substring(0, scanOffset);
      if (ToolPrefix.isToolCallMarker(segment.stripLeading()) && ch != '。' && ch != '\n') {
        continue;
      }
      if (!boundary) {
        segment = segment.substring(0, segmentEnd(segment, 0, segment.length()));
      }
      if (isSpeakable(segment)) {
        queuedText.append(segment);
        enqueue(segment);
      }
      pendingText.delete(0, segment.length());
      scanOffset = 0;
      scannedCodePoints = 0;
    }
  }

  synchronized void finish(String spoken, String displayText) {
    if (closed) {
      throw new CancellationException();
    }
    if (queuedText.isEmpty()) {
      enqueueRemaining(spoken, displayText);
    } else {
      String remainder = unspokenText(spoken).strip();
      enqueueRemaining(remainder, remainder);
    }
    finished = true;
    notifyAll();
  }

  private void enqueueRemaining(String text, String displayText) {
    int count = text.codePointCount(0, text.length());
    if (count <= MAX_SEGMENT_CODE_POINTS) {
      enqueue(text, displayText);
      return;
    }
    int start = 0;
    while (count > MAX_SEGMENT_CODE_POINTS) {
      int limit = text.offsetByCodePoints(start, MAX_SEGMENT_CODE_POINTS);
      int end = segmentEnd(text, start, limit);
      enqueue(text.substring(start, end));
      count -= text.codePointCount(start, end);
      start = end;
    }
    enqueue(text.substring(start));
  }

  private static int segmentEnd(String text, int start, int limit) {
    int minimum = text.offsetByCodePoints(start, MAX_SEGMENT_CODE_POINTS / 2);
    for (int end = limit; end > minimum;) {
      int ch = text.codePointBefore(end);
      if ("，、。？！；,.!?".indexOf(ch) >= 0 || Character.isWhitespace(ch)) {
        return end;
      }
      end -= Character.charCount(ch);
    }
    return limit;
  }

  private String unspokenText(String spoken) {
    int offset = 0;
    for (int i = 0; i < queuedText.length(); i++) {
      char ch = queuedText.charAt(i);
      if (Character.isWhitespace(ch)) {
        continue;
      }
      while (offset < spoken.length() && Character.isWhitespace(spoken.charAt(offset))) {
        offset++;
      }
      if (offset == spoken.length() || spoken.charAt(offset) != ch) {
        return spoken;
      }
      offset++;
    }
    return spoken.substring(offset);
  }

  private void enqueue(String text) {
    enqueue(text.strip(), text.strip());
  }

  private void enqueue(String text, String displayText) {
    if (!isSpeakable(text)) {
      return;
    }
    ready.addLast(new Speech(text, displayText));
    if (prepared) {
      fillWindow();
    }
    if (playback == null) {
      playback = new FutureTask<>(this::play);
      Thread.startVirtualThread(playback);
    }
    notifyAll();
  }

  private static boolean isSpeakable(String text) {
    String stripped = text.stripLeading();
    return !ToolPrefix.isToolCallMarker(stripped)
        && !stripped.startsWith(ToolPrefix.COMPACT.getPrefix())
        && stripped.codePoints().anyMatch(Character::isLetterOrDigit);
  }

  boolean await() throws InterruptedException, ExecutionException {
    FutureTask<Boolean> work;
    synchronized (this) {
      work = playback;
    }
    return work != null && work.get();
  }

  private boolean play() throws InterruptedException {
    if (!prepare.getAsBoolean()) {
      throw new CancellationException();
    }
    synchronized (this) {
      if (closed) {
        throw new CancellationException();
      }
      prepared = true;
    }
    boolean played = false;
    boolean success = true;
    while (true) {
      Sentence sentence;
      synchronized (this) {
        while (!closed && ready.isEmpty() && inFlight.isEmpty() && !finished) {
          wait();
        }
        if (closed) {
          throw new CancellationException();
        }
        fillWindow();
        if (inFlight.isEmpty()) {
          return played && success;
        }
        sentence = inFlight.getFirst();
      }

      boolean endOfAudio = false;
      if (!played) {
        var preBuffer = AudioUtils.readPreBuffer(sentence.packets, () -> closed);
        if (preBuffer.cancelled()) {
          throw new CancellationException();
        }
        for (byte[] packet : preBuffer.frames()) {
          send.accept(sentence.speech.displayText(), packet);
          played = true;
        }
        endOfAudio = preBuffer.endOfStream();
      }
      while (!endOfAudio) {
        byte[] packet = sentence.packets.take();
        if (packet.length == 0) {
          break;
        }
        send.accept(sentence.speech.displayText(), packet);
        played = true;
      }
      try {
        success &= sentence.synthesis.get();
      } catch (ExecutionException e) {
        success = false;
        LOG.warn("Rodak speech segment failed: textLength={} errorType={}",
            sentence.speech.text().length(), e.getCause().getClass().getSimpleName());
      }
      synchronized (this) {
        if (closed) {
          throw new CancellationException();
        }
        inFlight.removeFirst();
      }
    }
  }

  private void fillWindow() {
    while (inFlight.size() < MAX_IN_FLIGHT && !ready.isEmpty()) {
      var sentence = new Sentence(ready.removeFirst());
      inFlight.addLast(sentence);
      Thread.startVirtualThread(sentence.synthesis);
    }
  }

  @Override
  public synchronized void close() {
    closed = true;
    ready.clear();
    if (playback != null) {
      playback.cancel(true);
    }
    inFlight.forEach(sentence -> sentence.synthesis.cancel(true));
    inFlight.clear();
    notifyAll();
  }

  private record Speech(String text, String displayText) {}

  private final class Sentence {
    final Speech speech;
    // Leave space for the end marker even when synthesis runs ahead of playback.
    final ArrayBlockingQueue<byte[]> packets = new ArrayBlockingQueue<>(MAX_BUFFERED_FRAMES + 1);
    final FutureTask<Boolean> synthesis;
    private boolean overflowed;
    private boolean producerFinished;

    Sentence(Speech speech) {
      this.speech = speech;
      synthesis = new FutureTask<>(() -> {
        try {
          boolean completed = synthesize.apply(speech.text(), this::addPacket);
          synchronized (this) {
            return completed && !overflowed;
          }
        } finally {
          finishPackets();
        }
      });
    }

    private synchronized void finishPackets() {
      producerFinished = true;
      packets.add(END_OF_AUDIO);
    }

    private synchronized void addPacket(byte[] packet) {
      if (packet == null || packet.length == 0) {
        return;
      }
      if (closed || producerFinished || synthesis.isCancelled()) {
        throw new CancellationException();
      }
      if (packets.remainingCapacity() <= 1 || !packets.offer(packet)) {
        overflowed = true;
        throw new IllegalStateException("Rodak speech segment exceeded the audio buffer");
      }
    }
  }
}
