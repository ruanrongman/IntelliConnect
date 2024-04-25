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

public interface SessionSubscribedRequestOrBuilder extends
    // @@protoc_insertion_point(interface_extends:emqx.exhook.v2.SessionSubscribedRequest)
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
   * <code>string topic = 2;</code>
   */
  String getTopic();

  /**
   * <code>string topic = 2;</code>
   */
  com.google.protobuf.ByteString getTopicBytes();

  /**
   * <code>.emqx.exhook.v2.SubOpts subopts = 3;</code>
   */
  boolean hasSubopts();

  /**
   * <code>.emqx.exhook.v2.SubOpts subopts = 3;</code>
   */
  SubOpts getSubopts();

  /**
   * <code>.emqx.exhook.v2.SubOpts subopts = 3;</code>
   */
  SubOptsOrBuilder getSuboptsOrBuilder();

  /**
   * <code>.emqx.exhook.v2.RequestMeta meta = 4;</code>
   */
  boolean hasMeta();

  /**
   * <code>.emqx.exhook.v2.RequestMeta meta = 4;</code>
   */
  RequestMeta getMeta();

  /**
   * <code>.emqx.exhook.v2.RequestMeta meta = 4;</code>
   */
  RequestMetaOrBuilder getMetaOrBuilder();
}
