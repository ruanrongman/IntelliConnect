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
@Table(name = "product_device", schema = "cwliot1.8", catalog = "")
public class ProductDeviceEntity {
  private int id;
  private int modelId;
  private String clientId;
  private String subscribeTopic;
  private String online;
  private int allow;
  private String name;
  private String description;
  private String password;

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
  @Column(name = "model_id")
  public int getModelId() {
    return modelId;
  }

  public void setModelId(int modelId) {
    this.modelId = modelId;
  }

  @Basic
  @Column(name = "client_id")
  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  @Basic
  @Column(name = "subscribe_topic")
  public String getSubscribeTopic() {
    return subscribeTopic;
  }

  public void setSubscribeTopic(String subscribeTopic) {
    this.subscribeTopic = subscribeTopic;
  }

  @Basic
  @Column(name = "online")
  public String getOnline() {
    return online;
  }

  public void setOnline(String online) {
    this.online = online;
  }

  @Basic
  @Column(name = "allow")
  public int getAllow() {
    return allow;
  }

  public void setAllow(int allow) {
    this.allow = allow;
  }

  @Basic
  @Column(name = "name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Basic
  @Column(name = "description")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Basic
  @Column(name = "password")
  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    ProductDeviceEntity that = (ProductDeviceEntity) o;
    return id == that.id && modelId == that.modelId && allow == that.allow
        && Objects.equals(clientId, that.clientId)
        && Objects.equals(subscribeTopic, that.subscribeTopic)
        && Objects.equals(online, that.online) && Objects.equals(name, that.name)
        && Objects.equals(description, that.description) && Objects.equals(password, that.password);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, password, description, modelId, clientId, subscribeTopic, online,
        allow);
  }
}
