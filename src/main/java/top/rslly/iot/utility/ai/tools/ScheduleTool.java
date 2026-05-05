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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronExpression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.rslly.iot.models.TimeScheduleEntity;
import top.rslly.iot.services.agent.TimeScheduleServiceImpl;
import top.rslly.iot.services.wechat.WxProductActiveServiceImpl;
import top.rslly.iot.utility.QuartzCronDateUtils;
import top.rslly.iot.utility.QuartzManager;
import top.rslly.iot.utility.ai.IcAiException;
import top.rslly.iot.utility.ai.LlmDiyUtility;
import top.rslly.iot.utility.ai.ModelMessage;
import top.rslly.iot.utility.ai.ModelMessageRole;
import top.rslly.iot.utility.ai.llm.LLM;
import top.rslly.iot.utility.ai.prompts.ScheduleToolPrompt;
import top.rslly.iot.utility.ai.toolAgent.AgentEventSourceListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Data
@Slf4j
public class ScheduleTool implements BaseTool<String> {
  @Autowired
  private ScheduleToolPrompt scheduleToolPrompt;
  @Autowired
  private WxProductActiveServiceImpl wxProductActiveService;
  @Autowired
  private LlmDiyUtility llmDiyUtility;
  @Value("${ai.scheduleTool-llm}")
  private String llmName;
  @Value("${ai.scheduleTool-llm-speed:true}")
  private boolean speedUp;
  private String name = "scheduleTool";
  private String description = """
      Used for schedule management and reminder tasks, you can query, cancel, and set schedules.
      Args: User's schedule management needs.(str)
      """;
  @Autowired
  private TimeScheduleServiceImpl timeScheduleService;
  // 添加锁和条件变量映射
  private final Map<String, Lock> lockMap = new ConcurrentHashMap<>();
  private final Map<String, Condition> conditionMap = new ConcurrentHashMap<>();
  private final Map<String, String> dataMap = new ConcurrentHashMap<>();

  @Override
  public String run(String question) {
    return null;
  }

  @Override
  public String run(String question, Map<String, Object> globalMessage) {
    int productId = (int) globalMessage.get("productId");
    String openid = (String) globalMessage.get("openId");
    String chatId = (String) globalMessage.get("chatId");
    String appid = (String) globalMessage.get("microappid");
    Map<String, Queue<String>> queueMap =
        (Map<String, Queue<String>>) globalMessage.get("queueMap");
    var queue = queueMap.get(chatId);
    List<ModelMessage> messages = new ArrayList<>();
    boolean mcpIsTool = false;
    if (globalMessage.containsKey("mcpIsTool"))
      mcpIsTool = (boolean) globalMessage.get("mcpIsTool");

    // 只有在speedUp模式下才初始化锁和条件变量
    if (speedUp && !mcpIsTool) {
      lockMap.putIfAbsent(chatId, new ReentrantLock());
      conditionMap.putIfAbsent(chatId, lockMap.get(chatId).newCondition());
    }
    if (queue != null && !mcpIsTool) {
      queue.add(ToolPrefix.getToolCallPrefix(name));
    }

    ModelMessage systemMessage =
        new ModelMessage(ModelMessageRole.SYSTEM.value(),
            scheduleToolPrompt.getScheduleTool(appid, openid));
    ModelMessage userMessage = new ModelMessage(ModelMessageRole.USER.value(), question);
    messages.add(systemMessage);
    messages.add(userMessage);

    var finalObj = callLLMForThought(question, messages, queueMap, chatId, mcpIsTool, productId);
    if (finalObj == null) {
      cleanupResources(chatId);
      return "小主人抱歉哦，服务器现在繁忙。";
    }
    var obj = finalObj.getJSONObject("action");
    String answer = (String) obj.get("answer");
    JSONArray taskParameters = obj.getJSONArray("taskParameters");
    try {
      for (Object taskParameter : taskParameters) {
        process_llm_result((JSONObject) taskParameter, openid, appid, chatId, productId);
      }
      if (queue != null && !mcpIsTool) {
        queue.add("[DONE]");
      }
      cleanupResources(chatId);
      return answer;
    } catch (Exception e) {
      log.info("LLM error: " + e.getMessage());
      cleanupResources(chatId);
      if (appid != null && openid != null) {
        return "不好意思，提醒任务设置失败了，请检查是否输入时间小于20秒或者没有表达清楚请求。";
      } else
        return "不好意思，提醒任务设置失败了，请检查是否没有绑定产品，输入时间小于20秒或者没有表达清楚请求。";
    }
  }

  /**
   * 调用LLM获取thought，支持流式和非流式
   */
  private JSONObject callLLMForThought(String question, List<ModelMessage> messages,
      Map<String, Queue<String>> queueMap, String chatId, Boolean mcpIsTool, int productId) {
    LLM llm = llmDiyUtility.getDiyLlm(productId, llmName, "8");
    if (speedUp && !mcpIsTool) {
      // 使用流式调用，实时获取thought内容
      dataMap.remove(chatId);

      try {
        llm.streamJsonChat(question, messages, true,
            new AgentEventSourceListener(queueMap, chatId, this, "answer"));

        Lock chatLock = lockMap.get(chatId);
        Condition chatCondition = conditionMap.get(chatId);

        chatLock.lock();
        try {
          // 等待流式响应完成
          while (dataMap.get(chatId) == null) {
            chatCondition.await();
          }
        } catch (InterruptedException e) {
          log.error("等待LLM响应时被中断: {}", e.getMessage());
          Thread.currentThread().interrupt();
          return null;
        } finally {
          chatLock.unlock();
        }

        String data = dataMap.get(chatId);
        if (data == null || data.isEmpty()) {
          log.error("LLM返回空数据");
          return null;
        }

        try {
          return JSON.parseObject(data.replace("```json", "")
              .replace("```JSON", "").replace("```", ""));
        } catch (Exception e) {
          log.error("解析LLM响应JSON失败: {}, data: {}", e.getMessage(), data);
          return null;
        }
      } catch (Exception e) {
        log.error("流式调用LLM失败: {}", e.getMessage());
        return null;
      }
    } else {
      // 非流式调用
      try {
        return llm.jsonChat(question, messages, true);
      } catch (Exception e) {
        log.error("调用LLM失败: {}", e.getMessage());
        return null;
      }
    }
  }

  /**
   * 清理资源（锁、条件变量等）
   */
  private void cleanupResources(String chatId) {
    dataMap.remove(chatId);
    conditionMap.remove(chatId);
    lockMap.remove(chatId);
  }

  public void process_llm_result(JSONObject jsonObject, String openid, String appid, String chatId,
      int productId)
      throws IcAiException, ParseException {

    if (jsonObject.get("code").equals("200") || jsonObject.get("code").equals(200)) {
      // 如果appid和openid不为空就直接执行，否则从wxProductActiveEntityList 循环获取
      if (appid != null && openid != null) {
        executeScheduleTask(jsonObject, openid, appid, chatId, productId);
      } else {
        var wxProductActiveEntityList = wxProductActiveService.findAllByProductId(productId);
        if (wxProductActiveEntityList.isEmpty()) {
          throw new IcAiException("您还没有绑定产品，请先绑定设备后再设置日程提醒");
        }
        for (var entity : wxProductActiveEntityList) {
          String entityAppid = entity.getAppid();
          String entityOpenid = entity.getOpenid();
          executeScheduleTask(jsonObject, entityOpenid, entityAppid, chatId, productId);
        }
      }
    } else
      throw new IcAiException("llm response error");
  }

  private void executeScheduleTask(JSONObject jsonObject, String openid, String appid,
      String chatId, int productId) throws IcAiException, ParseException {
    String groupName = appid + ":" + openid;
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    String repeat = jsonObject.getString("repeat");
    String taskName = jsonObject.getString("task_name");
    String taskType = jsonObject.getString("taskType");
    String exec = jsonObject.getString("exec");
    String execCommand = jsonObject.getString("exec_command");
    String cron = "";
    if (taskType.equals("set")) {
      String timeStr = jsonObject.getString("time");
      if (repeat.equals("true")) {
        cron = jsonObject.getString("cron");
      } else {
        var time = formatter.parse(timeStr);
        if (time.before(new java.util.Date()))
          throw new IcAiException("time is before now");
        if (time.getTime() - new java.util.Date().getTime() < 20 * 1000)
          throw new IcAiException("time is too near");
        cron = QuartzCronDateUtils.getCron(time);
      }
      if (!exec.equals("true") && !exec.equals("false")) {
        throw new IcAiException("exec is not true or false");
      }
      if (!isValidCronExpression(cron)) {
        throw new IcAiException("cron error");
      }
      // String uuid = UUID.randomUUID().toString();
      if (timeScheduleService.findAllByAppidAndOpenidAndTaskName(appid, openid, taskName)
          .isEmpty()) {
        TimeScheduleEntity timeScheduleEntity = new TimeScheduleEntity();
        timeScheduleEntity.setOpenid(openid);
        timeScheduleEntity.setTaskName(taskName);
        timeScheduleEntity.setCron(cron);
        timeScheduleEntity.setAppid(appid);
        timeScheduleEntity.setProductId(productId);
        timeScheduleEntity.setExec(exec);
        if (execCommand == null) {
          execCommand = "";
        }
        if (execCommand.length() > 254) {
          execCommand = execCommand.substring(0, 254);
        }
        timeScheduleEntity.setExecCommand(execCommand);
        timeScheduleService.insert(timeScheduleEntity);
        QuartzManager.addJob(taskName, groupName, taskName, groupName, RemindJob.class, cron,
            openid, appid, exec, chatId, productId, execCommand);
      } else
        throw new IcAiException("task name is duplicate");
    } else if (taskType.equals("cancel")) {
      timeScheduleService.deleteByAppidAndOpenidAndTaskName(appid, openid, taskName);
      QuartzManager.removeJob(taskName, groupName, taskName, groupName);
    }
    log.info("obj:{}", jsonObject);
    log.info("time cron{}", cron);
  }

  private boolean isValidCronExpression(String cron) {
    try {
      return CronExpression.isValidExpression(cron);
    } catch (Exception e) {
      return false;
    }
  }
}
