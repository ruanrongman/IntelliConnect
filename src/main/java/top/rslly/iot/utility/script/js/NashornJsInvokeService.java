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
package top.rslly.iot.utility.script.js;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import delight.nashornsandbox.NashornSandbox;
import delight.nashornsandbox.NashornSandboxes;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import top.rslly.iot.utility.script.*;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class NashornJsInvokeService extends AbstractJsInvokeService implements ScriptInvokeService {
  // protected final Map<UUID, JsScriptInfo> scriptInfoMap = new ConcurrentHashMap<>();
  private NashornSandbox sandbox;
  private ScriptEngine engine;
  private ExecutorService monitorExecutorService;
  private ListeningExecutorService jsExecutor;

  private final ReentrantLock evalLock = new ReentrantLock();

  @Value("${js.local.use_js_sandbox}")
  private boolean useJsSandbox;

  @Value("${js.local.monitor_thread_pool_size}")
  private int monitorThreadPoolSize;

  @Value("${js.local.max_cpu_time}")
  private long maxCpuTime;

  @Getter
  @Value("${js.local.max_black_list_duration_sec:60}")
  private int maxBlackListDurationSec;

  @Getter
  @Value("${js.local.max_requests_timeout:0}")
  private long maxInvokeRequestsTimeout;

  @Value("${js.local.js_thread_pool_size:50}")
  private int jsExecutorThreadPoolSize;

  @PostConstruct
  @Override
  public void init() {
    jsExecutor =
        MoreExecutors.listeningDecorator(Executors.newWorkStealingPool(jsExecutorThreadPoolSize));
    if (useJsSandbox) {
      sandbox = NashornSandboxes.create();
      monitorExecutorService =
          IntelliConnectExecutors.newWorkStealingPool(monitorThreadPoolSize, "nashorn-js-monitor");
      sandbox.setExecutor(monitorExecutorService);
      sandbox.setMaxCPUTime(maxCpuTime);
      sandbox.allowNoBraces(false);
      sandbox.allow(JsUtils.class);
      sandbox.allow(java.util.ArrayList.class);
      sandbox.allowLoadFunctions(true);
      sandbox.setMaxPreparedStatements(30);
    } else {
      ScriptEngineManager factory = new ScriptEngineManager();
      engine = factory.getEngineByName("nashorn");
    }
  }

  @Override
  public ListenableFuture<UUID> eval(ScriptType scriptType, String scriptBody, String... argNames) {
    return null;
  }

  @Override
  public ListenableFuture<Object> invokeScript(UUID scriptId, Object... args) {
    return null;
  }

  @Override
  public ListenableFuture<Void> release(UUID scriptId) {
    return null;
  }

  @Override
  public ScriptLanguage getLanguage() {
    return ScriptLanguage.JS;
  }

  @PreDestroy
  @Override
  public void stop() {
    if (monitorExecutorService != null) {
      monitorExecutorService.shutdownNow();
    }
    if (jsExecutor != null) {
      jsExecutor.shutdownNow();
    }
  }

  @Override
  public ListenableFuture<UUID> doEval(UUID scriptId, JsScriptInfo scriptInfo, String jsScript) {
    return jsExecutor.submit(() -> {
      try {
        evalLock.lock();
        try {
          if (useJsSandbox) {
            sandbox.eval(jsScript);
          } else {
            engine.eval(jsScript);
          }
        } finally {
          evalLock.unlock();
        }
        // scriptInfoMap.put(scriptId, scriptInfo);
        return scriptId;
      } catch (Exception e) {
        e.printStackTrace();
        throw new IcScriptException(scriptId, IcScriptException.ErrorCode.COMPILATION, jsScript, e);
      }
    });
  }

  @Override
  public ListenableFuture<Object> doInvokeFunction(UUID scriptId, JsScriptInfo scriptInfo,
      Object[] args) {
    return jsExecutor.submit(() -> {
      try {
        if (useJsSandbox) {
          if (args.length == 0)
            return sandbox.getSandboxedInvocable().invokeFunction(scriptInfo.getFunctionName());
          return sandbox.getSandboxedInvocable().invokeFunction(scriptInfo.getFunctionName(), args);
        } else {
          if (args.length == 0)
            return ((Invocable) engine).invokeFunction(scriptInfo.getFunctionName());
          return ((Invocable) engine).invokeFunction(scriptInfo.getFunctionName(), args);
        }
      } catch (ScriptException e) {
        throw new IcScriptException(scriptId, IcScriptException.ErrorCode.RUNTIME, null, e);
      } catch (Exception e) {
        throw new IcScriptException(scriptId, IcScriptException.ErrorCode.OTHER, null, e);
      }
    });
  }

  @Override
  public void doRelease(UUID scriptId, JsScriptInfo scriptInfo) throws Exception {
    if (useJsSandbox) {
      sandbox.eval(scriptInfo.getFunctionName() + " = undefined;");
    } else {
      engine.eval(scriptInfo.getFunctionName() + " = undefined;");
    }
  }

}
