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

import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import top.rslly.iot.dao.WxProductActiveRepository;
import top.rslly.iot.models.WxProductActiveEntity;

import javax.annotation.Resource;
import java.util.List;

@Service
public class WxProductActiveServiceImpl implements WxProductActiveService {
  @Resource
  private WxProductActiveRepository wxProductActiveRepository;

  @Override
  public void setUp(WxProductActiveEntity wxProductActiveEntity) {
    if (!wxProductActiveRepository.findAllByOpenid(wxProductActiveEntity.getOpenid()).isEmpty()) {
      wxProductActiveRepository.updateProperty(wxProductActiveEntity.getOpenid(),
          wxProductActiveEntity.getProductId());
    }

    else
      wxProductActiveRepository.save(wxProductActiveEntity);
  }

  @Override
  public List<WxProductActiveEntity> findAllByProductIdAndOpenid(int productId, String openid) {
    return wxProductActiveRepository.findAllByProductIdAndOpenid(productId, openid);
  }

  @Override
  public List<WxProductActiveEntity> findAllByOpenid(String openid) {
    return wxProductActiveRepository.findAllByOpenid(openid);
  }
}
