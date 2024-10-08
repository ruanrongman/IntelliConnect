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
 * Protobuf type {@code emqx.exhook.v2.LoadedResponse}
 */
public final class LoadedResponse extends com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:emqx.exhook.v2.LoadedResponse)
    LoadedResponseOrBuilder {
  private static final long serialVersionUID = 0L;

  // Use LoadedResponse.newBuilder() to construct.
  private LoadedResponse(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }

  private LoadedResponse() {
    hooks_ = java.util.Collections.emptyList();
  }

  @Override
  public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
    return this.unknownFields;
  }

  private LoadedResponse(com.google.protobuf.CodedInputStream input,
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
            if (!((mutable_bitField0_ & 0x00000001) == 0x00000001)) {
              hooks_ = new java.util.ArrayList<HookSpec>();
              mutable_bitField0_ |= 0x00000001;
            }
            hooks_.add(input.readMessage(HookSpec.parser(), extensionRegistry));
            break;
          }
        }
      }
    } catch (com.google.protobuf.InvalidProtocolBufferException e) {
      throw e.setUnfinishedMessage(this);
    } catch (java.io.IOException e) {
      throw new com.google.protobuf.InvalidProtocolBufferException(e).setUnfinishedMessage(this);
    } finally {
      if (((mutable_bitField0_ & 0x00000001) == 0x00000001)) {
        hooks_ = java.util.Collections.unmodifiableList(hooks_);
      }
      this.unknownFields = unknownFields.build();
      makeExtensionsImmutable();
    }
  }

  public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
    return EmqxExHookProto.internal_static_emqx_exhook_v2_LoadedResponse_descriptor;
  }

  protected FieldAccessorTable internalGetFieldAccessorTable() {
    return EmqxExHookProto.internal_static_emqx_exhook_v2_LoadedResponse_fieldAccessorTable
        .ensureFieldAccessorsInitialized(LoadedResponse.class, Builder.class);
  }

  public static final int HOOKS_FIELD_NUMBER = 1;
  private java.util.List<HookSpec> hooks_;

  /**
   * <code>repeated .emqx.exhook.v2.HookSpec hooks = 1;</code>
   */
  public java.util.List<HookSpec> getHooksList() {
    return hooks_;
  }

  /**
   * <code>repeated .emqx.exhook.v2.HookSpec hooks = 1;</code>
   */
  public java.util.List<? extends HookSpecOrBuilder> getHooksOrBuilderList() {
    return hooks_;
  }

  /**
   * <code>repeated .emqx.exhook.v2.HookSpec hooks = 1;</code>
   */
  public int getHooksCount() {
    return hooks_.size();
  }

  /**
   * <code>repeated .emqx.exhook.v2.HookSpec hooks = 1;</code>
   */
  public HookSpec getHooks(int index) {
    return hooks_.get(index);
  }

  /**
   * <code>repeated .emqx.exhook.v2.HookSpec hooks = 1;</code>
   */
  public HookSpecOrBuilder getHooksOrBuilder(int index) {
    return hooks_.get(index);
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
    for (int i = 0; i < hooks_.size(); i++) {
      output.writeMessage(1, hooks_.get(i));
    }
    unknownFields.writeTo(output);
  }

  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1)
      return size;

    size = 0;
    for (int i = 0; i < hooks_.size(); i++) {
      size += com.google.protobuf.CodedOutputStream.computeMessageSize(1, hooks_.get(i));
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
    if (!(obj instanceof LoadedResponse)) {
      return super.equals(obj);
    }
    LoadedResponse other = (LoadedResponse) obj;

    boolean result = true;
    result = result && getHooksList().equals(other.getHooksList());
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
    if (getHooksCount() > 0) {
      hash = (37 * hash) + HOOKS_FIELD_NUMBER;
      hash = (53 * hash) + getHooksList().hashCode();
    }
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static LoadedResponse parseFrom(java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static LoadedResponse parseFrom(java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static LoadedResponse parseFrom(com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static LoadedResponse parseFrom(com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static LoadedResponse parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static LoadedResponse parseFrom(byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static LoadedResponse parseFrom(java.io.InputStream input) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }

  public static LoadedResponse parseFrom(java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input,
        extensionRegistry);
  }

  public static LoadedResponse parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
  }

  public static LoadedResponse parseDelimitedFrom(java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input,
        extensionRegistry);
  }

  public static LoadedResponse parseFrom(com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }

  public static LoadedResponse parseFrom(com.google.protobuf.CodedInputStream input,
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

  public static Builder newBuilder(LoadedResponse prototype) {
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
   * Protobuf type {@code emqx.exhook.v2.LoadedResponse}
   */
  public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder>
      implements
      // @@protoc_insertion_point(builder_implements:emqx.exhook.v2.LoadedResponse)
      LoadedResponseOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return EmqxExHookProto.internal_static_emqx_exhook_v2_LoadedResponse_descriptor;
    }

    protected FieldAccessorTable internalGetFieldAccessorTable() {
      return EmqxExHookProto.internal_static_emqx_exhook_v2_LoadedResponse_fieldAccessorTable
          .ensureFieldAccessorsInitialized(LoadedResponse.class, Builder.class);
    }

    // Construct using top.rslly.iot.utility.exhook.LoadedResponse.newBuilder()
    private Builder() {
      maybeForceBuilderInitialization();
    }

    private Builder(BuilderParent parent) {
      super(parent);
      maybeForceBuilderInitialization();
    }

    private void maybeForceBuilderInitialization() {
      if (com.google.protobuf.GeneratedMessageV3.alwaysUseFieldBuilders) {
        getHooksFieldBuilder();
      }
    }

    public Builder clear() {
      super.clear();
      if (hooksBuilder_ == null) {
        hooks_ = java.util.Collections.emptyList();
        bitField0_ = (bitField0_ & ~0x00000001);
      } else {
        hooksBuilder_.clear();
      }
      return this;
    }

    public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
      return EmqxExHookProto.internal_static_emqx_exhook_v2_LoadedResponse_descriptor;
    }

    public LoadedResponse getDefaultInstanceForType() {
      return LoadedResponse.getDefaultInstance();
    }

    public LoadedResponse build() {
      LoadedResponse result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    public LoadedResponse buildPartial() {
      LoadedResponse result = new LoadedResponse(this);
      int from_bitField0_ = bitField0_;
      if (hooksBuilder_ == null) {
        if (((bitField0_ & 0x00000001) == 0x00000001)) {
          hooks_ = java.util.Collections.unmodifiableList(hooks_);
          bitField0_ = (bitField0_ & ~0x00000001);
        }
        result.hooks_ = hooks_;
      } else {
        result.hooks_ = hooksBuilder_.build();
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
      if (other instanceof LoadedResponse) {
        return mergeFrom((LoadedResponse) other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(LoadedResponse other) {
      if (other == LoadedResponse.getDefaultInstance())
        return this;
      if (hooksBuilder_ == null) {
        if (!other.hooks_.isEmpty()) {
          if (hooks_.isEmpty()) {
            hooks_ = other.hooks_;
            bitField0_ = (bitField0_ & ~0x00000001);
          } else {
            ensureHooksIsMutable();
            hooks_.addAll(other.hooks_);
          }
          onChanged();
        }
      } else {
        if (!other.hooks_.isEmpty()) {
          if (hooksBuilder_.isEmpty()) {
            hooksBuilder_.dispose();
            hooksBuilder_ = null;
            hooks_ = other.hooks_;
            bitField0_ = (bitField0_ & ~0x00000001);
            hooksBuilder_ = com.google.protobuf.GeneratedMessageV3.alwaysUseFieldBuilders
                ? getHooksFieldBuilder()
                : null;
          } else {
            hooksBuilder_.addAllMessages(other.hooks_);
          }
        }
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
      LoadedResponse parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (LoadedResponse) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private int bitField0_;

    private java.util.List<HookSpec> hooks_ = java.util.Collections.emptyList();

    private void ensureHooksIsMutable() {
      if (!((bitField0_ & 0x00000001) == 0x00000001)) {
        hooks_ = new java.util.ArrayList<HookSpec>(hooks_);
        bitField0_ |= 0x00000001;
      }
    }

    private com.google.protobuf.RepeatedFieldBuilderV3<HookSpec, HookSpec.Builder, HookSpecOrBuilder> hooksBuilder_;

    /**
     * <code>repeated .emqx.exhook.v2.HookSpec hooks = 1;</code>
     */
    public java.util.List<HookSpec> getHooksList() {
      if (hooksBuilder_ == null) {
        return java.util.Collections.unmodifiableList(hooks_);
      } else {
        return hooksBuilder_.getMessageList();
      }
    }

    /**
     * <code>repeated .emqx.exhook.v2.HookSpec hooks = 1;</code>
     */
    public int getHooksCount() {
      if (hooksBuilder_ == null) {
        return hooks_.size();
      } else {
        return hooksBuilder_.getCount();
      }
    }

    /**
     * <code>repeated .emqx.exhook.v2.HookSpec hooks = 1;</code>
     */
    public HookSpec getHooks(int index) {
      if (hooksBuilder_ == null) {
        return hooks_.get(index);
      } else {
        return hooksBuilder_.getMessage(index);
      }
    }

    /**
     * <code>repeated .emqx.exhook.v2.HookSpec hooks = 1;</code>
     */
    public Builder setHooks(int index, HookSpec value) {
      if (hooksBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureHooksIsMutable();
        hooks_.set(index, value);
        onChanged();
      } else {
        hooksBuilder_.setMessage(index, value);
      }
      return this;
    }

    /**
     * <code>repeated .emqx.exhook.v2.HookSpec hooks = 1;</code>
     */
    public Builder setHooks(int index, HookSpec.Builder builderForValue) {
      if (hooksBuilder_ == null) {
        ensureHooksIsMutable();
        hooks_.set(index, builderForValue.build());
        onChanged();
      } else {
        hooksBuilder_.setMessage(index, builderForValue.build());
      }
      return this;
    }

    /**
     * <code>repeated .emqx.exhook.v2.HookSpec hooks = 1;</code>
     */
    public Builder addHooks(HookSpec value) {
      if (hooksBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureHooksIsMutable();
        hooks_.add(value);
        onChanged();
      } else {
        hooksBuilder_.addMessage(value);
      }
      return this;
    }

    /**
     * <code>repeated .emqx.exhook.v2.HookSpec hooks = 1;</code>
     */
    public Builder addHooks(int index, HookSpec value) {
      if (hooksBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        ensureHooksIsMutable();
        hooks_.add(index, value);
        onChanged();
      } else {
        hooksBuilder_.addMessage(index, value);
      }
      return this;
    }

    /**
     * <code>repeated .emqx.exhook.v2.HookSpec hooks = 1;</code>
     */
    public Builder addHooks(HookSpec.Builder builderForValue) {
      if (hooksBuilder_ == null) {
        ensureHooksIsMutable();
        hooks_.add(builderForValue.build());
        onChanged();
      } else {
        hooksBuilder_.addMessage(builderForValue.build());
      }
      return this;
    }

    /**
     * <code>repeated .emqx.exhook.v2.HookSpec hooks = 1;</code>
     */
    public Builder addHooks(int index, HookSpec.Builder builderForValue) {
      if (hooksBuilder_ == null) {
        ensureHooksIsMutable();
        hooks_.add(index, builderForValue.build());
        onChanged();
      } else {
        hooksBuilder_.addMessage(index, builderForValue.build());
      }
      return this;
    }

    /**
     * <code>repeated .emqx.exhook.v2.HookSpec hooks = 1;</code>
     */
    public Builder addAllHooks(Iterable<? extends HookSpec> values) {
      if (hooksBuilder_ == null) {
        ensureHooksIsMutable();
        com.google.protobuf.AbstractMessageLite.Builder.addAll(values, hooks_);
        onChanged();
      } else {
        hooksBuilder_.addAllMessages(values);
      }
      return this;
    }

    /**
     * <code>repeated .emqx.exhook.v2.HookSpec hooks = 1;</code>
     */
    public Builder clearHooks() {
      if (hooksBuilder_ == null) {
        hooks_ = java.util.Collections.emptyList();
        bitField0_ = (bitField0_ & ~0x00000001);
        onChanged();
      } else {
        hooksBuilder_.clear();
      }
      return this;
    }

    /**
     * <code>repeated .emqx.exhook.v2.HookSpec hooks = 1;</code>
     */
    public Builder removeHooks(int index) {
      if (hooksBuilder_ == null) {
        ensureHooksIsMutable();
        hooks_.remove(index);
        onChanged();
      } else {
        hooksBuilder_.remove(index);
      }
      return this;
    }

    /**
     * <code>repeated .emqx.exhook.v2.HookSpec hooks = 1;</code>
     */
    public HookSpec.Builder getHooksBuilder(int index) {
      return getHooksFieldBuilder().getBuilder(index);
    }

    /**
     * <code>repeated .emqx.exhook.v2.HookSpec hooks = 1;</code>
     */
    public HookSpecOrBuilder getHooksOrBuilder(int index) {
      if (hooksBuilder_ == null) {
        return hooks_.get(index);
      } else {
        return hooksBuilder_.getMessageOrBuilder(index);
      }
    }

    /**
     * <code>repeated .emqx.exhook.v2.HookSpec hooks = 1;</code>
     */
    public java.util.List<? extends HookSpecOrBuilder> getHooksOrBuilderList() {
      if (hooksBuilder_ != null) {
        return hooksBuilder_.getMessageOrBuilderList();
      } else {
        return java.util.Collections.unmodifiableList(hooks_);
      }
    }

    /**
     * <code>repeated .emqx.exhook.v2.HookSpec hooks = 1;</code>
     */
    public HookSpec.Builder addHooksBuilder() {
      return getHooksFieldBuilder().addBuilder(HookSpec.getDefaultInstance());
    }

    /**
     * <code>repeated .emqx.exhook.v2.HookSpec hooks = 1;</code>
     */
    public HookSpec.Builder addHooksBuilder(int index) {
      return getHooksFieldBuilder().addBuilder(index, HookSpec.getDefaultInstance());
    }

    /**
     * <code>repeated .emqx.exhook.v2.HookSpec hooks = 1;</code>
     */
    public java.util.List<HookSpec.Builder> getHooksBuilderList() {
      return getHooksFieldBuilder().getBuilderList();
    }

    private com.google.protobuf.RepeatedFieldBuilderV3<HookSpec, HookSpec.Builder, HookSpecOrBuilder> getHooksFieldBuilder() {
      if (hooksBuilder_ == null) {
        hooksBuilder_ =
            new com.google.protobuf.RepeatedFieldBuilderV3<HookSpec, HookSpec.Builder, HookSpecOrBuilder>(
                hooks_, ((bitField0_ & 0x00000001) == 0x00000001), getParentForChildren(),
                isClean());
        hooks_ = null;
      }
      return hooksBuilder_;
    }

    public final Builder setUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.setUnknownFieldsProto3(unknownFields);
    }

    public final Builder mergeUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.mergeUnknownFields(unknownFields);
    }


    // @@protoc_insertion_point(builder_scope:emqx.exhook.v2.LoadedResponse)
  }

  // @@protoc_insertion_point(class_scope:emqx.exhook.v2.LoadedResponse)
  private static final LoadedResponse DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new LoadedResponse();
  }

  public static LoadedResponse getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<LoadedResponse> PARSER =
      new com.google.protobuf.AbstractParser<LoadedResponse>() {
        public LoadedResponse parsePartialFrom(com.google.protobuf.CodedInputStream input,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
          return new LoadedResponse(input, extensionRegistry);
        }
      };

  public static com.google.protobuf.Parser<LoadedResponse> parser() {
    return PARSER;
  }

  @Override
  public com.google.protobuf.Parser<LoadedResponse> getParserForType() {
    return PARSER;
  }

  public LoadedResponse getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

