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
 * @Author: Hutengfei
 * @Description:
 * @Date Create in 2019/7/22 19:52
 */
public class ResultTool {
  public static JsonResult<?> success() {
    return new JsonResult<>(true);
  }

  public static <T> JsonResult<T> success(T data) {
    return new JsonResult<>(true, data);
  }

  public static JsonResult<?> fail() {
    return new JsonResult<>(false);
  }

  public static JsonResult<?> fail(ResultCode resultEnum) {
    return new JsonResult<>(false, resultEnum);
  }

}
