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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Objects;

@Entity
@Table(name = "time_schedule", schema = "cwliot1.8", catalog = "")
public class TimeScheduleEntity {
  private int id;
  private String openid;
  private String taskName;
  private String cron;
  private String appid;
  private String exec;
  private String execCommand;
  private Integer productId;

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
  @Column(name = "openid")
  public String getOpenid() {
    return openid;
  }

  public void setOpenid(String openid) {
    this.openid = openid;
  }

  @Basic
  @Column(name = "task_name")
  public String getTaskName() {
    return taskName;
  }

  public void setTaskName(String taskName) {
    this.taskName = taskName;
  }

  @Basic
  @Column(name = "cron")
  public String getCron() {
    return cron;
  }

  public void setCron(String cron) {
    this.cron = cron;
  }

  @Basic
  @Column(name = "appid")
  public String getAppid() {
    return appid;
  }

  public void setAppid(String appid) {
    this.appid = appid;
  }

  @Size(max = 255)
  @NotNull
  @Column(name = "exec", nullable = false)
  public String getExec() {
    return exec;
  }

  public void setExec(String exec) {
    this.exec = exec;
  }

  @Size(max = 255)
  @NotNull
  @Column(name = "exec_command", nullable = false)
  public String getExecCommand() {
    return execCommand;
  }

  public void setExecCommand(String execCommand) {
    this.execCommand = execCommand;
  }

  @NotNull
  @Column(name = "product_id", nullable = false)
  public Integer getProductId() {
    return productId;
  }

  public void setProductId(Integer productId) {
    this.productId = productId;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass())
      return false;
    TimeScheduleEntity that = (TimeScheduleEntity) o;
    return id == that.id && Objects.equals(openid, that.openid)
        && Objects.equals(taskName, that.taskName) && Objects.equals(cron, that.cron)
        && Objects.equals(appid, that.appid) && Objects.equals(exec, that.exec)
        && Objects.equals(execCommand, that.execCommand);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, openid, taskName, cron, appid, exec, execCommand);
  }
}
