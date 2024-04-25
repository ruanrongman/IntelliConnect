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
package top.rslly.iot.utility;

import com.alibaba.fastjson.JSONObject;
import com.sun.management.OperatingSystemMXBean;

import java.lang.management.ManagementFactory;

public class RuntimeMessage {
  public static JSONObject getMessage() {
    final long GB = 1024 * 1024 * 1024;
    double mb = 1024 * 1024 * 1.0;
    double totalMemory = Runtime.getRuntime().totalMemory() / mb;
    double freeMemory = Runtime.getRuntime().freeMemory() / mb;
    double maxMemory = Runtime.getRuntime().maxMemory() / mb;
    double JvmMemoryUsage = totalMemory / maxMemory * 100;
    OperatingSystemMXBean operatingSystemMXBean =
        (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    /*
     * System.out.println("进程CPU使用率： "+operatingSystemMXBean.getProcessCpuLoad()* 100+"%");
     * System.out.println("系统CPU使用率： "+operatingSystemMXBean.getSystemCpuLoad()* 100+"%");
     * System.out.println("物理内存总量： "+operatingSystemMXBean.getTotalPhysicalMemorySize()/GB+"GB");
     * System.out.println("物理内存剩余可用量： "+operatingSystemMXBean.getFreePhysicalMemorySize()/GB+"GB");
     * System.out.println("内存使用率： "+(double)100*operatingSystemMXBean.getFreePhysicalMemorySize()/
     * operatingSystemMXBean.getTotalPhysicalMemorySize()+"%");
     * System.out.println("交换空间总量： "+operatingSystemMXBean.getTotalSwapSpaceSize()/GB+"GB");
     * System.out.println("交换空间剩余可用量： "+operatingSystemMXBean.getFreeSwapSpaceSize()/GB+"GB");
     * System.out.println("CPU核心数： "+operatingSystemMXBean.getAvailableProcessors()+"个");
     * System.out.println("已提交虚拟内存量： "+operatingSystemMXBean.getCommittedVirtualMemorySize());
     * System.out.println("进程已使用CPU时间： "+operatingSystemMXBean.getProcessCpuTime()/1000000000.0+"秒")
     * ;
     */
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("SystemCpuUsage", operatingSystemMXBean.getSystemCpuLoad() * 100);
    jsonObject.put("jvmMemoryUsage", JvmMemoryUsage);
    jsonObject.put("memoryUsage",
        (double) 100 - 100 * operatingSystemMXBean.getFreePhysicalMemorySize()
            / operatingSystemMXBean.getTotalPhysicalMemorySize());
    return jsonObject;
  }
}
