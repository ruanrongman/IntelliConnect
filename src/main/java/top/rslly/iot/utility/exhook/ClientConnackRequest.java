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
 * Protobuf type {@code emqx.exhook.v2.ClientConnackRequest}
 */
public final class ClientConnackRequest extends com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:emqx.exhook.v2.ClientConnackRequest)
    ClientConnackRequestOrBuilder {
  private static final long serialVersionUID = 0L;

  // Use ClientConnackRequest.newBuilder() to construct.
  private ClientConnackRequest(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }

  private ClientConnackRequest() {
    resultCode_ = "";
    props_ = java.util.Collections.emptyList();
  }

  @Override
  public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
    return this.unknownFields;
  }

  private ClientConnackRequest(com.google.protobuf.CodedInputStream input,
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
            ConnInfo.Builder subBuilder = null;
            if (conninfo_ != null) {
              subBuilder = conninfo_.toBuilder();
            }
            conninfo_ = input.readMessage(ConnInfo.parser(), extensionRegistry);
            if (subBuilder != null) {
              subBuilder.mergeFrom(conninfo_);
              conninfo_ = subBuilder.buildPartial();
            }

            break;
          }
          case 18: {
            String s = input.readStringRequireUtf8();

            resultCode_ = s;
            break;
          }
          case 26: {
            if (!((mutable_bitField0_ & 0x00000004) == 0x00000004)) {
              props_ = new java.util.ArrayList<Property>();
              mutable_bitField0_ |= 0x00000004;
            }
            props_.add(input.readMessage(Property.parser(), extensionRegistry));
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
      if (((mutable_bitField0_ & 0x00000004) == 0x00000004)) {
        props_ = java.util.Collections.unmodifiableList(props_);
      }
      this.unknownFields = unknownFields.build();
      makeExtensionsImmutable();
    }
  }

  public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
    return EmqxExHookProto.internal_static_emqx_exhook_v2_ClientConnackRequest_descriptor;
  }

  protected FieldAccessorTable internalGetFieldAccessorTable() {
    return EmqxExHookProto.internal_static_emqx_exhook_v2_ClientConnackRequest_fieldAccessorTable
        .ensureFieldAccessorsInitialized(ClientConnackRequest.class, Builder.class);
  }

  private int bitField0_;
  public static final int CONNINFO_FIELD_NUMBER = 1;
  private ConnInfo conninfo_;

  /**
   * <code>.emqx.exhook.v2.ConnInfo conninfo = 1;</code>
   */
  public boolean hasConninfo() {
    return conninfo_ != null;
  }

  /**
   * <code>.emqx.exhook.v2.ConnInfo conninfo = 1;</code>
   */
  public ConnInfo getConninfo() {
    return conninfo_ == null ? ConnInfo.getDefaultInstance() : conninfo_;
  }

  /**
   * <code>.emqx.exhook.v2.ConnInfo conninfo = 1;</code>
   */
  public ConnInfoOrBuilder getConninfoOrBuilder() {
    return getConninfo();
  }

  public static final int RESULT_CODE_FIELD_NUMBER = 2;
  private volatile Object resultCode_;

  /**
   * <code>string result_code = 2;</code>
   */
  public String getResultCode() {
    Object ref = resultCode_;
    if (ref instanceof String) {
      return (String) ref;
    } else {
      com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
      String s = bs.toStringUtf8();
      resultCode_ = s;
      return s;
    }
  }

  /**
   * <code>string result_code = 2;</code>
   */
  public com.google.protobuf.ByteString getResultCodeBytes() {
    Object ref = resultCode_;
    if (ref instanceof String) {
      com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
      resultCode_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  public static final int PROPS_FIELD_NUMBER = 3;
  private java.util.List<Property> props_;

  /**
   * <code>repeated .emqx.exhook.v2.Property props = 3;</code>
   */
  public java.util.List<Property> getPropsList() {
    return props_;
  }

  /**
   * <code>repeated .emqx.exhook.v2.Property props = 3;</code>
   */
  public java.util.List<? extends PropertyOrBuilder> getPropsOrBuilderList() {
    return props_;
  }

  /**
   * <code>repeated .emqx.exhook.v2.Property props = 3;</code>
   */
  public int getPropsCount() {
    return props_.size();
  }

  /**
   * <code>repeated .emqx.exhook.v2.Property props = 3;</code>
   */
  public Property getProps(int index) {
    return props_.get(index);
  }

  /**
   * <code>repeated .emqx.exhook.v2.Property props = 3;</code>
   */
  public PropertyOrBuilder getPropsOrBuilder(int index) {
    return props_.get(index);
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
    if (conninfo_ != null) {
      output.writeMessage(1, getConninfo());
    }
    if (!getResultCodeBytes().isEmpty()) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 2, resultCode_);
    }
    for (int i = 0; i < props_.size(); i++) {
      output.writeMessage(3, props_.get(i));
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
    if (conninfo_ != null) {
      size += com.google.protobuf.CodedOutputStream.computeMessageSize(1, getConninfo());
    }
    if (!getResultCodeBytes().isEmpty()) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(2, resultCode_);
    }
    for (int i = 0; i < props_.size(); i++) {
      size += com.google.protobuf.CodedOutputStream.computeMessageSize(3, props_.get(i));
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
    if (!(obj instanceof ClientConnackRequest)) {
      return super.equals(obj);
    }
    ClientConnackRequest other = (ClientConnackRequest) obj;

    boolean result = true;
    result = result && (hasConninfo() == other.hasConninfo());
    if (hasConninfo()) {
      result = result && getConninfo().equals(other.getConninfo());
    }
    result = result && getResultCode().equals(other.getResultCode());
    result = result && getPropsList().equals(other.getPropsList());
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
    if (hasConninfo()) {
      hash = (37 * hash) + CONNINFO_FIELD_NUMBER;
      hash = (53 * hash) + getConninfo().hashCode();
    }
    hash = (37 * hash) + RESULT_CODE_FIELD_NUMBER;
    hash = (53 * hash) + getResultCode().hashCode();
    if (getPropsCount() > 0) {
      hash = (37 * hash) + PROPS_FIELD_NUMBER;
      hash = (53 * hash) + getPropsList().hashCode();
    }
    if (hasMeta()) {
      hash = (37 * hash) + META_FIELD_NUMBER;
      hash = (53 * hash) + getMeta().hashCode();
    }
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static ClientConnackRequest parseFrom(java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static ClientConnackRequest parseFrom(java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static ClientConnackRequest parseFrom(com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static ClientConnackRequest parseFrom(com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static ClientConnackRequest parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static ClientConnackRequest parseFrom(byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static ClientConnackRequest parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }

  public static ClientConnackRequest parseFrom(java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input,
        extensionRegistry);
  }

  public static ClientConnackRequest parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
  }

  public static ClientConnackRequest parseDelimitedFrom(java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input,
        extensionRegistry);
  }

  public static ClientConnackRequest parseFrom(com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }

  public static ClientConnackRequest parseFrom(com.google.protobuf.CodedInputStream input,
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

  public static Builder newBuilder(ClientConnackRequest prototype) {
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
   * Protobuf type {@code emqx.exhook.v2.ClientConnackRequest}
   */
  public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder>
      implements
      // @@protoc_insertion_point(builder_implements:emqx.exhook.v2.ClientConnackRequest)
      ClientConnackRequestOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return EmqxExHookProto.internal_static_emqx_exhook_v2_ClientConnackRequest_descriptor;
    }

    protected FieldAccessorTable internalGetFieldAccessorTable() {
      return EmqxExHookProto.internal_static_emqx_exhook_v2_ClientConnackRequest_fieldAccessorTable
          .ensureFieldAccessorsInitialized(ClientConnackRequest.class, Builder.class);
    }

    // Construct using top.rslly.iot.utility.exhook.ClientConnackRequest.newBuilder()
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
      }
    }

    public Builder clear() {
      super.clear();
      if (conninfoBuilder_ == null) {
        conninfo_ = null;
      } else {
        conninfo_ = null;
        conninfoBuilder_ = null;
      }
      resultCode_ = "";

      if (propsBuilder_ == null) {
        props_ = java.util.Collections.emptyList();
        bitField0_ = (bitField0_ & ~0x00000004);
      } else {
        propsBuilder_.clear();
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
      return EmqxExHookProto.internal_static_emqx_exhook_v2_ClientConnackRequest_descriptor;
    }

    public ClientConnackRequest getDefaultInstanceForType() {
      return ClientConnackRequest.getDefaultInstance();
    }

    public ClientConnackRequest build() {
      ClientConnackRequest result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    public ClientConnackRequest buildPartial() {
      ClientConnackRequest result = new ClientConnackRequest(this);
      int from_bitField0_ = bitField0_;
      int to_bitField0_ = 0;
      if (conninfoBuilder_ == null) {
        result.conninfo_ = conninfo_;
      } else {
        result.conninfo_ = conninfoBuilder_.build();
      }
      result.resultCode_ = resultCode_;
      if (propsBuilder_ == null) {
        if (((bitField0_ & 0x00000004) == 0x00000004)) {
          props_ = java.util.Collections.unmodifiableList(props_);
          bitField0_ = (bitField0_ & ~0x00000004);
        }
        result.props_ = props_;
      } else {
        result.props_ = propsBuilder_.build();
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
      if (other instanceof ClientConnackRequest) {
        return mergeFrom((ClientConnackRequest) other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(ClientConnackRequest other) {
      if (other == ClientConnackRequest.getDefaultInstance())
        return this;
      if (other.hasConninfo()) {
        mergeConninfo(other.getConninfo());
      }
      if (!other.getResultCode().isEmpty()) {
        resultCode_ = other.resultCode_;
        onChanged();
      }
      if (propsBuilder_ == null) {
        if (!other.props_.isEmpty()) {
          if (props_.isEmpty()) {
            props_ = other.props_;
            bitField0_ = (bitField0_ & ~0x00000004);
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
            bitField0_ = (bitField0_ & ~0x00000004);
            propsBuilder_ = com.google.protobuf.GeneratedMessageV3.alwaysUseFieldBuilders
                ? getPropsFieldBuilder()
                : null;
          } else {
            propsBuilder_.addAllMessages(other.props_);
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
      ClientConnackRequest parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (ClientConnackRequest) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private int bitField0_;

    private ConnInfo conninfo_ = null;
    private com.google.protobuf.SingleFieldBuilderV3<ConnInfo, ConnInfo.Builder, ConnInfoOrBuilder> conninfoBuilder_;

    /**
     * <code>.emqx.exhook.v2.ConnInfo conninfo = 1;</code>
     */
    public boolean hasConninfo() {
      return conninfoBuilder_ != null || conninfo_ != null;
    }

    /**
     * <code>.emqx.exhook.v2.ConnInfo conninfo = 1;</code>
     */
    public ConnInfo getConninfo() {
      if (conninfoBuilder_ == null) {
        return conninfo_ == null ? ConnInfo.getDefaultInstance() : conninfo_;
      } else {
        return conninfoBuilder_.getMessage();
      }
    }

    /**
     * <code>.emqx.exhook.v2.ConnInfo conninfo = 1;</code>
     */
    public Builder setConninfo(ConnInfo value) {
      if (conninfoBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        conninfo_ = value;
        onChanged();
      } else {
        conninfoBuilder_.setMessage(value);
      }

      return this;
    }

    /**
     * <code>.emqx.exhook.v2.ConnInfo conninfo = 1;</code>
     */
    public Builder setConninfo(ConnInfo.Builder builderForValue) {
      if (conninfoBuilder_ == null) {
        conninfo_ = builderForValue.build();
        onChanged();
      } else {
        conninfoBuilder_.setMessage(builderForValue.build());
      }

      return this;
    }

    /**
     * <code>.emqx.exhook.v2.ConnInfo conninfo = 1;</code>
     */
    public Builder mergeConninfo(ConnInfo value) {
      if (conninfoBuilder_ == null) {
        if (conninfo_ != null) {
          conninfo_ = ConnInfo.newBuilder(conninfo_).mergeFrom(value).buildPartial();
        } else {
          conninfo_ = value;
        }
        onChanged();
      } else {
        conninfoBuilder_.mergeFrom(value);
      }

      return this;
    }

    /**
     * <code>.emqx.exhook.v2.ConnInfo conninfo = 1;</code>
     */
    public Builder clearConninfo() {
      if (conninfoBuilder_ == null) {
        conninfo_ = null;
        onChanged();
      } else {
        conninfo_ = null;
        conninfoBuilder_ = null;
      }

      return this;
    }

    /**
     * <code>.emqx.exhook.v2.ConnInfo conninfo = 1;</code>
     */
    public ConnInfo.Builder getConninfoBuilder() {

      onChanged();
      return getConninfoFieldBuilder().getBuilder();
    }

    /**
     * <code>.emqx.exhook.v2.ConnInfo conninfo = 1;</code>
     */
    public ConnInfoOrBuilder getConninfoOrBuilder() {
      if (conninfoBuilder_ != null) {
        return conninfoBuilder_.getMessageOrBuilder();
      } else {
        return conninfo_ == null ? ConnInfo.getDefaultInstance() : conninfo_;
      }
    }

    /**
     * <code>.emqx.exhook.v2.ConnInfo conninfo = 1;</code>
     */
    private com.google.protobuf.SingleFieldBuilderV3<ConnInfo, ConnInfo.Builder, ConnInfoOrBuilder> getConninfoFieldBuilder() {
      if (conninfoBuilder_ == null) {
        conninfoBuilder_ =
            new com.google.protobuf.SingleFieldBuilderV3<ConnInfo, ConnInfo.Builder, ConnInfoOrBuilder>(
                getConninfo(), getParentForChildren(), isClean());
        conninfo_ = null;
      }
      return conninfoBuilder_;
    }

    private Object resultCode_ = "";

    /**
     * <code>string result_code = 2;</code>
     */
    public String getResultCode() {
      Object ref = resultCode_;
      if (!(ref instanceof String)) {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        resultCode_ = s;
        return s;
      } else {
        return (String) ref;
      }
    }

    /**
     * <code>string result_code = 2;</code>
     */
    public com.google.protobuf.ByteString getResultCodeBytes() {
      Object ref = resultCode_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b =
            com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        resultCode_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    /**
     * <code>string result_code = 2;</code>
     */
    public Builder setResultCode(String value) {
      if (value == null) {
        throw new NullPointerException();
      }

      resultCode_ = value;
      onChanged();
      return this;
    }

    /**
     * <code>string result_code = 2;</code>
     */
    public Builder clearResultCode() {

      resultCode_ = getDefaultInstance().getResultCode();
      onChanged();
      return this;
    }

    /**
     * <code>string result_code = 2;</code>
     */
    public Builder setResultCodeBytes(com.google.protobuf.ByteString value) {
      if (value == null) {
        throw new NullPointerException();
      }
      checkByteStringIsUtf8(value);

      resultCode_ = value;
      onChanged();
      return this;
    }

    private java.util.List<Property> props_ = java.util.Collections.emptyList();

    private void ensurePropsIsMutable() {
      if (!((bitField0_ & 0x00000004) == 0x00000004)) {
        props_ = new java.util.ArrayList<Property>(props_);
        bitField0_ |= 0x00000004;
      }
    }

    private com.google.protobuf.RepeatedFieldBuilderV3<Property, Property.Builder, PropertyOrBuilder> propsBuilder_;

    /**
     * <code>repeated .emqx.exhook.v2.Property props = 3;</code>
     */
    public java.util.List<Property> getPropsList() {
      if (propsBuilder_ == null) {
        return java.util.Collections.unmodifiableList(props_);
      } else {
        return propsBuilder_.getMessageList();
      }
    }

    /**
     * <code>repeated .emqx.exhook.v2.Property props = 3;</code>
     */
    public int getPropsCount() {
      if (propsBuilder_ == null) {
        return props_.size();
      } else {
        return propsBuilder_.getCount();
      }
    }

    /**
     * <code>repeated .emqx.exhook.v2.Property props = 3;</code>
     */
    public Property getProps(int index) {
      if (propsBuilder_ == null) {
        return props_.get(index);
      } else {
        return propsBuilder_.getMessage(index);
      }
    }

    /**
     * <code>repeated .emqx.exhook.v2.Property props = 3;</code>
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
     * <code>repeated .emqx.exhook.v2.Property props = 3;</code>
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
     * <code>repeated .emqx.exhook.v2.Property props = 3;</code>
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
     * <code>repeated .emqx.exhook.v2.Property props = 3;</code>
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
     * <code>repeated .emqx.exhook.v2.Property props = 3;</code>
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
     * <code>repeated .emqx.exhook.v2.Property props = 3;</code>
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
     * <code>repeated .emqx.exhook.v2.Property props = 3;</code>
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
     * <code>repeated .emqx.exhook.v2.Property props = 3;</code>
     */
    public Builder clearProps() {
      if (propsBuilder_ == null) {
        props_ = java.util.Collections.emptyList();
        bitField0_ = (bitField0_ & ~0x00000004);
        onChanged();
      } else {
        propsBuilder_.clear();
      }
      return this;
    }

    /**
     * <code>repeated .emqx.exhook.v2.Property props = 3;</code>
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
     * <code>repeated .emqx.exhook.v2.Property props = 3;</code>
     */
    public Property.Builder getPropsBuilder(int index) {
      return getPropsFieldBuilder().getBuilder(index);
    }

    /**
     * <code>repeated .emqx.exhook.v2.Property props = 3;</code>
     */
    public PropertyOrBuilder getPropsOrBuilder(int index) {
      if (propsBuilder_ == null) {
        return props_.get(index);
      } else {
        return propsBuilder_.getMessageOrBuilder(index);
      }
    }

    /**
     * <code>repeated .emqx.exhook.v2.Property props = 3;</code>
     */
    public java.util.List<? extends PropertyOrBuilder> getPropsOrBuilderList() {
      if (propsBuilder_ != null) {
        return propsBuilder_.getMessageOrBuilderList();
      } else {
        return java.util.Collections.unmodifiableList(props_);
      }
    }

    /**
     * <code>repeated .emqx.exhook.v2.Property props = 3;</code>
     */
    public Property.Builder addPropsBuilder() {
      return getPropsFieldBuilder().addBuilder(Property.getDefaultInstance());
    }

    /**
     * <code>repeated .emqx.exhook.v2.Property props = 3;</code>
     */
    public Property.Builder addPropsBuilder(int index) {
      return getPropsFieldBuilder().addBuilder(index, Property.getDefaultInstance());
    }

    /**
     * <code>repeated .emqx.exhook.v2.Property props = 3;</code>
     */
    public java.util.List<Property.Builder> getPropsBuilderList() {
      return getPropsFieldBuilder().getBuilderList();
    }

    private com.google.protobuf.RepeatedFieldBuilderV3<Property, Property.Builder, PropertyOrBuilder> getPropsFieldBuilder() {
      if (propsBuilder_ == null) {
        propsBuilder_ =
            new com.google.protobuf.RepeatedFieldBuilderV3<Property, Property.Builder, PropertyOrBuilder>(
                props_, ((bitField0_ & 0x00000004) == 0x00000004), getParentForChildren(),
                isClean());
        props_ = null;
      }
      return propsBuilder_;
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


    // @@protoc_insertion_point(builder_scope:emqx.exhook.v2.ClientConnackRequest)
  }

  // @@protoc_insertion_point(class_scope:emqx.exhook.v2.ClientConnackRequest)
  private static final ClientConnackRequest DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new ClientConnackRequest();
  }

  public static ClientConnackRequest getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<ClientConnackRequest> PARSER =
      new com.google.protobuf.AbstractParser<ClientConnackRequest>() {
        public ClientConnackRequest parsePartialFrom(com.google.protobuf.CodedInputStream input,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
          return new ClientConnackRequest(input, extensionRegistry);
        }
      };

  public static com.google.protobuf.Parser<ClientConnackRequest> parser() {
    return PARSER;
  }

  @Override
  public com.google.protobuf.Parser<ClientConnackRequest> getParserForType() {
    return PARSER;
  }

  public ClientConnackRequest getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

