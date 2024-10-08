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
 * Protobuf type {@code emqx.exhook.v2.HookSpec}
 */
public final class HookSpec extends com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:emqx.exhook.v2.HookSpec)
    HookSpecOrBuilder {
  private static final long serialVersionUID = 0L;

  // Use HookSpec.newBuilder() to construct.
  private HookSpec(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }

  private HookSpec() {
    name_ = "";
    topics_ = com.google.protobuf.LazyStringArrayList.EMPTY;
  }

  @Override
  public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
    return this.unknownFields;
  }

  private HookSpec(com.google.protobuf.CodedInputStream input,
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
            String s = input.readStringRequireUtf8();

            name_ = s;
            break;
          }
          case 18: {
            String s = input.readStringRequireUtf8();
            if (!((mutable_bitField0_ & 0x00000002) == 0x00000002)) {
              topics_ = new com.google.protobuf.LazyStringArrayList();
              mutable_bitField0_ |= 0x00000002;
            }
            topics_.add(s);
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
        topics_ = topics_.getUnmodifiableView();
      }
      this.unknownFields = unknownFields.build();
      makeExtensionsImmutable();
    }
  }

  public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
    return EmqxExHookProto.internal_static_emqx_exhook_v2_HookSpec_descriptor;
  }

  protected FieldAccessorTable internalGetFieldAccessorTable() {
    return EmqxExHookProto.internal_static_emqx_exhook_v2_HookSpec_fieldAccessorTable
        .ensureFieldAccessorsInitialized(HookSpec.class, Builder.class);
  }

  private int bitField0_;
  public static final int NAME_FIELD_NUMBER = 1;
  private volatile Object name_;

  /**
   * <pre>
   * The registered hooks name
   * Available value:
   *   "client.connect",      "client.connack"
   *   "client.connected",    "client.disconnected"
   *   "client.authenticate", "client.authorize"
   *   "client.subscribe",    "client.unsubscribe"
   *   "session.created",      "session.subscribed"
   *   "session.unsubscribed", "session.resumed"
   *   "session.discarded",    "session.takenover"
   *   "session.terminated"
   *   "message.publish", "message.delivered"
   *   "message.acked",   "message.dropped"
   * </pre>
   *
   * <code>string name = 1;</code>
   */
  public String getName() {
    Object ref = name_;
    if (ref instanceof String) {
      return (String) ref;
    } else {
      com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
      String s = bs.toStringUtf8();
      name_ = s;
      return s;
    }
  }

  /**
   * <pre>
   * The registered hooks name
   * Available value:
   *   "client.connect",      "client.connack"
   *   "client.connected",    "client.disconnected"
   *   "client.authenticate", "client.authorize"
   *   "client.subscribe",    "client.unsubscribe"
   *   "session.created",      "session.subscribed"
   *   "session.unsubscribed", "session.resumed"
   *   "session.discarded",    "session.takenover"
   *   "session.terminated"
   *   "message.publish", "message.delivered"
   *   "message.acked",   "message.dropped"
   * </pre>
   *
   * <code>string name = 1;</code>
   */
  public com.google.protobuf.ByteString getNameBytes() {
    Object ref = name_;
    if (ref instanceof String) {
      com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
      name_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  public static final int TOPICS_FIELD_NUMBER = 2;
  private com.google.protobuf.LazyStringList topics_;

  /**
   * <pre>
   * The topic filters for message hooks
   * </pre>
   *
   * <code>repeated string topics = 2;</code>
   */
  public com.google.protobuf.ProtocolStringList getTopicsList() {
    return topics_;
  }

  /**
   * <pre>
   * The topic filters for message hooks
   * </pre>
   *
   * <code>repeated string topics = 2;</code>
   */
  public int getTopicsCount() {
    return topics_.size();
  }

  /**
   * <pre>
   * The topic filters for message hooks
   * </pre>
   *
   * <code>repeated string topics = 2;</code>
   */
  public String getTopics(int index) {
    return topics_.get(index);
  }

  /**
   * <pre>
   * The topic filters for message hooks
   * </pre>
   *
   * <code>repeated string topics = 2;</code>
   */
  public com.google.protobuf.ByteString getTopicsBytes(int index) {
    return topics_.getByteString(index);
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
    if (!getNameBytes().isEmpty()) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 1, name_);
    }
    for (int i = 0; i < topics_.size(); i++) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 2, topics_.getRaw(i));
    }
    unknownFields.writeTo(output);
  }

  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1)
      return size;

    size = 0;
    if (!getNameBytes().isEmpty()) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, name_);
    }
    {
      int dataSize = 0;
      for (int i = 0; i < topics_.size(); i++) {
        dataSize += computeStringSizeNoTag(topics_.getRaw(i));
      }
      size += dataSize;
      size += 1 * getTopicsList().size();
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
    if (!(obj instanceof HookSpec)) {
      return super.equals(obj);
    }
    HookSpec other = (HookSpec) obj;

    boolean result = true;
    result = result && getName().equals(other.getName());
    result = result && getTopicsList().equals(other.getTopicsList());
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
    hash = (37 * hash) + NAME_FIELD_NUMBER;
    hash = (53 * hash) + getName().hashCode();
    if (getTopicsCount() > 0) {
      hash = (37 * hash) + TOPICS_FIELD_NUMBER;
      hash = (53 * hash) + getTopicsList().hashCode();
    }
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static HookSpec parseFrom(java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static HookSpec parseFrom(java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static HookSpec parseFrom(com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static HookSpec parseFrom(com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static HookSpec parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static HookSpec parseFrom(byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static HookSpec parseFrom(java.io.InputStream input) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }

  public static HookSpec parseFrom(java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input,
        extensionRegistry);
  }

  public static HookSpec parseDelimitedFrom(java.io.InputStream input) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
  }

  public static HookSpec parseDelimitedFrom(java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input,
        extensionRegistry);
  }

  public static HookSpec parseFrom(com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }

  public static HookSpec parseFrom(com.google.protobuf.CodedInputStream input,
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

  public static Builder newBuilder(HookSpec prototype) {
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
   * Protobuf type {@code emqx.exhook.v2.HookSpec}
   */
  public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder>
      implements
      // @@protoc_insertion_point(builder_implements:emqx.exhook.v2.HookSpec)
      HookSpecOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return EmqxExHookProto.internal_static_emqx_exhook_v2_HookSpec_descriptor;
    }

    protected FieldAccessorTable internalGetFieldAccessorTable() {
      return EmqxExHookProto.internal_static_emqx_exhook_v2_HookSpec_fieldAccessorTable
          .ensureFieldAccessorsInitialized(HookSpec.class, Builder.class);
    }

    // Construct using top.rslly.iot.utility.exhook.HookSpec.newBuilder()
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
      name_ = "";

      topics_ = com.google.protobuf.LazyStringArrayList.EMPTY;
      bitField0_ = (bitField0_ & ~0x00000002);
      return this;
    }

    public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
      return EmqxExHookProto.internal_static_emqx_exhook_v2_HookSpec_descriptor;
    }

    public HookSpec getDefaultInstanceForType() {
      return HookSpec.getDefaultInstance();
    }

    public HookSpec build() {
      HookSpec result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    public HookSpec buildPartial() {
      HookSpec result = new HookSpec(this);
      int from_bitField0_ = bitField0_;
      int to_bitField0_ = 0;
      result.name_ = name_;
      if (((bitField0_ & 0x00000002) == 0x00000002)) {
        topics_ = topics_.getUnmodifiableView();
        bitField0_ = (bitField0_ & ~0x00000002);
      }
      result.topics_ = topics_;
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
      if (other instanceof HookSpec) {
        return mergeFrom((HookSpec) other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(HookSpec other) {
      if (other == HookSpec.getDefaultInstance())
        return this;
      if (!other.getName().isEmpty()) {
        name_ = other.name_;
        onChanged();
      }
      if (!other.topics_.isEmpty()) {
        if (topics_.isEmpty()) {
          topics_ = other.topics_;
          bitField0_ = (bitField0_ & ~0x00000002);
        } else {
          ensureTopicsIsMutable();
          topics_.addAll(other.topics_);
        }
        onChanged();
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
      HookSpec parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (HookSpec) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private int bitField0_;

    private Object name_ = "";

    /**
     * <pre>
     * The registered hooks name
     * Available value:
     *   "client.connect",      "client.connack"
     *   "client.connected",    "client.disconnected"
     *   "client.authenticate", "client.authorize"
     *   "client.subscribe",    "client.unsubscribe"
     *   "session.created",      "session.subscribed"
     *   "session.unsubscribed", "session.resumed"
     *   "session.discarded",    "session.takenover"
     *   "session.terminated"
     *   "message.publish", "message.delivered"
     *   "message.acked",   "message.dropped"
     * </pre>
     *
     * <code>string name = 1;</code>
     */
    public String getName() {
      Object ref = name_;
      if (!(ref instanceof String)) {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        name_ = s;
        return s;
      } else {
        return (String) ref;
      }
    }

    /**
     * <pre>
     * The registered hooks name
     * Available value:
     *   "client.connect",      "client.connack"
     *   "client.connected",    "client.disconnected"
     *   "client.authenticate", "client.authorize"
     *   "client.subscribe",    "client.unsubscribe"
     *   "session.created",      "session.subscribed"
     *   "session.unsubscribed", "session.resumed"
     *   "session.discarded",    "session.takenover"
     *   "session.terminated"
     *   "message.publish", "message.delivered"
     *   "message.acked",   "message.dropped"
     * </pre>
     *
     * <code>string name = 1;</code>
     */
    public com.google.protobuf.ByteString getNameBytes() {
      Object ref = name_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b =
            com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        name_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    /**
     * <pre>
     * The registered hooks name
     * Available value:
     *   "client.connect",      "client.connack"
     *   "client.connected",    "client.disconnected"
     *   "client.authenticate", "client.authorize"
     *   "client.subscribe",    "client.unsubscribe"
     *   "session.created",      "session.subscribed"
     *   "session.unsubscribed", "session.resumed"
     *   "session.discarded",    "session.takenover"
     *   "session.terminated"
     *   "message.publish", "message.delivered"
     *   "message.acked",   "message.dropped"
     * </pre>
     *
     * <code>string name = 1;</code>
     */
    public Builder setName(String value) {
      if (value == null) {
        throw new NullPointerException();
      }

      name_ = value;
      onChanged();
      return this;
    }

    /**
     * <pre>
     * The registered hooks name
     * Available value:
     *   "client.connect",      "client.connack"
     *   "client.connected",    "client.disconnected"
     *   "client.authenticate", "client.authorize"
     *   "client.subscribe",    "client.unsubscribe"
     *   "session.created",      "session.subscribed"
     *   "session.unsubscribed", "session.resumed"
     *   "session.discarded",    "session.takenover"
     *   "session.terminated"
     *   "message.publish", "message.delivered"
     *   "message.acked",   "message.dropped"
     * </pre>
     *
     * <code>string name = 1;</code>
     */
    public Builder clearName() {

      name_ = getDefaultInstance().getName();
      onChanged();
      return this;
    }

    /**
     * <pre>
     * The registered hooks name
     * Available value:
     *   "client.connect",      "client.connack"
     *   "client.connected",    "client.disconnected"
     *   "client.authenticate", "client.authorize"
     *   "client.subscribe",    "client.unsubscribe"
     *   "session.created",      "session.subscribed"
     *   "session.unsubscribed", "session.resumed"
     *   "session.discarded",    "session.takenover"
     *   "session.terminated"
     *   "message.publish", "message.delivered"
     *   "message.acked",   "message.dropped"
     * </pre>
     *
     * <code>string name = 1;</code>
     */
    public Builder setNameBytes(com.google.protobuf.ByteString value) {
      if (value == null) {
        throw new NullPointerException();
      }
      checkByteStringIsUtf8(value);

      name_ = value;
      onChanged();
      return this;
    }

    private com.google.protobuf.LazyStringList topics_ =
        com.google.protobuf.LazyStringArrayList.EMPTY;

    private void ensureTopicsIsMutable() {
      if (!((bitField0_ & 0x00000002) == 0x00000002)) {
        topics_ = new com.google.protobuf.LazyStringArrayList(topics_);
        bitField0_ |= 0x00000002;
      }
    }

    /**
     * <pre>
     * The topic filters for message hooks
     * </pre>
     *
     * <code>repeated string topics = 2;</code>
     */
    public com.google.protobuf.ProtocolStringList getTopicsList() {
      return topics_.getUnmodifiableView();
    }

    /**
     * <pre>
     * The topic filters for message hooks
     * </pre>
     *
     * <code>repeated string topics = 2;</code>
     */
    public int getTopicsCount() {
      return topics_.size();
    }

    /**
     * <pre>
     * The topic filters for message hooks
     * </pre>
     *
     * <code>repeated string topics = 2;</code>
     */
    public String getTopics(int index) {
      return topics_.get(index);
    }

    /**
     * <pre>
     * The topic filters for message hooks
     * </pre>
     *
     * <code>repeated string topics = 2;</code>
     */
    public com.google.protobuf.ByteString getTopicsBytes(int index) {
      return topics_.getByteString(index);
    }

    /**
     * <pre>
     * The topic filters for message hooks
     * </pre>
     *
     * <code>repeated string topics = 2;</code>
     */
    public Builder setTopics(int index, String value) {
      if (value == null) {
        throw new NullPointerException();
      }
      ensureTopicsIsMutable();
      topics_.set(index, value);
      onChanged();
      return this;
    }

    /**
     * <pre>
     * The topic filters for message hooks
     * </pre>
     *
     * <code>repeated string topics = 2;</code>
     */
    public Builder addTopics(String value) {
      if (value == null) {
        throw new NullPointerException();
      }
      ensureTopicsIsMutable();
      topics_.add(value);
      onChanged();
      return this;
    }

    /**
     * <pre>
     * The topic filters for message hooks
     * </pre>
     *
     * <code>repeated string topics = 2;</code>
     */
    public Builder addAllTopics(Iterable<String> values) {
      ensureTopicsIsMutable();
      com.google.protobuf.AbstractMessageLite.Builder.addAll(values, topics_);
      onChanged();
      return this;
    }

    /**
     * <pre>
     * The topic filters for message hooks
     * </pre>
     *
     * <code>repeated string topics = 2;</code>
     */
    public Builder clearTopics() {
      topics_ = com.google.protobuf.LazyStringArrayList.EMPTY;
      bitField0_ = (bitField0_ & ~0x00000002);
      onChanged();
      return this;
    }

    /**
     * <pre>
     * The topic filters for message hooks
     * </pre>
     *
     * <code>repeated string topics = 2;</code>
     */
    public Builder addTopicsBytes(com.google.protobuf.ByteString value) {
      if (value == null) {
        throw new NullPointerException();
      }
      checkByteStringIsUtf8(value);
      ensureTopicsIsMutable();
      topics_.add(value);
      onChanged();
      return this;
    }

    public final Builder setUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.setUnknownFieldsProto3(unknownFields);
    }

    public final Builder mergeUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.mergeUnknownFields(unknownFields);
    }


    // @@protoc_insertion_point(builder_scope:emqx.exhook.v2.HookSpec)
  }

  // @@protoc_insertion_point(class_scope:emqx.exhook.v2.HookSpec)
  private static final HookSpec DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new HookSpec();
  }

  public static HookSpec getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<HookSpec> PARSER =
      new com.google.protobuf.AbstractParser<HookSpec>() {
        public HookSpec parsePartialFrom(com.google.protobuf.CodedInputStream input,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
          return new HookSpec(input, extensionRegistry);
        }
      };

  public static com.google.protobuf.Parser<HookSpec> parser() {
    return PARSER;
  }

  @Override
  public com.google.protobuf.Parser<HookSpec> getParserForType() {
    return PARSER;
  }

  public HookSpec getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

