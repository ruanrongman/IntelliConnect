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
@Table(name = "admin_config", schema = "cwliot1.8", catalog = "")
public class AdminConfigEntity {
  private int id;
  private String setKey;
  private String setValue;

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
  @Column(name = "set_key")
  public String getSetKey() {
    return setKey;
  }

  public void setSetKey(String setKey) {
    this.setKey = setKey;
  }

  @Basic
  @Column(name = "set_value")
  public String getSetValue() {
    return setValue;
  }

  public void setSetValue(String setValue) {
    this.setValue = setValue;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    AdminConfigEntity that = (AdminConfigEntity) o;
    return id == that.id && Objects.equals(setKey, that.setKey)
        && Objects.equals(setValue, that.setValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, setKey, setValue);
  }
}
