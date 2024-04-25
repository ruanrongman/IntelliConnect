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

public interface ConnInfoOrBuilder extends
    // @@protoc_insertion_point(interface_extends:emqx.exhook.v2.ConnInfo)
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
   * <code>string clientid = 2;</code>
   */
  String getClientid();

  /**
   * <code>string clientid = 2;</code>
   */
  com.google.protobuf.ByteString getClientidBytes();

  /**
   * <code>string username = 3;</code>
   */
  String getUsername();

  /**
   * <code>string username = 3;</code>
   */
  com.google.protobuf.ByteString getUsernameBytes();

  /**
   * <code>string peerhost = 4;</code>
   */
  String getPeerhost();

  /**
   * <code>string peerhost = 4;</code>
   */
  com.google.protobuf.ByteString getPeerhostBytes();

  /**
   * <code>uint32 sockport = 5;</code>
   */
  int getSockport();

  /**
   * <code>string proto_name = 6;</code>
   */
  String getProtoName();

  /**
   * <code>string proto_name = 6;</code>
   */
  com.google.protobuf.ByteString getProtoNameBytes();

  /**
   * <code>string proto_ver = 7;</code>
   */
  String getProtoVer();

  /**
   * <code>string proto_ver = 7;</code>
   */
  com.google.protobuf.ByteString getProtoVerBytes();

  /**
   * <code>uint32 keepalive = 8;</code>
   */
  int getKeepalive();
}
