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
 * Protobuf type {@code emqx.exhook.v2.BrokerInfo}
 */
public final class BrokerInfo extends com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:emqx.exhook.v2.BrokerInfo)
    BrokerInfoOrBuilder {
  private static final long serialVersionUID = 0L;

  // Use BrokerInfo.newBuilder() to construct.
  private BrokerInfo(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }

  private BrokerInfo() {
    version_ = "";
    sysdescr_ = "";
    uptime_ = 0L;
    datetime_ = "";
  }

  @Override
  public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
    return this.unknownFields;
  }

  private BrokerInfo(com.google.protobuf.CodedInputStream input,
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

            version_ = s;
            break;
          }
          case 18: {
            String s = input.readStringRequireUtf8();

            sysdescr_ = s;
            break;
          }
          case 24: {

            uptime_ = input.readInt64();
            break;
          }
          case 34: {
            String s = input.readStringRequireUtf8();

            datetime_ = s;
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
    return EmqxExHookProto.internal_static_emqx_exhook_v2_BrokerInfo_descriptor;
  }

  protected FieldAccessorTable internalGetFieldAccessorTable() {
    return EmqxExHookProto.internal_static_emqx_exhook_v2_BrokerInfo_fieldAccessorTable
        .ensureFieldAccessorsInitialized(BrokerInfo.class, Builder.class);
  }

  public static final int VERSION_FIELD_NUMBER = 1;
  private volatile Object version_;

  /**
   * <code>string version = 1;</code>
   */
  public String getVersion() {
    Object ref = version_;
    if (ref instanceof String) {
      return (String) ref;
    } else {
      com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
      String s = bs.toStringUtf8();
      version_ = s;
      return s;
    }
  }

  /**
   * <code>string version = 1;</code>
   */
  public com.google.protobuf.ByteString getVersionBytes() {
    Object ref = version_;
    if (ref instanceof String) {
      com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
      version_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  public static final int SYSDESCR_FIELD_NUMBER = 2;
  private volatile Object sysdescr_;

  /**
   * <code>string sysdescr = 2;</code>
   */
  public String getSysdescr() {
    Object ref = sysdescr_;
    if (ref instanceof String) {
      return (String) ref;
    } else {
      com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
      String s = bs.toStringUtf8();
      sysdescr_ = s;
      return s;
    }
  }

  /**
   * <code>string sysdescr = 2;</code>
   */
  public com.google.protobuf.ByteString getSysdescrBytes() {
    Object ref = sysdescr_;
    if (ref instanceof String) {
      com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
      sysdescr_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  public static final int UPTIME_FIELD_NUMBER = 3;
  private long uptime_;

  /**
   * <code>int64 uptime = 3;</code>
   */
  public long getUptime() {
    return uptime_;
  }

  public static final int DATETIME_FIELD_NUMBER = 4;
  private volatile Object datetime_;

  /**
   * <code>string datetime = 4;</code>
   */
  public String getDatetime() {
    Object ref = datetime_;
    if (ref instanceof String) {
      return (String) ref;
    } else {
      com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
      String s = bs.toStringUtf8();
      datetime_ = s;
      return s;
    }
  }

  /**
   * <code>string datetime = 4;</code>
   */
  public com.google.protobuf.ByteString getDatetimeBytes() {
    Object ref = datetime_;
    if (ref instanceof String) {
      com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
      datetime_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
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
    if (!getVersionBytes().isEmpty()) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 1, version_);
    }
    if (!getSysdescrBytes().isEmpty()) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 2, sysdescr_);
    }
    if (uptime_ != 0L) {
      output.writeInt64(3, uptime_);
    }
    if (!getDatetimeBytes().isEmpty()) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 4, datetime_);
    }
    unknownFields.writeTo(output);
  }

  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1)
      return size;

    size = 0;
    if (!getVersionBytes().isEmpty()) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, version_);
    }
    if (!getSysdescrBytes().isEmpty()) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(2, sysdescr_);
    }
    if (uptime_ != 0L) {
      size += com.google.protobuf.CodedOutputStream.computeInt64Size(3, uptime_);
    }
    if (!getDatetimeBytes().isEmpty()) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(4, datetime_);
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
    if (!(obj instanceof BrokerInfo)) {
      return super.equals(obj);
    }
    BrokerInfo other = (BrokerInfo) obj;

    boolean result = true;
    result = result && getVersion().equals(other.getVersion());
    result = result && getSysdescr().equals(other.getSysdescr());
    result = result && (getUptime() == other.getUptime());
    result = result && getDatetime().equals(other.getDatetime());
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
    hash = (37 * hash) + VERSION_FIELD_NUMBER;
    hash = (53 * hash) + getVersion().hashCode();
    hash = (37 * hash) + SYSDESCR_FIELD_NUMBER;
    hash = (53 * hash) + getSysdescr().hashCode();
    hash = (37 * hash) + UPTIME_FIELD_NUMBER;
    hash = (53 * hash) + com.google.protobuf.Internal.hashLong(getUptime());
    hash = (37 * hash) + DATETIME_FIELD_NUMBER;
    hash = (53 * hash) + getDatetime().hashCode();
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static BrokerInfo parseFrom(java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static BrokerInfo parseFrom(java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static BrokerInfo parseFrom(com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static BrokerInfo parseFrom(com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static BrokerInfo parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static BrokerInfo parseFrom(byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static BrokerInfo parseFrom(java.io.InputStream input) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }

  public static BrokerInfo parseFrom(java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input,
        extensionRegistry);
  }

  public static BrokerInfo parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
  }

  public static BrokerInfo parseDelimitedFrom(java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input,
        extensionRegistry);
  }

  public static BrokerInfo parseFrom(com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }

  public static BrokerInfo parseFrom(com.google.protobuf.CodedInputStream input,
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

  public static Builder newBuilder(BrokerInfo prototype) {
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
   * Protobuf type {@code emqx.exhook.v2.BrokerInfo}
   */
  public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder>
      implements
      // @@protoc_insertion_point(builder_implements:emqx.exhook.v2.BrokerInfo)
      BrokerInfoOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return EmqxExHookProto.internal_static_emqx_exhook_v2_BrokerInfo_descriptor;
    }

    protected FieldAccessorTable internalGetFieldAccessorTable() {
      return EmqxExHookProto.internal_static_emqx_exhook_v2_BrokerInfo_fieldAccessorTable
          .ensureFieldAccessorsInitialized(BrokerInfo.class, Builder.class);
    }

    // Construct using top.rslly.iot.utility.exhook.BrokerInfo.newBuilder()
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
      version_ = "";

      sysdescr_ = "";

      uptime_ = 0L;

      datetime_ = "";

      return this;
    }

    public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
      return EmqxExHookProto.internal_static_emqx_exhook_v2_BrokerInfo_descriptor;
    }

    public BrokerInfo getDefaultInstanceForType() {
      return BrokerInfo.getDefaultInstance();
    }

    public BrokerInfo build() {
      BrokerInfo result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    public BrokerInfo buildPartial() {
      BrokerInfo result = new BrokerInfo(this);
      result.version_ = version_;
      result.sysdescr_ = sysdescr_;
      result.uptime_ = uptime_;
      result.datetime_ = datetime_;
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
      if (other instanceof BrokerInfo) {
        return mergeFrom((BrokerInfo) other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(BrokerInfo other) {
      if (other == BrokerInfo.getDefaultInstance())
        return this;
      if (!other.getVersion().isEmpty()) {
        version_ = other.version_;
        onChanged();
      }
      if (!other.getSysdescr().isEmpty()) {
        sysdescr_ = other.sysdescr_;
        onChanged();
      }
      if (other.getUptime() != 0L) {
        setUptime(other.getUptime());
      }
      if (!other.getDatetime().isEmpty()) {
        datetime_ = other.datetime_;
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
      BrokerInfo parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (BrokerInfo) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private Object version_ = "";

    /**
     * <code>string version = 1;</code>
     */
    public String getVersion() {
      Object ref = version_;
      if (!(ref instanceof String)) {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        version_ = s;
        return s;
      } else {
        return (String) ref;
      }
    }

    /**
     * <code>string version = 1;</code>
     */
    public com.google.protobuf.ByteString getVersionBytes() {
      Object ref = version_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b =
            com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        version_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    /**
     * <code>string version = 1;</code>
     */
    public Builder setVersion(String value) {
      if (value == null) {
        throw new NullPointerException();
      }

      version_ = value;
      onChanged();
      return this;
    }

    /**
     * <code>string version = 1;</code>
     */
    public Builder clearVersion() {

      version_ = getDefaultInstance().getVersion();
      onChanged();
      return this;
    }

    /**
     * <code>string version = 1;</code>
     */
    public Builder setVersionBytes(com.google.protobuf.ByteString value) {
      if (value == null) {
        throw new NullPointerException();
      }
      checkByteStringIsUtf8(value);

      version_ = value;
      onChanged();
      return this;
    }

    private Object sysdescr_ = "";

    /**
     * <code>string sysdescr = 2;</code>
     */
    public String getSysdescr() {
      Object ref = sysdescr_;
      if (!(ref instanceof String)) {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        sysdescr_ = s;
        return s;
      } else {
        return (String) ref;
      }
    }

    /**
     * <code>string sysdescr = 2;</code>
     */
    public com.google.protobuf.ByteString getSysdescrBytes() {
      Object ref = sysdescr_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b =
            com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        sysdescr_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    /**
     * <code>string sysdescr = 2;</code>
     */
    public Builder setSysdescr(String value) {
      if (value == null) {
        throw new NullPointerException();
      }

      sysdescr_ = value;
      onChanged();
      return this;
    }

    /**
     * <code>string sysdescr = 2;</code>
     */
    public Builder clearSysdescr() {

      sysdescr_ = getDefaultInstance().getSysdescr();
      onChanged();
      return this;
    }

    /**
     * <code>string sysdescr = 2;</code>
     */
    public Builder setSysdescrBytes(com.google.protobuf.ByteString value) {
      if (value == null) {
        throw new NullPointerException();
      }
      checkByteStringIsUtf8(value);

      sysdescr_ = value;
      onChanged();
      return this;
    }

    private long uptime_;

    /**
     * <code>int64 uptime = 3;</code>
     */
    public long getUptime() {
      return uptime_;
    }

    /**
     * <code>int64 uptime = 3;</code>
     */
    public Builder setUptime(long value) {

      uptime_ = value;
      onChanged();
      return this;
    }

    /**
     * <code>int64 uptime = 3;</code>
     */
    public Builder clearUptime() {

      uptime_ = 0L;
      onChanged();
      return this;
    }

    private Object datetime_ = "";

    /**
     * <code>string datetime = 4;</code>
     */
    public String getDatetime() {
      Object ref = datetime_;
      if (!(ref instanceof String)) {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        datetime_ = s;
        return s;
      } else {
        return (String) ref;
      }
    }

    /**
     * <code>string datetime = 4;</code>
     */
    public com.google.protobuf.ByteString getDatetimeBytes() {
      Object ref = datetime_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b =
            com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        datetime_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    /**
     * <code>string datetime = 4;</code>
     */
    public Builder setDatetime(String value) {
      if (value == null) {
        throw new NullPointerException();
      }

      datetime_ = value;
      onChanged();
      return this;
    }

    /**
     * <code>string datetime = 4;</code>
     */
    public Builder clearDatetime() {

      datetime_ = getDefaultInstance().getDatetime();
      onChanged();
      return this;
    }

    /**
     * <code>string datetime = 4;</code>
     */
    public Builder setDatetimeBytes(com.google.protobuf.ByteString value) {
      if (value == null) {
        throw new NullPointerException();
      }
      checkByteStringIsUtf8(value);

      datetime_ = value;
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


    // @@protoc_insertion_point(builder_scope:emqx.exhook.v2.BrokerInfo)
  }

  // @@protoc_insertion_point(class_scope:emqx.exhook.v2.BrokerInfo)
  private static final BrokerInfo DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new BrokerInfo();
  }

  public static BrokerInfo getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<BrokerInfo> PARSER =
      new com.google.protobuf.AbstractParser<BrokerInfo>() {
        public BrokerInfo parsePartialFrom(com.google.protobuf.CodedInputStream input,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
          return new BrokerInfo(input, extensionRegistry);
        }
      };

  public static com.google.protobuf.Parser<BrokerInfo> parser() {
    return PARSER;
  }

  @Override
  public com.google.protobuf.Parser<BrokerInfo> getParserForType() {
    return PARSER;
  }

  public BrokerInfo getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

