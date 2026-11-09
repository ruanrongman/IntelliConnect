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
package top.rslly.iot.utility.ai.llm;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import top.rslly.iot.utility.ai.ModelMessage;

@Timeout(15)
class DeepSeekTimeoutTest {
  private HttpServer server;
  private ExecutorService serverExecutor;
  private String baseUrl;
  private final AtomicReference<String> timeoutHeader = new AtomicReference<>();

  @BeforeEach
  void startServer() throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    serverExecutor = Executors.newVirtualThreadPerTaskExecutor();
    server.setExecutor(serverExecutor);
    server.createContext("/", exchange -> {
      timeoutHeader.set(exchange.getRequestHeaders().getFirst("X-Stainless-Timeout"));
      exchange.getRequestBody().readAllBytes();
      exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
      exchange.sendResponseHeaders(200, 0);
      try (var output = exchange.getResponseBody()) {
        output.write(chunk("\"reasoning_content\":\"thinking\""));
        output.flush();
        TimeUnit.MILLISECONDS.sleep(400);
        output.write(chunk("\"content\":\"finished\""));
        output.write("data: [DONE]\n\n".getBytes(StandardCharsets.UTF_8));
        output.flush();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } catch (IOException e) {
        // The short-timeout test closes the connection while the model is thinking.
      } finally {
        exchange.close();
      }
    });
    server.start();
    baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
  }

  @AfterEach
  void stopServer() {
    server.stop(0);
    serverExecutor.shutdownNow();
    new LLMFactory().setRequestTimeoutSeconds(600);
  }

  @Test
  void thinkingStreamCompletesWithLongerConfiguredTimeout() throws Exception {
    DeepSeek model = new DeepSeek(baseUrl, "test-model", "test-key", true, 128,
        0.6, 0.85, false, Duration.ofSeconds(3));
    AtomicReference<String> reasoning = new AtomicReference<>();

    assertEquals("finished", stream(model, reasoning).get(10, TimeUnit.SECONDS));
    assertEquals("thinking", reasoning.get());
    assertEquals("3", timeoutHeader.get());
  }

  @Test
  void thinkingStreamStillFailsWhenRequestDeadlineExpires() {
    DeepSeek model = new DeepSeek(baseUrl, "test-model", "test-key", true, 128,
        0.6, 0.85, false, Duration.ofMillis(100));

    assertThrows(java.util.concurrent.ExecutionException.class,
        () -> stream(model, new AtomicReference<>()).get(10, TimeUnit.SECONDS));
  }

  @Test
  void factoryAppliesTimeoutAndDoesNotReuseClientWithOldTimeout() throws Exception {
    LLMFactory factory = new LLMFactory();
    factory.setRequestTimeoutSeconds(2);
    LLM first = LLMFactory.getLLM("test-model", baseUrl, "test-key", true, 128);
    assertEquals("finished", stream(first, new AtomicReference<>()).get(10, TimeUnit.SECONDS));
    assertEquals("2", timeoutHeader.get());

    factory.setRequestTimeoutSeconds(3);
    LLM second = LLMFactory.getLLM("test-model", baseUrl, "test-key", true, 128);
    assertNotSame(first, second);
    assertSame(second, LLMFactory.getLLM("test-model", baseUrl, "test-key", true, 128));
    assertEquals("finished", stream(second, new AtomicReference<>()).get(10, TimeUnit.SECONDS));
    assertEquals("3", timeoutHeader.get());
  }

  @Test
  void legacyConstructorUsesLongTaskDefaultTimeout() throws Exception {
    LLM model = new DeepSeek(baseUrl, "test-model", "test-key");
    assertEquals("finished", stream(model, new AtomicReference<>()).get(10, TimeUnit.SECONDS));
    assertEquals("600", timeoutHeader.get());
  }

  @Test
  void invalidTimeoutIsRejected() {
    LLMFactory factory = new LLMFactory();
    assertThrows(IllegalArgumentException.class, () -> factory.setRequestTimeoutSeconds(0));
    assertThrows(IllegalArgumentException.class, () -> factory.setRequestTimeoutSeconds(-1));
    assertThrows(IllegalArgumentException.class,
        () -> new DeepSeek(baseUrl, "test-model", "test-key", true, 128, 0.6, 0.85,
            false, Duration.ZERO));
  }

  private CompletableFuture<String> stream(LLM model, AtomicReference<String> reasoning) {
    CompletableFuture<String> result = new CompletableFuture<>();
    model.streamFunctionChat("hello", List.of(new ModelMessage("user", "hello")), List.of(),
        false, new FunctionStreamHandler() {
          @Override
          public void onReasoningDelta(String text) {
            reasoning.set(text);
          }

          @Override
          public void onDirectReplyComplete(String reply) {
            result.complete(reply);
          }

          @Override
          public void onFailure(Throwable error) {
            result.completeExceptionally(error);
          }
        });
    return result;
  }

  private byte[] chunk(String delta) {
    return ("data: {\"id\":\"test\",\"created\":0,\"model\":\"test-model\","
        + "\"choices\":[{\"index\":0,\"delta\":{" + delta + "}}]}\n\n")
            .getBytes(StandardCharsets.UTF_8);
  }
}
