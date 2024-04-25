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
package top.rslly.iot.utility.ai.tools;

import org.springframework.stereotype.Component;
import top.rslly.iot.utility.SpringBeanUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

@Component
public class Manage {
  public String runTool(String toolName, String question, int productId) {

    var obj = SpringBeanUtils.getBean(toolName);
    if (!isInterfaceImplemented(obj.getClass(), BaseTool.class))
      throw new IllegalArgumentException("the tool not implement the interface");
    try {
      // Class<?>[] parameterTypes = obj.getClass().getParameterTypes();
      Method method = obj.getClass().getDeclaredMethod("run", String.class);
      var res = method.invoke(obj, question);
      if (res == null) {
        method = obj.getClass().getDeclaredMethod("run", String.class, int.class);
        res = method.invoke(obj, question, productId);
      }
      if (res instanceof String)
        return (String) res;
      else if (res instanceof Map)
        return (String) ((Map<?, ?>) res).get("answer");
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      // e.printStackTrace();
      return "fail to execute tool";
    }
    return "fail to execute tool";
  }

  public static boolean isInterfaceImplemented(Class<?> clazz, Class<?> interfaceClass) {
    if (!interfaceClass.isInterface()) {
      throw new IllegalArgumentException("The second argument must be an interface.");
    }

    Class<?>[] interfaces = clazz.getInterfaces();
    for (Class<?> implementedInterface : interfaces) {
      if (implementedInterface.equals(interfaceClass)) {
        return true;
      }
    }

    return false;
  }
}

