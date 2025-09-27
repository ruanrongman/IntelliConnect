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
package top.rslly.iot.services;

public interface SafetyService {
  boolean controlAuthorizeProduct(String token, int productId);

  boolean controlAuthorizeModel(String token, int modelId);

  boolean controlAuthorizeDevice(String token, int deviceId);

  boolean controlAuthorizeDevice(String token, String deviceName);

  boolean controlAuthorizeFunction(String token, int functionId);

  boolean controlAuthorizeEvent(String token, int eventId);

  boolean controlAuthorizeAlarmEvent(String token, int alarmEventId);

  boolean controlAuthorizeEventData(String token, int eventDataId);

  boolean controlAuthorizeProductData(String token, int productDataId);

  boolean controlAuthorizeProductRole(String token, int productRoleId);

  boolean controlAuthorizeMcpServer(String token, int mcpServerId);

  boolean controlAuthorizeOta(String token, int id);

  boolean controlAuthorizeOtaXiaoZhi(String token, int id);

  boolean controlAuthorizeOta(String token, String name, String deviceName);

  boolean controlAuthorizeOtaPassive(String token, int id);

  boolean controlAuthorizeKnowledgeChat(String token, int id);

  boolean controlAuthorizeProductRouterSet(String token, int id);
}
