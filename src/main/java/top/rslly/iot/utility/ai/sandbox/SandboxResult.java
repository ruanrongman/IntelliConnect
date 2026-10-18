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

/**
 * 沙箱命令执行结果.
 *
 * @param exitCode 退出码, -1 表示执行本身失败(未拿到退出码)
 * @param stdout 标准输出
 * @param stderr 标准错误
 * @param error 执行错误信息, 正常完成为 null
 * @param durationMillis 执行耗时(毫秒)
 */
public record SandboxResult(int exitCode, String stdout, String stderr, String error,
    long durationMillis) {

  /** 执行是否成功 */
  public boolean ok() {
    return error == null && exitCode == 0;
  }

  /**
   * 拼装友好的多行摘要, 供 AI 工具直接作为字符串返回使用.
   */
  public String summary() {
    StringBuilder sb = new StringBuilder();
    if (!stdout.isBlank()) {
      sb.append(stdout).append('\n');
    }
    if (!stderr.isBlank()) {
      sb.append("[stderr] ").append(stderr).append('\n');
    }
    if (error != null && !error.isBlank()) {
      sb.append("[error] ").append(error).append('\n');
    }
    if (sb.isEmpty()) {
      sb.append("(no output, exit code ").append(exitCode).append(')');
    }
    return sb.toString().stripTrailing();
  }
}
