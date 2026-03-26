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

import com.alibaba.fastjson.annotation.JSONField;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;

@Entity
@Data
@Table(name = "user_config", schema = "cwliot1.8")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserConfigEntity {

  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  @Column(name = "user_id", nullable = false)
  @JSONField(serialize = false)
  private int userId;

  @Column(name = "product_id", nullable = false)
  @JSONField(serialize = false)
  private int productId;

  /**
   * Combine with module、task name、sub-task name, such as: `module.task_name.sub_task_name.opt_name`
   **/
  @Column(name = "name")
  private String name;

  @Column(name = "c_type")
  @Comment("Integer, Decimal(X), Text, Toggle, Range, List etc.")
  private String type;

  @Column(name = "value", length = 2000)
  private String value;

  @Column(name = "default_value")
  private String defaultValue;

  @Column(name = "v_min")
  private String min;

  @Column(name = "v_max")
  private String max;

  @Column(name = "required")
  private boolean required;

  @Column(name = "parent")
  @Comment("If this option was sub-option of another option, put the id of its parent here.")
  private long parent;

  @Column(name = "des")
  private String des;

  @Size(max = 255)
  @NotNull
  @Column(name = "is_wechat_user", nullable = false)
  private String isWechatUser;
}
