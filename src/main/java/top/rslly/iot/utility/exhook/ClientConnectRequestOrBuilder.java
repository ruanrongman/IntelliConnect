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

public interface ClientConnectRequestOrBuilder extends
    // @@protoc_insertion_point(interface_extends:emqx.exhook.v2.ClientConnectRequest)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>.emqx.exhook.v2.ConnInfo conninfo = 1;</code>
   */
  boolean hasConninfo();

  /**
   * <code>.emqx.exhook.v2.ConnInfo conninfo = 1;</code>
   */
  ConnInfo getConninfo();

  /**
   * <code>.emqx.exhook.v2.ConnInfo conninfo = 1;</code>
   */
  ConnInfoOrBuilder getConninfoOrBuilder();

  /**
   * <pre>
   * MQTT CONNECT packet's properties (MQTT v5.0)
   * It should be empty on MQTT v3.1.1/v3.1 or others protocol
   * </pre>
   *
   * <code>repeated .emqx.exhook.v2.Property props = 2;</code>
   */
  java.util.List<Property> getPropsList();

  /**
   * <pre>
   * MQTT CONNECT packet's properties (MQTT v5.0)
   * It should be empty on MQTT v3.1.1/v3.1 or others protocol
   * </pre>
   *
   * <code>repeated .emqx.exhook.v2.Property props = 2;</code>
   */
  Property getProps(int index);

  /**
   * <pre>
   * MQTT CONNECT packet's properties (MQTT v5.0)
   * It should be empty on MQTT v3.1.1/v3.1 or others protocol
   * </pre>
   *
   * <code>repeated .emqx.exhook.v2.Property props = 2;</code>
   */
  int getPropsCount();

  /**
   * <pre>
   * MQTT CONNECT packet's properties (MQTT v5.0)
   * It should be empty on MQTT v3.1.1/v3.1 or others protocol
   * </pre>
   *
   * <code>repeated .emqx.exhook.v2.Property props = 2;</code>
   */
  java.util.List<? extends PropertyOrBuilder> getPropsOrBuilderList();

  /**
   * <pre>
   * MQTT CONNECT packet's properties (MQTT v5.0)
   * It should be empty on MQTT v3.1.1/v3.1 or others protocol
   * </pre>
   *
   * <code>repeated .emqx.exhook.v2.Property props = 2;</code>
   */
  PropertyOrBuilder getPropsOrBuilder(int index);

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
