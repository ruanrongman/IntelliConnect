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
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.QueryByExampleExecutor;
import org.springframework.transaction.annotation.Transactional;
import top.rslly.iot.models.DataEntity;

import java.util.List;

public interface DataRepository extends JpaRepository<DataEntity, Long>,
    JpaSpecificationExecutor<DataEntity>, QueryByExampleExecutor<DataEntity> {
  List<DataEntity> findAllByTimeBetweenAndDeviceIdAndJsonKey(long time, long time2, int deviceId,
      String jsonKey);


  List<DataEntity> findByCharacteristicAndDeviceId(String characteristic, int deviceId);

  List<DataEntity> findAllByDeviceId(int deviceId);

  List<DataEntity> findAllByDeviceIdAndJsonKey(int deviceId, String jsonKey);

  List<DataEntity> findAllByTimeBetweenAndDeviceId(long time, long time2, int deviceId);

  @Query(
      value = "SELECT * FROM data WHERE device_id =?1 And json_key=?2 ORDER BY time DESC LIMIT 1",
      nativeQuery = true)
  List<DataEntity> findAllBySort(int deviceId, String jsonKey);

  @Transactional
  List<DataEntity> deleteAllByDeviceIdAndJsonKey(int deviceId, String jsonKey);

  @Transactional
  List<DataEntity> deleteAllByTimeBeforeAndDeviceIdAndJsonKey(long time, int deviceId,
      String jsonKey);

  @Transactional
  List<DataEntity> deleteByDeviceId(int deviceId);
  /*
   * @Transactional List<DataEntity> deleteById(int id);
   * 
   * @Transactional
   * 
   * @Modifying
   * 
   * @Query(value = "DELETE from data WHERE id IN ?1", nativeQuery = true) void
   * deleteByList(List<Integer> idList);
   */
}
