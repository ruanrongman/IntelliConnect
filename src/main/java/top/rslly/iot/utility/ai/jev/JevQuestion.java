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
package top.rslly.iot.utility.ai.jev;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;
import lombok.Getter;

/** Typed factories for TypeSafe questions. Structured values are kept as JSON values. */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class JevQuestion {
  private final String type;
  private final Object instructions;
  private final Object criteria;

  private JevQuestion(String type, Object instructions, Object criteria) {
    this.type = type;
    this.instructions = instructions;
    this.criteria = criteria;
  }

  public static JevQuestion noul(Object instructions) {
    return noul(instructions, null);
  }

  /** Criteria may contain "true" and/or "false" descriptions. */
  public static JevQuestion noul(Object instructions, Map<String, ?> criteria) {
    return new JevQuestion("noul", instructions, criteria);
  }

  public static JevQuestion choice(Object instructions, Map<String, ?> criteria) {
    return new JevQuestion("choice", instructions, criteria);
  }

  public static JevQuestion score(Object instructions, List<?> criteria) {
    return new JevQuestion("score", instructions, criteria);
  }
}
