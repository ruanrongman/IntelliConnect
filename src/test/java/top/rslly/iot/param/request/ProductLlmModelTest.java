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
package top.rslly.iot.param.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ProductLlmModelTest {
  private final Validator validator =
      Validation.buildDefaultValidatorFactory().getValidator();

  @Test
  void defaultsToDisabledThinkingWithDefaultBudget() {
    ProductLlmModel param = validParam();

    Assertions.assertFalse(param.isThinking());
    Assertions.assertEquals(1024, param.getThinkingBudget());
    Assertions.assertTrue(validator.validate(param).isEmpty());
  }

  @Test
  void acceptsThinkingBudgetBounds() {
    ProductLlmModel param = validParam();

    param.setThinkingBudget(0);
    Assertions.assertTrue(validator.validate(param).isEmpty());

    param.setThinkingBudget(8192);
    Assertions.assertTrue(validator.validate(param).isEmpty());
  }

  @Test
  void rejectsThinkingBudgetOutsideRangeAndNull() {
    ProductLlmModel param = validParam();

    param.setThinkingBudget(-1);
    Assertions.assertTrue(hasThinkingBudgetViolation(param));

    param.setThinkingBudget(8193);
    Assertions.assertTrue(hasThinkingBudgetViolation(param));

    param.setThinkingBudget(null);
    Assertions.assertTrue(hasThinkingBudgetViolation(param));
  }

  private ProductLlmModel validParam() {
    ProductLlmModel param = new ProductLlmModel();
    param.setModelName("test-model");
    param.setProductId(1);
    param.setProviderId(1);
    param.setToolsId("5");
    return param;
  }

  private boolean hasThinkingBudgetViolation(ProductLlmModel param) {
    return validator.validate(param).stream()
        .anyMatch(violation -> "thinkingBudget".contentEquals(
            violation.getPropertyPath().toString()));
  }
}

