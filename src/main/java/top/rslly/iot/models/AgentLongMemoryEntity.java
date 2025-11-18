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
@Table(name = "agent_long_memory", schema = "cwliot1.8", catalog = "")
public class AgentLongMemoryEntity {
  private int id;
  private int productId;
  private String memoryKey;
  private String description;
  private String memoryValue;

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
  @Column(name = "memory_key")
  public String getMemoryKey() {
    return memoryKey;
  }

  public void setMemoryKey(String memoryKey) {
    this.memoryKey = memoryKey;
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
  @Column(name = "memory_value")
  public String getMemoryValue() {
    return memoryValue;
  }

  public void setMemoryValue(String memoryValue) {
    this.memoryValue = memoryValue;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    AgentLongMemoryEntity that = (AgentLongMemoryEntity) o;
    return id == that.id && productId == that.productId && Objects.equals(memoryKey, that.memoryKey)
        && Objects.equals(description, that.description)
        && Objects.equals(memoryValue, that.memoryValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, productId, memoryKey, description, memoryValue);
  }
}
