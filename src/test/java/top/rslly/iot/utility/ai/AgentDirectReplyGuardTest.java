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
package top.rslly.iot.utility.ai;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import top.rslly.iot.utility.ai.mcp.McpAgent;
import top.rslly.iot.utility.ai.toolAgent.Agent;

import java.lang.reflect.Method;
import java.util.List;

class AgentDirectReplyGuardTest {

  @Test
  void agentSuppressesProgressOnlyDirectReplyBeforeToolObservation() throws Exception {
    Assertions.assertTrue(shouldSuppress(new Agent(), "我再抓一个澎湃新闻的热榜来综合一下～"));
    Assertions.assertTrue(shouldSuppress(new Agent(),
        "Called function mcp_xiaozhi_endpoint_1_trends_hub_get_thepaper_trending with arguments: {}"));
    Assertions.assertFalse(shouldSuppress(new Agent(), "腾讯新闻热榜主要集中在财经和民生话题。"));
  }

  @Test
  void mcpAgentSuppressesProgressOnlyDirectReplyBeforeToolObservation() throws Exception {
    Assertions.assertTrue(shouldSuppress(new McpAgent(), "查询中，我马上帮你综合热榜。"));
    Assertions.assertTrue(shouldSuppress(new McpAgent(),
        "Called function mcp_xiaozhi_endpoint_1_trends_hub_get_thepaper_trending for xiaozhi_endpoint_1:trends-hub-get-thepaper-trending with arguments: {}"));
    Assertions.assertFalse(shouldSuppress(new McpAgent(), "澎湃新闻热榜显示社会新闻关注度较高。"));
  }

  private boolean shouldSuppress(Object agent, String reply) throws Exception {
    Method method = agent.getClass().getDeclaredMethod("shouldSuppressDirectReply", String.class,
        List.class);
    method.setAccessible(true);
    return (boolean) method.invoke(agent, reply, List.of());
  }
}
