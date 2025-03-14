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
package top.rslly.iot.services.thingsModel;

import top.rslly.iot.models.ProductEntity;
import top.rslly.iot.models.ProductModelEntity;
import top.rslly.iot.param.prompt.ProductModelDescription;
import top.rslly.iot.param.request.ProductModel;
import top.rslly.iot.utility.result.JsonResult;

import java.util.List;

public interface ProductModelService {
  JsonResult<?> getProductModel(String token);

  JsonResult<?> getProductModel(int productId);

  JsonResult<?> postProductModel(ProductModel productModel);

  List<ProductModelDescription> getDescription(int productId);

  List<ProductModelEntity> findAllById(int id);

  List<ProductModelEntity> findAllByProductId(int productId);

  List<ProductModelEntity> findAllByProductIdAndName(int productId, String name);

  JsonResult<?> deleteProductModel(int id);

}
