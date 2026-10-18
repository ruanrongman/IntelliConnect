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
import com.alibaba.opensandbox.sandbox.domain.models.execd.executions.Execution;
import com.alibaba.opensandbox.sandbox.domain.models.execd.executions.ExecutionError;
import com.alibaba.opensandbox.sandbox.domain.models.execd.executions.RunCommandRequest;
import com.alibaba.opensandbox.sandbox.domain.pool.AcquirePolicy;
import com.alibaba.opensandbox.sandbox.domain.pool.PoolCreationSpec;
import com.alibaba.opensandbox.sandbox.infrastructure.pool.InMemoryPoolStateStore;
import com.alibaba.opensandbox.sandbox.pool.SandboxPool;
import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

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
 * <li><b>执行租约</b>: 执行类操作(命令/文件读写)在分段锁内取得实例并递增租约计数, 持租约期间空闲清扫与 LRU 驱逐都不得终止该实例; 释放租约只递减计数, 无需持锁。
 * 长命令执行中不再出现被 {@code sweepIdle()} 或容量驱逐杀掉实例的竞态。
 * <li><b>容量名额</b>: 创建名额由全局信号量控制(名额数即 {@code ai.sandbox.max-instances}, 启动后固定), 检查-创建原子化, 不存在跨会话的
 * check-then-act 竞态。 kill <b>确认成功</b>才释放名额; kill 失败转入延后重试队列期间名额继续占用, 远端容器总数因此不会突破上限; 名额不足时先驱逐空闲的
 * LRU 实例, 全忙或 kill 通道异常时拒绝创建。
 * <li><b>分段锁</b>: 128 把 {@link ReentrantLock} 按 chatId 哈希取模, 互斥同一会话的创建/摘除/清扫; 任何路径至多持有一把分段锁, kill /
 * close 等网络调用一律在锁外执行, 无锁序死锁。
 * <li><b>元数据生命周期</b>: 会话的全部状态(实例/租约计数/时间戳/技能清单)收敛在 {@code sessions} 单一 map 中,
 * 无实例且长期未用的死状态由定时清扫在锁内校验后安全移除, 不随 chatId 数量无限增长。
 * <li>回收时 kill 失败的沙箱转入延后重试队列, 由定时任务重试, 避免远端容器泄漏; 超过 30 分钟仍失败则放弃、释放名额并输出沙箱 ID 供人工清理。
 * <li>调用失败(如沙箱被外部杀死导致连接失效)时回收该实例并重建重试一次。
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
   * 沙箱服务是否可用(配置开关).
   */
  public boolean isAvailable() {
    return property.isEnabled();
  }

  /**
   * 指定技能文件是否已经解压进该会话的沙箱.
   *
   * @param chatId 会话标识, null/空 使用默认实例
   * @param filePath 技能文件在服务端的存储路径(唯一)
   */
  public boolean isSkillExtracted(String chatId, String filePath) {
    ChatState state = sessions.get(normalizeChatId(chatId));
    return state != null && state.extractedSkills.contains(filePath);
  }

  /**
   * 标记技能文件已解压进该会话的沙箱(仅在解压成功后调用).
   *
   * <p>
   * best-effort 语义: 若会话状态恰逢清扫, 标记丢失后下次会重新解压, 不影响正确性.
   */
  public void markSkillExtracted(String chatId, String filePath) {
    ChatState state = sessions.get(normalizeChatId(chatId));
    if (state != null) {
      state.extractedSkills.add(filePath);
    }
  }

  /**
   * 在指定会话的沙箱中执行一条 shell 命令.
   *
   * <p>
   * 失败时自动回收旧实例并重试一次; 二次失败返回 exitCode 为 -1 的结果, 不抛异常。
   *
   * @param chatId 会话标识, null/空 使用默认实例
   * @param command 完整 shell 命令字符串
   * @return 执行结果, 永不为 null
   */
  public SandboxResult runCommand(String chatId, String command) {
    Leased current = lease(chatId);
    if (current == null) {
      return new SandboxResult(-1, "", "",
          "Sandbox is unavailable (disabled, at capacity, or failed to create)", 0);
    }
    try {
      try {
        return executeCommand(current.sandbox(), command);
      } finally {
        release(current);
      }
    } catch (Exception first) {
      log.warn("沙箱命令执行失败, 尝试重建沙箱重试: {}", first.getMessage());
      recycleNow(chatId);
      Leased fresh = lease(chatId);
      if (fresh == null) {
        return new SandboxResult(-1, "", "", "Sandbox recreation failed: " + first.getMessage(), 0);
      }
      try {
        try {
          return executeCommand(fresh.sandbox(), command);
        } finally {
          release(fresh);
        }
      } catch (Exception second) {
        log.warn("沙箱命令重试仍失败: {}", second.getMessage());
        recycleNow(chatId);
        return new SandboxResult(-1, "",
            "", "Sandbox command execution failed: " + second.getMessage(), 0);
      }
    }
  }

  /**
   * 向指定会话的沙箱写入文本文件(路径不存在时自动创建, 覆盖已有内容).
   *
   * @return 是否成功
   */
  public boolean writeFile(String chatId, String path, String content) {
    return executeWithRetry(chatId, "写文件", sandbox -> {
      sandbox.files().writeFile(path, content);
      return Boolean.TRUE;
    }, Boolean.FALSE);
  }

  /**
   * 向指定会话的沙箱写入二进制文件(路径不存在时自动创建, 覆盖已有内容), 用于压缩包等非文本内容上传.
   *
   * @return 是否成功
   */
  public boolean writeFile(String chatId, String path, byte[] data) {
    return executeWithRetry(chatId, "写二进制文件", sandbox -> {
      sandbox.files().writeFile(path, data);
      return Boolean.TRUE;
    }, Boolean.FALSE);
  }

  /**
   * 读取指定会话沙箱内的文本文件内容.
   *
   * @return 文件内容, 失败返回 null
   */
  public String readFile(String chatId, String path) {
    return executeWithRetry(chatId, "读文件", sandbox -> sandbox.files().readFile(path), null);
  }

  /**
   * 上传压缩包到指定会话的沙箱并解压到指定目录, 供后续 skills 等功能把资源包传递进沙箱.
   *
   * <p>
   * 压缩包先写入沙箱内 {@code /tmp/<fileName>}, 再按后缀选择 unzip / tar 解压到 targetDir (unzip 不存在时 fallback 到
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
    String safeName = fileName == null ? "" : fileName.replace('\\', '/');
    safeName = safeName.substring(safeName.lastIndexOf('/') + 1);
    String suffix = detectArchiveSuffix(safeName);
    if (suffix == null) {
      return new SandboxResult(-1, "", "",
          "Unsupported archive format, only zip/tar.gz/tgz/tar are allowed", 0);
    }
    if (targetDir == null || !targetDir.startsWith("/")) {
      return new SandboxResult(-1, "", "", "targetDir must be an absolute path inside the sandbox",
          0);
    }
    String archivePath = ARCHIVE_TMP_DIR + "/" + safeName;
    if (!writeFile(chatId, archivePath, archive)) {
      return new SandboxResult(-1, "", "", "Failed to upload archive into the sandbox", 0);
    }
    String extractCommand;
    if (".zip".equals(suffix)) {
      // unzip 不存在时 fallback 到 python3 内置 zipfile 模块
      extractCommand = String.format(
          "unzip -o -q '%s' -d '%s' 2>/dev/null || python3 -m zipfile -e '%s' '%s'",
          archivePath, targetDir, archivePath, targetDir);
    } else if (".tar.gz".equals(suffix) || ".tgz".equals(suffix)) {
      extractCommand = String.format("tar -xzf '%s' -C '%s'", archivePath, targetDir);
    } else {
      extractCommand = String.format("tar -xf '%s' -C '%s'", archivePath, targetDir);
    }
    SandboxResult result = runCommand(chatId,
        "mkdir -p '" + targetDir + "' && " + extractCommand + " && rm -f '" + archivePath + "'");
    if (!result.ok()) {
      log.warn("压缩包解压失败: {}", result.summary());
    }
    return result;
  }

  /**
   * 归一化 chatId, 空值落到默认实例.
   */
  private String normalizeChatId(String chatId) {
    return (chatId == null || chatId.isBlank()) ? DEFAULT_CHAT_ID : chatId.trim();
  }

  /**
   * 在租约保护下执行沙箱操作, 失败时回收旧实例并重建重试一次.
   *
   * @param action 操作名称, 仅用于日志
   * @param op 沙箱操作
   * @param failure 不可用或重试仍失败时的兜底返回值
   */
  private <T> T executeWithRetry(String chatId, String action, SandboxOp<T> op, T failure) {
    Leased current = lease(chatId);
    if (current == null) {
      return failure;
    }
    try {
      try {
        return op.apply(current.sandbox());
      } finally {
        release(current);
      }
    } catch (Exception first) {
      log.warn("沙箱{}失败, 尝试重建沙箱重试: {}", action, first.getMessage());
      recycleNow(chatId);
      Leased fresh = lease(chatId);
      if (fresh == null) {
        return failure;
      }
      try {
        try {
          return op.apply(fresh.sandbox());
        } finally {
          release(fresh);
        }
      } catch (Exception second) {
        log.warn("沙箱{}重试仍失败: {}", action, second.getMessage());
        recycleNow(chatId);
        return failure;
      }
    }
  }

  /** 在 {@link Sandbox} 实例上执行且可能抛异常的操作 */
  @FunctionalInterface
  private interface SandboxOp<T> {
    T apply(Sandbox sandbox) throws Exception;
  }

  /**
   * 获取指定会话的沙箱实例并授予执行租约.
   *
   * <p>
   * 三段式流程, 任何时刻至多持有一把分段锁:
   *
   * <ol>
   * <li>锁内: 校验状态未被清扫重建, 刷新使用时间, 空闲超时的旧实例先摘除(锁外终止)并重试, 存活实例直接授予租约.
   * <li>锁外: 获取创建名额(容量不足时驱逐空闲 LRU 实例); 必须先放下自己的锁, 避免驱逐触及其他会话的锁造成锁序死锁.
   * <li>锁内: 创建并挂载新实例(同一会话的并发创建由分段锁串行化); 并发线程抢先建好则复用之并归还多占的名额.
   * </ol>
   *
   * <p>
   * 返回 null 表示不可用(容量不足且无可回收实例, 或创建失败).
   */
  private Leased lease(String chatId) {
    String key = normalizeChatId(chatId);
    while (true) {
      ChatState state = sessions.computeIfAbsent(key, k -> new ChatState());
      Sandbox doomed = null;
      ReentrantLock lock = lockFor(key);
      lock.lock();
      try {
        // 状态条目可能已被清扫重建, 校验失败则换新状态重试
        if (sessions.get(key) != state) {
          continue;
        }
        long previousNanos = state.lastUsedNanos;
        state.lastUsedNanos = System.nanoTime();
        Sandbox existing = state.sandbox;
        if (existing != null) {
          if (System.nanoTime() - previousNanos > property.getIdleTimeout().toNanos()) {
            doomed = detachUnderLock(state);
          }
          if (doomed == null) {
            // 实例存活(或正被其他租约占用), 直接复用
            state.inFlight.incrementAndGet();
            return new Leased(state, existing);
          }
        }
      } finally {
        lock.unlock();
      }
      if (doomed != null) {
        log.info("会话沙箱空闲超过 {}, 重建前回收, chatId: {}", property.getIdleTimeout(), key);
        terminate(doomed, key);
        continue;
      }
      break;
    }
    if (!acquireCapacityWithEviction()) {
      log.warn("沙箱实例名额不足且无可回收实例, 拒绝创建, chatId: {}", key);
      return null;
    }
    ReentrantLock lock = lockFor(key);
    lock.lock();
    try {
      ChatState state = sessions.computeIfAbsent(key, k -> new ChatState());
      state.lastUsedNanos = System.nanoTime();
      Sandbox existing = state.sandbox;
      if (existing != null) {
        // 并发线程已抢先建好, 复用之并归还多占的名额
        capacity.release();
        state.inFlight.incrementAndGet();
        return new Leased(state, existing);
      }
      Sandbox created = acquireSandbox(key);
      if (created == null) {
        capacity.release();
        return null;
      }
      state.sandbox = created;
      state.inFlight.incrementAndGet();
      return new Leased(state, created);
    } finally {
      lock.unlock();
    }
  }

  /** 释放执行租约: inFlight 大于 0 期间状态条目不可能被清扫, 持引用递减即可, 无需分段锁 */
  private void release(Leased leased) {
    leased.state().inFlight.decrementAndGet();
  }

  /** 已授予租约的沙箱, state 记录租约计数归属 */
  private record Leased(ChatState state, Sandbox sandbox) {}

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
      if (state.sandbox != null && state.inFlight.get() == 0) {
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
      if (sessions.get(victimKey) == victimState) {
        doomed = detachUnderLock(victimState);
      }
    } finally {
      lock.unlock();
    }
    if (doomed == null) {
      return false;
    }
    log.info("沙箱实例名额不足, 回收最久未使用的空闲实例, chatId: {}", victimKey);
    terminate(doomed, victimKey);
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
    // 沙箱销毁后文件不复存在, 技能解压标记一并清除
    state.extractedSkills.clear();
    return doomed;
  }

  private Sandbox createSandbox(String key) {
    try {
      Sandbox created = Sandbox.builder()
          .connectionConfig(buildConnectionConfig())
          .image(property.getImage())
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
        Sandbox acquired = pool.acquire(property.getRequestTimeout(),
            AcquirePolicy.RETRY_NEXT_IDLE_THEN_CREATE);
        log.info("沙箱预热池取用成功, chatId: {}, ID: {}", key, acquired.getId());
        return acquired;
      } catch (Exception e) {
        log.warn("沙箱预热池取用失败, 回退直接创建, chatId: {}, 原因: {}", key, e.getMessage());
      }
    }
    return createSandbox(key);
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
   * 回收指定会话的沙箱实例(执行失败重建路径); 存在未释放租约时跳过, 由持租约方自行触发.
   */
  private void recycleNow(String chatId) {
    String key = normalizeChatId(chatId);
    Sandbox doomed = null;
    ReentrantLock lock = lockFor(key);
    lock.lock();
    try {
      ChatState state = sessions.get(key);
      if (state != null) {
        doomed = detachUnderLock(state);
      }
    } finally {
      lock.unlock();
    }
    if (doomed != null) {
      terminate(doomed, key);
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
      pendingKills.put(pendingKey, sandbox);
      pendingKillSinceNanos.put(pendingKey, System.nanoTime());
      return;
    }
    onKilled(chatId + ":" + sandboxId, sandbox);
  }

  /** kill 确认成功(含 404 已不存在)后的统一释放动作: 释放名额并关闭客户端连接 */
  private void onKilled(String sandboxKey, Sandbox sandbox) {
    capacity.release();
    closeQuietly(sandboxKey, sandbox);
  }

  /**
   * 判断异常是否为远端沙箱已不存在的 404 错误(OpenSandbox 返回 {@code DOCKER::SANDBOX_NOT_FOUND}).
   */
  private boolean isSandboxNotFound(Exception e) {
    String message = e.getMessage();
    return message != null && message.contains("SANDBOX_NOT_FOUND");
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
  @Scheduled(fixedDelayString = "${ai.sandbox.sweep-interval-ms:60000}")
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
          if (sessions.get(key) == state) {
            doomed = detachUnderLock(state);
          }
        } finally {
          lock.unlock();
        }
        if (doomed != null) {
          log.info("会话沙箱空闲超过 {}, 自动回收, chatId: {}", property.getIdleTimeout(), key);
          terminate(doomed, key);
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
      if (current == state && state.sandbox == null && state.inFlight.get() == 0
          && isIdleExpired(state)) {
        sessions.remove(key);
      }
    } finally {
      lock.unlock();
    }
  }

  /** 定时重试 kill 失败的沙箱; 超过最大存活时长仍未成功则放弃、释放名额并输出沙箱 ID 供人工清理 */
  @Scheduled(fixedDelayString = "${ai.sandbox.sweep-interval-ms:60000}")
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
      if (killed) {
        pendingKills.remove(pendingKey);
        pendingKillSinceNanos.remove(pendingKey);
        capacity.release();
        closeQuietly(pendingKey, sandbox);
        continue;
      }
      Long since = pendingKillSinceNanos.get(pendingKey);
      if (since != null && System.nanoTime() - since > PENDING_KILL_MAX_AGE_NANOS) {
        log.error("沙箱 kill 重试超过 {} 分钟仍失败, 放弃重试并释放名额, 请在沙箱宿主机手动清理容器, sandboxKey: {}",
            TimeUnit.NANOSECONDS.toMinutes(PENDING_KILL_MAX_AGE_NANOS), pendingKey);
        pendingKills.remove(pendingKey);
        pendingKillSinceNanos.remove(pendingKey);
        capacity.release();
        closeQuietly(pendingKey, sandbox);
      }
    }
  }

  /** 应用关闭时释放全部沙箱资源(含 kill 失败待重试的实例) */
  @PreDestroy
  public void shutdown() {
    for (Map.Entry<String, ChatState> entry : sessions.entrySet()) {
      String key = entry.getKey();
      ChatState state = entry.getValue();
      Sandbox doomed = null;
      ReentrantLock lock = lockFor(key);
      lock.lock();
      try {
        if (sessions.get(key) == state) {
          doomed = detachUnderLock(state);
        }
      } finally {
        lock.unlock();
      }
      if (doomed != null) {
        terminate(doomed, key);
      }
    }
    for (Map.Entry<String, Sandbox> entry : pendingKills.entrySet()) {
      try {
        entry.getValue().kill();
      } catch (Exception e) {
        log.error("应用关闭时终止沙箱失败, 请在沙箱宿主机手动清理容器, sandboxKey: {}, 原因: {}",
            entry.getKey(), e.getMessage());
      }
      closeQuietly(entry.getKey(), entry.getValue());
    }
    pendingKills.clear();
    pendingKillSinceNanos.clear();
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
