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
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "product_skills")
public class ProductSkillsEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Integer id;

  @NotNull
  @Column(name = "product_id", nullable = false)
  private Integer productId;

  @Size(max = 255)
  @NotNull
  @Column(name = "name", nullable = false)
  private String name;

  @Size(max = 255)
  @NotNull
  @Column(name = "description", nullable = false)
  private String description;

  @Size(max = 255)
  @NotNull
  @Column(name = "file_path", nullable = false)
  private String filePath;

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass())
      return false;
    ProductSkillsEntity that = (ProductSkillsEntity) o;
    return Objects.equals(id, that.id) && Objects.equals(productId, that.productId)
        && Objects.equals(name, that.name) && Objects.equals(description, that.description)
        && Objects.equals(filePath, that.filePath);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, productId, name, description, filePath);
  }
}
