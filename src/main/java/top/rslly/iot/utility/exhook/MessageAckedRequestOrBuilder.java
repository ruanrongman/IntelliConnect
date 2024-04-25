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

public interface MessageAckedRequestOrBuilder extends
    // @@protoc_insertion_point(interface_extends:emqx.exhook.v2.MessageAckedRequest)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>.emqx.exhook.v2.ClientInfo clientinfo = 1;</code>
   */
  boolean hasClientinfo();

  /**
   * <code>.emqx.exhook.v2.ClientInfo clientinfo = 1;</code>
   */
  ClientInfo getClientinfo();

  /**
   * <code>.emqx.exhook.v2.ClientInfo clientinfo = 1;</code>
   */
  ClientInfoOrBuilder getClientinfoOrBuilder();

  /**
   * <code>.emqx.exhook.v2.Message message = 2;</code>
   */
  boolean hasMessage();

  /**
   * <code>.emqx.exhook.v2.Message message = 2;</code>
   */
  Message getMessage();

  /**
   * <code>.emqx.exhook.v2.Message message = 2;</code>
   */
  MessageOrBuilder getMessageOrBuilder();

  /**
   * <code>.emqx.exhook.v2.RequestMeta meta = 3;</code>
   */
  boolean hasMeta();

  /**
   * <code>.emqx.exhook.v2.RequestMeta meta = 3;</code>
   */
  RequestMeta getMeta();

  /**
   * <code>.emqx.exhook.v2.RequestMeta meta = 3;</code>
   */
  RequestMetaOrBuilder getMetaOrBuilder();
}
