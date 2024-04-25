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

/**
 * Protobuf type {@code emqx.exhook.v2.ClientSubscribeRequest}
 */
public final class ClientSubscribeRequest extends com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:emqx.exhook.v2.ClientSubscribeRequest)
    ClientSubscribeRequestOrBuilder {
  private static final long serialVersionUID = 0L;

  // Use ClientSubscribeRequest.newBuilder() to construct.
  private ClientSubscribeRequest(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }

  private ClientSubscribeRequest() {
    props_ = java.util.Collections.emptyList();
    topicFilters_ = java.util.Collections.emptyList();
  }

  @Override
  public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
    return this.unknownFields;
  }

  private ClientSubscribeRequest(com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    this();
    if (extensionRegistry == null) {
      throw new NullPointerException();
    }
    int mutable_bitField0_ = 0;
    com.google.protobuf.UnknownFieldSet.Builder unknownFields =
        com.google.protobuf.UnknownFieldSet.newBuilder();
    try {
      boolean done = false;
      while (!done) {
        int tag = input.readTag();
        switch (tag) {
          case 0:
            done = true;
            break;
          default: {
            if (!parseUnknownFieldProto3(input, unknownFields, extensionRegistry, tag)) {
              done = true;
            }
            break;
          }
          case 10: {
            ClientInfo.Builder subBuilder = null;
            if (clientinfo_ != null) {
              subBuilder = clientinfo_.toBuilder();
            }
            clientinfo_ = input.readMessage(ClientInfo.parser(), extensionRegistry);
            if (subBuilder != null) {
              subBuilder.mergeFrom(clientinfo_);
              clientinfo_ = subBuilder.buildPartial();
            }

            break;
          }
          case 18: {
            if (!((mutable_bitField0_ & 0x00000002) == 0x00000002)) {
              props_ = new java.util.ArrayList<Property>();
              mutable_bitField0_ |= 0x00000002;
            }
            props_.add(input.readMessage(Property.parser(), extensionRegistry));
            break;
          }
          case 26: {
            if (!((mutable_bitField0_ & 0x00000004) == 0x00000004)) {
              topicFilters_ = new java.util.ArrayList<TopicFilter>();
              mutable_bitField0_ |= 0x00000004;
            }
            topicFilters_.add(input.readMessage(TopicFilter.parser(), extensionRegistry));
            break;
          }
          case 34: {
            RequestMeta.Builder subBuilder = null;
            if (meta_ != null) {
              subBuilder = meta_.toBuilder();
            }
            meta_ = input.readMessage(RequestMeta.parser(), extensionRegistry);
            if (subBuilder != null) {
              subBuilder.mergeFrom(meta_);
              meta_ = subBuilder.buildPartial();
            }

            break;
          }
        }
      }
    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
      throw e.setUnfinishedMessage(this);
    } catch (java.io.IOException e) {
      throw new com.google.protobuf.InvalidProtocolBufferException(e).setUnfinishedMessage(this);
    } finally {
      if (((mutable_bitField0_ & 0x00000002) == 0x00000002)) {
        props_ = java.util.Collections.unmodifiableList(props_);
      }
      if (((mutable_bitField0_ & 0x00000004) == 0x00000004)) {
        topicFilters_ = java.util.Collections.unmodifiableList(topicFilters_);
      }
      this.unknownFields = unknownFields.build();
      makeExtensionsImmutable();
    }
  }

  public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
    return EmqxExHookProto.internal_static_emqx_exhook_v2_ClientSubscribeRequest_descriptor;
  }

  protected FieldAccessorTable internalGetFieldAccessorTable() {
    return EmqxExHookProto.internal_static_emqx_exhook_v2_ClientSubscribeRequest_fieldAccessorTable
        .ensureFieldAccessorsInitialized(ClientSubscribeRequest.class, Builder.class);
  }

  private int bitField0_;
  public static final int CLIENTINFO_FIELD_NUMBER = 1;
  private ClientInfo clientinfo_;

  /**
   * <code>.emqx.exhook.v2.ClientInfo clientinfo = 1;</code>
   */
  public boolean hasClientinfo() {
    return clientinfo_ != null;
  }

  /**
   * <code>.emqx.exhook.v2.ClientInfo clientinfo = 1;</code>
   */
  public ClientInfo getClientinfo() {
    return clientinfo_ == null ? ClientInfo.getDefaultInstance() : clientinfo_;
  }

  /**
   * <code>.emqx.exhook.v2.ClientInfo clientinfo = 1;</code>
   */
  public ClientInfoOrBuilder getClientinfoOrBuilder() {
    return getClientinfo();
  }

  public static final int PROPS_FIELD_NUMBER = 2;
  private java.util.List<Property> props_;

  /**
   * <code>repeated .emqx.exhook.v2.Property props = 2;</code>
   */
  public java.util.List<Property> getPropsList() {
    return props_;
  }

  /**
   * <code>repeated .emqx.exhook.v2.Property props = 2;</code>
   */
  public java.util.List<? extends PropertyOrBuilder> getPropsOrBuilderList() {
    return props_;
  }

  /**
   * <code>repeated .emqx.exhook.v2.Property props = 2;</code>
   */
  public int getPropsCount() {
    return props_.size();
  }

  /**
   * <code>repeated .emqx.exhook.v2.Property props = 2;</code>
   */
  public Property getProps(int index) {
    return props_.get(index);
  }

  /**
   * <code>repeated .emqx.exhook.v2.Property props = 2;</code>
   */
  public PropertyOrBuilder getPropsOrBuilder(int index) {
    return props_.get(index);
  }

  public static final int TOPIC_FILTERS_FIELD_NUMBER = 3;
  private java.util.List<TopicFilter> topicFilters_;

  /**
   * <code>repeated .emqx.exhook.v2.TopicFilter topic_filters = 3;</code>
   */
  public java.util.List<TopicFilter> getTopicFiltersList() {
    return topicFilters_;
  }

  /**
   * <code>repeated .emqx.exhook.v2.TopicFilter topic_filters = 3;</code>
   */
  public java.util.List<? extends TopicFilterOrBuilder> getTopicFiltersOrBuilderList() {
    return topicFilters_;
  }

  /**
   * <code>repeated .emqx.exhook.v2.TopicFilter topic_filters = 3;</code>
   */
  public int getTopicFiltersCount() {
    return topicFilters_.size();
  }

  /**
   * <code>repeated .emqx.exhook.v2.TopicFilter topic_filters = 3;</code>
   */
  public TopicFilter getTopicFilters(int index) {
    return topicFilters_.get(index);
  }

  /**
   * <code>repeated .emqx.exhook.v2.TopicFilter topic_filters = 3;</code>
   */
  public TopicFilterOrBuilder getTopicFiltersOrBuilder(int index) {
    return topicFilters_.get(index);
  }

  public static final int META_FIELD_NUMBER = 4;
  private RequestMeta meta_;

  /**
   * <code>.emqx.exhook.v2.RequestMeta meta = 4;</code>
   */
  public boolean hasMeta() {
    return meta_ != null;
  }

  /**
   * <code>.emqx.exhook.v2.RequestMeta meta = 4;</code>
   */
  public RequestMeta getMeta() {
    return meta_ == null ? RequestMeta.getDefaultInstance() : meta_;
  }

  /**
   * <code>.emqx.exhook.v2.RequestMeta meta = 4;</code>
   */
  public RequestMetaOrBuilder getMetaOrBuilder() {
    return getMeta();
  }

  private byte memoizedIsInitialized = -1;

  public final boolean isInitialized() {
    byte isInitialized = memoizedIsInitialized;
    if (isInitialized == 1)
      return true;
    if (isInitialized == 0)
      return false;

    memoizedIsInitialized = 1;
    return true;
  }

  public void writeTo(com.google.protobuf.CodedOutputStream output) throws java.io.IOException {
    if (clientinfo_ != null) {
      output.writeMessage(1, getClientinfo());
    }
    for (int i = 0; i < props_.size(); i++) {
      output.writeMessage(2, props_.get(i));
    }
    for (int i = 0; i < topicFilters_.size(); i++) {
      output.writeMessage(3, topicFilters_.get(i));
    }
    if (meta_ != null) {
      output.writeMessage(4, getMeta());
    }
    unknownFields.writeTo(output);
  }

  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1)
      return size;

    size = 0;
    if (clientinfo_ != null) {
      size += com.google.protobuf.CodedOutputStream.computeMessageSize(1, getClientinfo());
    }
    for (int i = 0; i < props_.size(); i++) {
      size += com.google.protobuf.CodedOutputStream.computeMessageSize(2, props_.get(i));
    }
    for (int i = 0; i < topicFilters_.size(); i++) {
      size += com.google.protobuf.CodedOutputStream.computeMessageSize(3, topicFilters_.get(i));
    }
    if (meta_ != null) {
      size += com.google.protobuf.CodedOutputStream.computeMessageSize(4, getMeta());
    }
    size += unknownFields.getSerializedSize();
    memoizedSize = size;
    return size;
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof ClientSubscribeRequest)) {
      return super.equals(obj);
    }
    ClientSubscribeRequest other = (ClientSubscribeRequest) obj;

    boolean result = true;
    result = result && (hasClientinfo() == other.hasClientinfo());
    if (hasClientinfo()) {
      result = result && getClientinfo().equals(other.getClientinfo());
    }
    result = result && getPropsList().equals(other.getPropsList());
    result = result && getTopicFiltersList().equals(other.getTopicFiltersList());
    result = result && (hasMeta() == other.hasMeta());
    if (hasMeta()) {
      result = result && getMeta().equals(other.getMeta());
    }
    result = result && unknownFields.equals(other.unknownFields);
    return result;
  }

  @Override
  public int hashCode() {
    if (memoizedHashCode != 0) {
      return memoizedHashCode;
    }
    int hash = 41;
    hash = (19 * hash) + getDescriptor().hashCode();
    if (hasClientinfo()) {
      hash = (37 * hash) + CLIENTINFO_FIELD_NUMBER;
      hash = (53 * hash) + getClientinfo().hashCode();
    }
    if (getPropsCount() > 0) {
      hash = (37 * hash) + PROPS_FIELD_NUMBER;
      hash = (53 * hash) + getPropsList().hashCode();
    }
    if (getTopicFiltersCount() > 0) {
      hash = (37 * hash) + TOPIC_FILTERS_FIELD_NUMBER;
      hash = (53 * hash) + getTopicFiltersList().hashCode();
    }
    if (hasMeta()) {
      hash = (37 * hash) + META_FIELD_NUMBER;
      hash = (53 * hash) + getMeta().hashCode();
    }
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static ClientSubscribeRequest parseFrom(java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static ClientSubscribeRequest parseFrom(java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static ClientSubscribeRequest parseFrom(com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static ClientSubscribeRequest parseFrom(com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static ClientSubscribeRequest parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static ClientSubscribeRequest parseFrom(byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static ClientSubscribeRequest parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }

  public static ClientSubscribeRequest parseFrom(java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input,
        extensionRegistry);
  }

  public static ClientSubscribeRequest parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
  }

  public static ClientSubscribeRequest parseDelimitedFrom(java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input,
        extensionRegistry);
  }

  public static ClientSubscribeRequest parseFrom(com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }

  public static ClientSubscribeRequest parseFrom(com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input,
        extensionRegistry);
  }

  public Builder newBuilderForType() {
    return newBuilder();
  }

  public static Builder newBuilder() {
    return DEFAULT_INSTANCE.toBuilder();
  }

  public static Builder newBuilder(ClientSubscribeRequest prototype) {
    return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
  }

  public Builder toBuilder() {
    return this == DEFAULT_INSTANCE ? new Builder() : new Builder().mergeFrom(this);
  }

  @Override
  protected Builder newBuilderForType(BuilderParent parent) {
    Builder builder = new Builder(parent);
    return builder;
  }

  /**
   * Protobuf type {@code emqx.exhook.v2.ClientSubscribeRequest}
   */
  public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder>
      implements
      // @@protoc_insertion_point(builder_implements:emqx.exhook.v2.ClientSubscribeRequest)
      ClientSubscribeRequestOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return EmqxExHookProto.internal_static_emqx_exhook_v2_ClientSubscribeRequest_descriptor;
    }

    protected FieldAccessorTable internalGetFieldAccessorTable() {
      return EmqxExHookProto.internal_static_emqx_exhook_v2_ClientSubscribeRequest_fieldAccessorTable
          .ensureFieldAccessorsInitialized(ClientSubscribeRequest.class, Builder.class);
    }

    // Construct using top.rslly.iot.utility.exhook.ClientSubscribeRequest.newBuilder()
    private Builder() {
      maybeForceBuilderInitialization();
    }

    private Builder(BuilderParent parent) {
      super(parent);
      maybeForceBuilderInitialization();
    }

    private void maybeForceBuilderInitialization() {
      if (com.google.protobuf.GeneratedMessageV3.alwaysUseFieldBuilders) {
        getPropsFieldBuilder();
        getTopicFiltersFieldBuilder();
      }
    }

    public Builder clear() {
      super.clear();
      if (clientinfoBuilder_ == null) {
        clientinfo_ = null;
      } else {
        clientinfo_ = null;
        clientinfoBuilder_ = null;
      }
      if (propsBuilder_ == null) {
        props_ = java.util.Collections.emptyList();
        bitField0_ = (bitField0_ & ~0x00000002);
      } else {
        propsBuilder_.clear();
      }
      if (topicFiltersBuilder_ == null) {
        topicFilters_ = java.util.Collections.emptyList();
        bitField0_ = (bitField0_ & ~0x00000004);
      } else {
        topicFiltersBuilder_.clear();
      }
      if (metaBuilder_ == null) {
        meta_ = null;
      } else {
        meta_ = null;
        metaBuilder_ = null;
      }
      return this;
    }

    public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
      return EmqxExHookProto.internal_static_emqx_exhook_v2_ClientSubscribeRequest_descriptor;
    }

    public ClientSubscribeRequest getDefaultInstanceForType() {
      return ClientSubscribeRequest.getDefaultInstance();
    }

    public ClientSubscribeRequest build() {
      ClientSubscribeRequest result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    public ClientSubscribeRequest buildPartial() {
      ClientSubscribeRequest result = new ClientSubscribeRequest(this);
      int from_bitField0_ = bitField0_;
      int to_bitField0_ = 0;
      if (clientinfoBuilder_ == null) {
        result.clientinfo_ = clientinfo_;
      } else {
        result.clientinfo_ = clientinfoBuilder_.build();
      }
      if (propsBuilder_ == null) {
        if (((bitField0_ & 0x00000002) == 0x00000002)) {
          props_ = java.util.Collections.unmodifiableList(props_);
          bitField0_ = (bitField0_ & ~0x00000002);
        }
        result.props_ = props_;
      } else {
        result.props_ = propsBuilder_.build();
      }
      if (topicFiltersBuilder_ == null) {
        if (((bitField0_ & 0x00000004) == 0x00000004)) {
          topicFilters_ = java.util.Collections.unmodifiableList(topicFilters_);
          bitField0_ = (bitField0_ & ~0x00000004);
        }
        result.topicFilters_ = topicFilters_;
      } else {
        result.topicFilters_ = topicFiltersBuilder_.build();
      }
      if (metaBuilder_ == null) {
        result.meta_ = meta_;
      } else {
        result.meta_ = metaBuilder_.build();
      }
      result.bitField0_ = to_bitField0_;
      onBuilt();
      return result;
    }

    public Builder clone() {
      return (Builder) super.clone();
    }

    public Builder setField(com.google.protobuf.Descriptors.FieldDescriptor field, Object value) {
      return (Builder) super.setField(field, value);
    }

    public Builder clearField(com.google.protobuf.Descriptors.FieldDescriptor field) {
      return (Builder) super.clearField(field);
    }

    public Builder clearOneof(com.google.protobuf.Descriptors.OneofDescriptor oneof) {
      return (Builder) super.clearOneof(oneof);
    }

    public Builder setRepeatedField(com.google.protobuf.Descriptors.FieldDescriptor field,
        int index, Object value) {
      return (Builder) super.setRepeatedField(field, index, value);
    }

    public Builder addRepeatedField(com.google.protobuf.Descriptors.FieldDescriptor field,
        Object value) {
      return (Builder) super.addRepeatedField(field, value);
    }

    public Builder mergeFrom(com.google.protobuf.Message other) {
      if (other instanceof ClientSubscribeRequest) {
        return mergeFrom((ClientSubscribeRequest) other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(ClientSubscribeRequest other) {
      if (other == ClientSubscribeRequest.getDefaultInstance())
        return this;
      if (other.hasClientinfo()) {
        mergeClientinfo(other.getClientinfo());
      }
      if (propsBuilder_ == null) {
        if (!other.props_.isEmpty()) {
          if (props_.isEmpty()) {
            props_ = other.props_;
            bitField0_ = (bitField0_ & ~0x00000002);
          } else {
            ensurePropsIsMutable();
            props_.addAll(other.props_);
          }
          onChanged();
        }
      } else {
        if (!other.props_.isEmpty()) {
          if (propsBuilder_.isEmpty()) {
            propsBuilder_.dispose();
            propsBuilder_ = null;
            props_ = other.props_;
            bitField0_ = (bitField0_ & ~0x00000002);
            propsBuilder_ = com.google.protobuf.GeneratedMessageV3.alwaysUseFieldBuilders
                ? getPropsFieldBuilder()
                : null;
          } else {
            propsBuilder_.addAllMessages(other.props_);
          }
        }
      }
      if (topicFiltersBuilder_ == null) {
        if (!other.topicFilters_.isEmpty()) {
          if (topicFilters_.isEmpty()) {
            topicFilters_ = other.topicFilters_;
            bitField0_ = (bitField0_ & ~0x00000004);
          } else {
            ensureTopicFiltersIsMutable();
            topicFilters_.addAll(other.topicFilters_);
          }
          onChanged();
        }
      } else {
        if (!other.topicFilters_.isEmpty()) {
          if (topicFiltersBuilder_.isEmpty()) {
            topicFiltersBuilder_.dispose();
            topicFiltersBuilder_ = null;
            topicFilters_ = other.topicFilters_;
            bitField0_ = (bitField0_ & ~0x00000004);
            topicFiltersBuilder_ = com.google.protobuf.GeneratedMessageV3.alwaysUseFieldBuilders
                ? getTopicFiltersFieldBuilder()
                : null;
          } else {
            topicFiltersBuilder_.addAllMessages(other.topicFilters_);
          }
        }
      }
      if (other.hasMeta()) {
        mergeMeta(other.getMeta());
      }
      this.mergeUnknownFields(other.unknownFields);
      onChanged();
      return this;
    }

    public final boolean isInitialized() {
      return true;
    }

    public Builder mergeFrom(com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
      ClientSubscribeRequest parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (ClientSubscribeRequest) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private int bitField0_;

    private ClientInfo clientinfo_ = null;
    private com.google.protobuf.SingleFieldBuilderV3<ClientInfo, ClientInfo.Builder, ClientInfoOrBuilder> clientinfoBuilder_;

    /**
     * <code>.emqx.exhook.v2.ClientInfo clientinfo = 1;</code>
     */
    public boolean hasClientinfo() {
      return clientinfoBuilder_ != null || clientinfo_ != null;
    }

    /**
     * <code>.emqx.exhook.v2.ClientInfo clientinfo = 1;</code>
     */
    public ClientInfo getClientinfo() {
      if (clientinfoBuilder_ == null) {
        return clientinfo_ == null ? ClientInfo.getDefaultInstance() : clientinfo_;
      } else {
        return clientinfoBuilder_.getMessage();
      }
    }

    /**
     * <code>.emqx.exhook.v2.ClientInfo clientinfo = 1;</code>
     */
    public Builder setClientinfo(ClientInfo value) {
      if (clientinfoBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        clientinfo_ = value;
        onChanged();
      } else {
        clientinfoBuilder_.setMessage(value);
      }

      return this;
    }

    /**
     * <code>.emqx.exhook.v2.ClientInfo clientinfo = 1;</code>
     */
    public Builder setClientinfo(ClientInfo.Builder builderForValue) {
      if (clientinfoBuilder_ == null) {
        clientinfo_ = builderForValue.build();
        onChanged();
      } else {
        clientinfoBuilder_.setMessage(builderForValue.build());
      }

      return this;
    }

    /**
     * <code>.emqx.exhook.v2.ClientInfo clientinfo = 1;</code>
     */
    public Builder mergeClientinfo(ClientInfo value) {
      if (clientinfoBuilder_ == null) {
        if (clientinfo_ != null) {
          clientinfo_ = ClientInfo.newBuilder(clientinfo_).mergeFrom(value).buildPartial();
        } else {
          clientinfo_ = value;
        }
        onChanged();
      } else {
        clientinfoBuilder_.mergeFrom(value);
      }

      return this;
    }

    /**
     * <code>.emqx.exhook.v2.ClientInfo clientinfo = 1;</code>
     */
    public Builder clearClientinfo() {
      if (clientinfoBuilder_ == null) {
        clientinfo_ = null;
        onChanged();
      } else {
        clientinfo_ = null;
        clientinfoBuilder_ = null;
      }

      return this;
    }

    /**
     * <code>.emqx.exhook.v2.ClientInfo clientinfo = 1;</code>
     */
    public ClientInfo.Builder getClientinfoBuilder() {

      onChanged();
      return getClientinfoFieldBuilder().getBuilder();
    }

    /**
     * <code>.emqx.exhook.v2.ClientInfo clientinfo = 1;</code>
     */
    public ClientInfoOrBuilder getClientinfoOrBuilder() {
      if (clientinfoBuilder_ != null) {
        return clientinfoBuilder_.getMessageOrBuilder();
      } else {
        return clientinfo_ == null ? ClientInfo.getDefaultInstance() : clientinfo_;
      }
    }

    /**
     * <code>.emqx.exhook.v2.ClientInfo clientinfo = 1;</code>
     */
    private com.google.protobuf.SingleFieldBuilderV3<ClientInfo, ClientInfo.Builder, ClientInfoOrBuilder> getClientinfoFieldBuilder() {
      if (clientinfoBuilder_ == null) {
        clientinfoBuilder_ =
            new com.google.protobuf.SingleFieldBuilderV3<ClientInfo, ClientInfo.Builder, ClientInfoOrBuilder>(
                getClientinfo(), getParentForChildren(), isClean());
        clientinfo_ = null;
      }
      return clientinfoBuilder_;
    }

    private java.util.List<Property> props_ = java.util.Collections.emptyList();

    private void ensurePropsIsMutable() {
      if (!((bitField0_ & 0x00000002) == 0x00000002)) {
        props_ = new java.util.ArrayList<Property>(props_);
        bitField0_ |= 0x00000002;
      }
    }

    private com.google.protobuf.RepeatedFieldBuilderV3<Property, Property.Builder, PropertyOrBuilder> propsBuilder_;

    /**
     * <code>repeated .emqx.exhook.v2.Property props = 2;</code>
     */
    public java.util.List<Property> getPropsList() {
      if (propsBuilder_ == null) {
        return java.util.Collections.unmodifiableList(props_);
      } else {
        return propsBuilder_.getMessageList();
      }
    }

    /**
     * <code>repeated .emqx.exhook.v2.Property props = 2;</code>
     */
    public int getPropsCount() {
      if (propsBuilder_ == null) {
        return props_.size();
      } else {
        return propsBuilder_.getCount();
      }
    }

    /**
     * <code>repeated .emqx.exhook.v2.Property props = 2;</code>
     */
    public Property getProps(int index) {
      if (propsBuilder_ == null) {
        return props_.get(index);
      } else {
        return propsBuilder_.getMessage(index);
      }
    }

    /**
     * <code>repeated .emqx.exhook.v2.Property props = 2;</code>
     */
    public Builder setProps(int index, Property value) {
      if (propsBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensurePropsIsMutable();
        props_.set(index, value);
        onChanged();
      } else {
        propsBuilder_.setMessage(index, value);
      }
      return this;
    }

    /**
     * <code>repeated .emqx.exhook.v2.Property props = 2;</code>
     */
    public Builder setProps(int index, Property.Builder builderForValue) {
      if (propsBuilder_ == null) {
        ensurePropsIsMutable();
        props_.set(index, builderForValue.build());
        onChanged();
      } else {
        propsBuilder_.setMessage(index, builderForValue.build());
      }
      return this;
    }

    /**
     * <code>repeated .emqx.exhook.v2.Property props = 2;</code>
     */
    public Builder addProps(Property value) {
      if (propsBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensurePropsIsMutable();
        props_.add(value);
        onChanged();
      } else {
        propsBuilder_.addMessage(value);
      }
      return this;
    }

    /**
     * <code>repeated .emqx.exhook.v2.Property props = 2;</code>
     */
    public Builder addProps(int index, Property value) {
      if (propsBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensurePropsIsMutable();
        props_.add(index, value);
        onChanged();
      } else {
        propsBuilder_.addMessage(index, value);
      }
      return this;
    }

    /**
     * <code>repeated .emqx.exhook.v2.Property props = 2;</code>
     */
    public Builder addProps(Property.Builder builderForValue) {
      if (propsBuilder_ == null) {
        ensurePropsIsMutable();
        props_.add(builderForValue.build());
        onChanged();
      } else {
        propsBuilder_.addMessage(builderForValue.build());
      }
      return this;
    }

    /**
     * <code>repeated .emqx.exhook.v2.Property props = 2;</code>
     */
    public Builder addProps(int index, Property.Builder builderForValue) {
      if (propsBuilder_ == null) {
        ensurePropsIsMutable();
        props_.add(index, builderForValue.build());
        onChanged();
      } else {
        propsBuilder_.addMessage(index, builderForValue.build());
      }
      return this;
    }

    /**
     * <code>repeated .emqx.exhook.v2.Property props = 2;</code>
     */
    public Builder addAllProps(Iterable<? extends Property> values) {
      if (propsBuilder_ == null) {
        ensurePropsIsMutable();
        com.google.protobuf.AbstractMessageLite.Builder.addAll(values, props_);
        onChanged();
      } else {
        propsBuilder_.addAllMessages(values);
      }
      return this;
    }

    /**
     * <code>repeated .emqx.exhook.v2.Property props = 2;</code>
     */
    public Builder clearProps() {
      if (propsBuilder_ == null) {
        props_ = java.util.Collections.emptyList();
        bitField0_ = (bitField0_ & ~0x00000002);
        onChanged();
      } else {
        propsBuilder_.clear();
      }
      return this;
    }

    /**
     * <code>repeated .emqx.exhook.v2.Property props = 2;</code>
     */
    public Builder removeProps(int index) {
      if (propsBuilder_ == null) {
        ensurePropsIsMutable();
        props_.remove(index);
        onChanged();
      } else {
        propsBuilder_.remove(index);
      }
      return this;
    }

    /**
     * <code>repeated .emqx.exhook.v2.Property props = 2;</code>
     */
    public Property.Builder getPropsBuilder(int index) {
      return getPropsFieldBuilder().getBuilder(index);
    }

    /**
     * <code>repeated .emqx.exhook.v2.Property props = 2;</code>
     */
    public PropertyOrBuilder getPropsOrBuilder(int index) {
      if (propsBuilder_ == null) {
        return props_.get(index);
      } else {
        return propsBuilder_.getMessageOrBuilder(index);
      }
    }

    /**
     * <code>repeated .emqx.exhook.v2.Property props = 2;</code>
     */
    public java.util.List<? extends PropertyOrBuilder> getPropsOrBuilderList() {
      if (propsBuilder_ != null) {
        return propsBuilder_.getMessageOrBuilderList();
      } else {
        return java.util.Collections.unmodifiableList(props_);
      }
    }

    /**
     * <code>repeated .emqx.exhook.v2.Property props = 2;</code>
     */
    public Property.Builder addPropsBuilder() {
      return getPropsFieldBuilder().addBuilder(Property.getDefaultInstance());
    }

    /**
     * <code>repeated .emqx.exhook.v2.Property props = 2;</code>
     */
    public Property.Builder addPropsBuilder(int index) {
      return getPropsFieldBuilder().addBuilder(index, Property.getDefaultInstance());
    }

    /**
     * <code>repeated .emqx.exhook.v2.Property props = 2;</code>
     */
    public java.util.List<Property.Builder> getPropsBuilderList() {
      return getPropsFieldBuilder().getBuilderList();
    }

    private com.google.protobuf.RepeatedFieldBuilderV3<Property, Property.Builder, PropertyOrBuilder> getPropsFieldBuilder() {
      if (propsBuilder_ == null) {
        propsBuilder_ =
            new com.google.protobuf.RepeatedFieldBuilderV3<Property, Property.Builder, PropertyOrBuilder>(
                props_, ((bitField0_ & 0x00000002) == 0x00000002), getParentForChildren(),
                isClean());
        props_ = null;
      }
      return propsBuilder_;
    }

    private java.util.List<TopicFilter> topicFilters_ = java.util.Collections.emptyList();

    private void ensureTopicFiltersIsMutable() {
      if (!((bitField0_ & 0x00000004) == 0x00000004)) {
        topicFilters_ = new java.util.ArrayList<TopicFilter>(topicFilters_);
        bitField0_ |= 0x00000004;
      }
    }

    private com.google.protobuf.RepeatedFieldBuilderV3<TopicFilter, TopicFilter.Builder, TopicFilterOrBuilder> topicFiltersBuilder_;

    /**
     * <code>repeated .emqx.exhook.v2.TopicFilter topic_filters = 3;</code>
     */
    public java.util.List<TopicFilter> getTopicFiltersList() {
      if (topicFiltersBuilder_ == null) {
        return java.util.Collections.unmodifiableList(topicFilters_);
      } else {
        return topicFiltersBuilder_.getMessageList();
      }
    }

    /**
     * <code>repeated .emqx.exhook.v2.TopicFilter topic_filters = 3;</code>
     */
    public int getTopicFiltersCount() {
      if (topicFiltersBuilder_ == null) {
        return topicFilters_.size();
      } else {
        return topicFiltersBuilder_.getCount();
      }
    }

    /**
     * <code>repeated .emqx.exhook.v2.TopicFilter topic_filters = 3;</code>
     */
    public TopicFilter getTopicFilters(int index) {
      if (topicFiltersBuilder_ == null) {
        return topicFilters_.get(index);
      } else {
        return topicFiltersBuilder_.getMessage(index);
      }
    }

    /**
     * <code>repeated .emqx.exhook.v2.TopicFilter topic_filters = 3;</code>
     */
    public Builder setTopicFilters(int index, TopicFilter value) {
      if (topicFiltersBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureTopicFiltersIsMutable();
        topicFilters_.set(index, value);
        onChanged();
      } else {
        topicFiltersBuilder_.setMessage(index, value);
      }
      return this;
    }

    /**
     * <code>repeated .emqx.exhook.v2.TopicFilter topic_filters = 3;</code>
     */
    public Builder setTopicFilters(int index, TopicFilter.Builder builderForValue) {
      if (topicFiltersBuilder_ == null) {
        ensureTopicFiltersIsMutable();
        topicFilters_.set(index, builderForValue.build());
        onChanged();
      } else {
        topicFiltersBuilder_.setMessage(index, builderForValue.build());
      }
      return this;
    }

    /**
     * <code>repeated .emqx.exhook.v2.TopicFilter topic_filters = 3;</code>
     */
    public Builder addTopicFilters(TopicFilter value) {
      if (topicFiltersBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureTopicFiltersIsMutable();
        topicFilters_.add(value);
        onChanged();
      } else {
        topicFiltersBuilder_.addMessage(value);
      }
      return this;
    }

    /**
     * <code>repeated .emqx.exhook.v2.TopicFilter topic_filters = 3;</code>
     */
    public Builder addTopicFilters(int index, TopicFilter value) {
      if (topicFiltersBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureTopicFiltersIsMutable();
        topicFilters_.add(index, value);
        onChanged();
      } else {
        topicFiltersBuilder_.addMessage(index, value);
      }
      return this;
    }

    /**
     * <code>repeated .emqx.exhook.v2.TopicFilter topic_filters = 3;</code>
     */
    public Builder addTopicFilters(TopicFilter.Builder builderForValue) {
      if (topicFiltersBuilder_ == null) {
        ensureTopicFiltersIsMutable();
        topicFilters_.add(builderForValue.build());
        onChanged();
      } else {
        topicFiltersBuilder_.addMessage(builderForValue.build());
      }
      return this;
    }

    /**
     * <code>repeated .emqx.exhook.v2.TopicFilter topic_filters = 3;</code>
     */
    public Builder addTopicFilters(int index, TopicFilter.Builder builderForValue) {
      if (topicFiltersBuilder_ == null) {
        ensureTopicFiltersIsMutable();
        topicFilters_.add(index, builderForValue.build());
        onChanged();
      } else {
        topicFiltersBuilder_.addMessage(index, builderForValue.build());
      }
      return this;
    }

    /**
     * <code>repeated .emqx.exhook.v2.TopicFilter topic_filters = 3;</code>
     */
    public Builder addAllTopicFilters(Iterable<? extends TopicFilter> values) {
      if (topicFiltersBuilder_ == null) {
        ensureTopicFiltersIsMutable();
        com.google.protobuf.AbstractMessageLite.Builder.addAll(values, topicFilters_);
        onChanged();
      } else {
        topicFiltersBuilder_.addAllMessages(values);
      }
      return this;
    }

    /**
     * <code>repeated .emqx.exhook.v2.TopicFilter topic_filters = 3;</code>
     */
    public Builder clearTopicFilters() {
      if (topicFiltersBuilder_ == null) {
        topicFilters_ = java.util.Collections.emptyList();
        bitField0_ = (bitField0_ & ~0x00000004);
        onChanged();
      } else {
        topicFiltersBuilder_.clear();
      }
      return this;
    }

    /**
     * <code>repeated .emqx.exhook.v2.TopicFilter topic_filters = 3;</code>
     */
    public Builder removeTopicFilters(int index) {
      if (topicFiltersBuilder_ == null) {
        ensureTopicFiltersIsMutable();
        topicFilters_.remove(index);
        onChanged();
      } else {
        topicFiltersBuilder_.remove(index);
      }
      return this;
    }

    /**
     * <code>repeated .emqx.exhook.v2.TopicFilter topic_filters = 3;</code>
     */
    public TopicFilter.Builder getTopicFiltersBuilder(int index) {
      return getTopicFiltersFieldBuilder().getBuilder(index);
    }

    /**
     * <code>repeated .emqx.exhook.v2.TopicFilter topic_filters = 3;</code>
     */
    public TopicFilterOrBuilder getTopicFiltersOrBuilder(int index) {
      if (topicFiltersBuilder_ == null) {
        return topicFilters_.get(index);
      } else {
        return topicFiltersBuilder_.getMessageOrBuilder(index);
      }
    }

    /**
     * <code>repeated .emqx.exhook.v2.TopicFilter topic_filters = 3;</code>
     */
    public java.util.List<? extends TopicFilterOrBuilder> getTopicFiltersOrBuilderList() {
      if (topicFiltersBuilder_ != null) {
        return topicFiltersBuilder_.getMessageOrBuilderList();
      } else {
        return java.util.Collections.unmodifiableList(topicFilters_);
      }
    }

    /**
     * <code>repeated .emqx.exhook.v2.TopicFilter topic_filters = 3;</code>
     */
    public TopicFilter.Builder addTopicFiltersBuilder() {
      return getTopicFiltersFieldBuilder().addBuilder(TopicFilter.getDefaultInstance());
    }

    /**
     * <code>repeated .emqx.exhook.v2.TopicFilter topic_filters = 3;</code>
     */
    public TopicFilter.Builder addTopicFiltersBuilder(int index) {
      return getTopicFiltersFieldBuilder().addBuilder(index, TopicFilter.getDefaultInstance());
    }

    /**
     * <code>repeated .emqx.exhook.v2.TopicFilter topic_filters = 3;</code>
     */
    public java.util.List<TopicFilter.Builder> getTopicFiltersBuilderList() {
      return getTopicFiltersFieldBuilder().getBuilderList();
    }

    private com.google.protobuf.RepeatedFieldBuilderV3<TopicFilter, TopicFilter.Builder, TopicFilterOrBuilder> getTopicFiltersFieldBuilder() {
      if (topicFiltersBuilder_ == null) {
        topicFiltersBuilder_ =
            new com.google.protobuf.RepeatedFieldBuilderV3<TopicFilter, TopicFilter.Builder, TopicFilterOrBuilder>(
                topicFilters_, ((bitField0_ & 0x00000004) == 0x00000004), getParentForChildren(),
                isClean());
        topicFilters_ = null;
      }
      return topicFiltersBuilder_;
    }

    private RequestMeta meta_ = null;
    private com.google.protobuf.SingleFieldBuilderV3<RequestMeta, RequestMeta.Builder, RequestMetaOrBuilder> metaBuilder_;

    /**
     * <code>.emqx.exhook.v2.RequestMeta meta = 4;</code>
     */
    public boolean hasMeta() {
      return metaBuilder_ != null || meta_ != null;
    }

    /**
     * <code>.emqx.exhook.v2.RequestMeta meta = 4;</code>
     */
    public RequestMeta getMeta() {
      if (metaBuilder_ == null) {
        return meta_ == null ? RequestMeta.getDefaultInstance() : meta_;
      } else {
        return metaBuilder_.getMessage();
      }
    }

    /**
     * <code>.emqx.exhook.v2.RequestMeta meta = 4;</code>
     */
    public Builder setMeta(RequestMeta value) {
      if (metaBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        meta_ = value;
        onChanged();
      } else {
        metaBuilder_.setMessage(value);
      }

      return this;
    }

    /**
     * <code>.emqx.exhook.v2.RequestMeta meta = 4;</code>
     */
    public Builder setMeta(RequestMeta.Builder builderForValue) {
      if (metaBuilder_ == null) {
        meta_ = builderForValue.build();
        onChanged();
      } else {
        metaBuilder_.setMessage(builderForValue.build());
      }

      return this;
    }

    /**
     * <code>.emqx.exhook.v2.RequestMeta meta = 4;</code>
     */
    public Builder mergeMeta(RequestMeta value) {
      if (metaBuilder_ == null) {
        if (meta_ != null) {
          meta_ = RequestMeta.newBuilder(meta_).mergeFrom(value).buildPartial();
        } else {
          meta_ = value;
        }
        onChanged();
      } else {
        metaBuilder_.mergeFrom(value);
      }

      return this;
    }

    /**
     * <code>.emqx.exhook.v2.RequestMeta meta = 4;</code>
     */
    public Builder clearMeta() {
      if (metaBuilder_ == null) {
        meta_ = null;
        onChanged();
      } else {
        meta_ = null;
        metaBuilder_ = null;
      }

      return this;
    }

    /**
     * <code>.emqx.exhook.v2.RequestMeta meta = 4;</code>
     */
    public RequestMeta.Builder getMetaBuilder() {

      onChanged();
      return getMetaFieldBuilder().getBuilder();
    }

    /**
     * <code>.emqx.exhook.v2.RequestMeta meta = 4;</code>
     */
    public RequestMetaOrBuilder getMetaOrBuilder() {
      if (metaBuilder_ != null) {
        return metaBuilder_.getMessageOrBuilder();
      } else {
        return meta_ == null ? RequestMeta.getDefaultInstance() : meta_;
      }
    }

    /**
     * <code>.emqx.exhook.v2.RequestMeta meta = 4;</code>
     */
    private com.google.protobuf.SingleFieldBuilderV3<RequestMeta, RequestMeta.Builder, RequestMetaOrBuilder> getMetaFieldBuilder() {
      if (metaBuilder_ == null) {
        metaBuilder_ =
            new com.google.protobuf.SingleFieldBuilderV3<RequestMeta, RequestMeta.Builder, RequestMetaOrBuilder>(
                getMeta(), getParentForChildren(), isClean());
        meta_ = null;
      }
      return metaBuilder_;
    }

    public final Builder setUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.setUnknownFieldsProto3(unknownFields);
    }

    public final Builder mergeUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.mergeUnknownFields(unknownFields);
    }


    // @@protoc_insertion_point(builder_scope:emqx.exhook.v2.ClientSubscribeRequest)
  }

  // @@protoc_insertion_point(class_scope:emqx.exhook.v2.ClientSubscribeRequest)
  private static final ClientSubscribeRequest DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new ClientSubscribeRequest();
  }

  public static ClientSubscribeRequest getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<ClientSubscribeRequest> PARSER =
      new com.google.protobuf.AbstractParser<ClientSubscribeRequest>() {
        public ClientSubscribeRequest parsePartialFrom(com.google.protobuf.CodedInputStream input,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
          return new ClientSubscribeRequest(input, extensionRegistry);
        }
      };

  public static com.google.protobuf.Parser<ClientSubscribeRequest> parser() {
    return PARSER;
  }

  @Override
  public com.google.protobuf.Parser<ClientSubscribeRequest> getParserForType() {
    return PARSER;
  }

  public ClientSubscribeRequest getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

