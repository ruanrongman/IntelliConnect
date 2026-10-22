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
package top.rslly.iot.controllers;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import top.rslly.iot.services.SafetyServiceImpl;
import top.rslly.iot.services.agent.WorkspaceService;
import top.rslly.iot.utility.result.*;

@RestController
@RequestMapping("/api/v2/workspaces")
@RequiredArgsConstructor
@Slf4j
public class WorkspaceController {
  private final WorkspaceService workspaceService;
  private final SafetyServiceImpl safetyService;

  @GetMapping
  public JsonResult<?> list(@RequestHeader("Authorization") String token,
      @RequestParam int productId, @RequestParam(defaultValue = "1") int pageNum,
      @RequestParam(defaultValue = "100") int pageSize) {
    authorize(token, productId);
    return ResultTool.success(workspaceService.listWorkspaces(productId, pageNum, pageSize));
  }

  @GetMapping("/files")
  public JsonResult<?> files(@RequestHeader("Authorization") String token,
      @RequestParam int productId, @RequestParam String chatId,
      @RequestParam(defaultValue = "1") int pageNum,
      @RequestParam(defaultValue = "20") int pageSize) {
    authorize(token, productId);
    return ResultTool.success(workspaceService.listFiles(productId, chatId, pageNum, pageSize));
  }

  @PostMapping(value = "/files", consumes = "multipart/form-data")
  public JsonResult<?> upload(@RequestHeader("Authorization") String token,
      @RequestParam int productId, @RequestParam String chatId,
      @RequestPart("file") MultipartFile[] uploads) {
    authorize(token, productId);
    return ResultTool.success(workspaceService.upload(productId, chatId, uploads, "upload"));
  }

  @GetMapping("/files/{fileId}/text")
  public JsonResult<?> text(@RequestHeader("Authorization") String token,
      @RequestParam int productId, @RequestParam String chatId, @PathVariable String fileId,
      @RequestParam(defaultValue = "0") long offset,
      @RequestParam(defaultValue = "16000") int limit) {
    authorize(token, productId);
    return ResultTool.success(workspaceService.read(productId, chatId, fileId, offset, limit));
  }

  @GetMapping("/files/{fileId}/download")
  public void download(@RequestHeader("Authorization") String token,
      @RequestParam int productId, @RequestParam String chatId, @PathVariable String fileId,
      HttpServletResponse response) throws IOException {
    authorize(token, productId);
    var download = workspaceService.download(productId, chatId, fileId);
    try (var input = download.stream()) {
      response.setContentType("application/octet-stream");
      response.setHeader("X-Content-Type-Options", "nosniff");
      response.setHeader("Cache-Control", "no-store");
      response.setHeader("Content-Disposition", ContentDisposition.attachment()
          .filename(download.file().getName(), StandardCharsets.UTF_8).build().toString());
      response.setContentLengthLong(download.file().getSize());
      input.transferTo(response.getOutputStream());
    }
  }

  @DeleteMapping("/files/{fileId}")
  public JsonResult<?> delete(@RequestHeader("Authorization") String token,
      @RequestParam int productId, @RequestParam String chatId, @PathVariable String fileId) {
    authorize(token, productId);
    workspaceService.delete(productId, chatId, fileId);
    return ResultTool.success();
  }

  private void authorize(String token, int productId) {
    if (!safetyService.controlAuthorizeProduct(token, productId)) {
      throw new SecurityException("No permission for this product");
    }
  }

  @ExceptionHandler(SecurityException.class)
  public ResponseEntity<JsonResult<?>> forbidden(SecurityException exception) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(ResultTool.fail(ResultCode.NO_PERMISSION));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<JsonResult<?>> invalid(IllegalArgumentException exception) {
    JsonResult<?> result = ResultTool.fail(ResultCode.PARAM_NOT_VALID);
    result.setErrorMsg(exception.getMessage());
    return ResponseEntity.badRequest().body(result);
  }

  @ExceptionHandler(org.springframework.dao.DataAccessException.class)
  public ResponseEntity<JsonResult<?>> storageError(Exception exception) {
    log.error("Workspace database operation failed", exception);
    JsonResult<?> result = ResultTool.fail(ResultCode.COMMON_FAIL);
    result.setErrorMsg("工作区存储暂不可用，请检查数据库迁移和存储配置");
    return ResponseEntity.internalServerError().body(result);
  }
}
