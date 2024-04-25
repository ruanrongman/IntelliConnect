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
import org.springframework.stereotype.Repository;
import top.rslly.iot.models.ProductDataEntity;
import top.rslly.iot.models.ProductModelEntity;

import javax.transaction.Transactional;
import java.util.List;

@Repository
public interface ProductDataRepository extends JpaRepository<ProductDataEntity, Long> {
  List<ProductDataEntity> findAllByModelId(int modelId);

  List<ProductDataEntity> findAllByModelIdAndJsonKey(int modelId, String jsonKey);

  List<ProductDataEntity> findAllByStorageType(String storageType);

  List<ProductDataEntity> findAllByType(String type);

  @Transactional
  List<ProductDataEntity> deleteById(int id);
}
