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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Map;

// The repository's Eclipse formatter predates record and sealed interface syntax.
// @formatter:off
public record JevResponse(String model, Map<String, Answer> answers, Usage usage) {

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
  @JsonSubTypes({
      @JsonSubTypes.Type(value = NoulAnswer.class, name = "noul"),
      @JsonSubTypes.Type(value = ChoiceAnswer.class, name = "choice"),
      @JsonSubTypes.Type(value = ScoreAnswer.class, name = "score")
  })
  public sealed interface Answer permits NoulAnswer, ChoiceAnswer, ScoreAnswer {
  }

  public record NoulAnswer(Double noul) implements Answer {
  }

  public record ChoiceAnswer(String choice, Map<String, Double> probabilities,
      Double confidence) implements Answer {
  }

  public record ScoreAnswer(Double score, Map<String, Object> legend,
      Map<String, Double> probabilities, Double confidence) implements Answer {
  }

  public record Usage(@JsonProperty("input_tokens") Long inputTokens,
      @JsonProperty("output_tokens") Long outputTokens) {
  }
}
// @formatter:on
