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
package top.rslly.iot.utility.ai.voice;

import cn.xfyun.api.IatClient;
import cn.xfyun.model.response.iat.IatResponse;
import cn.xfyun.model.response.iat.IatResult;
import cn.xfyun.service.iat.AbstractIatWebSocketListener;
import okhttp3.Response;
import okhttp3.WebSocket;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.security.SignatureException;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * IAT( iFly Auto Transform ) 语音听写
 */
public class IatClientApp {
  private static final String appId = "XXX"; // 在控制台-我的应用获取
  private static final String apiSecret = "XXXX"; // 在控制台-我的应用-语音听写（流式版）获取
  private static final String apiKey = "XXX"; // 在控制台-我的应用-语音听写（流式版）获取
  private static String filePath = "audio/rtasr.pcm";
  private static String resourcePath;

  static {
    try {
      resourcePath = IatClientApp.class.getResource("/").toURI().getPath();
    } catch (URISyntaxException e) {
      e.printStackTrace();
    }
  }


  public static void main(String[] args) throws IOException, SignatureException {
    IatClient iatClient =
        new IatClient.Builder().signature(appId, apiKey, apiSecret).encoding("lame").build();

    SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd HH:mm:ss.SSS");
    Date dateBegin = new Date();

    URL url = new URL("https://xfyun-doc.xfyun.cn/1597644669753474/iat_mp3_16k.mp3");
    URLConnection connection = url.openConnection();
    InputStream fs = connection.getInputStream();
    // File file = new File(resourcePath + filePath);
    StringBuffer finalResult = new StringBuffer();
    iatClient.send(fs, new AbstractIatWebSocketListener() {
      @Override
      public void onSuccess(WebSocket webSocket, IatResponse iatResponse) {
        if (iatResponse.getCode() != 0) {
          System.out.println("code=>" + iatResponse.getCode() + " error=>"
              + iatResponse.getMessage() + " sid=" + iatResponse.getSid());
          System.out.println("错误码查询链接：https://www.xfyun.cn/document/error-code");
          return;
        }

        if (iatResponse.getData() != null) {
          if (iatResponse.getData().getResult() != null) {
            IatResult.Ws[] wss = iatResponse.getData().getResult().getWs();
            String text = "";
            for (IatResult.Ws ws : wss) {
              IatResult.Cw[] cws = ws.getCw();

              for (IatResult.Cw cw : cws) {
                text += cw.getW();
              }
            }

            try {
              finalResult.append(text);
              System.out.println("中间识别结果 ==》" + text);
            } catch (Exception e) {
              e.printStackTrace();
            }
          }

          if (iatResponse.getData().getStatus() == 2) {
            // resp.data.status ==2 说明数据全部返回完毕，可以关闭连接，释放资源
            System.out.println("session end ");
            Date dateEnd = new Date();
            System.out.println(sdf.format(dateBegin) + "开始");
            System.out.println(sdf.format(dateEnd) + "结束");
            System.out.println("耗时:" + (dateEnd.getTime() - dateBegin.getTime()) + "ms");
            System.out.println("最终识别结果" + finalResult.toString());
            System.out.println("本次识别sid ==》" + iatResponse.getSid());
            iatClient.closeWebsocket();
            System.exit(0);
          } else {
            // 根据返回的数据处理
            // System.out.println(StringUtils.gson.toJson(iatResponse));
          }
        }
      }

      @Override
      public void onFail(WebSocket webSocket, Throwable t, Response response) {}
    });
  }
}
