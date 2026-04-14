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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.JsonValue;
import com.openai.core.http.StreamResponse;
import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam;
import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionFunctionTool;
import com.openai.models.chat.completions.ChatCompletionMessageParam;
import com.openai.models.chat.completions.ChatCompletionMessageToolCall;
import com.openai.models.chat.completions.ChatCompletionSystemMessageParam;
import com.openai.models.chat.completions.ChatCompletionTool;
import com.openai.models.chat.completions.ChatCompletionToolChoiceOption;
import com.openai.models.chat.completions.ChatCompletionUserMessageParam;
import com.openai.models.chat.completions.ChatCompletionContentPart;
import com.openai.models.chat.completions.ChatCompletionContentPartImage;
import com.openai.models.chat.completions.ChatCompletionContentPartText;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import top.rslly.iot.utility.ai.ModelMessage;
import top.rslly.iot.utility.ai.ModelMessageRole;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class DeepSeek implements LLM {
  private static final String DEFAULT_URL = "https://api.deepseek.com";
  private static final String DEFAULT_MODEL = "deepseek-chat";
  private static final String FALLBACK_MESSAGE =
      "抱歉，当前服务器繁忙";
  private static final String DONE_SIGNAL = "[DONE]";
  private static final ExecutorService STREAM_EXECUTOR =
      Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("deepseek-stream-", 0).factory());

  private final OpenAIClient client;
  private final String baseUrl;
  private final String model;

  public DeepSeek(String apiKey) {
    this(DEFAULT_URL, DEFAULT_MODEL, apiKey);
  }

  public DeepSeek(String url, String model, String apiKey) {
    this.baseUrl = normalizeBaseUrl(url);
    this.model = model;
    this.client = OpenAIOkHttpClient.builder()
        .apiKey(apiKey)
        .baseUrl(this.baseUrl)
        .timeout(Duration.ofSeconds(3000))
        .maxRetries(2)
        .build();
  }

  @Override
  public JSONObject jsonChat(String content, List<ModelMessage> messages, boolean search) {
    try {
      String response = extractText(client.chat().completions().create(buildTextParams(messages)));
      log.debug("model output:{} ", response);
      String temp = stripMarkdownFence(response);
      JSONObject jsonResponse = JSON.parseObject(temp);
      if (jsonResponse == null) {
        throw new IllegalStateException("json parse error");
      }
      return jsonResponse;
    } catch (Exception e) {
      log.error("model error", e);
      JSONObject action = new JSONObject();
      JSONObject answer = new JSONObject();
      answer.put("answer", FALLBACK_MESSAGE);
      action.put("action", answer);
      return action;
    }
  }

  @Override
  public String commonChat(String content, List<ModelMessage> messages, boolean search) {
    try {
      String response = extractText(client.chat().completions().create(buildTextParams(messages)));
      log.debug("model output:{} ", response);
      return response;
    } catch (Exception e) {
      log.error("model error", e);
      return FALLBACK_MESSAGE;
    }
  }

  @Override
  public boolean supportsFunctionRouting() {
    return true;
  }

  @Override
  public FunctionRouterResult functionRouterChat(String content, List<ModelMessage> messages,
      List<FunctionRouterToolSpec> toolSpecs) {
    try {
      ChatCompletion completion = client.chat().completions()
          .create(buildFunctionParams(messages, toolSpecs));
      return toFunctionRouterResult(completion);
    } catch (Exception e) {
      log.error("function router error", e);
      return FunctionRouterResult.error(FALLBACK_MESSAGE);
    }
  }

  @Override
  public void streamFunctionRouterChat(String content, List<ModelMessage> messages,
      List<FunctionRouterToolSpec> toolSpecs, FunctionRouterStreamHandler handler) {
    if (handler == null) {
      return;
    }
    handler.onOpen();
    STREAM_EXECUTOR.execute(() -> {
      StringBuilder replyBuilder = new StringBuilder();
      Map<Long, PartialToolCall> partialToolCalls = new LinkedHashMap<>();
      try (StreamResponse<ChatCompletionChunk> streamResponse =
          client.chat().completions().createStreaming(buildFunctionParams(messages, toolSpecs))) {
        var iterator = streamResponse.stream().iterator();
        while (iterator.hasNext()) {
          ChatCompletionChunk chunk = iterator.next();
          for (ChatCompletionChunk.Choice choice : chunk.choices()) {
            ChatCompletionChunk.Choice.Delta delta = choice.delta();
            delta.content().filter(text -> !text.isBlank()).ifPresent(text -> {
              replyBuilder.append(text);
              handler.onTextDelta(text);
            });
            delta.toolCalls()
                .ifPresent(toolCalls -> mergePartialToolCalls(partialToolCalls, toolCalls));
          }
        }
        if (!partialToolCalls.isEmpty()) {
          PartialToolCall toolCall = partialToolCalls.values().iterator().next();
          handler.onToolCall(toolCall.functionName.toString(), toolCall.arguments.toString());
        } else {
          handler.onDirectReplyComplete(replyBuilder.toString());
        }
      } catch (Exception e) {
        log.error("stream function router error", e);
        handler.onFailure(e);
      }
    });
  }

  @Override
  public void streamJsonChat(String content, List<ModelMessage> messages, boolean search,
      EventSourceListener listener) {
    LegacyEventSource eventSource = new LegacyEventSource(baseUrl + "/chat/completions");
    listener.onOpen(eventSource, null);
    STREAM_EXECUTOR.execute(() -> {
      try {
        ChatCompletionCreateParams params = buildTextParams(messages);
        try (StreamResponse<ChatCompletionChunk> streamResponse =
            client.chat().completions().createStreaming(params)) {
          eventSource.setStreamResponse(streamResponse);
          var iterator = streamResponse.stream().iterator();
          while (!eventSource.isCancelled() && iterator.hasNext()) {
            ChatCompletionChunk chunk = iterator.next();
            emitLegacyChunk(listener, eventSource, chunk);
          }
          if (!eventSource.isCancelled()) {
            listener.onEvent(eventSource, null, null, DONE_SIGNAL);
          }
          listener.onClosed(eventSource);
        }
      } catch (Exception e) {
        if (eventSource.isCancelled()) {
          listener.onClosed(eventSource);
          return;
        }
        log.error("model error", e);
        listener.onFailure(eventSource, e, null);
      }
    });
  }

  @Override
  public String imageToWord(String question, String url) {
    try {
      ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
          .model(model)
          .addMessage(ChatCompletionUserMessageParam.builder()
              .contentOfArrayOfContentParts(List.of(
                  ChatCompletionContentPart.ofText(ChatCompletionContentPartText.builder()
                      .text(question)
                      .type(JsonValue.from("text"))
                      .build()),
                  ChatCompletionContentPart.ofImageUrl(ChatCompletionContentPartImage.builder()
                      .type(JsonValue.from("image_url"))
                      .imageUrl(ChatCompletionContentPartImage.ImageUrl.builder()
                          .url(url)
                          .build())
                      .build())))
              .build())
          .build();
      String response = extractText(client.chat().completions().create(params));
      log.debug("model output:{} ", response);
      return response;
    } catch (Exception e) {
      log.error("model error", e);
      return FALLBACK_MESSAGE;
    }
  }

  private ChatCompletionCreateParams buildTextParams(List<ModelMessage> messages) {
    return ChatCompletionCreateParams.builder()
        .model(model)
        .messages(toMessageParams(messages))
        .build();
  }

  private ChatCompletionCreateParams buildFunctionParams(List<ModelMessage> messages,
      List<FunctionRouterToolSpec> toolSpecs) {
    ChatCompletionCreateParams.Builder builder = ChatCompletionCreateParams.builder()
        .model(model)
        .messages(toMessageParams(messages));
    if (toolSpecs != null && !toolSpecs.isEmpty()) {
      builder.toolChoice(ChatCompletionToolChoiceOption.Auto.AUTO)
          .parallelToolCalls(false);
      for (FunctionRouterToolSpec toolSpec : toolSpecs) {
        builder.addTool(buildFunctionTool(toolSpec));
      }
    }
    return builder
        .build();
  }

  private List<ChatCompletionMessageParam> toMessageParams(List<ModelMessage> messages) {
    List<ChatCompletionMessageParam> messageList = new ArrayList<>();
    for (ModelMessage message : messages) {
      if (message == null || message.getRole() == null || message.getContent() == null) {
        continue;
      }
      String role = message.getRole();
      String content = message.getContent().toString();
      if (ModelMessageRole.SYSTEM.value().equals(role)) {
        messageList.add(ChatCompletionMessageParam.ofSystem(
            ChatCompletionSystemMessageParam.builder().content(content).build()));
      } else if (ModelMessageRole.USER.value().equals(role)) {
        messageList.add(ChatCompletionMessageParam.ofUser(
            ChatCompletionUserMessageParam.builder().content(content).build()));
      } else if (ModelMessageRole.ASSISTANT.value().equals(role)) {
        messageList.add(ChatCompletionMessageParam.ofAssistant(
            ChatCompletionAssistantMessageParam.builder().content(content).build()));
      }
    }
    return messageList;
  }

  private String extractText(ChatCompletion completion) {
    return completion.choices().stream()
        .findFirst()
        .map(choice -> choice.message().content().orElse(""))
        .orElse("");
  }

  private FunctionRouterResult toFunctionRouterResult(ChatCompletion completion) {
    return completion.choices().stream()
        .findFirst()
        .map(choice -> {
          var toolCalls = choice.message().toolCalls().orElse(List.of());
          if (!toolCalls.isEmpty()) {
            return toToolCallResult(toolCalls.get(0));
          }
          return FunctionRouterResult.directReply(choice.message().content().orElse(""));
        })
        .orElse(FunctionRouterResult.error(FALLBACK_MESSAGE));
  }

  private FunctionRouterResult toToolCallResult(ChatCompletionMessageToolCall toolCall) {
    if (toolCall == null || !toolCall.isFunction()) {
      return FunctionRouterResult.error(FALLBACK_MESSAGE);
    }
    var functionToolCall = toolCall.asFunction();
    return FunctionRouterResult.toolCall(functionToolCall.function().name(),
        functionToolCall.function().arguments());
  }

  private ChatCompletionTool buildFunctionTool(FunctionRouterToolSpec toolSpec) {
    Map<String, Object> parameters =
        toolSpec.parameters() == null || toolSpec.parameters().isEmpty()
            ? FunctionRouterToolSpec.defaultArgsSchema()
            : toolSpec.parameters();
    return ChatCompletionTool.ofFunction(ChatCompletionFunctionTool.builder()
        .type(JsonValue.from("function"))
        .function(FunctionDefinition.builder()
            .name(toolSpec.name())
            .description(toolSpec.description())
            .parameters(FunctionParameters.builder()
                .putAllAdditionalProperties(toJsonValueMap(parameters))
                .build())
            .strict(toolSpec.strict())
            .build())
        .build());
  }

  private Map<String, JsonValue> toJsonValueMap(Map<String, Object> source) {
    Map<String, JsonValue> target = new LinkedHashMap<>();
    source.forEach((key, value) -> target.put(key, JsonValue.from(value)));
    return target;
  }

  private void mergePartialToolCalls(Map<Long, PartialToolCall> partialToolCalls,
      List<ChatCompletionChunk.Choice.Delta.ToolCall> toolCalls) {
    for (ChatCompletionChunk.Choice.Delta.ToolCall toolCall : toolCalls) {
      long index = toolCall.index();
      PartialToolCall partialToolCall =
          partialToolCalls.computeIfAbsent(index, ignored -> new PartialToolCall());
      toolCall.id().ifPresent(id -> partialToolCall.id = id);
      toolCall.function().ifPresent(function -> {
        function.name().ifPresent(partialToolCall.functionName::append);
        function.arguments().ifPresent(partialToolCall.arguments::append);
      });
    }
  }

  private void emitLegacyChunk(EventSourceListener listener, LegacyEventSource eventSource,
      ChatCompletionChunk chunk) {
    chunk.choices().stream()
        .map(choice -> choice.delta().content().orElse(null))
        .filter(Objects::nonNull)
        .filter(text -> !text.isBlank())
        .forEach(text -> listener.onEvent(eventSource, null, null, toLegacyChunk(text)));
  }

  private String toLegacyChunk(String content) {
    JSONObject root = new JSONObject();
    JSONArray choices = new JSONArray();
    JSONObject choice = new JSONObject();
    JSONObject delta = new JSONObject();
    delta.put("content", content);
    choice.put("delta", delta);
    choices.add(choice);
    root.put("choices", choices);
    return root.toJSONString();
  }

  private String stripMarkdownFence(String raw) {
    return raw.replace("```json", "").replace("```JSON", "").replace("```", "").trim();
  }

  private static String normalizeBaseUrl(String url) {
    String normalized = url == null || url.isBlank() ? DEFAULT_URL : url.trim();
    if (normalized.endsWith("/")) {
      normalized = normalized.substring(0, normalized.length() - 1);
    }
    if (normalized.endsWith("/v1") || normalized.endsWith("/beta")) {
      return normalized;
    }
    return normalized + "/v1";
  }

  private static final class LegacyEventSource implements EventSource {
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final AtomicReference<AutoCloseable> streamResponse = new AtomicReference<>();
    private final Request request;

    private LegacyEventSource(String url) {
      this.request = new Request.Builder().url(url).build();
    }

    @Override
    public Request request() {
      return request;
    }

    @Override
    public void cancel() {
      cancelled.set(true);
      AutoCloseable response = streamResponse.getAndSet(null);
      if (response != null) {
        try {
          response.close();
        } catch (Exception ignored) {
          // Ignore close failures during listener-driven cancellation.
        }
      }
    }

    private boolean isCancelled() {
      return cancelled.get();
    }

    private void setStreamResponse(AutoCloseable response) {
      if (cancelled.get()) {
        try {
          response.close();
        } catch (Exception ignored) {
          // Ignore close failures during a race with cancellation.
        }
        return;
      }
      streamResponse.set(response);
    }
  }

  private static final class PartialToolCall {
    private String id = "";
    private final StringBuilder functionName = new StringBuilder();
    private final StringBuilder arguments = new StringBuilder();
  }
}
