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
package top.rslly.iot.utility.ai.chain;


import com.zhipu.oapi.service.v4.model.ChatMessageRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.rslly.iot.services.UserConfigServiceImpl;
import top.rslly.iot.services.agent.ProductToolsBanService;
import top.rslly.iot.services.agent.ProductToolsBanServiceImpl;
import top.rslly.iot.services.knowledgeGraphic.KnowledgeGraphicServiceImpl;
import top.rslly.iot.services.wechat.WxUserServiceImpl;
import top.rslly.iot.utility.Cast;
import top.rslly.iot.utility.RedisUtil;
import top.rslly.iot.utility.ai.ModelMessage;
import top.rslly.iot.utility.ai.llm.FunctionRouterResult;
import top.rslly.iot.utility.ai.mcp.McpAgent;
import top.rslly.iot.utility.ai.toolAgent.Agent;
import top.rslly.iot.utility.ai.tools.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Slf4j
public class Router {
  @Autowired
  private ClassifierTool classifierTool;
  @Autowired
  private MusicTool musicTool;
  @Autowired
  private ControlTool controlTool;
  @Autowired
  private ChatTool chatTool;
  @Autowired
  private WeatherTool weatherTool;
  @Autowired
  private WxBoundProductTool wxBoundProductTool;
  @Autowired
  private RedisUtil redisUtil;
  @Autowired
  private Agent agent;
  @Autowired
  private WxUserServiceImpl wxUserService;
  @Autowired
  private WxProductActiveTool wxProductActiveTool;
  @Autowired
  private ScheduleTool scheduleTool;
  @Autowired
  private ProductRoleTool productRoleTool;
  @Autowired
  private McpAgent mcpAgent;
  @Autowired
  private MemoryTool memoryTool;
  @Autowired
  private GoodByeTool goodByeTool;
  @Autowired
  private ProductToolsBanServiceImpl productToolsBanService;
  @Autowired
  private LongMemoryTool longMemoryTool;
  @Autowired
  private KnowledgeGraphicTool knowledgeGraphicTool;
  @Autowired
  private KnowledgeGraphicServiceImpl knowledgeGraphicService;
  @Autowired
  private UserConfigServiceImpl userConfigService;
  @Autowired
  private FunctionCallingRouterTool functionCallingRouterTool;
  @Value("${ai.branch-prediction.enabled:true}")
  private boolean branchPredictionEnabled;
  @Value("${ai.router.mode:prompt}")
  private String routerMode;
  public static final Map<String, Queue<String>> queueMap = new ConcurrentHashMap<>();

  // chatId in WeChat module need to use openid
  public String response(String content, String chatId, int productId, String... dataArgs) {
    List<ModelMessage> memory;
    String answer;
    String toolResult = "";
    Map<String, Object> globalMessage = new HashMap<>();
    globalMessage.put("productId", productId);
    globalMessage.put("chatId", chatId);
    globalMessage.put("queueMap", queueMap);
    // 从 WebSocket session 获取客户端 IP
    jakarta.websocket.Session session = top.rslly.iot.utility.smartVoice.XiaoZhiWebsocket.clients
        .get(chatId);
    if (session != null) {
      String clientIp = top.rslly.iot.utility.smartVoice.XiaoZhiWebsocket.getClientIp(session);
      if (clientIp != null && !clientIp.equals("unknown")) {
        globalMessage.put("clientIp", clientIp);
      }
    }
    Queue<String> queue = createResponseQueue();
    queueMap.put(chatId, queue);
    Object memory_cache;
    if (dataArgs.length > 0 && !dataArgs[0].equals("false")) {
      globalMessage.put("openId", chatId);
      globalMessage.put("microappid", dataArgs[0]);
      chatId = dataArgs[0] + chatId;
      queueMap.put(chatId, queue);
      globalMessage.put("chatId", chatId);
    }
    memory_cache = redisUtil.get("memory" + chatId);
    if (memory_cache != null)
      try {
        memory = Cast.castList(memory_cache, ModelMessage.class);
      } catch (Exception e) {
        e.printStackTrace();
        memory = new ArrayList<>();
      }
    else
      memory = new ArrayList<>();
    globalMessage.put("memory", memory);
    if (isFunctionRouterMode()) {
      RouteExecutionResult routeExecutionResult =
          resolveFunctionRoute(content, globalMessage, productId, chatId, dataArgs);
      answer = routeExecutionResult.answer();
      toolResult = routeExecutionResult.toolResult();
    } else {
      // ========== 分支预测启动 ==========
      final AtomicReference<String> predictionResult = new AtomicReference<>(null);
      final CountDownLatch predictionLatch = new CountDownLatch(1);
      final Queue<String> predictionQueue = new ConcurrentLinkedQueue<>();
      final AtomicBoolean predictionCancelled = new AtomicBoolean(false);
      Thread predictionThread = null;

      if (branchPredictionEnabled) {
        final String predictionContent = content;
        final Map<String, Object> predictionGlobalMessage = new HashMap<>(globalMessage);
        final Map<String, Queue<String>> predictionQueueMap = new ConcurrentHashMap<>();
        final String predictionChatId = chatId + "_prediction";
        predictionQueueMap.put(predictionChatId, predictionQueue);
        predictionGlobalMessage.put("queueMap", predictionQueueMap);
        predictionGlobalMessage.put("chatId", predictionChatId);
        // predictionGlobalMessage.put("memory", new ArrayList<>(memory));

        predictionThread = Thread.ofVirtual().start(() -> {
          try {
            if (predictionCancelled.get())
              return;
            String result = chatTool.run(predictionContent, predictionGlobalMessage);
            if (!predictionCancelled.get()) {
              predictionResult.set(result);
            }
            log.debug("[BranchPrediction] completed, chatId={}", predictionChatId);
          } catch (Exception e) {
            log.warn("[BranchPrediction] failed, chatId={}", predictionChatId, e);
          } finally {
            predictionLatch.countDown();
          }
        });
      }
      // ========== 分支预测启动 END ==========
      var resultMap = classifierTool.run(content, globalMessage);
      String args;
      Object argsObject = resultMap.get("args");
      if (argsObject != null) {
        args = argsObject.toString();
      } else {
        args = "";
      }
      List<String> banTools = productToolsBanService.getProductToolsBanList(productId);
      if (resultMap.containsKey("value") && !args.equals("")) {
        List<String> value = Cast.castList(resultMap.get("value"), String.class);
        if (!value.isEmpty()) {
          if (banTools.contains(value.get(0))) {
            value.set(0, "5");
          }
          switch (value.get(0)) {
            case "1" -> {
              predictionCancelled.set(true);
              predictionQueue.clear();
              toolResult = weatherTool.run(args, globalMessage);
              answer = ToolPrefix.WEATHER.getPrefix() + toolResult;
            }
            case "2" -> {
              predictionCancelled.set(true);
              predictionQueue.clear();
              toolResult = controlTool.run(args, globalMessage);
              answer = ToolPrefix.CONTROL.getPrefix() + toolResult;
            }
            case "3" -> {
              predictionCancelled.set(true);
              predictionQueue.clear();
              var musicMap = musicTool.run(args, globalMessage);
              toolResult = musicMap.get("answer");
              answer = ToolPrefix.MUSIC.getPrefix() + toolResult + musicMap.get("url");
            }
            case "4" -> {
              predictionCancelled.set(true);
              predictionQueue.clear();
              toolResult = agent.run(content, globalMessage);
              answer = ToolPrefix.AGENT.getPrefix() + toolResult;
            }
            case "5" -> {
              answer = resolveChatToolAnswer(content, globalMessage,
                  predictionThread, predictionResult, predictionCancelled,
                  predictionLatch, queue, predictionQueue, chatId);
            }
            case "6" -> {
              predictionCancelled.set(true);
              predictionQueue.clear();
              toolResult = wxBoundProductTool.run(args, globalMessage);
              answer = ToolPrefix.WX_BOUND_PRODUCT.getPrefix() + toolResult;
            }
            case "7" -> {
              predictionCancelled.set(true);
              predictionQueue.clear();
              toolResult = wxProductActiveTool.run(args, globalMessage);
              answer = ToolPrefix.WX_PRODUCT_ACTIVE.getPrefix() + toolResult;
            }
            case "8" -> {
              predictionCancelled.set(true);
              predictionQueue.clear();
              if (dataArgs.length > 0 && dataArgs[0].equals("false"))
                toolResult = "定时任务禁止递归调用!!!";
              else {
                toolResult = scheduleTool.run(args, globalMessage);
              }
              answer = ToolPrefix.SCHEDULE.getPrefix() + toolResult;
            }
            case "9" -> {
              predictionCancelled.set(true);
              predictionQueue.clear();
              toolResult = productRoleTool.run(args, globalMessage);
              answer = ToolPrefix.PRODUCT_ROLE.getPrefix() + toolResult;
            }
            case "10" -> {
              predictionCancelled.set(true);
              predictionQueue.clear();
              toolResult = mcpAgent.run(args, globalMessage);
              answer = ToolPrefix.MCP_AGENT.getPrefix() + toolResult;
            }
            case "11" -> {
              predictionCancelled.set(true);
              predictionQueue.clear();
              toolResult = goodByeTool.run(args, globalMessage);
              answer = ToolPrefix.GOODBYE.getPrefix() + toolResult;
            }
            default -> {
              answer = resolveChatToolAnswer(content, globalMessage,
                  predictionThread, predictionResult, predictionCancelled,
                  predictionLatch, queue, predictionQueue, chatId);
            }
          }
        } else {
          answer = resolveChatToolAnswer(content, globalMessage,
              predictionThread, predictionResult, predictionCancelled,
              predictionLatch, queue, predictionQueue, chatId);
        }
      } else {
        answer = resolveChatToolAnswer(content, globalMessage,
            predictionThread, predictionResult, predictionCancelled,
            predictionLatch, queue, predictionQueue, chatId);
      }
    }
    if (toolResult == null || toolResult.equals(""))
      toolResult = answer;
    ModelMessage userContent = new ModelMessage(ChatMessageRole.USER.value(), content);
    ModelMessage chatMessage = new ModelMessage(ChatMessageRole.ASSISTANT.value(), toolResult);
    memory.add(userContent);
    memory.add(chatMessage);
    // System.out.println(memory.size());
    // slide memory window
    memoryTool.run(content, globalMessage);
    if (memory.size() > 6) {
      longMemoryTool.run(content, globalMessage);
      memory.subList(0, memory.size() - 6).clear();
      // if (!banTools.contains("knowledgeGraphic")) {
      // knowledgeGraphicTool.run(content, globalMessage);
      // if (!banTools.contains("clearKnowledgeGraphicNode")) {
      // knowledgeGraphicService.clearNode(productId);
      // }
      // }
      if (userConfigService.getConfigValue(productId, "knowledge_graph.toggle").equals("true")) {
        knowledgeGraphicTool.run(content, globalMessage);
      }
      if (userConfigService.getConfigValue(productId, "knowledge_graph.forget.toggle")
          .equals("true")) {
        knowledgeGraphicService.clearNode(productId);
      }
    }
    redisUtil.set("memory" + chatId, memory, 24 * 3600);
    return answer;
  }

  static Queue<String> createResponseQueue() {
    return new ConcurrentLinkedQueue<>();
  }

  private boolean isFunctionRouterMode() {
    return "function".equalsIgnoreCase(routerMode);
  }

  private RouteExecutionResult resolveFunctionRoute(String content,
      Map<String, Object> globalMessage,
      int productId, String chatId, String... dataArgs) {
    FunctionRouterResult result = functionCallingRouterTool.run(content, globalMessage);
    if (result.isToolCall()) {
      Optional<FunctionRouterRoute> route =
          FunctionRouterRoute.fromFunctionName(result.getFunctionName());
      if (route.isPresent()) {
        return executeToolRoute(route.get().getTaskId(), content, result.getArguments(),
            globalMessage, dataArgs);
      }
      log.warn("Unknown function router target {}, fallback to legacy router, chatId={}",
          result.getFunctionName(), chatId);
    } else if (result.isDirectReply()) {
      return new RouteExecutionResult(result.getReply(), result.getReply());
    } else if (result.isUnsupported()) {
      log.debug("Function router unsupported for current LLM, fallback to legacy router, chatId={}",
          chatId);
    } else {
      log.warn("Function router failed, fallback to legacy router, chatId={}", chatId);
    }

    var resultMap = classifierTool.run(content, globalMessage);
    String args = resultMap.get("args") == null ? "" : resultMap.get("args").toString();
    List<String> value = Cast.castList(resultMap.get("value"), String.class);
    if (value == null || value.isEmpty()) {
      String directReply = chatTool.run(content, globalMessage);
      return new RouteExecutionResult(directReply, directReply);
    }
    return executeToolRoute(value.get(0), content, args, globalMessage, dataArgs);
  }

  private RouteExecutionResult executeToolRoute(String routeId, String content, String args,
      Map<String, Object> globalMessage, String... dataArgs) {
    String answer;
    String toolResult;
    switch (routeId) {
      case "1" -> {
        toolResult = weatherTool.run(args, globalMessage);
        answer = ToolPrefix.WEATHER.getPrefix() + toolResult;
      }
      case "2" -> {
        toolResult = controlTool.run(args, globalMessage);
        answer = ToolPrefix.CONTROL.getPrefix() + toolResult;
      }
      case "3" -> {
        var musicMap = musicTool.run(args, globalMessage);
        toolResult = musicMap.get("answer");
        answer = ToolPrefix.MUSIC.getPrefix() + toolResult + musicMap.get("url");
      }
      case "4" -> {
        toolResult = agent.run(content, globalMessage);
        answer = ToolPrefix.AGENT.getPrefix() + toolResult;
      }
      case "6" -> {
        toolResult = wxBoundProductTool.run(args, globalMessage);
        answer = ToolPrefix.WX_BOUND_PRODUCT.getPrefix() + toolResult;
      }
      case "7" -> {
        toolResult = wxProductActiveTool.run(args, globalMessage);
        answer = ToolPrefix.WX_PRODUCT_ACTIVE.getPrefix() + toolResult;
      }
      case "8" -> {
        if (dataArgs.length > 0 && dataArgs[0].equals("false")) {
          toolResult = "瀹氭椂浠诲姟绂佹閫掑綊璋冪敤!!!";
        } else {
          toolResult = scheduleTool.run(args, globalMessage);
        }
        answer = ToolPrefix.SCHEDULE.getPrefix() + toolResult;
      }
      case "9" -> {
        toolResult = productRoleTool.run(args, globalMessage);
        answer = ToolPrefix.PRODUCT_ROLE.getPrefix() + toolResult;
      }
      case "10" -> {
        toolResult = mcpAgent.run(args, globalMessage);
        answer = ToolPrefix.MCP_AGENT.getPrefix() + toolResult;
      }
      case "11" -> {
        toolResult = goodByeTool.run(args, globalMessage);
        answer = ToolPrefix.GOODBYE.getPrefix() + toolResult;
      }
      default -> {
        answer = chatTool.run(content, globalMessage);
        toolResult = answer;
      }
    }
    return new RouteExecutionResult(answer, toolResult);
  }

  private String resolveChatToolAnswer(
      String content,
      Map<String, Object> globalMessage,
      Thread predictionThread,
      AtomicReference<String> predictionResult,
      AtomicBoolean predictionCancelled,
      CountDownLatch predictionLatch,
      Queue<String> mainQueue,
      Queue<String> predictionQueue,
      String chatId) {

    if (!branchPredictionEnabled || predictionThread == null) {
      return chatTool.run(content, globalMessage);
    }

    // ✅ 修复1：只要预测线程还活着(或已有结果)，就认为HIT，不再看队列是否为空
    if (predictionThread.isAlive() || predictionResult.get() != null) {
      log.debug("[BranchPrediction] HIT, chatId={}", chatId);

      Thread.ofVirtual().start(() -> {
        try {
          while (predictionThread.isAlive() || !predictionQueue.isEmpty()) {
            if (predictionCancelled.get()) {
              predictionQueue.clear();
              break;
            }
            String token = predictionQueue.poll();
            if (token != null) {
              mainQueue.offer(token);
            } else {
              // ✅ 修复2：线程还活着但队列暂时为空，yield等待新token
              Thread.yield();
            }
          }
        } catch (Exception e) {
          predictionQueue.clear();
          Thread.currentThread().interrupt();
        }
      });

      try {
        predictionLatch.await(100, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.warn("[BranchPrediction] interrupted, chatId={}", chatId);
      }

      String result = predictionResult.get();
      if (result != null) {
        return result;
      }

      log.warn("[BranchPrediction] null result, chatId={}", chatId);
      predictionCancelled.set(true);
      predictionQueue.clear();
      return "";
    }

    // MISS：预测线程已死且无结果（异常退出）
    log.debug("[BranchPrediction] MISS, chatId={}", chatId);
    predictionCancelled.set(true);
    predictionQueue.clear();
    return chatTool.run(content, globalMessage);
  }

  private record RouteExecutionResult(String answer, String toolResult) {}
}
