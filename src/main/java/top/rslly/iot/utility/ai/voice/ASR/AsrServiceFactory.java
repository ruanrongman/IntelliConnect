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
package top.rslly.iot.utility.ai.voice.ASR;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * ASR服务工厂类
 */
@Slf4j
@Component
public class AsrServiceFactory {

  @Value("${ai.asr.provider:funasr}")
  private String defaultProvider;

  @Autowired
  private Audio2Text audio2Text;

  @Autowired
  private FunAsrClient funAsrClient;

  /**
   * 获取默认的ASR服务
   */
  public AsrService getService() {
    return getService(defaultProvider);
  }

  /**
   * 根据提供商名称获取ASR服务
   *
   * @param provider 提供商名称（dashscope/funasr）
   * @return ASR服务实例
   */
  public AsrService getService(String provider) {
    if ("funasr".equalsIgnoreCase(provider)) {
      return funAsrClient;
    }
    if ("dashscope".equalsIgnoreCase(provider)) {
      return audio2Text;
    }
    log.warn("未知的ASR提供商: {}, 使用默认服务", provider);
    return "funasr".equalsIgnoreCase(defaultProvider) ? funAsrClient : audio2Text;
  }

}
