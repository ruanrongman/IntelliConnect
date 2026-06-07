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
package top.rslly.iot.utility.ai.rag;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

class RagUtilityTest {

  @Test
  void parseMultipartFileParsesTxt() throws Exception {
    MockMultipartFile file = new MockMultipartFile("file", "note.txt", "text/plain",
        "hello document".getBytes(StandardCharsets.UTF_8));

    String text = RagUtility.parseMultipartFile(file);

    Assertions.assertTrue(text.contains("hello document"));
  }

  @Test
  void parseMultipartFileParsesMarkdown() throws Exception {
    MockMultipartFile file = new MockMultipartFile("file", "note.md", "text/markdown",
        "# Title\n\nmarkdown body".getBytes(StandardCharsets.UTF_8));

    String text = RagUtility.parseMultipartFile(file);

    Assertions.assertTrue(text.contains("Title"));
    Assertions.assertTrue(text.contains("markdown body"));
  }

  @Test
  void parseMultipartFileRejectsUnsupportedExtension() {
    MockMultipartFile file = new MockMultipartFile("file", "note.zip", "application/zip",
        "zip body".getBytes(StandardCharsets.UTF_8));

    Assertions.assertThrows(UnsupportedOperationException.class,
        () -> RagUtility.parseMultipartFile(file));
  }

  @Test
  void parseMultipartFileRejectsMissingExtension() {
    MockMultipartFile file = new MockMultipartFile("file", "note", "text/plain",
        "plain body".getBytes(StandardCharsets.UTF_8));

    Assertions.assertThrows(UnsupportedOperationException.class,
        () -> RagUtility.parseMultipartFile(file));
  }

  @Test
  void parseMultipartFileRejectsEmptyFileName() {
    MockMultipartFile file = new MockMultipartFile("file", "", "text/plain",
        "plain body".getBytes(StandardCharsets.UTF_8));

    Assertions.assertThrows(IllegalArgumentException.class,
        () -> RagUtility.parseMultipartFile(file));
  }
}
