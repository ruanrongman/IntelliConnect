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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.mail.MessagingException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class SendEmail {
  // 注入邮件工具类
  @Autowired(required = false)
  private JavaMailSender javaMailSender;
  @Autowired(required = false)
  private TemplateEngine templateEngine;
  // 注入邮箱配置
  @Value("${spring.mail.username}")
  private String fromEmail;

  @Value("${spring.mail.nickname:创万联}") // 默认值为"创万联"
  private String fromName;


  // status表示类型是否正常
  @Async("taskExecutor")
  public void contextLoads(String[] to, String subject, String template, Map<String, Object> map) {
    log.info("异步执行发送邮件");
    // 需要借助Helper类
    MimeMessageHelper helper = null;
    try {
      helper = new MimeMessageHelper(javaMailSender.createMimeMessage(), true);
    } catch (MessagingException e) {
      log.error("send email error{}", e.getMessage());
      return;
    }
    // 使用模板thymeleaf
    // Context是导这个包import org.thymeleaf.context.Context;
    Context context = new Context();
    // 定义模板数据
    context.setVariables(map);
    // 获取thymeleaf的html模板

    String emailContent;
    emailContent = templateEngine.process(template, context); // 指定模板路径


    try {
      helper.setFrom(fromEmail, fromName);
      helper.setTo(to);
      // helper.setBcc("密送人");
      helper.setSubject(subject);
      helper.setSentDate(new Date());// 发送时间
      // 第一个参数要发送的内容，第二个参数是不是Html格式。
      helper.setText(emailContent, true);
      javaMailSender.send(helper.getMimeMessage());

    } catch (Exception e) {
      log.error("send email error{}", e.getMessage());
    }
  }
}
