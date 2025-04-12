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
@Table(name = "mqtt_acl", schema = "cwliot1.8", catalog = "")
public class MqttAclEntity {
  private int id;
  private String username;
  private String permission;
  private String action;
  private String topic;
  private Byte qos;
  private Byte retain;

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
  @Column(name = "permission")
  public String getPermission() {
    return permission;
  }

  public void setPermission(String permission) {
    this.permission = permission;
  }

  @Basic
  @Column(name = "action")
  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  @Basic
  @Column(name = "topic")
  public String getTopic() {
    return topic;
  }

  public void setTopic(String topic) {
    this.topic = topic;
  }

  @Basic
  @Column(name = "qos")
  public Byte getQos() {
    return qos;
  }

  public void setQos(Byte qos) {
    this.qos = qos;
  }

  @Basic
  @Column(name = "retain")
  public Byte getRetain() {
    return retain;
  }

  public void setRetain(Byte retain) {
    this.retain = retain;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    MqttAclEntity that = (MqttAclEntity) o;
    return Objects.equals(id, that.id) && Objects.equals(username, that.username)
        && Objects.equals(permission, that.permission) && Objects.equals(action, that.action)
        && Objects.equals(topic, that.topic) && Objects.equals(qos, that.qos)
        && Objects.equals(retain, that.retain);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, username, permission, action, topic, qos, retain);
  }
}
