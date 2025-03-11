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
package top.rslly.iot.controllers;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import top.rslly.iot.param.request.WxBindProduct;
import top.rslly.iot.param.request.WxUser;
import top.rslly.iot.services.wechat.WxProductBindServiceImpl;
import top.rslly.iot.services.wechat.WxUserServiceImpl;
import top.rslly.iot.utility.SHA1;
import top.rslly.iot.utility.result.JsonResult;
import top.rslly.iot.utility.wx.DealWx;
import top.rslly.iot.utility.wx.SmartRobot;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@RestController
@Slf4j
public class Wx {

  @Autowired
  private WxUserServiceImpl wxUserService;
  @Autowired
  private SHA1 sha1;
  @Autowired
  private DealWx dealWx;
  @Autowired
  private WxProductBindServiceImpl wxProductBindService;
  @Autowired
  private SmartRobot smartRobot;
  @Value("${wx.msg.token}")
  private String TOKEN;
  @Value("${wx.appid}")
  private String appid;
  @Value("${wx.micro.appid}")
  private String microappid;
  @Value("${wx.micro.ToUserName}")
  private String ToUserName;
  @Value("${wx.micro.ToUserName2}")
  private String ToUserName2;
  @Value("${wx.micro.appid2}")
  private String microappid2;


  @RequestMapping(value = "/wxLogin", method = RequestMethod.POST)
  public JsonResult<?> wxLogin(@RequestBody WxUser wxUser) throws IOException {
    return wxUserService.wxLogin(wxUser);
  }

  @RequestMapping(value = "/wxRegister", method = RequestMethod.POST)
  public JsonResult<?> wxRegister(@RequestBody WxUser wxUser) throws IOException {
    return wxUserService.wxRegister(wxUser);
  }

  @RequestMapping(value = "/wxBindProduct", method = RequestMethod.GET)
  public JsonResult<?> wxBindProduct(@RequestHeader("Authorization") String header) {
    return wxProductBindService.wxGetBindProduct(header);
  }

  @RequestMapping(value = "/wxBindProduct", method = RequestMethod.POST)
  public JsonResult<?> wxBindProduct(@RequestBody WxBindProduct wxBindProduct,
      @RequestHeader("Authorization") String header) {
    return wxProductBindService.wxBindProduct(wxBindProduct, header);
  }

  @RequestMapping(value = "/wxBindProduct", method = RequestMethod.DELETE)
  public JsonResult<?> wxUnBindProduct(@RequestBody WxBindProduct wxBindProduct,
      @RequestHeader("Authorization") String header) {
    return wxProductBindService.wxUnBindProduct(wxBindProduct, header);
  }

  @RequestMapping(value = "/wxmsg", method = {RequestMethod.GET, RequestMethod.POST})
  public void WxMsg(HttpServletRequest request, HttpServletResponse response) {
    boolean isGet = request.getMethod().equalsIgnoreCase("get");
    // 微信加密签名，signature结合了开发者填写的token参数和请求中的timestamp参数、nonce参数。
    try {
      if (isGet) {
        String signature = request.getParameter("signature");
        // 时间戳
        String timestamp = request.getParameter("timestamp");
        // 随机数
        String nonce = request.getParameter("nonce");
        // 随机字符串
        String echostr = request.getParameter("echostr");
        log.info("signature={}, timestamp={}, nonce={}, echostr={}",
            signature, timestamp, nonce, echostr);
        String[] strArray = new String[] {TOKEN, timestamp, nonce};
        Arrays.sort(strArray);
        StringBuilder sb = new StringBuilder();
        for (String str : strArray) {
          sb.append(str);
        }
        String encrypt = sha1.getDigestOfString(sb.toString().getBytes());
        // System.out.println(encrypt);
        if (encrypt.equals(signature)) {
          response.getOutputStream().write(echostr.getBytes());
        }
      } else {
        // 进入POST聊天处理
        // 将请求、响应的编码均设置为UTF-8（防止中文乱码）
        request.setCharacterEncoding("UTF-8");
        response.setCharacterEncoding("UTF-8");
        // System.out.println(request);
        // 接收消息并返回消息
        // String result = acceptMessage(request, response);
        InputStream is;
        is = request.getInputStream();
        String bodyInfo = IOUtils.toString(is, StandardCharsets.UTF_8);
        try {

          // System.out.println(bodyInfo);
          String microId = JSON.parseObject(bodyInfo).getString("ToUserName");

          String msgType = JSON.parseObject(bodyInfo).getString("MsgType");
          if (msgType.equals("text")) {
            String msg = JSON.parseObject(bodyInfo).getString("Content");
            String openid = JSON.parseObject(bodyInfo).getString("FromUserName");
            String userid = DigestUtils.md5DigestAsHex(openid.getBytes(StandardCharsets.UTF_8));
            // String ans = router.response(msg);
            if (microId.equals(ToUserName)) {
              if (msg.equals("注册账户")) {
                var res = wxUserService.wxRegister(openid);
                if (res != null)
                  dealWx.sendContent(openid, "注册成功", microappid);
              } else
                smartRobot.smartSendContent(openid, msg, microappid);
            } else if (microId.equals(ToUserName2)) {
              if (msg.equals("注册账户")) {
                var res = wxUserService.wxRegister(openid);
                if (res != null)
                  dealWx.sendContent(openid, "注册成功", microappid2);
              } else
                smartRobot.smartSendContent(openid, msg, microappid2);
            }
          } else if (msgType.equals("image")) {
            String openid = JSON.parseObject(bodyInfo).getString("FromUserName");
            String imageUrl = JSON.parseObject(bodyInfo).getString("PicUrl");
            if (microId.equals(ToUserName))
              smartRobot.smartImageSendContent(openid, imageUrl, microappid);
            else if (microId.equals(ToUserName2))
              smartRobot.smartImageSendContent(openid, imageUrl, microappid2);
          } else if (msgType.equals("event")) {
            String event = JSON.parseObject(bodyInfo).getString("Event");
            String openid = (String) JSON.parseObject(bodyInfo).get("FromUserName");
            if (event.equals("user_enter_tempsession")) {
              if (microId.equals(ToUserName))
                dealWx.sendContent(openid, "创万联AI小助手正在为你服务", microappid);
              else if (microId.equals(ToUserName2))
                dealWx.sendContent(openid, "创万联AI小助手正在为你服务", microappid2);
            }
          }
        } catch (JSONException e) {
          try {
            // String bodyInfo = IOUtils.toString(is, StandardCharsets.UTF_8);
            log.info("bodyinfo{}", bodyInfo);
            // xml字符串转Document对象
            Document document = DocumentHelper.parseText(bodyInfo);
            // 获取根节点
            Element root = document.getRootElement();
            Element type = root.element("MsgType");
            String openid = root.element("FromUserName").getText();
            if (type.getText().equals("text")) {
              // 获取根节点下的tag1标签

              // 获取tag1标签下多个conts的标签集合
              /*
               * List<Element> contsList = tag1.elements("conts"); //取集合中一个标签，并拿到标签中间的值 String user
               * = contsList.get(0).element("user").getText(); //取其中一个标签上属性为id的值 String id =
               * contsList.get(0).attributeValue("id");
               */
              // var s= Arrays.toString(SHACoder.en(openid));
              String content = root.element("Content").getText();
              String userid = DigestUtils.md5DigestAsHex(openid.getBytes(StandardCharsets.UTF_8));
              if (content.equals("消息推送密钥")) {
                dealWx.sendContent(openid, userid, appid);
              } else if (content.equals("注册账户")) {
                var res = wxUserService.wxRegister(openid);
                if (res != null)
                  dealWx.sendContent(openid, "注册成功", appid);
              } else {
                // String ans = router.response(content);
                smartRobot.smartSendContent(openid, content, appid);
              }
            } else if (type.getText().equals("voice")) {
              String voiceContent = root.element("Recognition").getText();
              String mediaId = root.element("MediaId").getText();
              String url = dealWx.getMedia(mediaId, appid);
              if (url != null && !url.equals("")) {
                smartRobot.dealVoice(openid, url, appid);
              }
            } else if (type.getText().equals("image")) {
              String imageUrl = root.element("PicUrl").getText();
              smartRobot.smartImageSendContent(openid, imageUrl, appid);
            }
            // e.printStackTrace();
          } catch (Exception e2) {
            log.error("微信处理POST请求异常{}", e2.getMessage());
          }
        }

        // 响应消息
        PrintWriter out = response.getWriter();
        out.print("success");
        out.close();
      }
    } catch (Exception ex) {
      log.error("微信帐号接口配置失败！{}", ex.getMessage());
    }


  }
}
