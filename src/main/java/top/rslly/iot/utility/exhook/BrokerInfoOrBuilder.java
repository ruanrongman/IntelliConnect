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
package top.rslly.iot.utility.exhook;

public interface BrokerInfoOrBuilder extends
    // @@protoc_insertion_point(interface_extends:emqx.exhook.v2.BrokerInfo)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>string version = 1;</code>
   */
  String getVersion();

  /**
   * <code>string version = 1;</code>
   */
  com.google.protobuf.ByteString getVersionBytes();

  /**
   * <code>string sysdescr = 2;</code>
   */
  String getSysdescr();

  /**
   * <code>string sysdescr = 2;</code>
   */
  com.google.protobuf.ByteString getSysdescrBytes();

  /**
   * <code>int64 uptime = 3;</code>
   */
  long getUptime();

  /**
   * <code>string datetime = 4;</code>
   */
  String getDatetime();

  /**
   * <code>string datetime = 4;</code>
   */
  com.google.protobuf.ByteString getDatetimeBytes();
}
