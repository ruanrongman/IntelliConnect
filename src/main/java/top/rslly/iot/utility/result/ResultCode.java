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
package top.rslly.iot.utility.result;

/**
 * @Description: 返回码定义 规定: #1表示成功 #1001～1999 区间表示参数错误 #2001～2999 区间表示用户错误 #3001～3999 区间表示接口异常
 */
public enum ResultCode {
  /* 成功 */
  SUCCESS(200, "成功"),

  /* 默认失败 */
  COMMON_FAIL(999, "失败"),

  /* 参数错误：1000～1999 */
  PARAM_NOT_VALID(1001, "参数无效"), PARAM_IS_BLANK(1002, "参数为空"), PARAM_TYPE_ERROR(1003,
      "参数类型错误"), PARAM_NOT_COMPLETE(1004, "参数缺失"),

  /* 用户错误 */
  USER_NOT_LOGIN(2001, "用户未登录"), USER_ACCOUNT_EXPIRED(2002, "账号已过期"), USER_CREDENTIALS_ERROR(2003,
      "密码错误"), USER_CREDENTIALS_EXPIRED(2004, "密码过期"), USER_ACCOUNT_DISABLE(2005,
          "账号不可用"), USER_ACCOUNT_LOCKED(2006, "账号被锁定"), USER_ACCOUNT_NOT_EXIST(2007,
              "账号不存在"), USER_ACCOUNT_ALREADY_EXIST(2008,
                  "账号已存在"), USER_ACCOUNT_USE_BY_OTHERS(2009, "账号下线"), EMAIL_ALREADY_EXIST(2010,
                      "邮箱已存在"), USER_CODE_ERROR(2011, "验证码错误"),

  /* 业务错误 */
  NO_PERMISSION(3001, "没有权限"), HAS_DEPENDENCIES(3002, "存在依赖关系，无法删除"), DEVICE_ABANDON(3003,
      "设备被禁用"), DEVICE_TIMEOUT(3004, "设备响应超时");

  private Integer code;
  private String message;

  ResultCode(Integer code, String message) {
    this.code = code;
    this.message = message;
  }

  public Integer getCode() {
    return code;
  }

  public void setCode(Integer code) {
    this.code = code;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  /**
   * 根据code获取message
   *
   * @param code input the code to get message
   * @return String
   */
  public static String getMessageByCode(Integer code) {
    for (ResultCode ele : values()) {
      if (ele.getCode().equals(code)) {
        return ele.getMessage();
      }
    }
    return null;
  }
}
