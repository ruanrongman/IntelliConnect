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
package top.rslly.iot.utility.ai.promptTemplate;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StringUtils {
  public StringUtils() {}

  public static boolean isEmpty(String str) {
    return null == str || "".equals(str.trim());
  }

  public static boolean isNotEmpty(String str) {
    return null != str && !"".equals(str.trim());
  }

  public static String formatString(String tpl, Map<String, String> params) {
    List<KvWrapper> paramWrappers =
        params.entrySet().stream().map(StringUtils::toWrapper).collect(Collectors.toList());
    paramWrappers.forEach((x) -> {
      String placeHolderName = String.format("{%s}", x.key);
      x.idxInTpl = tpl.indexOf(placeHolderName);
    });
    List<String> sortedValues =
        paramWrappers.stream().sorted(Comparator.comparing(KvWrapper::getIdxInTpl))
            .map(KvWrapper::getValue).collect(Collectors.toList());
    String standardTpl = tpl;

    String placeHolderName;
    for (Iterator<String> var5 = params.keySet().iterator(); var5.hasNext(); standardTpl =
        standardTpl.replace(placeHolderName, "%s")) {
      String s = var5.next();
      // String.format("{%s}", s);
      placeHolderName = String.format("{%s}", s);
    }

    return String.format(standardTpl, sortedValues.toArray());
  }

  public static String formatString(String tpl, String placeHolderPrefix, String placeHolderSuffix,
      Map<String, String> params) {
    List<KvWrapper> paramWrappers =
        params.entrySet().stream().map(StringUtils::toWrapper).collect(Collectors.toList());
    paramWrappers.forEach((x) -> {
      String placeHolderName = placeHolderPrefix + x.key + placeHolderSuffix;
      x.idxInTpl = tpl.indexOf(placeHolderName);
    });
    String standardTpl = tpl;

    String placeHolderName;
    for (Iterator<String> var7 = params.keySet().iterator(); var7.hasNext(); standardTpl =
        standardTpl.replace(placeHolderName, "%s")) {
      String s = var7.next();
      placeHolderName = placeHolderPrefix + s + placeHolderSuffix;
    }

    return String.format(standardTpl, paramWrappers.stream()
        .sorted(Comparator.comparing(KvWrapper::getIdxInTpl)).map(KvWrapper::getValue).toArray());
  }

  private static StringUtils.KvWrapper toWrapper(Map.Entry<String, String> kv) {
    StringUtils.KvWrapper wrapper = new StringUtils.KvWrapper();
    wrapper.key = kv.getKey();
    wrapper.value = kv.getValue();
    return wrapper;
  }

  public static void main(String[] args) throws Exception {
    Map<String, String> params1 = new HashMap();
    params1.put("name", "wangxiaodong");
    String result = formatString("My name is {name}", params1);
    System.out.println(result);
    Map<String, String> params2 = new HashMap();
    params2.put("name", "kunkun");
    params2.put("like1", "sing");
    params2.put("like2", "hop");
    params2.put("like3", "basketball");
    result = formatString("My name is {name}, I like {like1}, {like2}, {like3}", params2);
    System.out.println(result);
    result = formatString("My name is {name}, I like {like1}, {like2}, {like3}", "{", "}", params2);
    System.out.println(result);
  }

  private static class KvWrapper {
    String key;
    String value;
    int idxInTpl;

    private KvWrapper() {}

    public String getKey() {
      return this.key;
    }

    public void setKey(String key) {
      this.key = key;
    }

    public String getValue() {
      return this.value;
    }

    public void setValue(String value) {
      this.value = value;
    }

    public int getIdxInTpl() {
      return this.idxInTpl;
    }

    public void setIdxInTpl(int idxInTpl) {
      this.idxInTpl = idxInTpl;
    }
  }
}
