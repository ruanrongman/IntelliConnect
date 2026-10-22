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

import java.io.InputStream;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;
import top.rslly.iot.models.WorkspaceEntity;
import top.rslly.iot.models.WorkspaceFileEntity;

public interface WorkspaceService {
  Page<WorkspaceEntity> listWorkspaces(int productId, int pageNum, int pageSize);

  FilePage listFiles(int productId, String chatId, int pageNum, int pageSize);

  List<WorkspaceFileEntity> upload(int productId, String chatId, MultipartFile[] files,
      String source);

  WorkspaceFileEntity write(int productId, String chatId, String name, String content);

  TextPage read(int productId, String chatId, String fileId, long offset, int limit);

  Download download(int productId, String chatId, String fileId);

  void delete(int productId, String chatId, String fileId);

  String importToSandbox(int productId, String chatId, String fileId);

  WorkspaceFileEntity exportFromSandbox(int productId, String chatId, String path);

  record FilePage(Page<WorkspaceFileEntity> files, long usedBytes, long maxBytes, long maxFileBytes) {}

  record TextPage(String content, long offset, long nextOffset, boolean truncated) {}

  record Download(WorkspaceFileEntity file, InputStream stream) {}
}
