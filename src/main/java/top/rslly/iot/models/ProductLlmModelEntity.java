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
package top.rslly.iot.models;

import javax.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "product_llm_model", schema = "cwliot1.8", catalog = "")
public class ProductLlmModelEntity {
  private int id;
  private String modelName;
  private int productId;
  private int providerId;
  private String toolsId;

  @Basic
  @Column(name = "model_name")
  public String getModelName() {
    return modelName;
  }

  public void setModelName(String modelName) {
    this.modelName = modelName;
  }

  @Basic
  @Column(name = "product_id")
  public int getProductId() {
    return productId;
  }

  public void setProductId(int productId) {
    this.productId = productId;
  }

  @Basic
  @Column(name = "provider_id")
  public int getProviderId() {
    return providerId;
  }

  public void setProviderId(int providerId) {
    this.providerId = providerId;
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  @Basic
  @Column(name = "tools_id")
  public String getToolsId() {
    return toolsId;
  }

  public void setToolsId(String toolsId) {
    this.toolsId = toolsId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    ProductLlmModelEntity that = (ProductLlmModelEntity) o;
    return id == that.id && productId == that.productId && providerId == that.providerId
        && Objects.equals(modelName, that.modelName) && Objects.equals(toolsId, that.toolsId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, modelName, toolsId, productId, providerId);
  }
}
