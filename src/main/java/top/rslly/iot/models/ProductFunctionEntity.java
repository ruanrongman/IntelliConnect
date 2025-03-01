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
@Table(name = "product_function", schema = "cwliot1.8", catalog = "")
public class ProductFunctionEntity {

  private int id;
  private String functionName;
  private String jsonKey;
  private String dataType;
  private int modelId;
  private String description;
  private String type;
  private String max;
  private String min;
  private String step;
  private String unit;

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
  @Column(name = "function_name")
  public String getFunctionName() {
    return functionName;
  }

  public void setFunctionName(String functionName) {
    this.functionName = functionName;
  }

  @Basic
  @Column(name = "json_key")
  public String getJsonKey() {
    return jsonKey;
  }

  public void setJsonKey(String jsonKey) {
    this.jsonKey = jsonKey;
  }

  @Basic
  @Column(name = "data_type")
  public String getDataType() {
    return dataType;
  }

  public void setDataType(String dataType) {
    this.dataType = dataType;
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
  @Column(name = "description")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @Basic
  @Column(name = "type")
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  @Basic
  @Column(name = "max")
  public String getMax() {
    return max;
  }

  public void setMax(String max) {
    this.max = max;
  }

  @Basic
  @Column(name = "min")
  public String getMin() {
    return min;
  }

  public void setMin(String min) {
    this.min = min;
  }

  @Basic
  @Column(name = "step")
  public String getStep() {
    return step;
  }

  public void setStep(String step) {
    this.step = step;
  }

  @Basic
  @Column(name = "unit")
  public String getUnit() {
    return unit;
  }

  public void setUnit(String unit) {
    this.unit = unit;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    ProductFunctionEntity that = (ProductFunctionEntity) o;
    return id == that.id && modelId == that.modelId
        && Objects.equals(functionName, that.functionName) && Objects.equals(jsonKey, that.jsonKey)
        && Objects.equals(dataType, that.dataType) && Objects.equals(description, that.description)
        && Objects.equals(type, that.type) && Objects.equals(max, that.max)
        && Objects.equals(min, that.min) && Objects.equals(step, that.step)
        && Objects.equals(unit, that.unit);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, functionName, jsonKey, dataType, modelId, description, type, max, min,
        step, unit);
  }
}
