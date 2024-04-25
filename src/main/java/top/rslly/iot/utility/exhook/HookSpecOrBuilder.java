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

public interface HookSpecOrBuilder extends
    // @@protoc_insertion_point(interface_extends:emqx.exhook.v2.HookSpec)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <pre>
   * The registered hooks name
   * Available value:
   *   "client.connect",      "client.connack"
   *   "client.connected",    "client.disconnected"
   *   "client.authenticate", "client.authorize"
   *   "client.subscribe",    "client.unsubscribe"
   *   "session.created",      "session.subscribed"
   *   "session.unsubscribed", "session.resumed"
   *   "session.discarded",    "session.takenover"
   *   "session.terminated"
   *   "message.publish", "message.delivered"
   *   "message.acked",   "message.dropped"
   * </pre>
   *
   * <code>string name = 1;</code>
   */
  String getName();

  /**
   * <pre>
   * The registered hooks name
   * Available value:
   *   "client.connect",      "client.connack"
   *   "client.connected",    "client.disconnected"
   *   "client.authenticate", "client.authorize"
   *   "client.subscribe",    "client.unsubscribe"
   *   "session.created",      "session.subscribed"
   *   "session.unsubscribed", "session.resumed"
   *   "session.discarded",    "session.takenover"
   *   "session.terminated"
   *   "message.publish", "message.delivered"
   *   "message.acked",   "message.dropped"
   * </pre>
   *
   * <code>string name = 1;</code>
   */
  com.google.protobuf.ByteString getNameBytes();

  /**
   * <pre>
   * The topic filters for message hooks
   * </pre>
   *
   * <code>repeated string topics = 2;</code>
   */
  java.util.List<String> getTopicsList();

  /**
   * <pre>
   * The topic filters for message hooks
   * </pre>
   *
   * <code>repeated string topics = 2;</code>
   */
  int getTopicsCount();

  /**
   * <pre>
   * The topic filters for message hooks
   * </pre>
   *
   * <code>repeated string topics = 2;</code>
   */
  String getTopics(int index);

  /**
   * <pre>
   * The topic filters for message hooks
   * </pre>
   *
   * <code>repeated string topics = 2;</code>
   */
  com.google.protobuf.ByteString getTopicsBytes(int index);
}
