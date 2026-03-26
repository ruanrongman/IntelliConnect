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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.rslly.iot.utility.HttpRequestUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 和风天气服务类 支持JWT认证
 */
@Data
@Component
@Slf4j
public class QWeatherService {

  @Value("${weather.qweather.enabled:false}")
  private boolean enabled;

  @Value("${weather.qweather.api-host:api.qweather.com}")
  private String apiHost;

  @Value("${weather.qweather.project-id:}")
  private String projectId;

  @Value("${weather.qweather.key-id:}")
  private String keyId;

  @Value("${weather.qweather.private-key:}")
  private String privateKey;

  @Autowired
  private HttpRequestUtils httpRequestUtils;

  // JWT Token缓存
  private String cachedToken;
  private long tokenExpireTime;

  /**
   * 生成JWT Token
   */
  public String generateToken() {
    try {
      // 解码私钥
      byte[] privateKeyBytes = Base64.getDecoder().decode(privateKey);
      PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
      KeyFactory keyFactory = KeyFactory.getInstance("EdDSA");
      PrivateKey pk = keyFactory.generatePrivate(keySpec);

      // Header
      String headerJson = "{\"alg\": \"EdDSA\", \"kid\": \"" + keyId + "\"}";

      // Payload
      long iat = ZonedDateTime.now(ZoneOffset.UTC).toEpochSecond() - 30;
      long exp = iat + 900; // 15分钟有效期

      String payloadJson =
          "{\"sub\": \"" + projectId + "\", \"iat\": " + iat + ", \"exp\": " + exp + "}";

      // Base64url header+payload
      String headerEncoded =
          Base64.getUrlEncoder().encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
      String payloadEncoded =
          Base64.getUrlEncoder().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
      String data = headerEncoded + "." + payloadEncoded;

      // Sign
      Signature signer = Signature.getInstance("EdDSA");
      signer.initSign(pk);
      signer.update(data.getBytes(StandardCharsets.UTF_8));
      byte[] signature = signer.sign();

      String signatureEncoded = Base64.getUrlEncoder().encodeToString(signature);

      // 缓存token
      cachedToken = data + "." + signatureEncoded;
      tokenExpireTime = exp * 1000; // 转换为毫秒

      return cachedToken;
    } catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException
        | SignatureException e) {
      log.error("Failed to generate QWeather JWT token: {}", e.getMessage());
      return null;
    }
  }

  /**
   * 获取有效的JWT Token（带缓存）
   */
  public String getValidToken() {
    long now = System.currentTimeMillis();
    // 如果token不存在或即将过期（提前60秒刷新）
    if (cachedToken == null || tokenExpireTime - now < 60000) {
      return generateToken();
    }
    return cachedToken;
  }

  /**
   * 获取城市LocationID 通过经纬度查询
   */
  public String getLocationId(double longitude, double latitude) throws IOException {
    String token = getValidToken();
    if (token == null) {
      return null;
    }

    String url = "https://" + apiHost + "/v2/city/lookup?location="
        + String.format("%.2f,%.2f", longitude, latitude);

    Map<String, String> headers = new HashMap<>();
    headers.put("Authorization", "Bearer " + token);

    var response = httpRequestUtils.httpGet(url, headers);
    if (response.isSuccessful()) {
      String body = Objects.requireNonNull(response.body()).string();
      JSONObject json = JSON.parseObject(body);
      if ("200".equals(json.getString("code"))) {
        JSONArray locations = json.getJSONArray("location");
        if (locations != null && !locations.isEmpty()) {
          return locations.getJSONObject(0).getString("id");
        }
      }
    }
    return null;
  }

  /**
   * 获取城市LocationID 通过城市名称查询
   */
  public String getLocationIdByCityName(String cityName) throws IOException {
    String token = getValidToken();
    if (token == null) {
      return null;
    }

    String url = "https://" + apiHost + "/geo/v2/city/lookup?location=" + cityName;

    Map<String, String> headers = new HashMap<>();
    headers.put("Authorization", "Bearer " + token);

    var response = httpRequestUtils.httpGet(url, headers);
    if (response.isSuccessful()) {
      String body = Objects.requireNonNull(response.body()).string();
      JSONObject json = JSON.parseObject(body);
      if ("200".equals(json.getString("code"))) {
        JSONArray locations = json.getJSONArray("location");
        if (locations != null && !locations.isEmpty()) {
          return locations.getJSONObject(0).getString("id");
        }
      }
    }
    return null;
  }

  /**
   * 获取实时天气
   */
  public JSONObject getWeatherNow(String locationId) throws IOException {
    String token = getValidToken();
    if (token == null) {
      return null;
    }

    String url = "https://" + apiHost + "/v7/weather/now?location=" + locationId;

    Map<String, String> headers = new HashMap<>();
    headers.put("Authorization", "Bearer " + token);

    var response = httpRequestUtils.httpGet(url, headers);
    if (response.isSuccessful()) {
      String body = Objects.requireNonNull(response.body()).string();
      return JSON.parseObject(body);
    }
    return null;
  }

  /**
   * 获取天气预报
   * 
   * @param locationId 城市ID
   * @param days 天数: 3d, 7d, 10d, 15d, 30d
   */
  public JSONObject getWeatherForecast(String locationId, String days) throws IOException {
    String token = getValidToken();
    if (token == null) {
      return null;
    }

    String url = "https://" + apiHost + "/v7/weather/" + days + "?location=" + locationId;

    Map<String, String> headers = new HashMap<>();
    headers.put("Authorization", "Bearer " + token);

    var response = httpRequestUtils.httpGet(url, headers);
    if (response.isSuccessful()) {
      String body = Objects.requireNonNull(response.body()).string();
      return JSON.parseObject(body);
    }
    return null;
  }

  /**
   * 将和风天气实时数据转换为统一格式
   */
  public String convertNowToLivesFormat(JSONObject nowData) {
    if (nowData == null || !"200".equals(nowData.getString("code"))) {
      return "[]";
    }

    JSONObject now = nowData.getJSONObject("now");
    if (now == null) {
      return "[]";
    }

    // 转换为类似高德天气的格式
    JSONObject livesItem = new JSONObject();
    livesItem.put("weather", now.getString("text")); // 天气状况
    livesItem.put("temperature", now.getString("temp")); // 温度
    livesItem.put("winddirection", now.getString("windDir")); // 风向
    livesItem.put("windpower", now.getString("windScale")); // 风力等级
    livesItem.put("humidity", now.getString("humidity")); // 湿度
    livesItem.put("pressure", now.getString("pressure")); // 气压
    livesItem.put("visibility", now.getString("vis")); // 能见度
    livesItem.put("feelsLike", now.getString("feelsLike")); // 体感温度
    livesItem.put("precip", now.getString("precip")); // 降水量
    livesItem.put("cloud", now.getString("cloud")); // 云量
    livesItem.put("obsTime", now.getString("obsTime")); // 观测时间

    JSONArray livesArray = new JSONArray();
    livesArray.add(livesItem);
    return livesArray.toString();
  }

  /**
   * 将和风天气预报数据转换为统一格式
   */
  public String convertForecastToFormat(JSONObject forecastData) {
    if (forecastData == null || !"200".equals(forecastData.getString("code"))) {
      return "[]";
    }

    JSONArray daily = forecastData.getJSONArray("daily");
    if (daily == null) {
      return "[]";
    }

    // 转换为类似高德天气的格式
    JSONArray forecastsArray = new JSONArray();
    JSONObject forecastItem = new JSONObject();
    JSONArray castsArray = new JSONArray();

    for (int i = 0; i < daily.size(); i++) {
      JSONObject day = daily.getJSONObject(i);
      JSONObject cast = new JSONObject();
      cast.put("date", day.getString("fxDate")); // 日期
      cast.put("week", ""); // 高德有星期，和风没有

      JSONObject dayWeather = new JSONObject();
      dayWeather.put("weather", day.getString("textDay")); // 白天天气
      dayWeather.put("temperature", day.getString("tempMax")); // 最高温度

      JSONObject nightWeather = new JSONObject();
      nightWeather.put("weather", day.getString("textNight")); // 夜间天气
      nightWeather.put("temperature", day.getString("tempMin")); // 最低温度

      cast.put("day", dayWeather);
      cast.put("night", nightWeather);
      cast.put("dayweather", day.getString("textDay"));
      cast.put("nightweather", day.getString("textNight"));
      cast.put("daytemp", day.getString("tempMax"));
      cast.put("nighttemp", day.getString("tempMin"));
      cast.put("daywind", day.getString("windDirDay"));
      cast.put("nightwind", day.getString("windDirNight"));
      cast.put("daypower", day.getString("windScaleDay"));
      cast.put("nightpower", day.getString("windScaleNight"));
      cast.put("humidity", day.getString("humidity"));
      cast.put("sunrise", day.getString("sunrise"));
      cast.put("sunset", day.getString("sunset"));

      castsArray.add(cast);
    }

    forecastItem.put("casts", castsArray);
    forecastsArray.add(forecastItem);
    return forecastsArray.toString();
  }

  /**
   * 检查服务是否可用
   */
  public boolean isAvailable() {
    return enabled && projectId != null && !projectId.isEmpty() && keyId != null
        && !keyId.isEmpty() && privateKey != null && !privateKey.isEmpty();
  }
}
