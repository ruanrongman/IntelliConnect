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
package top.rslly.iot.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import top.rslly.iot.models.OtaXiaozhiEntity;

import java.util.List;

public interface OtaXiaozhiRepository extends JpaRepository<OtaXiaozhiEntity, Long> {

  List<OtaXiaozhiEntity> findAllById(int id);

  List<OtaXiaozhiEntity> findAllByProductId(int productId);

  List<OtaXiaozhiEntity> findAllByProductIdAndDeviceId(int productId, String deviceId);

  List<OtaXiaozhiEntity> findAllByDeviceId(String deviceId);

  List<OtaXiaozhiEntity> findAllByDeviceIdAndUserNameAndRole(String deviceId, String userName,
      String role);

  @Modifying
  @Transactional
  @Query("UPDATE OtaXiaozhiEntity e SET e.status = :setStatus WHERE e.deviceId = :deviceId")
  void updateStatus(@Param("deviceId") String deviceId, @Param("setStatus") String setStatus);

  @Modifying
  @Transactional
  @Query("UPDATE OtaXiaozhiEntity e SET e.boardType = :boardType WHERE e.deviceId = :deviceId")
  void updateBoardType(@Param("deviceId") String deviceId, @Param("boardType") String boardType);

  @Modifying
  @Transactional
  @Query("UPDATE OtaXiaozhiEntity e SET e.boardName = :boardName WHERE e.deviceId = :deviceId")
  void updateBoardName(@Param("deviceId") String deviceId, @Param("boardName") String boardName);

  @Modifying
  @Transactional
  @Query("UPDATE OtaXiaozhiEntity e SET e.status = 'disconnected'")
  void cleanStatus();

  @Transactional
  List<OtaXiaozhiEntity> deleteAllById(int id);
}
