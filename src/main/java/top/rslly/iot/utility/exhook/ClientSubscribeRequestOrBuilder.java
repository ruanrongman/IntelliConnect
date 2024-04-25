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
package top.rslly.iot.utility.exhook;

public interface ClientSubscribeRequestOrBuilder extends
    // @@protoc_insertion_point(interface_extends:emqx.exhook.v2.ClientSubscribeRequest)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>.emqx.exhook.v2.ClientInfo clientinfo = 1;</code>
   */
  boolean hasClientinfo();

  /**
   * <code>.emqx.exhook.v2.ClientInfo clientinfo = 1;</code>
   */
  ClientInfo getClientinfo();

  /**
   * <code>.emqx.exhook.v2.ClientInfo clientinfo = 1;</code>
   */
  ClientInfoOrBuilder getClientinfoOrBuilder();

  /**
   * <code>repeated .emqx.exhook.v2.Property props = 2;</code>
   */
  java.util.List<Property> getPropsList();

  /**
   * <code>repeated .emqx.exhook.v2.Property props = 2;</code>
   */
  Property getProps(int index);

  /**
   * <code>repeated .emqx.exhook.v2.Property props = 2;</code>
   */
  int getPropsCount();

  /**
   * <code>repeated .emqx.exhook.v2.Property props = 2;</code>
   */
  java.util.List<? extends PropertyOrBuilder> getPropsOrBuilderList();

  /**
   * <code>repeated .emqx.exhook.v2.Property props = 2;</code>
   */
  PropertyOrBuilder getPropsOrBuilder(int index);

  /**
   * <code>repeated .emqx.exhook.v2.TopicFilter topic_filters = 3;</code>
   */
  java.util.List<TopicFilter> getTopicFiltersList();

  /**
   * <code>repeated .emqx.exhook.v2.TopicFilter topic_filters = 3;</code>
   */
  TopicFilter getTopicFilters(int index);

  /**
   * <code>repeated .emqx.exhook.v2.TopicFilter topic_filters = 3;</code>
   */
  int getTopicFiltersCount();

  /**
   * <code>repeated .emqx.exhook.v2.TopicFilter topic_filters = 3;</code>
   */
  java.util.List<? extends TopicFilterOrBuilder> getTopicFiltersOrBuilderList();

  /**
   * <code>repeated .emqx.exhook.v2.TopicFilter topic_filters = 3;</code>
   */
  TopicFilterOrBuilder getTopicFiltersOrBuilder(int index);

  /**
   * <code>.emqx.exhook.v2.RequestMeta meta = 4;</code>
   */
  boolean hasMeta();

  /**
   * <code>.emqx.exhook.v2.RequestMeta meta = 4;</code>
   */
  RequestMeta getMeta();

  /**
   * <code>.emqx.exhook.v2.RequestMeta meta = 4;</code>
   */
  RequestMetaOrBuilder getMetaOrBuilder();
}
