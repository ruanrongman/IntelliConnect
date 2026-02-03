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
package top.rslly.iot.utility.script;

import com.google.common.util.concurrent.FutureCallback;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.Nullable;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@AllArgsConstructor
public class ScriptStatCallback<T> implements FutureCallback<T> {

  private final AtomicInteger successMsgs;
  private final AtomicInteger timeoutMsgs;
  private final AtomicInteger failedMsgs;

  @Override
  public void onSuccess(@Nullable T result) {
    successMsgs.incrementAndGet();
  }

  @Override
  public void onFailure(Throwable t) {
    if (t instanceof TimeoutException
        || (t.getCause() != null && t.getCause() instanceof TimeoutException)) {
      timeoutMsgs.incrementAndGet();
    } else {
      failedMsgs.incrementAndGet();
    }
  }
}
