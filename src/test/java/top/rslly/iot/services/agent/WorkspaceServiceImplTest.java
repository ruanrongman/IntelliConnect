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
package top.rslly.iot.services.agent;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.data.domain.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import top.rslly.iot.dao.*;
import top.rslly.iot.models.*;
import top.rslly.iot.utility.ai.sandbox.SandboxService;
import top.rslly.iot.utility.ai.workspace.WorkspaceProperty;

class WorkspaceServiceImplTest {
  @TempDir
  Path temporary;
  WorkspaceRepository workspaces;
  WorkspaceFileRepository files;
  ProductRepository products;
  WxProductBindRepository wxBindings;
  AgentMemoryService memory;
  SandboxService sandbox;
  WorkspaceProperty property;
  PlatformTransactionManager manager;
  WorkspaceServiceImpl service;
  Map<String, WorkspaceEntity> workspaceRows;
  Map<String, WorkspaceFileEntity> fileRows;

  @BeforeEach
  void setup() {
    workspaces = mock(WorkspaceRepository.class);
    files = mock(WorkspaceFileRepository.class);
    products = mock(ProductRepository.class);
    wxBindings = mock(WxProductBindRepository.class);
    memory = mock(AgentMemoryService.class);
    sandbox = mock(SandboxService.class);
    manager = mock(PlatformTransactionManager.class);
    property = new WorkspaceProperty();
    property.setRoot(temporary.resolve("storage").toString());
    service =
        new WorkspaceServiceImpl(workspaces, files, products, memory, sandbox, property, manager,
            wxBindings);
    workspaceRows = new ConcurrentHashMap<>();
    fileRows = new ConcurrentHashMap<>();
    AtomicLong ids = new AtomicLong();
    when(manager.getTransaction(any())).thenAnswer(call -> new SimpleTransactionStatus());
    when(products.findAllById(anyInt())).thenReturn(List.of(new ProductEntity()));
    when(memory.isChatIdValidForProduct(anyInt(), anyString())).thenAnswer(
        call -> ((String) call.getArgument(1)).equals("chatProduct" + call.getArgument(0))
            || (call.getArgument(0).equals(12)
                && call.getArgument(1).equals("chatProduct1274:5d:22:c7:a5:44")));
    when(workspaces.findByProductIdAndChatId(anyInt(), anyString())).thenAnswer(call -> Optional
        .ofNullable(workspaceRows.get(call.getArgument(0) + ":" + call.getArgument(1))));
    when(workspaces.saveAndFlush(any())).thenAnswer(call -> {
      WorkspaceEntity row = call.getArgument(0);
      row.setId(ids.incrementAndGet());
      workspaceRows.put(row.getProductId() + ":" + row.getChatId(), row);
      return row;
    });
    when(files.saveAndFlush(any())).thenAnswer(call -> {
      WorkspaceFileEntity row = call.getArgument(0);
      fileRows.put(row.getId(), row);
      return row;
    });
    when(files.totalSize(anyLong())).thenAnswer(call -> fileRows.values().stream()
        .filter(row -> row.getWorkspaceId().equals(call.getArgument(0)))
        .mapToLong(WorkspaceFileEntity::getSize).sum());
    when(files.findByIdAndWorkspaceId(anyString(), anyLong()))
        .thenAnswer(call -> Optional.ofNullable(fileRows.get(call.getArgument(0)))
            .filter(row -> row.getWorkspaceId().equals(call.getArgument(1))));
    when(files.findAllByWorkspaceId(anyLong(), any()))
        .thenAnswer(call -> new PageImpl<>(fileRows.values().stream()
            .filter(row -> row.getWorkspaceId().equals(call.getArgument(0))).toList()));
    doAnswer(call -> {
      WorkspaceFileEntity row = call.getArgument(0);
      fileRows.remove(row.getId());
      return null;
    }).when(files).delete(any(WorkspaceFileEntity.class));
  }

  @Test
  void preservesBinaryBytesAndDuplicateChineseNamesWithoutOverwriting() throws Exception {
    byte[] content = {0, 1, 2, (byte) 255, 10};
    MockMultipartFile upload =
        new MockMultipartFile("file", "报告.xlsx", "application/octet-stream", content);
    var saved =
        service.upload(12, "chatProduct12", new MockMultipartFile[] {upload, upload}, "upload");
    assertNotEquals(saved.get(0).getId(), saved.get(1).getId());
    assertEquals("报告.xlsx", saved.get(0).getName());
    try (var stream = service.download(12, "chatProduct12", saved.get(0).getId()).stream()) {
      assertArrayEquals(content, stream.readAllBytes());
    }
    assertEquals(10, service.listFiles(12, "chatProduct12", 1, 20).usedBytes());
    assertThrows(IllegalArgumentException.class,
        () -> service.read(12, "chatProduct12", saved.get(0).getId(), 0, 10));
    assertEquals(2, diskFiles().size());
  }

  @Test
  void isolatesProductsAndConversationsEvenWhenFileIdIsKnown() {
    var saved = service.write(12, "chatProduct12", "a.txt", "secret");
    service.write(13, "chatProduct13", "b.txt", "other");
    assertThrows(IllegalArgumentException.class,
        () -> service.download(13, "chatProduct13", saved.getId()));
    assertThrows(IllegalArgumentException.class,
        () -> service.download(13, "chatProduct12", saved.getId()));
    assertThrows(IllegalArgumentException.class,
        () -> service.listFiles(1, "chatProduct12", 1, 20));
    assertThrows(IllegalArgumentException.class,
        () -> service.listFiles(12, "chatProduct12:stream:request-1", 1, 20));
    assertThrows(IllegalArgumentException.class,
        () -> service.download(12, "chatProduct12", "../anything"));
  }

  @Test
  void retainsWorkspaceAfterConversationNoLongerExistsAndHashesDeviceIds() throws Exception {
    String chat = "chatProduct1274:5d:22:c7:a5:44";
    var saved = service.write(12, chat, "设备.md", "hello");
    when(memory.isChatIdValidForProduct(12, chat)).thenReturn(false);
    assertEquals("hello", service.read(12, chat, saved.getId(), 0, 100).content());
    Path path = diskFiles().getFirst();
    assertEquals(64, path.getParent().getFileName().toString().length());
    assertFalse(path.toString().contains(chat));
    verify(memory).isChatIdValidForProduct(12, chat);
  }

  @Test
  void boundsTextPreviewAndSupportsContinuation() {
    property.setMaxReadChars(4);
    var saved = service.write(12, "chatProduct12", "hello.html", "<b>你好</b>");
    var first = service.read(12, "chatProduct12", saved.getId(), 0, 1000);
    assertEquals("<b>你", first.content());
    assertTrue(first.truncated());
    assertEquals(4, first.nextOffset());
    assertEquals("好</b",
        service.read(12, "chatProduct12", saved.getId(), first.nextOffset(), 100).content());
    assertFalse(service.read(12, "chatProduct12", saved.getId(), 9, 10).truncated());
    assertThrows(IllegalArgumentException.class,
        () -> service.read(12, "chatProduct12", saved.getId(), -1, 10));
  }

  @Test
  void cleansWholeBatchOnOversizeOrDatabaseFailure() throws Exception {
    property.setMaxWorkspaceBytes(5);
    var upload = new MockMultipartFile("file", "a.bin", "application/octet-stream", new byte[3]);
    assertThrows(IllegalArgumentException.class,
        () -> service.upload(12, "chatProduct12", new MockMultipartFile[] {upload, upload},
            "chat"));
    assertTrue(diskFiles().isEmpty());
    verify(manager).rollback(any());
    fileRows.clear();
    doThrow(new org.springframework.dao.DataAccessResourceFailureException("database unavailable"))
        .when(files).saveAndFlush(any());
    assertThrows(org.springframework.dao.DataAccessResourceFailureException.class,
        () -> service.write(12, "chatProduct12", "a.txt", "ok"));
    assertTrue(diskFiles().isEmpty());
  }

  @Test
  void cleansPublishedFilesWhenDatabaseCommitFails() throws Exception {
    doThrow(new IllegalStateException("commit failed")).when(manager).commit(any());
    assertThrows(IllegalStateException.class,
        () -> service.write(12, "chatProduct12", "a.txt", "ok"));
    assertTrue(diskFiles().isEmpty());
  }

  @Test
  void restoresDiskFileWhenDeleteTransactionFails() throws Exception {
    var saved = service.write(12, "chatProduct12", "a.txt", "saved");
    doThrow(new IllegalStateException("delete failed")).when(files).flush();
    assertThrows(IllegalArgumentException.class,
        () -> service.delete(12, "chatProduct12", saved.getId()));
    assertEquals("saved", Files.readString(diskFiles().getFirst()));
    assertEquals(saved.getId(), diskFiles().getFirst().getFileName().toString());
  }

  @Test
  void serializesConcurrentUploadsForQuotaAndWorkspaceCreation() throws Exception {
    property.setMaxWorkspaceBytes(48);
    ExecutorService executor = Executors.newFixedThreadPool(8);
    CountDownLatch start = new CountDownLatch(1);
    try {
      List<Future<Boolean>> futures = new ArrayList<>();
      for (int i = 0; i < 8; i++) {
        futures.add(executor.submit(() -> {
          start.await();
          try {
            service.write(12, "chatProduct12", "a.txt", "0123456789abcdef");
            return true;
          } catch (IllegalArgumentException e) {
            return false;
          }
        }));
      }
      start.countDown();
      int successes = 0;
      for (Future<Boolean> future : futures)
        if (future.get(10, TimeUnit.SECONDS))
          successes++;
      assertEquals(3, successes);
      assertEquals(48, service.listFiles(12, "chatProduct12", 1, 20).usedBytes());
      assertEquals(3, diskFiles().size());
      verify(workspaces).saveAndFlush(any());
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void rejectsPathNamesAndSymlinkedStorage() throws Exception {
    for (String name : List.of("../secret", "C:\\secret", "/tmp/a", "..", "bad\nname")) {
      assertThrows(IllegalArgumentException.class,
          () -> service.write(12, "chatProduct12", name, "bad"));
    }
    Path link = temporary.resolve("linked");
    try {
      Files.createSymbolicLink(link, temporary);
    } catch (IOException | UnsupportedOperationException e) {
      Assumptions.assumeTrue(false, "Symbolic links unavailable on this Windows account");
    }
    property.setRoot(link.toString());
    assertThrows(IllegalArgumentException.class,
        () -> service.write(12, "chatProduct12", "a.txt", "bad"));
  }

  @Test
  void acceptsOnlyExactlyBoundWechatConversationForProduct() {
    String chat = "wx-applicationopenid-user";
    when(wxBindings.existsConversationByProductId(12, chat)).thenReturn(true);
    var saved = service.write(12, chat, "a.txt", "wechat");
    assertEquals("wechat", service.read(12, chat, saved.getId(), 0, 100).content());
    assertThrows(IllegalArgumentException.class, () -> service.write(13, chat, "a.txt", "other"));
  }

  @Test
  void importsAndExportsBinaryWithoutPassingBytesThroughModel() throws Exception {
    when(sandbox.isAvailable()).thenReturn(true);
    when(sandbox.importWorkspaceFile(eq("chatProduct12"), anyString(), any(), anyLong()))
        .thenAnswer(call -> {
          assertArrayEquals(new byte[] {0, 1, 2},
              ((InputStream) call.getArgument(2)).readAllBytes());
          return true;
        });
    when(sandbox.exportWorkspaceFile("chatProduct12", "results/report.xlsx",
        property.getMaxFileBytes()))
            .thenReturn(new byte[] {0, 1, 2});
    var saved = service.exportFromSandbox(12, "chatProduct12", "results/report.xlsx");
    assertEquals("sandbox", saved.getSource());
    assertEquals("/workspace/imports/" + saved.getId() + "/report.xlsx",
        service.importToSandbox(12, "chatProduct12", saved.getId()));
    when(sandbox.isAvailable()).thenReturn(false);
    assertThrows(IllegalArgumentException.class,
        () -> service.importToSandbox(12, "chatProduct12", saved.getId()));
    assertEquals(1, service.listFiles(12, "chatProduct12", 1, 20).files().getTotalElements());
  }

  private List<Path> diskFiles() throws IOException {
    if (!Files.exists(Path.of(property.getRoot())))
      return List.of();
    try (var stream = Files.walk(Path.of(property.getRoot()))) {
      return stream.filter(Files::isRegularFile).toList();
    }
  }
}
