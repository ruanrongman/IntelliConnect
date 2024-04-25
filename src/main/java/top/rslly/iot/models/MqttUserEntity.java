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
import java.sql.Timestamp;
import java.util.Objects;

@Entity
@Table(name = "mqtt_user", schema = "cwliot1.8", catalog = "")
public class MqttUserEntity {
  private int id;
  private String username;
  private String password;
  private String salt;
  private Byte isSuperuser;
  private Timestamp created;

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
  @Column(name = "username")
  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  @Basic
  @Column(name = "password")
  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  @Basic
  @Column(name = "salt")
  public String getSalt() {
    return salt;
  }

  public void setSalt(String salt) {
    this.salt = salt;
  }

  @Basic
  @Column(name = "is_superuser")
  public Byte getIsSuperuser() {
    return isSuperuser;
  }

  public void setIsSuperuser(Byte isSuperuser) {
    this.isSuperuser = isSuperuser;
  }

  @Basic
  @Column(name = "created")
  public Timestamp getCreated() {
    return created;
  }

  public void setCreated(Timestamp created) {
    this.created = created;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    MqttUserEntity that = (MqttUserEntity) o;
    return Objects.equals(id, that.id) && Objects.equals(username, that.username)
        && Objects.equals(password, that.password) && Objects.equals(salt, that.salt)
        && Objects.equals(isSuperuser, that.isSuperuser) && Objects.equals(created, that.created);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, username, password, salt, isSuperuser, created);
  }
}
