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
package top.rslly.iot.param.request;

import lombok.Data;

import jakarta.validation.constraints.*;

@Data
public class ProductVoiceDiy {
  private int productId;
  @NotNull(message = "pitch 不能为空")
  @DecimalMin(value = "0.5", message = "pitch 不能小于 0.5")
  @DecimalMax(value = "2.0", message = "pitch 不能大于 2.0")
  private Float pitch;

  @NotNull(message = "speed 不能为空")
  @DecimalMin(value = "0.5", message = "speed 不能小于 0.5")
  @DecimalMax(value = "2.0", message = "speed 不能大于 2.0")
  private Float speed;
}
