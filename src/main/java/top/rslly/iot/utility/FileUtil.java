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
package top.rslly.iot.utility;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileUtil {

  public static void uploadFile(byte[] file, String filePath, String fileName) throws Exception {
    File targetFile = new File(filePath);
    if (!targetFile.exists()) {
      if (!targetFile.mkdirs()) {
        throw new IOException("Failed to create target directory.");
      }
    }
    // 确保文件路径以系统相关的路径分隔符结尾
    String systemPath = System.getProperty("file.separator");
    String safeFilePath = filePath.endsWith(systemPath) ? filePath : filePath + systemPath;
    FileOutputStream out = new FileOutputStream(safeFilePath + fileName);
    out.write(file);
    out.flush();
    out.close();
  }


}
