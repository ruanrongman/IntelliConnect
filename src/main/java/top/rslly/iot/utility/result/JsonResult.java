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

import java.io.Serializable;


public class JsonResult<T> implements Serializable {
  private Boolean success;
  private Integer errorCode;
  private String errorMsg;
  private T data;

  public JsonResult() {}

  public JsonResult(boolean success) {
    this.success = success;
    this.errorCode = success ? ResultCode.SUCCESS.getCode() : ResultCode.COMMON_FAIL.getCode();
    this.errorMsg = success ? ResultCode.SUCCESS.getMessage() : ResultCode.COMMON_FAIL.getMessage();
  }

  public JsonResult(boolean success, ResultCode resultEnum) {
    this.success = success;
    this.errorCode = success ? ResultCode.SUCCESS.getCode()
        : (resultEnum == null ? ResultCode.COMMON_FAIL.getCode() : resultEnum.getCode());
    this.errorMsg = success ? ResultCode.SUCCESS.getMessage()
        : (resultEnum == null ? ResultCode.COMMON_FAIL.getMessage() : resultEnum.getMessage());
  }

  public JsonResult(boolean success, T data) {
    this.success = success;
    this.errorCode = success ? ResultCode.SUCCESS.getCode() : ResultCode.COMMON_FAIL.getCode();
    this.errorMsg = success ? ResultCode.SUCCESS.getMessage() : ResultCode.COMMON_FAIL.getMessage();
    this.data = data;
  }

  public JsonResult(boolean success, ResultCode resultEnum, T data) {
    this.success = success;
    this.errorCode = success ? ResultCode.SUCCESS.getCode()
        : (resultEnum == null ? ResultCode.COMMON_FAIL.getCode() : resultEnum.getCode());
    this.errorMsg = success ? ResultCode.SUCCESS.getMessage()
        : (resultEnum == null ? ResultCode.COMMON_FAIL.getMessage() : resultEnum.getMessage());
    this.data = data;
  }

  public Boolean getSuccess() {
    return success;
  }

  public void setSuccess(Boolean success) {
    this.success = success;
  }

  public Integer getErrorCode() {
    return errorCode;
  }

  public void setErrorCode(Integer errorCode) {
    this.errorCode = errorCode;
  }

  public String getErrorMsg() {
    return errorMsg;
  }

  public void setErrorMsg(String errorMsg) {
    this.errorMsg = errorMsg;
  }

  public T getData() {
    return data;
  }

  public void setData(T data) {
    this.data = data;
  }
}
