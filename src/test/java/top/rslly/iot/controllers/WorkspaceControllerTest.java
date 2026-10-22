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
package top.rslly.iot.controllers;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import top.rslly.iot.models.WorkspaceFileEntity;
import top.rslly.iot.services.SafetyServiceImpl;
import top.rslly.iot.services.agent.WorkspaceService;

class WorkspaceControllerTest {
  WorkspaceService service;
  SafetyServiceImpl safety;
  MockMvc mvc;

  @BeforeEach
  void setup() {
    service = mock(WorkspaceService.class);
    safety = mock(SafetyServiceImpl.class);
    mvc = MockMvcBuilders.standaloneSetup(new WorkspaceController(service, safety)).build();
  }

  @Test
  void rejectsEveryOperationBeforeTouchingStorageWithoutProductPermission() throws Exception {
    for (String path : new String[] {"", "/files", "/files/known/text", "/files/known/download"}) {
      mvc.perform(get("/api/v2/workspaces" + path).header("Authorization", "token")
          .param("productId", "12").param("chatId", "chatProduct12"))
          .andExpect(status().isForbidden()).andExpect(jsonPath("$.errorCode").value(3001));
    }
    mvc.perform(delete("/api/v2/workspaces/files/known").header("Authorization", "token")
        .param("productId", "12").param("chatId", "chatProduct12"))
        .andExpect(status().isForbidden());
    mvc.perform(multipart("/api/v2/workspaces/files").file("file", new byte[] {1})
        .header("Authorization", "token").param("productId", "12").param("chatId", "chatProduct12"))
        .andExpect(status().isForbidden());
    verifyNoInteractions(service);
  }

  @Test
  void requiresAuthorizationAndConversationParameters() throws Exception {
    mvc.perform(get("/api/v2/workspaces").param("productId", "12"))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/v2/workspaces/files/known/download").header("Authorization", "token")
        .param("productId", "12")).andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }

  @Test
  void streamsDownloadAsAnAttachmentAndPassesCompleteScope() throws Exception {
    when(safety.controlAuthorizeProduct("token", 12)).thenReturn(true);
    WorkspaceFileEntity file = new WorkspaceFileEntity();
    file.setName("report.html");
    file.setSize(3);
    when(service.download(12, "chatProduct12", "known"))
        .thenReturn(
            new WorkspaceService.Download(file, new ByteArrayInputStream(new byte[] {0, 1, 2})));
    mvc.perform(get("/api/v2/workspaces/files/known/download").header("Authorization", "token")
        .param("productId", "12").param("chatId", "chatProduct12"))
        .andExpect(status().isOk()).andExpect(content().bytes(new byte[] {0, 1, 2}))
        .andExpect(header().string("Content-Type", "application/octet-stream"))
        .andExpect(header().string("X-Content-Type-Options", "nosniff"))
        .andExpect(header().string("Cache-Control", "no-store"))
        .andExpect(header().string("Content-Disposition",
            org.hamcrest.Matchers.startsWith("attachment;")));
  }

  @Test
  void refusesKnownFileIdFromAnotherConversation() throws Exception {
    when(safety.controlAuthorizeProduct("token", 12)).thenReturn(true);
    when(service.download(12, "chatProduct12", "known"))
        .thenThrow(new IllegalArgumentException("Workspace file not found in this conversation"));
    mvc.perform(get("/api/v2/workspaces/files/known/download").header("Authorization", "token")
        .param("productId", "12").param("chatId", "chatProduct12"))
        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value(1001));
  }
}
