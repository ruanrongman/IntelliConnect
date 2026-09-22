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
package top.rslly.iot.utility.ai.voice;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class VoiceTimbreTest {
  @ParameterizedTest
  @CsvSource({"longxiaochun,longxiaochun_v3", "longxiaoxia,longxiaoxia_v3",
      "longxiaocheng,longsanshu_v3", "longxiaobai,longxing_v3", "longshu,longshu_v3",
      "longtong,longhuhu_v3"})
  void legacyVoicesRemainValidButAreNotOfferedInPrompts(String legacy, String target) {
    assertTrue(VoiceTimbre.isValidVoice(legacy));
    assertTrue(VoiceTimbre.isValidVoice("  " + legacy + "  "));
    String normalized = VoiceTimbre.normalizeVoice(legacy);
    assertEquals("cosy_v3_flash_" + target, normalized);
    assertTrue(VoiceTimbre.isValidVoice(normalized));
    assertTrue(VoiceTimbre.stream().noneMatch(v -> v.getTimbre().equals(legacy)));
  }

  @Test
  void frontendCatalogContainsExactlyTheSelectableBackendVoices() throws Exception {
    String frontend = Files.readString(Path.of("web/src/views/productRole/voiceOptions.js"));
    var ids = Pattern.compile("value:\\s*'([^']+)'").matcher(frontend).results()
        .map(m -> m.group(1)).toList();
    Set<String> backend = VoiceTimbre.stream().map(VoiceTimbre::getTimbre)
        .collect(Collectors.toSet());
    assertEquals(ids.size(), Set.copyOf(ids).size(), "Frontend IDs must be unique");
    assertEquals(backend, Set.copyOf(ids));
    assertEquals(VoiceTimbre.values().length,
        Arrays.stream(VoiceTimbre.values()).map(VoiceTimbre::getTimbre).distinct().count());
  }

  @Test
  void invalidVoicesAreRejected() {
    assertFalse(VoiceTimbre.isValidVoice(null));
    assertFalse(VoiceTimbre.isValidVoice(" "));
    assertFalse(VoiceTimbre.isValidVoice("cosy_v3_flash_longxiaocheng_v3"));
    assertFalse(VoiceTimbre.isValidVoice("cosy_v3_flash_longxiaobai_v3"));
    assertFalse(VoiceTimbre.isValidVoice("cosy_v3_flash_longtong_v3"));
  }
}
