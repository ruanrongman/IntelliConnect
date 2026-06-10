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
package top.rslly.iot.services.agent;

import top.rslly.iot.models.CodingAgentSession;
import top.rslly.iot.param.request.CodingAgentSessionParam;
import top.rslly.iot.param.request.CodingAgentSessionUploadParam;
import top.rslly.iot.utility.result.JsonResult;

import java.util.List;

public interface CodingAgentSessionService {
  List<CodingAgentSession> findAll();

  List<CodingAgentSession> findAllById(int id);

  List<CodingAgentSession> findAllByAgentId(int agentId);

  List<CodingAgentSession> findAllBySessionIdAndCwd(String sessionId, String cwd);

  List<CodingAgentSession> findAllByAgentIdAndSessionIdAndCwd(int agentId, String sessionId,
      String cwd);

  JsonResult<?> getCodingAgentSession(String token);

  JsonResult<?> postCodingAgentSession(CodingAgentSessionParam codingAgentSessionParam);

  JsonResult<?> updateCodingAgentSession(CodingAgentSessionParam codingAgentSessionParam);

  JsonResult<?> uploadCodingAgentSession(
      CodingAgentSessionUploadParam codingAgentSessionUploadParam);

  JsonResult<?> getCodingAgentSessionByDevice(int agentId, String pairCode);

  JsonResult<?> deleteCodingAgentSession(int id);
}
