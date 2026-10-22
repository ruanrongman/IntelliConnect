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
package top.rslly.iot.utility.ai.sandbox;

import com.alibaba.opensandbox.sandbox.Sandbox;
import com.alibaba.opensandbox.sandbox.config.ConnectionConfig;
import com.alibaba.opensandbox.sandbox.domain.exceptions.SandboxApiException;
import com.alibaba.opensandbox.sandbox.domain.exceptions.SandboxNotFoundException;
import com.alibaba.opensandbox.sandbox.domain.models.execd.executions.Execution;
import com.alibaba.opensandbox.sandbox.domain.models.execd.executions.ExecutionError;
import com.alibaba.opensandbox.sandbox.domain.models.execd.executions.RunCommandRequest;
import com.alibaba.opensandbox.sandbox.domain.pool.AcquirePolicy;
import com.alibaba.opensandbox.sandbox.domain.pool.PoolCreationSpec;
import com.alibaba.opensandbox.sandbox.infrastructure.pool.InMemoryPoolStateStore;
import com.alibaba.opensandbox.sandbox.pool.SandboxPool;
import jakarta.annotation.PreDestroy;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import top.rslly.iot.utility.ai.GlobalMessageContext;

/**
 * OpenSandbox 远程沙箱服务, 供 bash 执行 / 文件编辑类 AI 工具复用.
 *
 * <p>
 * 设计要点:
 *
 * <ul>
 * <li><b>按 chatId 隔离</b>: 每个会话(chatId)独占一个 Sandbox 实例, 会话之间互不可见对方的文件系统与执行状态; 没有会话上下文的调用落入
 * {@value #DEFAULT_CHAT_ID} 默认实例。
 * <li>懒创建 + 常驻复用: 实例跨调用保留, 空闲超过 {@code ai.sandbox.idle-timeout} 由定时任务回收, 下次调用再重建。
 * <li><b>执行租约</b>: 执行类操作在分段锁内获取和释放租约, 持租约期间空闲清扫与 LRU 驱逐都不得终止该实例。 长命令执行中不再出现被 {@code sweepIdle()}
 * 或容量驱逐杀掉实例的竞态。
 * <li><b>容量名额</b>: 创建名额由全局信号量控制(名额数即 {@code ai.sandbox.max-instances}, 启动后固定), 检查-创建原子化, 不存在跨会话的
 * check-then-act 竞态。 kill <b>确认成功</b>才释放名额; kill 失败转入延后重试队列期间名额继续占用, 远端容器总数因此不会突破上限; 名额不足时先驱逐空闲的
 * LRU 实例, 全忙或 kill 通道异常时拒绝创建。
 * <li><b>分段锁</b>: 128 把 {@link ReentrantLock} 按 chatId 哈希取模, 互斥同一会话的创建/摘除/清扫; 任何路径至多持有一把分段锁, kill /
 * close 等网络调用一律在锁外执行, 无锁序死锁。
 * <li><b>元数据生命周期</b>: 会话的全部状态(实例/租约计数/时间戳/技能清单)收敛在 {@code sessions} 单一 map 中,
 * 无实例且长期未用的死状态由定时清扫在锁内校验后安全移除, 不随 chatId 数量无限增长。
 * <li>回收时 kill 失败的沙箱转入延后重试队列, 由定时任务重试, 避免远端容器泄漏; 超过 30 分钟仍失败则放弃、释放名额并输出沙箱 ID 供人工清理。
 * <li>仅 SDK 明确确认沙箱不存在时使该代实例失效, 租约排空后回收并重建重试一次; 普通操作错误保留原实例与文件。
 * <li>同会话只有一个创建者, 技能准备全程使用同一实例租约; 维护任务使用独立调度器, 关闭后拒绝新租约并在在途操作完成后回收。
 * <li>公开 API 均不向外抛异常, 以 {@link SandboxResult} / null / false 表达结果, 方便上层 AI 工具以纯字符串形式返回给大模型。
 * </ul>
 *
 * <p>
 * 运维注意:
 *
 * <ul>
 * <li>Java SDK (com.alibaba.opensandbox:sandbox) 没有提供 CodeInterpreter 模块, 官方 code-interpreter SDK
 * 只有 Python / JavaScript / C# 版本。 如需在沙箱里执行 Python 代码, 通过 Commands API 写临时脚本再执行。
 * <li>运行前先在沙盒宿主机上预拉取镜像, 避免首次创建沙箱时现场 pull 导致 SDK 读超时:
 * {@code docker pull sandbox-registry.cn-zhangjiakou.cr.aliyuncs.com/opensandbox/code-interpreter:v1.1.0}
 * <li>OpenSandbox Server 未启用 api_key 时, {@code ai.sandbox.api-key} 可任意填写。
 * </ul>
 */
@Service
@Slf4j
public class SandboxService {

  /** 沙箱内默认工作目录, 各 AI 工具的文件操作约定在此目录下进行 */
  public static final String WORKSPACE_DIR = "/workspace";

  /** 缺省实例标识: 调用方没有 chatId 上下文时使用 */
  public static final String DEFAULT_CHAT_ID = "default";

  /** 压缩包上传到沙箱内的临时存放目录 */
  private static final String ARCHIVE_TMP_DIR = "/tmp";

  /** 支持的压缩包后缀 */
  private static final String[] ARCHIVE_SUFFIXES = {".zip", ".tar.gz", ".tgz", ".tar"};

  /** 分段锁数量 */
  private static final int LOCK_COUNT = 128;

  /** kill 重试的最大存活时长(纳秒), 超时放弃并释放名额 */
  private static final long PENDING_KILL_MAX_AGE_NANOS = TimeUnit.MINUTES.toNanos(30);

  private final SandboxProperty property;

  private final AtomicBoolean closing = new AtomicBoolean();

  /** chatId -> 会话状态(实例/租约计数/时间戳/技能清单), 唯一的会话元数据入口 */
  private final ConcurrentHashMap<String, ChatState> sessions = new ConcurrentHashMap<>();

  /** 分段锁, 按 chatId 哈希互斥同一会话的创建/摘除/清扫; 任何路径至多持有一把 */
  private final ReentrantLock[] locks;

  /** 创建名额信号量: kill 确认成功才释放, 约束远端容器总数不超过 max-instances */
  private final Semaphore capacity;

  /**
   * kill 失败待重试的沙箱: 键为 "chatId:sandboxId". 本地账目已清但远端容器未终止时, 延后重试 kill, 避免远端容器泄漏; 超过 纳秒(30
   * 分钟)仍未成功则放弃、释放名额并记录沙箱 ID 供人工清理.
   */
  private final ConcurrentHashMap<String, Sandbox> pendingKills = new ConcurrentHashMap<>();

  /** kill 失败时间戳(纳秒), 与 {@link #pendingKills} 的键一一对应 */
  private final ConcurrentHashMap<String, Long> pendingKillSinceNanos = new ConcurrentHashMap<>();

  /**
   * SandboxPool 预热池(可空): 后台维护 {@code pool-max-idle} 个预热好的空闲实例, 新会话取用免去容器冷创建的秒级等待; 取用失败或未启用时回退
   * {@link #createSandbox} 现场创建. 池内空闲实例是常驻缓冲, 不占用 {@link #capacity} 名额.
   */
  private final SandboxPool pool;

  @Autowired
  public SandboxService(SandboxProperty property) {
    this.property = property;
    this.locks = new ReentrantLock[LOCK_COUNT];
    for (int i = 0; i < LOCK_COUNT; i++) {
      this.locks[i] = new ReentrantLock();
    }
    this.capacity = new Semaphore(property.getMaxInstances());
    this.pool = buildPool();
  }

  /**
   * 沙箱服务是否启用且尚未关闭.
   */
  public boolean isAvailable() {
    return property.isEnabled() && !closing.get();
  }

  /** 沙箱归属稳定对话, 不能使用每次流式请求都会变化的 chatId. */
  public static String resolveChatId(Map<String, Object> globalMessage) {
    String chatId = GlobalMessageContext.memoryChatId(globalMessage);
    return chatId == null || chatId.isBlank() ? DEFAULT_CHAT_ID : chatId.trim();
  }

  /**
   * 指定技能文件是否已经解压进该会话的沙箱.
   *
   * @param chatId 会话标识, null/空 使用默认实例
   * @param filePath 技能文件在服务端的存储路径(唯一)
   */
  public boolean isSkillExtracted(String chatId, String filePath) {
    String key = normalizeChatId(chatId);
    ReentrantLock lock = lockFor(key);
    lock.lock();
    try {
      ChatState state = sessions.get(key);
      return state != null && state.sandbox != null && !state.invalid
          && !isIdleExpired(state) && state.extractedSkills.contains(filePath);
    } finally {
      lock.unlock();
    }
  }

  /**
   * 在指定会话的沙箱中执行一条 shell 命令.
   *
   * <p>
   * 仅确认沙箱不存在时重建重试一次; 普通错误保留实例, 返回 exitCode 为 -1 的结果。
   *
   * @param chatId 会话标识, null/空 使用默认实例
   * @param command 完整 shell 命令字符串
   * @return 执行结果, 永不为 null
   */
  public SandboxResult runCommand(String chatId, String command) {
    for (int attempt = 0; attempt < 2; attempt++) {
      Leased current = lease(chatId);
      if (current == null) {
        return commandFailure(
            "Sandbox is unavailable (disabled, closing, at capacity, or failed to create)");
      }
      try {
        renewBeforeUse(current.sandbox());
        SandboxResult result = executeCommand(current.sandbox(), command);
        if (attempt > 0) {
          return new SandboxResult(result.exitCode(), result.stdout(),
              "[notice] the previous sandbox no longer existed; files from earlier calls were lost\n"
                  + result.stderr(),
              result.error(), result.durationMillis());
        }
        return result;
      } catch (Exception e) {
        if (!isSandboxNotFound(e)) {
          log.warn("沙箱命令执行失败, 保留实例, chatId: {}, 原因: {}", chatId, e.getMessage());
          return commandFailure("Sandbox command execution failed: " + e.getMessage());
        }
        invalidate(current);
      } finally {
        release(current);
      }
    }
    return commandFailure("Sandbox no longer exists after retry");
  }

  private SandboxResult commandFailure(String message) {
    return new SandboxResult(-1, "", "", message, 0);
  }

  /**
   * 向指定会话的沙箱写入文本文件(路径不存在时自动创建, 覆盖已有内容).
   *
   * @return 是否成功
   */
  public boolean writeFile(String chatId, String path, String content) {
    return executeWithRetry(chatId, "写文件", leased -> {
      leased.sandbox().files().writeFile(path, content);
      return Boolean.TRUE;
    }, Boolean.FALSE);
  }

  /**
   * 向指定会话的沙箱写入二进制文件(路径不存在时自动创建, 覆盖已有内容), 用于压缩包等非文本内容上传.
   *
   * @return 是否成功
   */
  public boolean writeFile(String chatId, String path, byte[] data) {
    return executeWithRetry(chatId, "写二进制文件", leased -> {
      leased.sandbox().files().writeFile(path, data);
      return Boolean.TRUE;
    }, Boolean.FALSE);
  }

  /**
   * 读取指定会话沙箱内的文本文件内容.
   *
   * @return 文件内容, 失败返回 null
   */
  public String readFile(String chatId, String path) {
    return executeWithRetry(chatId, "读文件", leased -> leased.sandbox().files().readFile(path), null);
  }

  /** Copy files with no-follow directory descriptors; never follow a sandbox symlink. */
  private static final String WORKSPACE_TRANSFER_SCRIPT =
      """
          import os, sys, stat
          action, relative, temporary, maximum = sys.argv[1:]
          maximum = int(maximum)
          def copy_limited(src, dst):
              total = 0
              while True:
                  chunk = src.read(65536)
                  if not chunk: break
                  total += len(chunk)
                  if total > maximum: raise ValueError('File size limit exceeded')
                  dst.write(chunk)
          parts = relative.split('/')
          descriptor = os.open('/workspace', os.O_RDONLY | os.O_DIRECTORY | os.O_NOFOLLOW)
          try:
              for part in parts[:-1]:
                  if action == 'import':
                      try: os.mkdir(part, dir_fd=descriptor)
                      except FileExistsError: pass
                  child = os.open(part, os.O_RDONLY | os.O_DIRECTORY | os.O_NOFOLLOW, dir_fd=descriptor)
                  os.close(descriptor)
                  descriptor = child
              if action == 'export':
                  source = os.open(parts[-1], os.O_RDONLY | os.O_NOFOLLOW | os.O_NONBLOCK, dir_fd=descriptor)
                  info = os.fstat(source)
                  if not stat.S_ISREG(info.st_mode) or info.st_size > maximum:
                      os.close(source)
                      raise ValueError('Not a regular file or size limit exceeded')
                  with os.fdopen(source, 'rb') as src, open(temporary, 'xb') as dst:
                      copy_limited(src, dst)
              else:
                  target = os.open(parts[-1], os.O_WRONLY | os.O_CREAT | os.O_EXCL | os.O_NOFOLLOW, 0o600, dir_fd=descriptor)
                  try:
                      with open(temporary, 'rb') as src, os.fdopen(target, 'wb') as dst:
                          copy_limited(src, dst)
                  except BaseException:
                      os.unlink(parts[-1], dir_fd=descriptor)
                      raise
          finally:
              os.close(descriptor)
          """;

  public boolean importWorkspaceFile(String chatId, String path, InputStream input, long maximum) {
    String relative = workspaceRelativePath(path);
    if (relative == null || chatId == null || chatId.isBlank() || maximum < 1) {
      return false;
    }
    Leased current = lease(chatId);
    if (current == null) {
      return false;
    }
    String temporary = "/tmp/workspace-import-" + UUID.randomUUID();
    try {
      renewBeforeUse(current.sandbox());
      // Bounded even when this API is called outside WorkspaceService.
      byte[] bytes = boundedBytes(input, maximum);
      current.sandbox().files().writeFile(temporary, bytes);
      return executeCommand(current.sandbox(),
          transferCommand("import", relative, temporary, maximum)).ok();
    } catch (Exception e) {
      if (isSandboxNotFound(e)) {
        invalidate(current);
      }
      log.warn("Workspace sandbox import failed, chatId: {}", chatId);
      return false;
    } finally {
      removeTransferTemporary(current, temporary);
      release(current);
    }
  }

  /** Export must never create/retry with an empty replacement instance. */
  public byte[] exportWorkspaceFile(String chatId, String path, long maximum) {
    String relative = workspaceRelativePath(path);
    if (relative == null || chatId == null || chatId.isBlank() || maximum < 1) {
      return null;
    }
    Leased current = leaseExisting(chatId);
    if (current == null) {
      return null;
    }
    String temporary = "/tmp/workspace-export-" + UUID.randomUUID();
    try {
      renewBeforeUse(current.sandbox());
      if (!executeCommand(current.sandbox(),
          transferCommand("export", relative, temporary, maximum)).ok()) {
        return null;
      }
      try (InputStream input = current.sandbox().files().readStream(temporary)) {
        return boundedBytes(input, maximum);
      }
    } catch (Exception e) {
      if (isSandboxNotFound(e)) {
        invalidate(current);
      }
      log.warn("Workspace sandbox export failed, chatId: {}", chatId);
      return null;
    } finally {
      removeTransferTemporary(current, temporary);
      release(current);
    }
  }

  private String workspaceRelativePath(String path) {
    if (path == null || path.isBlank() || path.contains("\\")
        || path.chars().anyMatch(Character::isISOControl)) {
      return null;
    }
    String relative = path.startsWith(WORKSPACE_DIR + "/")
        ? path.substring(WORKSPACE_DIR.length() + 1)
        : path;
    if (relative.startsWith("/") || relative.length() > 2048) {
      return null;
    }
    for (String part : relative.split("/", -1)) {
      if (part.isEmpty() || part.equals(".") || part.equals("..")) {
        return null;
      }
    }
    return relative;
  }

  private byte[] boundedBytes(InputStream input, long maximum) throws java.io.IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    byte[] buffer = new byte[8192];
    int count;
    while ((count = input.read(buffer)) != -1) {
      if (count > maximum - output.size()) {
        throw new java.io.IOException("Workspace file size limit exceeded");
      }
      output.write(buffer, 0, count);
    }
    return output.toByteArray();
  }

  private String transferCommand(String action, String relative, String temporary, long maximum) {
    return "python3 -c " + shellQuote(WORKSPACE_TRANSFER_SCRIPT) + " " + action + " "
        + shellQuote(relative) + " " + shellQuote(temporary) + " " + maximum;
  }

  private void removeTransferTemporary(Leased current, String temporary) {
    try {
      current.sandbox().files().deleteFiles(java.util.List.of(temporary));
    } catch (Exception e) {
      log.debug("Unable to remove sandbox transfer temporary file");
    }
  }

  private Leased leaseExisting(String chatId) {
    String key = normalizeChatId(chatId);
    ReentrantLock lock = lockFor(key);
    lock.lock();
    try {
      ChatState state = sessions.get(key);
      if (!isAvailable() || state == null || state.sandbox == null || state.invalid
          || state.creating || state.retiring
          || (state.inFlight.get() == 0 && isIdleExpired(state))) {
        return null;
      }
      state.lastUsedNanos = System.nanoTime();
      state.inFlight.incrementAndGet();
      return new Leased(key, state, state.sandbox);
    } finally {
      lock.unlock();
    }
  }

  /**
   * 上传压缩包到指定会话的沙箱并解压到指定目录, 供后续 skills 等功能把资源包传递进沙箱.
   *
   * <p>
   * 压缩包先写入沙箱内唯一的临时文件, 再按后缀选择 unzip / tar 解压到 targetDir (unzip 不存在时 fallback 到
   * {@code python3 -m zipfile}), 完成后清理临时压缩包。
   *
   * @param chatId 会话标识, null/空 使用默认实例
   * @param archive 压缩包二进制内容
   * @param fileName 压缩包文件名(仅取末级名称, 需带 .zip/.tar.gz/.tgz/.tar 后缀)
   * @param targetDir 沙箱内解压目标目录(绝对路径)
   * @return 解压命令的执行结果, 调用方据 {@link SandboxResult#ok()} 判断成败
   */
  public SandboxResult uploadAndExtract(String chatId, byte[] archive, String fileName,
      String targetDir) {
    return executeWithRetry(chatId, "解压文件",
        leased -> extractArchive(leased.sandbox(), archive, fileName, targetDir),
        commandFailure("Failed to upload or extract archive"));
  }

  /** 技能检查、上传、解压、标记和目录查询在同一实例的租约内完成. */
  public SandboxResult prepareSkill(String chatId, String filePath, String targetDir,
      Callable<byte[]> archiveSupplier) {
    return executeWithRetry(chatId, "准备技能", leased -> {
      synchronized (leased.state().extractedSkills) {
        if (!leased.state().extractedSkills.contains(filePath)) {
          SandboxResult extracted =
              extractArchive(leased.sandbox(), archiveSupplier.call(), filePath, targetDir);
          if (!extracted.ok()) {
            return extracted;
          }
          leased.state().extractedSkills.add(filePath);
        }
        renewBeforeUse(leased.sandbox());
        return executeCommand(leased.sandbox(), "ls -R " + shellQuote(targetDir) + " | head -50");
      }
    }, commandFailure("Failed to prepare skill in sandbox"));
  }

  private SandboxResult extractArchive(Sandbox sandbox, byte[] archive, String fileName,
      String targetDir) throws Exception {
    String safeName = fileName == null ? "" : fileName.replace('\\', '/');
    safeName = safeName.substring(safeName.lastIndexOf('/') + 1);
    String suffix = detectArchiveSuffix(safeName);
    if (suffix == null) {
      return commandFailure("Unsupported archive format, only zip/tar.gz/tgz/tar are allowed");
    }
    if (targetDir == null || !targetDir.startsWith("/")) {
      return commandFailure("targetDir must be an absolute path inside the sandbox");
    }
    String rawArchivePath = ARCHIVE_TMP_DIR + "/" + UUID.randomUUID() + suffix;
    sandbox.files().writeFile(rawArchivePath, archive);
    String archivePath = shellQuote(rawArchivePath);
    String destination = shellQuote(targetDir);
    String extractCommand;
    if (".zip".equals(suffix)) {
      extractCommand = "unzip -o -q " + archivePath + " -d " + destination
          + " 2>/dev/null || python3 -m zipfile -e " + archivePath + " " + destination;
    } else {
      extractCommand = "tar " + (".tar".equals(suffix) ? "-xf " : "-xzf ")
          + archivePath + " -C " + destination;
    }
    renewBeforeUse(sandbox);
    return executeCommand(sandbox,
        "mkdir -p " + destination + " && (" + extractCommand + ") && rm -f " + archivePath);
  }

  private String shellQuote(String path) {
    return "'" + path.replace("'", "'\\''") + "'";
  }

  /**
   * 归一化 chatId, 空值落到默认实例.
   */
  private String normalizeChatId(String chatId) {
    return (chatId == null || chatId.isBlank()) ? DEFAULT_CHAT_ID : chatId.trim();
  }

  /**
   * 在租约保护下执行沙箱操作, 仅确认实例不存在时重建重试一次.
   *
   * @param action 操作名称, 仅用于日志
   * @param op 沙箱操作
   * @param failure 不可用或重试仍失败时的兜底返回值
   */
  private <T> T executeWithRetry(String chatId, String action, SandboxOp<T> op, T failure) {
    for (int attempt = 0; attempt < 2; attempt++) {
      Leased current = lease(chatId);
      if (current == null) {
        return failure;
      }
      try {
        renewBeforeUse(current.sandbox());
        return op.apply(current);
      } catch (Exception e) {
        if (!isSandboxNotFound(e)) {
          log.warn("沙箱{}失败, 保留实例, chatId: {}, 原因: {}", action, chatId, e.getMessage());
          return failure;
        }
        invalidate(current);
      } finally {
        release(current);
      }
    }
    return failure;
  }

  @FunctionalInterface
  private interface SandboxOp<T> {
    T apply(Leased leased) throws Exception;
  }

  /** 同会话只有一个创建者; 等待创建或失效实例租约排空时释放分段锁. */
  private Leased lease(String chatId) {
    String key = normalizeChatId(chatId);
    ReentrantLock lock = lockFor(key);
    while (isAvailable()) {
      ChatState state = sessions.computeIfAbsent(key, k -> new ChatState(lock.newCondition()));
      Sandbox doomed = null;
      boolean create = false;
      lock.lock();
      try {
        if (!isAvailable()) {
          return null;
        }
        if (sessions.get(key) != state) {
          continue;
        }
        if (state.creating || state.retiring || (state.invalid && state.inFlight.get() > 0)) {
          try {
            state.changed.await();
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
          }
          continue;
        }
        Sandbox existing = state.sandbox;
        if (existing != null) {
          if (state.invalid || (state.inFlight.get() == 0 && isIdleExpired(state))) {
            doomed = detachUnderLock(state);
          } else {
            state.lastUsedNanos = System.nanoTime();
            state.inFlight.incrementAndGet();
            return new Leased(key, state, existing);
          }
        } else {
          state.creating = true;
          state.lastUsedNanos = System.nanoTime();
          create = true;
        }
      } finally {
        lock.unlock();
      }
      if (doomed != null) {
        finishRetirement(key, state, doomed);
      } else if (create) {
        return createAndLease(key, state);
      }
    }
    return null;
  }

  private Leased createAndLease(String key, ChatState state) {
    boolean reserved = false;
    Sandbox created = null;
    boolean attached = false;
    try {
      reserved = acquireCapacityWithEviction();
      if (reserved && isAvailable()) {
        created = acquireSandbox(key);
      }
    } catch (Exception e) {
      log.warn("沙箱创建失败, chatId: {}, 原因: {}", key, e.getMessage());
    } finally {
      // 创建失败时先归还名额, 再唤醒等待者, 避免后继请求抢在归还之前误判满额.
      if (reserved && created == null) {
        capacity.release();
      }
      ReentrantLock lock = lockFor(key);
      lock.lock();
      try {
        if (created != null && isAvailable()) {
          state.sandbox = created;
          state.invalid = false;
          state.lastUsedNanos = System.nanoTime();
          state.inFlight.incrementAndGet();
          attached = true;
        }
        state.creating = false;
        state.changed.signalAll();
      } finally {
        lock.unlock();
      }
      if (!attached && created != null) {
        terminate(created, key);
      }
    }
    return attached ? new Leased(key, state, created) : null;
  }

  /** 最近使用时间与最后一次租约释放在同一把锁内更新, 关闭/失效时补做回收. */
  private void release(Leased leased) {
    Sandbox doomed = null;
    ReentrantLock lock = lockFor(leased.chatId());
    lock.lock();
    try {
      ChatState state = leased.state();
      state.lastUsedNanos = System.nanoTime();
      if (state.inFlight.decrementAndGet() == 0 && (closing.get() || state.invalid)) {
        doomed = detachUnderLock(state);
      }
      state.changed.signalAll();
    } finally {
      lock.unlock();
    }
    if (doomed != null) {
      finishRetirement(leased.chatId(), leased.state(), doomed);
    }
  }

  /** 只使失败租约对应的实例失效, 最后一个租约释放后才终止. */
  private void invalidate(Leased leased) {
    ReentrantLock lock = lockFor(leased.chatId());
    lock.lock();
    try {
      ChatState state = leased.state();
      if (sessions.get(leased.chatId()) == state && state.sandbox == leased.sandbox()) {
        state.invalid = true;
        state.changed.signalAll();
      }
    } finally {
      lock.unlock();
    }
  }

  /** 远端有效期覆盖一次操作及随后完整的空闲窗口, 避免服务端先于本地回收. */
  private Duration sandboxLifetime() {
    return property.getIdleTimeout().plus(property.getCommandTimeout())
        .plus(property.getRequestTimeout()).plusMinutes(1);
  }

  private void renewBeforeUse(Sandbox sandbox) {
    try {
      sandbox.renew(sandboxLifetime());
    } catch (Exception e) {
      if (isSandboxNotFound(e)) {
        throw e;
      }
      // 短暂的续期失败不代表容器失效, 继续原实例上的操作, 避免直接重建导致文件丢失.
      log.warn("沙箱续期失败, 保留原实例继续执行, ID: {}, 原因: {}", sandbox.getId(), e.getMessage());
    }
  }

  /** 已授予租约的沙箱, state 记录租约计数归属 */
  private record Leased(String chatId, ChatState state, Sandbox sandbox) {}

  /**
   * 获取一个创建名额, 容量不足时驱逐空闲的 LRU 实例再试.
   *
   * <p>
   * 存在 kill 失败待重试的实例时不再驱逐健康实例(避免 kill 通道异常引发连锁回收), 直接让位给创建拒绝流程; 驱逐后名额是否真正释放取决于 kill 是否成功, 由随后的
   * tryAcquire 兜底判断.
   */
  private boolean acquireCapacityWithEviction() {
    if (capacity.tryAcquire()) {
      return true;
    }
    for (int i = 0; i < 3 && pendingKills.isEmpty(); i++) {
      if (!evictOneIdle()) {
        return false;
      }
      if (capacity.tryAcquire()) {
        return true;
      }
    }
    return false;
  }

  /** 挑选并驱逐一个"存活且无租约"的最久未使用实例; true 表示已摘除并终止(可能 kill 失败) */
  private boolean evictOneIdle() {
    String victimKey = null;
    ChatState victimState = null;
    long oldestNanos = Long.MAX_VALUE;
    for (Map.Entry<String, ChatState> entry : sessions.entrySet()) {
      ChatState state = entry.getValue();
      if (state.sandbox != null && !state.creating && state.inFlight.get() == 0) {
        long last = state.lastUsedNanos;
        if (last < oldestNanos) {
          oldestNanos = last;
          victimKey = entry.getKey();
          victimState = state;
        }
      }
    }
    if (victimKey == null) {
      return false;
    }
    Sandbox doomed = null;
    ReentrantLock lock = lockFor(victimKey);
    lock.lock();
    try {
      // 锁内复核: 期间可能被其他租约/回收抢先
      if (sessions.get(victimKey) == victimState && victimState.lastUsedNanos == oldestNanos) {
        doomed = detachUnderLock(victimState);
      }
    } finally {
      lock.unlock();
    }
    if (doomed == null) {
      return false;
    }
    log.info("沙箱实例名额不足, 回收最久未使用的空闲实例, chatId: {}", victimKey);
    finishRetirement(victimKey, victimState, doomed);
    return true;
  }

  /**
   * 在持有分段锁的前提下摘除会话的沙箱实例; 返回摘下的实例, 已无实例或存在未释放租约时返回 null.
   */
  private Sandbox detachUnderLock(ChatState state) {
    if (state.sandbox == null || state.inFlight.get() != 0) {
      return null;
    }
    Sandbox doomed = state.sandbox;
    state.sandbox = null;
    state.invalid = false;
    state.retiring = true;
    // 沙箱销毁后文件不复存在, 技能解压标记一并清除
    state.extractedSkills.clear();
    return doomed;
  }

  /** 旧实例的名额完成释放或转入重试队列后, 才唤醒同会话的创建者. */
  private void finishRetirement(String key, ChatState state, Sandbox sandbox) {
    try {
      terminate(sandbox, key);
    } finally {
      ReentrantLock lock = lockFor(key);
      lock.lock();
      try {
        state.retiring = false;
        state.changed.signalAll();
      } finally {
        lock.unlock();
      }
    }
  }

  private Sandbox createSandbox(String key) {
    try {
      Sandbox created = Sandbox.builder()
          .connectionConfig(buildConnectionConfig())
          .image(property.getImage())
          .timeout(sandboxLifetime())
          .readyTimeout(property.getRequestTimeout())
          .skipHealthCheck(property.isSkipHealthCheck())
          .build();
      log.info("沙箱创建成功, chatId: {}, ID: {}", key, created.getId());
      prepareWorkspace(created);
      return created;
    } catch (Exception e) {
      log.error("沙箱创建失败, chatId: {}, 原因: {}", key, e.getMessage());
      return null;
    }
  }

  /**
   * 为会话获取一个全新沙箱: 优先从预热池取用(秒级), 失败回退现场冷创建.
   *
   * <p>
   * 预热池取用是独占语义——实例从空闲缓冲移除后归属当前会话, 后续回收仍走 {@link #terminate} 的 kill 路径, 不归还池(归还需清空工作区,
   * 否则上个会话的文件会泄露给下个会话)。
   */
  private Sandbox acquireSandbox(String key) {
    if (pool != null) {
      try {
        // acquire 的第一个参数是取出后沙箱的有效期, 不是 HTTP 请求超时.
        Sandbox acquired = pool.acquire(sandboxLifetime(),
            AcquirePolicy.RETRY_NEXT_IDLE_THEN_CREATE);
        log.info("沙箱预热池取用成功, chatId: {}, ID: {}", key, acquired.getId());
        return acquired;
      } catch (Exception e) {
        log.warn("沙箱预热池取用失败, 回退直接创建, chatId: {}, 原因: {}", key, e.getMessage());
      }
    }
    return isAvailable() ? createSandbox(key) : null;
  }

  private ConnectionConfig buildConnectionConfig() {
    return ConnectionConfig.builder()
        .domain(property.getDomain())
        .protocol(property.getProtocol())
        .apiKey(property.getApiKey())
        .requestTimeout(property.getRequestTimeout())
        .useServerProxy(property.isUseServerProxy())
        .build();
  }

  /**
   * 构建 SandboxPool 预热池(未启用或启动失败返回 null, 服务回退冷创建).
   */
  private SandboxPool buildPool() {
    if (!property.isEnabled() || !property.isPoolEnabled()) {
      return null;
    }
    try {
      SandboxPool created = SandboxPool.builder()
          .poolName("intelliconnect-sandbox")
          .maxIdle(property.getPoolMaxIdle())
          // SDK 未提供默认 stateStore, 单机部署用内存实现即可(空闲实例索引/主锁都存本进程)
          .stateStore(new InMemoryPoolStateStore())
          .connectionConfig(buildConnectionConfig())
          .creationSpec(PoolCreationSpec.builder().image(property.getImage()).build())
          .warmupSandboxPreparer(this::prepareWorkspace)
          .idleTimeout(property.getIdleTimeout())
          .warmupSkipHealthCheck(property.isSkipHealthCheck())
          .acquireSkipHealthCheck(property.isSkipHealthCheck())
          .build();
      created.start();
      log.info("沙箱预热池已启动, poolName: intelliconnect-sandbox, maxIdle: {}",
          property.getPoolMaxIdle());
      return created;
    } catch (Exception e) {
      log.warn("沙箱预热池启动失败, 将回退为直接创建沙箱: {}", e.getMessage());
      return null;
    }
  }

  /**
   * 初始化工作目录, 直接走 SDK 避免递归租约获取.
   */
  private void prepareWorkspace(Sandbox created) {
    try {
      RunCommandRequest request = RunCommandRequest.builder()
          .command("mkdir -p " + WORKSPACE_DIR)
          .timeout(property.getCommandTimeout())
          .build();
      created.commands().run(request);
    } catch (Exception e) {
      log.warn("沙箱工作目录初始化失败: {}", e.getMessage());
    }
  }

  /**
   * 终止一个已从会话摘除的沙箱实例(调用方不得持有任何分段锁).
   *
   * <p>
   * kill 确认成功才释放创建名额; kill 失败转入 {@code pendingKills} 延后重试, 名额与 close 均暂缓—— 客户端连接被关闭后 kill 重试可能永远失败,
   * 名额占用则保证远端容器总数不突破上限.
   */
  private void terminate(Sandbox sandbox, String chatId) {
    String sandboxId = sandbox.getId();
    try {
      sandbox.kill();
    } catch (Exception e) {
      if (isSandboxNotFound(e)) {
        // 远端容器已不存在(首次 kill 可能已生效或被服务端回收), 视为 kill 成功走同一条释放通道
        log.info("沙箱已不存在, 视为终止成功, chatId: {}, ID: {}", chatId, sandboxId);
        onKilled(chatId + ":" + sandboxId, sandbox);
        return;
      }
      String pendingKey = chatId + ":" + sandboxId;
      log.warn("终止沙箱失败, 转入延后重试, chatId: {}, ID: {}, 原因: {}", chatId, sandboxId, e.getMessage());
      pendingKillSinceNanos.put(pendingKey, System.nanoTime());
      pendingKills.put(pendingKey, sandbox);
      return;
    }
    onKilled(chatId + ":" + sandboxId, sandbox);
  }

  /** kill 确认成功(含明确的沙箱不存在错误)后的统一释放动作: 释放名额并关闭客户端连接 */
  private void onKilled(String sandboxKey, Sandbox sandbox) {
    capacity.release();
    closeQuietly(sandboxKey, sandbox);
  }

  /**
   * 仅使用 SDK 异常类型或错误码确认实例不存在, 文件 404 和普通消息均不能作为依据.
   */
  private boolean isSandboxNotFound(Exception e) {
    for (Throwable cause = e; cause != null; cause = cause.getCause()) {
      if (cause instanceof SandboxNotFoundException) {
        return true;
      }
      if (cause instanceof SandboxApiException api && api.getError() != null) {
        String code = api.getError().getCode();
        if ("SANDBOX_NOT_FOUND".equals(code)
            || (code != null && code.endsWith("::SANDBOX_NOT_FOUND"))) {
          return true;
        }
      }
    }
    return false;
  }

  private void closeQuietly(String sandboxKey, Sandbox sandbox) {
    try {
      sandbox.close();
    } catch (Exception e) {
      log.warn("关闭沙箱客户端失败, sandboxKey: {}, 原因: {}", sandboxKey, e.getMessage());
    }
  }

  private SandboxResult executeCommand(Sandbox target, String command) throws Exception {
    RunCommandRequest request = RunCommandRequest.builder()
        .command(command)
        // 显式落位工作目录, 与 BashTool/EditTool 对模型的声明保持一致;
        // 不设置时命令跑在容器默认目录(如 /root), 相对路径写出的文件会"消失"
        .workingDirectory(WORKSPACE_DIR)
        .timeout(property.getCommandTimeout())
        .build();
    long start = System.nanoTime();
    Execution execution = target.commands().run(request);
    long durationNanos = System.nanoTime() - start;
    return new SandboxResult(
        execution.getExitCode() == null ? 0 : execution.getExitCode(),
        joinLogs(execution),
        joinStderr(execution),
        describeError(execution),
        durationNanos / 1_000_000);
  }

  private String joinLogs(Execution execution) {
    try {
      return execution.getLogs().getStdout().stream()
          .map(item -> item.getText())
          .collect(Collectors.joining("\n"));
    } catch (Exception e) {
      return "";
    }
  }

  private String joinStderr(Execution execution) {
    try {
      return execution.getLogs().getStderr().stream()
          .map(item -> item.getText())
          .collect(Collectors.joining("\n"));
    } catch (Exception e) {
      return "";
    }
  }

  private String describeError(Execution execution) {
    ExecutionError error = execution.getError();
    if (error == null) {
      return null;
    }
    return error.getName() + ": " + error.getValue();
  }

  private String detectArchiveSuffix(String fileName) {
    String lower = fileName.toLowerCase();
    for (String suffix : ARCHIVE_SUFFIXES) {
      if (lower.endsWith(suffix)) {
        return suffix;
      }
    }
    return null;
  }

  private boolean isIdleExpired(ChatState state) {
    return System.nanoTime() - state.lastUsedNanos > property.getIdleTimeout().toNanos();
  }

  private ReentrantLock lockFor(String key) {
    return locks[Math.floorMod(key.hashCode(), locks.length)];
  }

  /** 定时清扫各会话中空闲超时的沙箱实例, 并安全移除长期无实例的死状态 */
  @Scheduled(fixedDelayString = "${ai.sandbox.sweep-interval-ms:60000}",
      scheduler = "sandboxTaskScheduler")
  public void sweepIdle() {
    for (Map.Entry<String, ChatState> entry : sessions.entrySet()) {
      String key = entry.getKey();
      ChatState state = entry.getValue();
      if (state.sandbox == null) {
        removeDeadState(key, state);
        continue;
      }
      if (isIdleExpired(state)) {
        Sandbox doomed = null;
        ReentrantLock lock = lockFor(key);
        lock.lock();
        try {
          if (sessions.get(key) == state && !state.creating && !state.retiring
              && isIdleExpired(state)) {
            doomed = detachUnderLock(state);
          }
        } finally {
          lock.unlock();
        }
        if (doomed != null) {
          log.info("会话沙箱空闲超过 {}, 自动回收, chatId: {}", property.getIdleTimeout(), key);
          finishRetirement(key, state, doomed);
        }
      }
    }
  }

  /**
   * 安全移除"无实例、无租约、长期未用"的死状态: 分段锁内复核, 避免与正在获取租约的线程交错.
   */
  private void removeDeadState(String key, ChatState state) {
    if (state.inFlight.get() != 0 || !isIdleExpired(state)) {
      return;
    }
    ReentrantLock lock = lockFor(key);
    lock.lock();
    try {
      ChatState current = sessions.get(key);
      if (current == state && !state.creating && !state.retiring
          && state.sandbox == null && state.inFlight.get() == 0
          && isIdleExpired(state)) {
        sessions.remove(key);
      }
    } finally {
      lock.unlock();
    }
  }

  /** 定时重试 kill 失败的沙箱; 超过最大存活时长仍未成功则放弃、释放名额并输出沙箱 ID 供人工清理 */
  @Scheduled(fixedDelayString = "${ai.sandbox.sweep-interval-ms:60000}",
      scheduler = "sandboxTaskScheduler")
  public void retryPendingKills() {
    for (Map.Entry<String, Sandbox> entry : pendingKills.entrySet()) {
      String pendingKey = entry.getKey();
      Sandbox sandbox = entry.getValue();
      boolean killed;
      try {
        sandbox.kill();
        killed = true;
      } catch (Exception e) {
        if (isSandboxNotFound(e)) {
          // 远端容器已不存在, 重试 kill 不可能再成功, 视为 kill 成功释放名额, 避免假性名额耗尽
          log.info("沙箱已不存在, 重试视为终止成功, sandboxKey: {}", pendingKey);
          killed = true;
        } else {
          log.warn("沙箱 kill 重试失败: {}, 原因: {}", pendingKey, e.getMessage());
          killed = false;
        }
      }
      if (killed && pendingKills.remove(pendingKey, sandbox)) {
        pendingKillSinceNanos.remove(pendingKey);
        capacity.release();
        closeQuietly(pendingKey, sandbox);
        continue;
      }
      Long since = pendingKillSinceNanos.get(pendingKey);
      if (!killed && since != null && System.nanoTime() - since > PENDING_KILL_MAX_AGE_NANOS
          && pendingKills.remove(pendingKey, sandbox)) {
        log.error("沙箱 kill 重试超过 {} 分钟仍失败, 放弃重试并释放名额, 请在沙箱宿主机手动清理容器, sandboxKey: {}",
            TimeUnit.NANOSECONDS.toMinutes(PENDING_KILL_MAX_AGE_NANOS), pendingKey);
        pendingKillSinceNanos.remove(pendingKey);
        capacity.release();
        closeQuietly(pendingKey, sandbox);
      }
    }
  }

  /** 应用关闭时释放全部沙箱资源(含 kill 失败待重试的实例) */
  @PreDestroy
  public void shutdown() {
    if (!closing.compareAndSet(false, true)) {
      return;
    }
    for (Map.Entry<String, ChatState> entry : sessions.entrySet()) {
      String key = entry.getKey();
      ChatState state = entry.getValue();
      Sandbox doomed = null;
      ReentrantLock lock = lockFor(key);
      lock.lock();
      try {
        if (sessions.get(key) == state) {
          state.changed.signalAll();
          doomed = detachUnderLock(state);
        }
      } finally {
        lock.unlock();
      }
      if (doomed != null) {
        finishRetirement(key, state, doomed);
      }
    }
    retryPendingKills();
    if (!pendingKills.isEmpty()) {
      log.error("应用关闭时仍有沙箱终止失败, 请在宿主机清理: {}", pendingKills.keySet());
    }
    if (pool != null) {
      try {
        // 先把 maxIdle 归零, 停止 reconcile 后台线程的预热补充,
        // 否则 releaseAllIdle 释放空闲实例后 reconcile 会立即补创建新沙箱
        pool.resize(0);
        // shutdown 只停线程不杀容器, 空闲实例必须先经 releaseAllIdle 逐个远程 kill
        int released = pool.releaseAllIdle();
        log.info("沙箱预热池空闲实例已释放, 数量: {}", released);
      } catch (Exception e) {
        log.error("沙箱预热池空闲实例释放失败, 请在沙箱宿主机手动清理容器, 原因: {}", e.getMessage());
      }
      try {
        // 优雅停机: 等待在途操作排空后停止预热池后台线程
        pool.shutdown(true);
      } catch (Exception e) {
        log.error("沙箱预热池关闭失败, 请在沙箱宿主机手动清理空闲容器, 原因: {}", e.getMessage());
      }
    }
  }

  /** 单个会话的全部运行时状态 */
  private static final class ChatState {
    final Condition changed;
    volatile boolean creating;
    volatile boolean retiring;
    volatile boolean invalid;

    ChatState(Condition changed) {
      this.changed = changed;
    }

    /** 当前沙箱实例, null 表示无实例(已回收或尚未创建) */
    volatile Sandbox sandbox;
    /** 执行租约计数, 大于 0 时回收/清扫/驱逐均不得终止当前实例 */
    final AtomicInteger inFlight = new AtomicInteger();
    /** 最近一次使用时间(纳秒) */
    volatile long lastUsedNanos;
    /** 已解压进该会话沙箱的技能文件集合, 摘除实例时一并清除 */
    final Set<String> extractedSkills = ConcurrentHashMap.newKeySet();
  }
}
