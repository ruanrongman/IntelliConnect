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
 * Protobuf type {@code emqx.exhook.v2.ProviderLoadedRequest}
 */
public final class ProviderLoadedRequest extends com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:emqx.exhook.v2.ProviderLoadedRequest)
    ProviderLoadedRequestOrBuilder {
  private static final long serialVersionUID = 0L;

  // Use ProviderLoadedRequest.newBuilder() to construct.
  private ProviderLoadedRequest(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }

  private ProviderLoadedRequest() {}

  @Override
  public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
    return this.unknownFields;
  }

  private ProviderLoadedRequest(com.google.protobuf.CodedInputStream input,
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
            BrokerInfo.Builder subBuilder = null;
            if (broker_ != null) {
              subBuilder = broker_.toBuilder();
            }
            broker_ = input.readMessage(BrokerInfo.parser(), extensionRegistry);
            if (subBuilder != null) {
              subBuilder.mergeFrom(broker_);
              broker_ = subBuilder.buildPartial();
            }

            break;
          }
          case 18: {
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
      this.unknownFields = unknownFields.build();
      makeExtensionsImmutable();
    }
  }

  public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
    return EmqxExHookProto.internal_static_emqx_exhook_v2_ProviderLoadedRequest_descriptor;
  }

  protected FieldAccessorTable internalGetFieldAccessorTable() {
    return EmqxExHookProto.internal_static_emqx_exhook_v2_ProviderLoadedRequest_fieldAccessorTable
        .ensureFieldAccessorsInitialized(ProviderLoadedRequest.class, Builder.class);
  }

  public static final int BROKER_FIELD_NUMBER = 1;
  private BrokerInfo broker_;

  /**
   * <code>.emqx.exhook.v2.BrokerInfo broker = 1;</code>
   */
  public boolean hasBroker() {
    return broker_ != null;
  }

  /**
   * <code>.emqx.exhook.v2.BrokerInfo broker = 1;</code>
   */
  public BrokerInfo getBroker() {
    return broker_ == null ? BrokerInfo.getDefaultInstance() : broker_;
  }

  /**
   * <code>.emqx.exhook.v2.BrokerInfo broker = 1;</code>
   */
  public BrokerInfoOrBuilder getBrokerOrBuilder() {
    return getBroker();
  }

  public static final int META_FIELD_NUMBER = 2;
  private RequestMeta meta_;

  /**
   * <code>.emqx.exhook.v2.RequestMeta meta = 2;</code>
   */
  public boolean hasMeta() {
    return meta_ != null;
  }

  /**
   * <code>.emqx.exhook.v2.RequestMeta meta = 2;</code>
   */
  public RequestMeta getMeta() {
    return meta_ == null ? RequestMeta.getDefaultInstance() : meta_;
  }

  /**
   * <code>.emqx.exhook.v2.RequestMeta meta = 2;</code>
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
    if (broker_ != null) {
      output.writeMessage(1, getBroker());
    }
    if (meta_ != null) {
      output.writeMessage(2, getMeta());
    }
    unknownFields.writeTo(output);
  }

  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1)
      return size;

    size = 0;
    if (broker_ != null) {
      size += com.google.protobuf.CodedOutputStream.computeMessageSize(1, getBroker());
    }
    if (meta_ != null) {
      size += com.google.protobuf.CodedOutputStream.computeMessageSize(2, getMeta());
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
    if (!(obj instanceof ProviderLoadedRequest)) {
      return super.equals(obj);
    }
    ProviderLoadedRequest other = (ProviderLoadedRequest) obj;

    boolean result = true;
    result = result && (hasBroker() == other.hasBroker());
    if (hasBroker()) {
      result = result && getBroker().equals(other.getBroker());
    }
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
    if (hasBroker()) {
      hash = (37 * hash) + BROKER_FIELD_NUMBER;
      hash = (53 * hash) + getBroker().hashCode();
    }
    if (hasMeta()) {
      hash = (37 * hash) + META_FIELD_NUMBER;
      hash = (53 * hash) + getMeta().hashCode();
    }
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static ProviderLoadedRequest parseFrom(java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static ProviderLoadedRequest parseFrom(java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static ProviderLoadedRequest parseFrom(com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static ProviderLoadedRequest parseFrom(com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static ProviderLoadedRequest parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static ProviderLoadedRequest parseFrom(byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static ProviderLoadedRequest parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }

  public static ProviderLoadedRequest parseFrom(java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input,
        extensionRegistry);
  }

  public static ProviderLoadedRequest parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
  }

  public static ProviderLoadedRequest parseDelimitedFrom(java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input,
        extensionRegistry);
  }

  public static ProviderLoadedRequest parseFrom(com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }

  public static ProviderLoadedRequest parseFrom(com.google.protobuf.CodedInputStream input,
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

  public static Builder newBuilder(ProviderLoadedRequest prototype) {
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
   * Protobuf type {@code emqx.exhook.v2.ProviderLoadedRequest}
   */
  public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder>
      implements
      // @@protoc_insertion_point(builder_implements:emqx.exhook.v2.ProviderLoadedRequest)
      ProviderLoadedRequestOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return EmqxExHookProto.internal_static_emqx_exhook_v2_ProviderLoadedRequest_descriptor;
    }

    protected FieldAccessorTable internalGetFieldAccessorTable() {
      return EmqxExHookProto.internal_static_emqx_exhook_v2_ProviderLoadedRequest_fieldAccessorTable
          .ensureFieldAccessorsInitialized(ProviderLoadedRequest.class, Builder.class);
    }

    // Construct using top.rslly.iot.utility.exhook.ProviderLoadedRequest.newBuilder()
    private Builder() {
      maybeForceBuilderInitialization();
    }

    private Builder(BuilderParent parent) {
      super(parent);
      maybeForceBuilderInitialization();
    }

    private void maybeForceBuilderInitialization() {
      if (com.google.protobuf.GeneratedMessageV3.alwaysUseFieldBuilders) {
      }
    }

    public Builder clear() {
      super.clear();
      if (brokerBuilder_ == null) {
        broker_ = null;
      } else {
        broker_ = null;
        brokerBuilder_ = null;
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
      return EmqxExHookProto.internal_static_emqx_exhook_v2_ProviderLoadedRequest_descriptor;
    }

    public ProviderLoadedRequest getDefaultInstanceForType() {
      return ProviderLoadedRequest.getDefaultInstance();
    }

    public ProviderLoadedRequest build() {
      ProviderLoadedRequest result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    public ProviderLoadedRequest buildPartial() {
      ProviderLoadedRequest result = new ProviderLoadedRequest(this);
      if (brokerBuilder_ == null) {
        result.broker_ = broker_;
      } else {
        result.broker_ = brokerBuilder_.build();
      }
      if (metaBuilder_ == null) {
        result.meta_ = meta_;
      } else {
        result.meta_ = metaBuilder_.build();
      }
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
      if (other instanceof ProviderLoadedRequest) {
        return mergeFrom((ProviderLoadedRequest) other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(ProviderLoadedRequest other) {
      if (other == ProviderLoadedRequest.getDefaultInstance())
        return this;
      if (other.hasBroker()) {
        mergeBroker(other.getBroker());
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
      ProviderLoadedRequest parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (ProviderLoadedRequest) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private BrokerInfo broker_ = null;
    private com.google.protobuf.SingleFieldBuilderV3<BrokerInfo, BrokerInfo.Builder, BrokerInfoOrBuilder> brokerBuilder_;

    /**
     * <code>.emqx.exhook.v2.BrokerInfo broker = 1;</code>
     */
    public boolean hasBroker() {
      return brokerBuilder_ != null || broker_ != null;
    }

    /**
     * <code>.emqx.exhook.v2.BrokerInfo broker = 1;</code>
     */
    public BrokerInfo getBroker() {
      if (brokerBuilder_ == null) {
        return broker_ == null ? BrokerInfo.getDefaultInstance() : broker_;
      } else {
        return brokerBuilder_.getMessage();
      }
    }

    /**
     * <code>.emqx.exhook.v2.BrokerInfo broker = 1;</code>
     */
    public Builder setBroker(BrokerInfo value) {
      if (brokerBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        broker_ = value;
        onChanged();
      } else {
        brokerBuilder_.setMessage(value);
      }

      return this;
    }

    /**
     * <code>.emqx.exhook.v2.BrokerInfo broker = 1;</code>
     */
    public Builder setBroker(BrokerInfo.Builder builderForValue) {
      if (brokerBuilder_ == null) {
        broker_ = builderForValue.build();
        onChanged();
      } else {
        brokerBuilder_.setMessage(builderForValue.build());
      }

      return this;
    }

    /**
     * <code>.emqx.exhook.v2.BrokerInfo broker = 1;</code>
     */
    public Builder mergeBroker(BrokerInfo value) {
      if (brokerBuilder_ == null) {
        if (broker_ != null) {
          broker_ = BrokerInfo.newBuilder(broker_).mergeFrom(value).buildPartial();
        } else {
          broker_ = value;
        }
        onChanged();
      } else {
        brokerBuilder_.mergeFrom(value);
      }

      return this;
    }

    /**
     * <code>.emqx.exhook.v2.BrokerInfo broker = 1;</code>
     */
    public Builder clearBroker() {
      if (brokerBuilder_ == null) {
        broker_ = null;
        onChanged();
      } else {
        broker_ = null;
        brokerBuilder_ = null;
      }

      return this;
    }

    /**
     * <code>.emqx.exhook.v2.BrokerInfo broker = 1;</code>
     */
    public BrokerInfo.Builder getBrokerBuilder() {

      onChanged();
      return getBrokerFieldBuilder().getBuilder();
    }

    /**
     * <code>.emqx.exhook.v2.BrokerInfo broker = 1;</code>
     */
    public BrokerInfoOrBuilder getBrokerOrBuilder() {
      if (brokerBuilder_ != null) {
        return brokerBuilder_.getMessageOrBuilder();
      } else {
        return broker_ == null ? BrokerInfo.getDefaultInstance() : broker_;
      }
    }

    /**
     * <code>.emqx.exhook.v2.BrokerInfo broker = 1;</code>
     */
    private com.google.protobuf.SingleFieldBuilderV3<BrokerInfo, BrokerInfo.Builder, BrokerInfoOrBuilder> getBrokerFieldBuilder() {
      if (brokerBuilder_ == null) {
        brokerBuilder_ =
            new com.google.protobuf.SingleFieldBuilderV3<BrokerInfo, BrokerInfo.Builder, BrokerInfoOrBuilder>(
                getBroker(), getParentForChildren(), isClean());
        broker_ = null;
      }
      return brokerBuilder_;
    }

    private RequestMeta meta_ = null;
    private com.google.protobuf.SingleFieldBuilderV3<RequestMeta, RequestMeta.Builder, RequestMetaOrBuilder> metaBuilder_;

    /**
     * <code>.emqx.exhook.v2.RequestMeta meta = 2;</code>
     */
    public boolean hasMeta() {
      return metaBuilder_ != null || meta_ != null;
    }

    /**
     * <code>.emqx.exhook.v2.RequestMeta meta = 2;</code>
     */
    public RequestMeta getMeta() {
      if (metaBuilder_ == null) {
        return meta_ == null ? RequestMeta.getDefaultInstance() : meta_;
      } else {
        return metaBuilder_.getMessage();
      }
    }

    /**
     * <code>.emqx.exhook.v2.RequestMeta meta = 2;</code>
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
     * <code>.emqx.exhook.v2.RequestMeta meta = 2;</code>
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
     * <code>.emqx.exhook.v2.RequestMeta meta = 2;</code>
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
     * <code>.emqx.exhook.v2.RequestMeta meta = 2;</code>
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
     * <code>.emqx.exhook.v2.RequestMeta meta = 2;</code>
     */
    public RequestMeta.Builder getMetaBuilder() {

      onChanged();
      return getMetaFieldBuilder().getBuilder();
    }

    /**
     * <code>.emqx.exhook.v2.RequestMeta meta = 2;</code>
     */
    public RequestMetaOrBuilder getMetaOrBuilder() {
      if (metaBuilder_ != null) {
        return metaBuilder_.getMessageOrBuilder();
      } else {
        return meta_ == null ? RequestMeta.getDefaultInstance() : meta_;
      }
    }

    /**
     * <code>.emqx.exhook.v2.RequestMeta meta = 2;</code>
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


    // @@protoc_insertion_point(builder_scope:emqx.exhook.v2.ProviderLoadedRequest)
  }

  // @@protoc_insertion_point(class_scope:emqx.exhook.v2.ProviderLoadedRequest)
  private static final ProviderLoadedRequest DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new ProviderLoadedRequest();
  }

  public static ProviderLoadedRequest getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<ProviderLoadedRequest> PARSER =
      new com.google.protobuf.AbstractParser<ProviderLoadedRequest>() {
        public ProviderLoadedRequest parsePartialFrom(com.google.protobuf.CodedInputStream input,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
          return new ProviderLoadedRequest(input, extensionRegistry);
        }
      };

  public static com.google.protobuf.Parser<ProviderLoadedRequest> parser() {
    return PARSER;
  }

  @Override
  public com.google.protobuf.Parser<ProviderLoadedRequest> getParserForType() {
    return PARSER;
  }

  public ProviderLoadedRequest getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

