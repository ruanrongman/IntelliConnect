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
import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.rslly.iot.utility.ai.ModelMessage;
import top.rslly.iot.utility.ai.ModelMessageRole;
import top.rslly.iot.utility.ai.llm.LLM;
import top.rslly.iot.utility.ai.llm.LLMFactory;
import top.rslly.iot.utility.ai.prompts.SearchToolPrompt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Component
@Data
@Slf4j
public class SearchTool implements BaseTool<String> {
  @Autowired
  private SearchToolPrompt searchToolPrompt;
  @Value("${ai.searchTool-llm}")
  private String llmName;
  @Value("${ai.chromeDrive-path}")
  private String chromeDrivePath;
  @Value("${ai.chrome-path}")
  private String chromePath;
  private static ExecutorService executor = Executors.newFixedThreadPool(20);
  private String name = "searchTool";
  private String description = """
      Tools for online search
      Args: you want to search for online(str)
      """;

  public static WebDriver getDriver(String chromeDrivePath, String chromePath) {
    // 指定驱动路径
    System.setProperty("webdriver.chrome.driver", chromeDrivePath);
    System.setProperty("webdriver.chrome.silentOutput", "true");
    // 谷歌驱动
    ChromeOptions options = new ChromeOptions();
    // 允许所有请求
    options.addArguments("--remote-allow-origins=*");

    // 设置谷歌浏览器路径
    options.setBinary(chromePath);
    options.addArguments(
        "user-agent='Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36 Edg/135.0.0.0'");
    options.addArguments("--disable-gpu");
    options.addArguments("--headless");
    options.addArguments("--no-sandbox");
    options.addArguments("--disable-dev-shm-usage");
    options.addArguments("--disable-extensions");
    options.addArguments("--disable-images");
    options.addArguments("--silent");
    // options.setExperimentalOption("excludeSwitches", new String[]{"enable-logging"});

    return new ChromeDriver(options);
  }

  public static String fetchPage(String url, String chromeDrivePath, String chromePath) {
    WebDriver driver = getDriver(chromeDrivePath, chromePath);
    String text;
    try {
      driver.get(url);
      text = driver.findElement(By.tagName("body")).getText();
    } catch (Exception e) {
      text = "搜索失败";
    } finally {
      driver.quit();
    }
    return text;
  }

  public static List<Map<String, String>> getResult(String question, String chromeDrivePath,
      String chromePath) {
    List<Map<String, String>> results = new ArrayList<>();
    WebDriver driver = getDriver(chromeDrivePath, chromePath);
    driver.manage().timeouts().implicitlyWait(3, TimeUnit.SECONDS); // 隐式等待5秒
    // 最大化窗口
    driver.manage().window().maximize(); // 最大化窗口
    // 设置隐性等待时间
    // 启动需要打开的网页
    driver.get("https://cn.bing.com");
    var el = driver.findElement(By.id("sb_form_q"));
    el.sendKeys(question);
    el.sendKeys(Keys.ENTER);
    var searchResult = driver.findElements(By.className("b_algo"));
    int i = 0;
    for (var result : searchResult) {
      Map<String, String> resultMap = new HashMap<>();
      if (i != 0) {
        var titleElement = result.findElement(By.cssSelector("a"));
        String title = titleElement.getText();
        String link = result.findElement(By.cssSelector("a")).getAttribute("href");
        resultMap.put("title", title);
        resultMap.put("url", link);
        results.add(resultMap);
      }
      ++i;
      if (i == 6)
        break;
    }
    // System.out.println(resultMap);
    driver.quit();
    return results;
  }

  public static String process_search_result(String question, String chromeDrivePath,
      String chromePath) {
    var results = getResult(question, chromeDrivePath, chromePath);

    Map<CompletableFuture<String>, Map<String, String>> futureToUrl = new HashMap<>();
    JSONObject resultObject = new JSONObject();
    // 提交任务
    List<CompletableFuture<String>> futures = new ArrayList<>();
    for (Map<String, String> result : results) {
      try {
        CompletableFuture<String> future =
            CompletableFuture.supplyAsync(
                () -> fetchPage(result.get("url"), chromeDrivePath, chromePath), executor);
        futureToUrl.put(future, result);
        futures.add(future);
      } catch (Exception ignored) {

      }
    }

    // 等待所有任务完成
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

    // 处理结果
    for (CompletableFuture<String> future : futures) {
      try {
        Map<String, String> urlInfo = futureToUrl.get(future);
        String pageText = future.join(); // 或者使用 future.get()
        JSONObject jsonObject = new JSONObject();
        // System.out.printf("Title: %s\nURL: %s\nText: %s...\n",
        // urlInfo.get("title"), urlInfo.get("url"),
        // pageText.substring(0, Math.min(pageText.length(), 1000)));

        // 注意这里只存储一次即可
        jsonObject.put("link", urlInfo.get("url"));
        jsonObject.put("text", pageText.substring(0, Math.min(pageText.length(), 800)));
        resultObject.put("title:" + urlInfo.get("title"), jsonObject);
      } catch (Exception e) {
        Map<String, String> urlInfo = futureToUrl.get(future);
        System.out.printf("%s generated an exception: %s\n", urlInfo.get("url"), e.getMessage());
      }
    }

    executor.shutdown(); // 关闭线程池
    return resultObject.toJSONString();
  }

  @Override
  public String run(String question) {
    LLM llm = LLMFactory.getLLM(llmName);
    List<ModelMessage> messages = new ArrayList<>();
    try {
      ModelMessage systemMessage =
          new ModelMessage(ModelMessageRole.SYSTEM.value(),
              searchToolPrompt
                  .getSearchTool(process_search_result(question, chromeDrivePath, chromePath)));
      log.info("systemMessage: " + systemMessage.getContent());
      ModelMessage userMessage = new ModelMessage(ModelMessageRole.USER.value(), question);
      messages.add(systemMessage);
      messages.add(userMessage);

      // process_llm_result(obj, openid, appid);
      return llm.commonChat(question, messages, false);
    } catch (Exception e) {
      // e.printStackTrace();
      log.info("LLM error: " + e.getMessage());
      return "不好意思，查询失败，请重新尝试！";
    }
  }

  @Override
  public String run(String question, Map<String, Object> globalMessage) {
    return null;
  }
}
