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
@Table(name = "ota_xiaozhi_passive", schema = "cwliot1.8", catalog = "")
public class OtaXiaozhiPassiveEntity {
  private int id;
  private int otaId;
  private int productId;
  private String versionName;

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
  @Column(name = "ota_id")
  public int getOtaId() {
    return otaId;
  }

  public void setOtaId(int otaId) {
    this.otaId = otaId;
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
  @Column(name = "version_name")
  public String getVersionName() {
    return versionName;
  }

  public void setVersionName(String versionName) {
    this.versionName = versionName;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    OtaXiaozhiPassiveEntity that = (OtaXiaozhiPassiveEntity) o;
    return id == that.id && otaId == that.otaId && productId == that.productId
        && Objects.equals(versionName, that.versionName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, otaId, productId, versionName);
  }
}
