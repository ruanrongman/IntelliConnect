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

import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "product_role", schema = "cwliot1.8", catalog = "")
public class ProductRoleEntity {

  private int id;
  private int productId;
  private String assistantName;
  private String userName;
  private String role;
  private String roleIntroduction;
  private String voice;

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
  @Column(name = "product_id")
  public int getProductId() {
    return productId;
  }

  public void setProductId(int productId) {
    this.productId = productId;
  }

  @Basic
  @Column(name = "assistant_name")
  public String getAssistantName() {
    return assistantName;
  }

  public void setAssistantName(String assistantName) {
    this.assistantName = assistantName;
  }

  @Basic
  @Column(name = "user_name")
  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  @Basic
  @Column(name = "role")
  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }

  @Basic
  @Column(name = "role_introduction")
  public String getRoleIntroduction() {
    return roleIntroduction;
  }

  public void setRoleIntroduction(String roleIntroduction) {
    this.roleIntroduction = roleIntroduction;
  }

  @Basic
  @Column(name = "voice")
  public String getVoice() {
    return voice;
  }

  public void setVoice(String voice) {
    this.voice = voice;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    ProductRoleEntity that = (ProductRoleEntity) o;
    return id == that.id && productId == that.productId
        && Objects.equals(assistantName, that.assistantName)
        && Objects.equals(userName, that.userName) && Objects.equals(role, that.role)
        && Objects.equals(roleIntroduction, that.roleIntroduction)
        && Objects.equals(voice, that.voice);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, productId, assistantName, userName, role, roleIntroduction, voice);
  }
}
