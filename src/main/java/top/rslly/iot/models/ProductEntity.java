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
@Table(name = "product", schema = "cwliot1.8", catalog = "")
public class ProductEntity {
  private int id;
  private String productName;
  private String keyvalue;
  private int register;
  private int mqttUser;

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
  @Column(name = "product_name")
  public String getProductName() {
    return productName;
  }

  public void setProductName(String productName) {
    this.productName = productName;
  }

  @Basic
  @Column(name = "keyvalue")
  public String getKeyvalue() {
    return keyvalue;
  }

  public void setKeyvalue(String keyvalue) {
    this.keyvalue = keyvalue;
  }

  @Basic
  @Column(name = "register")
  public int getRegister() {
    return register;
  }

  public void setRegister(int register) {
    this.register = register;
  }

  @Basic
  @Column(name = "mqtt_user")
  public int getMqttUser() {
    return mqttUser;
  }

  public void setMqttUser(int mqttUser) {
    this.mqttUser = mqttUser;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    ProductEntity that = (ProductEntity) o;
    return id == that.id && register == that.register && mqttUser == that.mqttUser
        && Objects.equals(productName, that.productName) && Objects.equals(keyvalue, that.keyvalue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, productName, keyvalue, register, mqttUser);
  }
}
