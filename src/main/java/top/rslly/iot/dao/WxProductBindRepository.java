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
import org.springframework.data.jpa.repository.Query;
import top.rslly.iot.models.WxProductBindEntity;
import top.rslly.iot.param.response.WxBindProductResponse;

import javax.transaction.Transactional;
import java.util.List;

public interface WxProductBindRepository extends JpaRepository<WxProductBindEntity, Long> {
  List<WxProductBindEntity> findAllByOpenid(String openid);

  List<WxProductBindEntity> findByOpenidAndProductId(String openid, int productId);

  List<WxProductBindEntity> findAllByProductId(int productId);

  @Query("SELECT new top.rslly.iot.param.response.WxBindProductResponse(e.id, e.productId) FROM WxProductBindEntity e WHERE e.openid = ?1")
  List<WxBindProductResponse> findProductIdByOpenid(String openid);


  @Transactional
  List<WxProductBindEntity> deleteById(int id);

  @Transactional
  List<WxProductBindEntity> deleteAllByProductId(int productId);

  @Transactional
  List<WxProductBindEntity> deleteByOpenidAndProductId(String openid, int productId);
}
