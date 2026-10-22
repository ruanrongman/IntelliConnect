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

import java.io.*;
import java.nio.channels.Channels;
import java.nio.charset.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import top.rslly.iot.dao.*;
import top.rslly.iot.models.*;
import top.rslly.iot.utility.ai.sandbox.SandboxService;
import top.rslly.iot.utility.ai.workspace.WorkspaceProperty;

@Service
@Slf4j
public class WorkspaceServiceImpl implements WorkspaceService {
  private final WorkspaceRepository workspaces;
  private final WorkspaceFileRepository files;
  private final ProductRepository products;
  private final WxProductBindRepository wxBindings;
  private final AgentMemoryService memory;
  private final SandboxService sandbox;
  private final WorkspaceProperty property;
  private final TransactionTemplate transaction;
  private final Object[] locks = new Object[128];

  public WorkspaceServiceImpl(WorkspaceRepository workspaces, WorkspaceFileRepository files,
      ProductRepository products, AgentMemoryService memory, SandboxService sandbox,
      WorkspaceProperty property, PlatformTransactionManager transactionManager,
      WxProductBindRepository wxBindings) {
    this.workspaces = workspaces;
    this.files = files;
    this.products = products;
    this.wxBindings = wxBindings;
    this.memory = memory;
    this.sandbox = sandbox;
    this.property = property;
    this.transaction = new TransactionTemplate(transactionManager);
    this.transaction.setPropagationBehavior(
        org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    Arrays.setAll(locks, i -> new Object());
  }

  @Override
  public Page<WorkspaceEntity> listWorkspaces(int productId, int pageNum, int pageSize) {
    requireProduct(productId);
    return workspaces.findAllByProductId(productId, page(pageNum, pageSize));
  }

  @Override
  public FilePage listFiles(int productId, String chatId, int pageNum, int pageSize) {
    var pageable = page(pageNum, pageSize);
    var workspace = scope(productId, chatId);
    return workspace == null
        ? new FilePage(Page.empty(pageable), 0, property.getMaxWorkspaceBytes(),
            property.getMaxFileBytes())
        : new FilePage(files.findAllByWorkspaceId(workspace.getId(), pageable),
            files.totalSize(workspace.getId()), property.getMaxWorkspaceBytes(),
            property.getMaxFileBytes());
  }

  @Override
  public List<WorkspaceFileEntity> upload(int productId, String chatId, MultipartFile[] uploads,
      String source) {
    if (uploads == null || uploads.length == 0) {
      return List.of();
    }
    if (uploads.length > 100) {
      throw invalid("Upload at most 100 files at a time");
    }
    List<Incoming> incoming = new ArrayList<>();
    for (MultipartFile upload : uploads) {
      if (upload == null || upload.isEmpty()) {
        throw invalid("Cannot upload an empty file");
      }
      if (upload.getSize() > property.getMaxFileBytes()) {
        throw invalid("File exceeds workspace file size limit");
      }
      String name = filename(upload.getOriginalFilename());
      incoming.add(new Incoming(name, source, () -> {
        try {
          return upload.getInputStream();
        } catch (IOException e) {
          throw invalid("Cannot read uploaded file");
        }
      }));
    }
    return save(productId, chatId, incoming);
  }

  @Override
  public WorkspaceFileEntity write(int productId, String chatId, String name, String content) {
    if (content == null || content.length() > property.getMaxFileBytes()) {
      throw invalid("Missing or oversized text content");
    }
    byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
    return save(productId, chatId,
        List.of(new Incoming(filename(name), "agent", () -> new ByteArrayInputStream(bytes))))
            .getFirst();
  }

  private List<WorkspaceFileEntity> save(int productId, String chatId, List<Incoming> incoming) {
    synchronized (lock(productId, chatId)) {
      scope(productId, chatId);
      List<Path> created = new ArrayList<>();
      try {
        return transaction.execute(status -> {
          WorkspaceEntity workspace = workspaces.findByProductIdAndChatId(productId, chatId)
              .orElseGet(() -> {
                WorkspaceEntity entry = new WorkspaceEntity();
                entry.setProductId(productId);
                entry.setChatId(chatId);
                entry.setCreatedAt(System.currentTimeMillis());
                return workspaces.saveAndFlush(entry);
              });
          Path directory = directory(productId, chatId, true);
          long used = files.totalSize(workspace.getId());
          List<WorkspaceFileEntity> result = new ArrayList<>();
          for (Incoming input : incoming) {
            WorkspaceFileEntity entry = new WorkspaceFileEntity();
            entry.setId(UUID.randomUUID().toString());
            entry.setWorkspaceId(workspace.getId());
            entry.setName(input.name());
            entry.setSource(input.source());
            entry.setCreatedAt(System.currentTimeMillis());
            Path temporary = directory.resolve(".upload-" + entry.getId());
            Path destination = directory.resolve(entry.getId());
            created.add(temporary);
            created.add(destination);
            try (InputStream stream = input.stream().get();
                OutputStream out = Files.newOutputStream(temporary, StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)) {
              long size = copyLimited(stream, out,
                  Math.min(property.getMaxFileBytes(), property.getMaxWorkspaceBytes() - used));
              entry.setSize(size);
              used += size;
            } catch (IOException e) {
              throw invalid("Unable to save workspace file");
            }
            try {
              Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
              throw invalid("Unable to finalize workspace file");
            }
            result.add(files.saveAndFlush(entry));
          }
          return result;
        });
      } catch (RuntimeException e) {
        created.forEach(this::cleanup);
        throw e;
      }
    }
  }

  @Override
  public TextPage read(int productId, String chatId, String fileId, long offset, int limit) {
    if (offset < 0 || offset > property.getMaxFileBytes() || limit < 1) {
      throw invalid("Invalid text offset or limit");
    }
    int count = Math.min(limit, property.getMaxReadChars());
    Download download = download(productId, chatId, fileId);
    try (Reader reader = new InputStreamReader(download.stream(),
        StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT))) {
      long remaining = offset;
      char[] buffer = new char[Math.min(count, 8192)];
      while (remaining > 0) {
        int n = reader.read(buffer, 0, (int) Math.min(buffer.length, remaining));
        if (n < 0) {
          return new TextPage("", offset, offset, false);
        }
        remaining -= n;
      }
      StringBuilder text = new StringBuilder();
      while (text.length() < count + 1) {
        int n = reader.read(buffer, 0, Math.min(buffer.length, count + 1 - text.length()));
        if (n < 0) {
          break;
        }
        text.append(buffer, 0, n);
      }
      if (text.chars().anyMatch(c -> c == 0 || (c < 32 && c != '\n' && c != '\r' && c != '\t'))) {
        throw invalid("Binary file: download it or import it into the sandbox");
      }
      boolean truncated = text.length() > count;
      return new TextPage(text.substring(0, Math.min(count, text.length())), offset,
          offset + Math.min(count, text.length()), truncated);
    } catch (IOException e) {
      throw invalid(
          "Text preview requires a UTF-8 text file; use download or sandbox for binary files");
    }
  }

  @Override
  public Download download(int productId, String chatId, String fileId) {
    synchronized (lock(productId, chatId)) {
      var file = requireFile(productId, chatId, fileId);
      Path path = directory(productId, chatId, false).resolve(file.getId());
      try {
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
          throw invalid("Workspace file is missing");
        }
        return new Download(file, Channels.newInputStream(Files.newByteChannel(path,
            StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)));
      } catch (IOException e) {
        throw invalid("Unable to open workspace file");
      }
    }
  }

  @Override
  public void delete(int productId, String chatId, String fileId) {
    synchronized (lock(productId, chatId)) {
      var file = requireFile(productId, chatId, fileId);
      Path path = directory(productId, chatId, false).resolve(file.getId());
      Path tombstone = path.resolveSibling(".delete-" + UUID.randomUUID());
      boolean moved = false;
      try {
        if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
          if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            throw invalid("Invalid workspace file");
          }
          Files.move(path, tombstone, StandardCopyOption.ATOMIC_MOVE);
          moved = true;
        }
        transaction.executeWithoutResult(status -> {
          files.delete(file);
          files.flush();
        });
      } catch (Exception e) {
        if (moved) {
          try {
            Files.move(tombstone, path, StandardCopyOption.ATOMIC_MOVE);
          } catch (IOException restoreError) {
            log.error("Unable to restore workspace file {}", fileId, restoreError);
          }
        }
        throw invalid("Unable to delete workspace file");
      }
      cleanup(tombstone);
    }
  }

  @Override
  public String importToSandbox(int productId, String chatId, String fileId) {
    requireSandbox();
    Download download = download(productId, chatId, fileId);
    String destination =
        "/workspace/imports/" + download.file().getId() + "/" + download.file().getName();
    try (InputStream stream = download.stream()) {
      if (!sandbox.importWorkspaceFile(chatId, destination, stream, property.getMaxFileBytes())) {
        throw invalid("Sandbox file import failed");
      }
      return destination;
    } catch (IOException e) {
      throw invalid("Sandbox file import failed");
    }
  }

  @Override
  public WorkspaceFileEntity exportFromSandbox(int productId, String chatId, String path) {
    requireSandbox();
    scope(productId, chatId);
    // The SDK stream is consumed and closed inside the sandbox lease, before local publication.
    byte[] bytes = sandbox.exportWorkspaceFile(chatId, path, property.getMaxFileBytes());
    if (bytes == null) {
      throw invalid(
          "Sandbox file export failed: file/instance missing, unsafe path, or size limit exceeded");
    }
    String name = filename(path.substring(path.lastIndexOf('/') + 1));
    return save(productId, chatId,
        List.of(new Incoming(name, "sandbox", () -> new ByteArrayInputStream(bytes)))).getFirst();
  }

  private WorkspaceFileEntity requireFile(int productId, String chatId, String fileId) {
    if (fileId == null || !fileId.matches("[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}")) {
      throw invalid("Invalid file ID");
    }
    var workspace = scope(productId, chatId);
    if (workspace == null) {
      throw invalid("Workspace file not found");
    }
    return files.findByIdAndWorkspaceId(fileId, workspace.getId())
        .orElseThrow(() -> invalid("Workspace file not found in this conversation"));
  }

  private WorkspaceEntity scope(int productId, String chatId) {
    requireProduct(productId);
    if (chatId == null || chatId.isBlank() || chatId.length() > 255
        || !chatId.equals(chatId.trim()) || chatId.chars().anyMatch(Character::isISOControl)) {
      throw invalid("Invalid stable conversation ID");
    }
    var existing = workspaces.findByProductIdAndChatId(productId, chatId);
    if (existing.isPresent()) {
      return existing.get();
    }
    if (!memory.isChatIdValidForProduct(productId, chatId)
        && !wxBindings.existsConversationByProductId(productId, chatId)) {
      throw invalid("Conversation does not belong to this product");
    }
    return null;
  }

  private void requireProduct(int productId) {
    if (products.findAllById(productId).isEmpty()) {
      throw invalid("Product not found");
    }
  }

  private void requireSandbox() {
    if (!sandbox.isAvailable()) {
      throw invalid("Sandbox service is unavailable");
    }
  }

  private Path directory(int productId, String chatId, boolean create) {
    try {
      String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
          .digest(chatId.getBytes(StandardCharsets.UTF_8)));
      Path root = Path.of(property.getRoot()).toAbsolutePath().normalize();
      Path directory = root.resolve(Integer.toString(productId)).resolve(hash);
      Path current = directory.getRoot();
      for (Path part : directory) {
        current = current.resolve(part);
        if (Files.isSymbolicLink(current) || (Files.exists(current, LinkOption.NOFOLLOW_LINKS)
            && !Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS))) {
          throw invalid("Unsafe workspace directory");
        }
        if (create && !Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
          try {
            Files.createDirectory(current);
          } catch (FileAlreadyExistsException ignored) {
            if (Files.isSymbolicLink(current)
                || !Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS)) {
              throw invalid("Unsafe workspace directory");
            }
          }
        }
      }
      return directory;
    } catch (IOException | java.security.NoSuchAlgorithmException e) {
      throw invalid("Workspace storage is unavailable");
    }
  }

  private String filename(String name) {
    if (name == null || name.isBlank() || name.length() > 255 || name.equals(".")
        || name.equals("..")
        || name.contains("/") || name.contains("\\") || name.contains(":")
        || name.chars().anyMatch(Character::isISOControl)) {
      throw invalid("Invalid file name");
    }
    return name;
  }

  private Pageable page(int number, int size) {
    if (number < 1 || size < 1 || size > 100) {
      throw invalid("Page number must be positive and page size must be 1..100");
    }
    return PageRequest.of(number - 1, size, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
  }

  private Object lock(int productId, String chatId) {
    return locks[Math.floorMod(Objects.hash(productId, chatId), locks.length)];
  }

  private long copyLimited(InputStream input, OutputStream output, long maximum)
      throws IOException {
    long total = 0;
    byte[] buffer = new byte[8192];
    int count;
    while ((count = input.read(buffer)) != -1) {
      if (count > maximum - total) {
        throw invalid("Workspace capacity or file size limit exceeded");
      }
      output.write(buffer, 0, count);
      total += count;
    }
    return total;
  }

  private void cleanup(Path path) {
    try {
      Files.deleteIfExists(path);
    } catch (IOException e) {
      log.warn("Unable to remove temporary workspace file {}", path.getFileName());
    }
  }

  private IllegalArgumentException invalid(String message) {
    return new IllegalArgumentException(message);
  }

  private record Incoming(String name, String source, Supplier<InputStream> stream) {}
}
