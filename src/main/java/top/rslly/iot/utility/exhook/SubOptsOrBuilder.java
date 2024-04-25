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

public interface SubOptsOrBuilder extends
    // @@protoc_insertion_point(interface_extends:emqx.exhook.v2.SubOpts)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <pre>
   * The QoS level
   * </pre>
   *
   * <code>uint32 qos = 1;</code>
   */
  int getQos();

  /**
   * <pre>
   * The group name for shared subscription
   * </pre>
   *
   * <code>string share = 2;</code>
   */
  String getShare();

  /**
   * <pre>
   * The group name for shared subscription
   * </pre>
   *
   * <code>string share = 2;</code>
   */
  com.google.protobuf.ByteString getShareBytes();

  /**
   * <pre>
   * The Retain Handling option (MQTT v5.0)
   *  0 = Send retained messages at the time of the subscribe
   *  1 = Send retained messages at subscribe only if the subscription does
   *       not currently exist
   *  2 = Do not send retained messages at the time of the subscribe
   * </pre>
   *
   * <code>uint32 rh = 3;</code>
   */
  int getRh();

  /**
   * <pre>
   * The Retain as Published option (MQTT v5.0)
   *  If 1, Application Messages forwarded using this subscription keep the
   *        RETAIN flag they were published with.
   *  If 0, Application Messages forwarded using this subscription have the
   *        RETAIN flag set to 0.
   * Retained messages sent when the subscription is established have the RETAIN flag set to 1.
   * </pre>
   *
   * <code>uint32 rap = 4;</code>
   */
  int getRap();

  /**
   * <pre>
   * The No Local option (MQTT v5.0)
   * If the value is 1, Application Messages MUST NOT be forwarded to a
   * connection with a ClientID equal to the ClientID of the publishing
   * </pre>
   *
   * <code>uint32 nl = 5;</code>
   */
  int getNl();
}
