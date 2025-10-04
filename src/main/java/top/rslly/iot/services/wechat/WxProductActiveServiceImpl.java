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
package top.rslly.iot.services.wechat;

import org.springframework.stereotype.Service;
import top.rslly.iot.dao.WxProductActiveRepository;
import top.rslly.iot.models.WxProductActiveEntity;
import top.rslly.iot.services.wechat.WxProductActiveService;

import javax.annotation.Resource;
import java.util.List;

@Service
public class WxProductActiveServiceImpl implements WxProductActiveService {
  @Resource
  private WxProductActiveRepository wxProductActiveRepository;

  @Override
  public void setUp(WxProductActiveEntity wxProductActiveEntity) {
    if (!wxProductActiveRepository.findAllByAppidAndOpenid(wxProductActiveEntity.getAppid(),
        wxProductActiveEntity.getOpenid()).isEmpty()) {
      wxProductActiveRepository.updateProperty(wxProductActiveEntity.getAppid(),
          wxProductActiveEntity.getOpenid(),
          wxProductActiveEntity.getProductId());
    }

    else
      wxProductActiveRepository.save(wxProductActiveEntity);
  }

  @Override
  public List<WxProductActiveEntity> findAllByProductIdAndAppidAndOpenid(int productId,
      String appid, String openid) {
    return wxProductActiveRepository.findAllByProductIdAndAppidAndOpenid(productId, appid, openid);
  }

  @Override
  public List<WxProductActiveEntity> findAllByAppidAndOpenid(String appid, String openid) {
    return wxProductActiveRepository.findAllByAppidAndOpenid(appid, openid);
  }
}
