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
package top.rslly.iot.utility.ai.tools;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.rslly.iot.utility.script.ControlScriptFactory;
import top.rslly.iot.utility.script.js.JsScriptInfo;
import top.rslly.iot.utility.script.js.NashornJsInvokeService;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@Data
@Slf4j
public class JsExecuteTool implements BaseTool<String> {
  private String name = "jsExecuteTool";
  private String description = """
      Execute JavaScript statements in the Nashorn engine (ECMAScript 5.1).
      The script MUST directly return a non-null value.
      The return value MUST be convertible to String.
      Do NOT define functions; use plain executable statements only.
      If execution fails, return an error string or JSON string.
      Args: The input parameter is JavaScript code.(str)
      """;
  @Autowired
  private NashornJsInvokeService nashornJsInvokeService;

  @Override
  public String run(String question) {
    return "";
  }

  @Override
  public String run(String question, Map<String, Object> globalMessage) {
    int productId = (int) globalMessage.get("productId");
    var jsCode = ControlScriptFactory.generateControlNodeScript("skillsFunc" + productId,
        question);
    // log.info("jsCode: {}", jsCode);
    JsScriptInfo jsScriptInfo = new JsScriptInfo("skillsFunc" + productId);
    var uuid = UUID.randomUUID();
    // var scriptId=nashornJsInvokeService.doEval(uuid,jsScriptInfo,"function control() { return
    // 'Hello, world!'; }");
    // nashornJsInvokeService.compileJs(script);
    try {
      // 等待编译完成（最多10秒）
      var scriptId = nashornJsInvokeService.doEval(uuid, jsScriptInfo, jsCode);
      UUID compiledScriptId = scriptId.get(10, java.util.concurrent.TimeUnit.SECONDS);

      // 执行函数调用（最多10秒）
      var result = nashornJsInvokeService.doInvokeFunction(uuid, jsScriptInfo, new Object[] {});
      Object returnValue = result.get(10, java.util.concurrent.TimeUnit.SECONDS);
      // log.info("Function returned: {}", returnValue);
      // 成功执行后释放资源
      nashornJsInvokeService.release(uuid);

      return returnValue.toString();

    } catch (java.util.concurrent.TimeoutException e) {
      log.info("js execute timeout: {}", e.getMessage());
      // 即使超时也要尝试释放资源
      try {
        nashornJsInvokeService.release(uuid);
      } catch (Exception releaseException) {
        log.warn("Failed to release script resources after timeout: {}",
            releaseException.getMessage());
      }
      return "js execute timeout error";
    } catch (Exception e) {
      log.info("js execute error: {}", e.getMessage());
      // 异常时也尝试释放资源
      try {
        nashornJsInvokeService.release(uuid);
      } catch (Exception releaseException) {
        log.warn("Failed to release script resources after error: {}",
            releaseException.getMessage());
      }
      return "js execute error";
    }
  }
}
