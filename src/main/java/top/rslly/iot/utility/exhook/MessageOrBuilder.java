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

public interface MessageOrBuilder extends
    // @@protoc_insertion_point(interface_extends:emqx.exhook.v2.Message)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>string node = 1;</code>
   */
  String getNode();

  /**
   * <code>string node = 1;</code>
   */
  com.google.protobuf.ByteString getNodeBytes();

  /**
   * <code>string id = 2;</code>
   */
  String getId();

  /**
   * <code>string id = 2;</code>
   */
  com.google.protobuf.ByteString getIdBytes();

  /**
   * <code>uint32 qos = 3;</code>
   */
  int getQos();

  /**
   * <code>string from = 4;</code>
   */
  String getFrom();

  /**
   * <code>string from = 4;</code>
   */
  com.google.protobuf.ByteString getFromBytes();

  /**
   * <code>string topic = 5;</code>
   */
  String getTopic();

  /**
   * <code>string topic = 5;</code>
   */
  com.google.protobuf.ByteString getTopicBytes();

  /**
   * <code>bytes payload = 6;</code>
   */
  com.google.protobuf.ByteString getPayload();

  /**
   * <code>uint64 timestamp = 7;</code>
   */
  long getTimestamp();

  /**
   * <pre>
   * The key of header can be:
   *  - username:
   *    * Readonly
   *    * The username of sender client
   *    * Value type: utf8 string
   *  - protocol:
   *    * Readonly
   *    * The protocol name of sender client
   *    * Value type: string enum with "mqtt", "mqtt-sn", ...
   *  - peerhost:
   *    * Readonly
   *    * The peerhost of sender client
   *    * Value type: ip address string
   *  - allow_publish:
   *    * Writable
   *    * Whether to allow the message to be published by emqx
   *    * Value type: string enum with "true", "false", default is "true"
   * Notes: All header may be missing, which means that the message does not
   *   carry these headers. We can guarantee that clients coming from MQTT,
   *   MQTT-SN, CoAP, LwM2M and other natively supported protocol clients will
   *   carry these headers, but there is no guarantee that messages published
   *   by other means will do, e.g. messages published by HTTP-API
   * </pre>
   *
   * <code>map&lt;string, string&gt; headers = 8;</code>
   */
  int getHeadersCount();

  /**
   * <pre>
   * The key of header can be:
   *  - username:
   *    * Readonly
   *    * The username of sender client
   *    * Value type: utf8 string
   *  - protocol:
   *    * Readonly
   *    * The protocol name of sender client
   *    * Value type: string enum with "mqtt", "mqtt-sn", ...
   *  - peerhost:
   *    * Readonly
   *    * The peerhost of sender client
   *    * Value type: ip address string
   *  - allow_publish:
   *    * Writable
   *    * Whether to allow the message to be published by emqx
   *    * Value type: string enum with "true", "false", default is "true"
   * Notes: All header may be missing, which means that the message does not
   *   carry these headers. We can guarantee that clients coming from MQTT,
   *   MQTT-SN, CoAP, LwM2M and other natively supported protocol clients will
   *   carry these headers, but there is no guarantee that messages published
   *   by other means will do, e.g. messages published by HTTP-API
   * </pre>
   *
   * <code>map&lt;string, string&gt; headers = 8;</code>
   */
  boolean containsHeaders(String key);

  /**
   * Use {@link #getHeadersMap()} instead.
   */
  @Deprecated
  java.util.Map<String, String> getHeaders();

  /**
   * <pre>
   * The key of header can be:
   *  - username:
   *    * Readonly
   *    * The username of sender client
   *    * Value type: utf8 string
   *  - protocol:
   *    * Readonly
   *    * The protocol name of sender client
   *    * Value type: string enum with "mqtt", "mqtt-sn", ...
   *  - peerhost:
   *    * Readonly
   *    * The peerhost of sender client
   *    * Value type: ip address string
   *  - allow_publish:
   *    * Writable
   *    * Whether to allow the message to be published by emqx
   *    * Value type: string enum with "true", "false", default is "true"
   * Notes: All header may be missing, which means that the message does not
   *   carry these headers. We can guarantee that clients coming from MQTT,
   *   MQTT-SN, CoAP, LwM2M and other natively supported protocol clients will
   *   carry these headers, but there is no guarantee that messages published
   *   by other means will do, e.g. messages published by HTTP-API
   * </pre>
   *
   * <code>map&lt;string, string&gt; headers = 8;</code>
   */
  java.util.Map<String, String> getHeadersMap();

  /**
   * <pre>
   * The key of header can be:
   *  - username:
   *    * Readonly
   *    * The username of sender client
   *    * Value type: utf8 string
   *  - protocol:
   *    * Readonly
   *    * The protocol name of sender client
   *    * Value type: string enum with "mqtt", "mqtt-sn", ...
   *  - peerhost:
   *    * Readonly
   *    * The peerhost of sender client
   *    * Value type: ip address string
   *  - allow_publish:
   *    * Writable
   *    * Whether to allow the message to be published by emqx
   *    * Value type: string enum with "true", "false", default is "true"
   * Notes: All header may be missing, which means that the message does not
   *   carry these headers. We can guarantee that clients coming from MQTT,
   *   MQTT-SN, CoAP, LwM2M and other natively supported protocol clients will
   *   carry these headers, but there is no guarantee that messages published
   *   by other means will do, e.g. messages published by HTTP-API
   * </pre>
   *
   * <code>map&lt;string, string&gt; headers = 8;</code>
   */

  String getHeadersOrDefault(String key, String defaultValue);

  /**
   * <pre>
   * The key of header can be:
   *  - username:
   *    * Readonly
   *    * The username of sender client
   *    * Value type: utf8 string
   *  - protocol:
   *    * Readonly
   *    * The protocol name of sender client
   *    * Value type: string enum with "mqtt", "mqtt-sn", ...
   *  - peerhost:
   *    * Readonly
   *    * The peerhost of sender client
   *    * Value type: ip address string
   *  - allow_publish:
   *    * Writable
   *    * Whether to allow the message to be published by emqx
   *    * Value type: string enum with "true", "false", default is "true"
   * Notes: All header may be missing, which means that the message does not
   *   carry these headers. We can guarantee that clients coming from MQTT,
   *   MQTT-SN, CoAP, LwM2M and other natively supported protocol clients will
   *   carry these headers, but there is no guarantee that messages published
   *   by other means will do, e.g. messages published by HTTP-API
   * </pre>
   *
   * <code>map&lt;string, string&gt; headers = 8;</code>
   */

  String getHeadersOrThrow(String key);
}
