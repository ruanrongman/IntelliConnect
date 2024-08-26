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
package top.rslly.iot.models.influxdb;

import lombok.Data;
import org.influxdb.annotation.Measurement;
import top.rslly.iot.utility.influxdb.ano.Tag;
import top.rslly.iot.utility.influxdb.ano.TimeColumn;

@Data
@Measurement(name = "event_storage")
public class EventStorageTimeEntity {
  @TimeColumn
  private Long time;
  @Tag
  private int deviceId;
  private String jsonKey;
  private String value;
  private String characteristic;
}