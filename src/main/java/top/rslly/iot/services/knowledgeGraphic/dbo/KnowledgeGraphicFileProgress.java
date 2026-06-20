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
package top.rslly.iot.services.knowledgeGraphic.dbo;

import lombok.Data;

@Data
public class KnowledgeGraphicFileProgress {
  private String taskId;

  private String status;

  private String message;

  private String fileName;

  private int percent;

  private int currentChunk;

  private int totalChunks;

  private boolean finished;

  private boolean success;

  private String errorMessage;

  public static KnowledgeGraphicFileProgress create(String taskId, String fileName) {
    KnowledgeGraphicFileProgress progress = new KnowledgeGraphicFileProgress();
    progress.setTaskId(taskId);
    progress.setFileName(fileName);
    progress.update("pending", 0, "任务已创建");
    return progress;
  }

  public synchronized void update(String status, int percent, String message) {
    this.status = status;
    this.percent = Math.max(0, Math.min(100, percent));
    this.message = message;
  }

  public synchronized void updateChunk(String fileName, int currentChunk, int totalChunks,
      int percent, String message) {
    this.fileName = fileName;
    this.currentChunk = currentChunk;
    this.totalChunks = totalChunks;
    update("extracting", percent, message);
  }

  public synchronized void complete(String message) {
    this.finished = true;
    this.success = true;
    this.errorMessage = null;
    update("success", 100, message);
  }

  public synchronized void fail(String message) {
    this.finished = true;
    this.success = false;
    this.errorMessage = message;
    update("error", this.percent, message);
  }
}
