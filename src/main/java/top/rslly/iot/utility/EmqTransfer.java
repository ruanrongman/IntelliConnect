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
package top.rslly.iot.utility;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.rslly.iot.services.ProductDeviceServiceImpl;
import top.rslly.iot.utility.properties.EmqProperty;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Component
public class EmqTransfer {
  @Autowired
  private EmqProperty emqProperty;
  @Autowired
  private HttpRequestUtils httpRequestUtils;

  public boolean ConnectStatus(String ToolClientid) throws IOException {
    var emqResponse = httpRequestUtils.httpGetAuthentication(emqProperty.getKey(),
        emqProperty.getSecret(), emqProperty.getUrl());
    // System.out.println(s.body().string());
    String msg = Objects.requireNonNull(emqResponse.body()).string();
    JSONArray s1 = JSON.parseObject(msg).getJSONArray("data");
    // List<String> l1= new LinkedList<>();
    // Map<String,String> l1= new HashMap<>();
    if (s1.size() == 0)
      return false;
    for (int i = 0; i < s1.size(); i++) {
      JSONObject jo = s1.getJSONObject(i);
      String clientid = jo.getString("clientid");
      String online = jo.getString("connected");
      // System.out.println(clientid);
      // l1.put(clientid,online);
      if (clientid.equals(ToolClientid) && online.equals("true")) {
        return true;
      }

    }
    return false;
    /*
     * // var msg1=l1.get("00000002"); var msg1=l1.get(ToolClientid); if(msg1==null) return false;
     * else return msg1.equals("true");
     */
  }

  public Map<String, String> ConnectMap() throws IOException {
    var emqResponse = httpRequestUtils.httpGetAuthentication(emqProperty.getKey(),
        emqProperty.getSecret(), emqProperty.getUrl());
    String msg = Objects.requireNonNull(emqResponse.body()).string();
    Map<String, String> connectMap = new HashMap<>();
    JSONArray deviceArray = JSON.parseObject(msg).getJSONArray("data");
    for (int i = 0; i < deviceArray.size(); i++) {
      JSONObject jo = deviceArray.getJSONObject(i);
      String clientid = jo.getString("clientid");
      String online = jo.getString("connected");
      // System.out.println(clientid);
      connectMap.put(clientid, online);
    }
    return connectMap;
  }

}
