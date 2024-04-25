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
 * Protobuf type {@code emqx.exhook.v2.RequestMeta}
 */
public final class RequestMeta extends com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:emqx.exhook.v2.RequestMeta)
    RequestMetaOrBuilder {
  private static final long serialVersionUID = 0L;

  // Use RequestMeta.newBuilder() to construct.
  private RequestMeta(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }

  private RequestMeta() {
    node_ = "";
    version_ = "";
    sysdescr_ = "";
    clusterName_ = "";
  }

  @Override
  public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
    return this.unknownFields;
  }

  private RequestMeta(com.google.protobuf.CodedInputStream input,
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

            node_ = s;
            break;
          }
          case 18: {
            String s = input.readStringRequireUtf8();

            version_ = s;
            break;
          }
          case 26: {
            String s = input.readStringRequireUtf8();

            sysdescr_ = s;
            break;
          }
          case 34: {
            String s = input.readStringRequireUtf8();

            clusterName_ = s;
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
    return EmqxExHookProto.internal_static_emqx_exhook_v2_RequestMeta_descriptor;
  }

  protected FieldAccessorTable internalGetFieldAccessorTable() {
    return EmqxExHookProto.internal_static_emqx_exhook_v2_RequestMeta_fieldAccessorTable
        .ensureFieldAccessorsInitialized(RequestMeta.class, Builder.class);
  }

  public static final int NODE_FIELD_NUMBER = 1;
  private volatile Object node_;

  /**
   * <code>string node = 1;</code>
   */
  public String getNode() {
    Object ref = node_;
    if (ref instanceof String) {
      return (String) ref;
    } else {
      com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
      String s = bs.toStringUtf8();
      node_ = s;
      return s;
    }
  }

  /**
   * <code>string node = 1;</code>
   */
  public com.google.protobuf.ByteString getNodeBytes() {
    Object ref = node_;
    if (ref instanceof String) {
      com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
      node_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  public static final int VERSION_FIELD_NUMBER = 2;
  private volatile Object version_;

  /**
   * <code>string version = 2;</code>
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
   * <code>string version = 2;</code>
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

  public static final int SYSDESCR_FIELD_NUMBER = 3;
  private volatile Object sysdescr_;

  /**
   * <code>string sysdescr = 3;</code>
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
   * <code>string sysdescr = 3;</code>
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

  public static final int CLUSTER_NAME_FIELD_NUMBER = 4;
  private volatile Object clusterName_;

  /**
   * <code>string cluster_name = 4;</code>
   */
  public String getClusterName() {
    Object ref = clusterName_;
    if (ref instanceof String) {
      return (String) ref;
    } else {
      com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
      String s = bs.toStringUtf8();
      clusterName_ = s;
      return s;
    }
  }

  /**
   * <code>string cluster_name = 4;</code>
   */
  public com.google.protobuf.ByteString getClusterNameBytes() {
    Object ref = clusterName_;
    if (ref instanceof String) {
      com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
      clusterName_ = b;
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
    if (!getNodeBytes().isEmpty()) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 1, node_);
    }
    if (!getVersionBytes().isEmpty()) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 2, version_);
    }
    if (!getSysdescrBytes().isEmpty()) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 3, sysdescr_);
    }
    if (!getClusterNameBytes().isEmpty()) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 4, clusterName_);
    }
    unknownFields.writeTo(output);
  }

  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1)
      return size;

    size = 0;
    if (!getNodeBytes().isEmpty()) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, node_);
    }
    if (!getVersionBytes().isEmpty()) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(2, version_);
    }
    if (!getSysdescrBytes().isEmpty()) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(3, sysdescr_);
    }
    if (!getClusterNameBytes().isEmpty()) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(4, clusterName_);
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
    if (!(obj instanceof RequestMeta)) {
      return super.equals(obj);
    }
    RequestMeta other = (RequestMeta) obj;

    boolean result = true;
    result = result && getNode().equals(other.getNode());
    result = result && getVersion().equals(other.getVersion());
    result = result && getSysdescr().equals(other.getSysdescr());
    result = result && getClusterName().equals(other.getClusterName());
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
    hash = (37 * hash) + NODE_FIELD_NUMBER;
    hash = (53 * hash) + getNode().hashCode();
    hash = (37 * hash) + VERSION_FIELD_NUMBER;
    hash = (53 * hash) + getVersion().hashCode();
    hash = (37 * hash) + SYSDESCR_FIELD_NUMBER;
    hash = (53 * hash) + getSysdescr().hashCode();
    hash = (37 * hash) + CLUSTER_NAME_FIELD_NUMBER;
    hash = (53 * hash) + getClusterName().hashCode();
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static RequestMeta parseFrom(java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static RequestMeta parseFrom(java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static RequestMeta parseFrom(com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static RequestMeta parseFrom(com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static RequestMeta parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static RequestMeta parseFrom(byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static RequestMeta parseFrom(java.io.InputStream input) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }

  public static RequestMeta parseFrom(java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input,
        extensionRegistry);
  }

  public static RequestMeta parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
  }

  public static RequestMeta parseDelimitedFrom(java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input,
        extensionRegistry);
  }

  public static RequestMeta parseFrom(com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }

  public static RequestMeta parseFrom(com.google.protobuf.CodedInputStream input,
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

  public static Builder newBuilder(RequestMeta prototype) {
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
   * Protobuf type {@code emqx.exhook.v2.RequestMeta}
   */
  public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder>
      implements
      // @@protoc_insertion_point(builder_implements:emqx.exhook.v2.RequestMeta)
      RequestMetaOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return EmqxExHookProto.internal_static_emqx_exhook_v2_RequestMeta_descriptor;
    }

    protected FieldAccessorTable internalGetFieldAccessorTable() {
      return EmqxExHookProto.internal_static_emqx_exhook_v2_RequestMeta_fieldAccessorTable
          .ensureFieldAccessorsInitialized(RequestMeta.class, Builder.class);
    }

    // Construct using top.rslly.iot.utility.exhook.RequestMeta.newBuilder()
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
      node_ = "";

      version_ = "";

      sysdescr_ = "";

      clusterName_ = "";

      return this;
    }

    public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
      return EmqxExHookProto.internal_static_emqx_exhook_v2_RequestMeta_descriptor;
    }

    public RequestMeta getDefaultInstanceForType() {
      return RequestMeta.getDefaultInstance();
    }

    public RequestMeta build() {
      RequestMeta result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    public RequestMeta buildPartial() {
      RequestMeta result = new RequestMeta(this);
      result.node_ = node_;
      result.version_ = version_;
      result.sysdescr_ = sysdescr_;
      result.clusterName_ = clusterName_;
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
      if (other instanceof RequestMeta) {
        return mergeFrom((RequestMeta) other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(RequestMeta other) {
      if (other == RequestMeta.getDefaultInstance())
        return this;
      if (!other.getNode().isEmpty()) {
        node_ = other.node_;
        onChanged();
      }
      if (!other.getVersion().isEmpty()) {
        version_ = other.version_;
        onChanged();
      }
      if (!other.getSysdescr().isEmpty()) {
        sysdescr_ = other.sysdescr_;
        onChanged();
      }
      if (!other.getClusterName().isEmpty()) {
        clusterName_ = other.clusterName_;
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
      RequestMeta parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (RequestMeta) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private Object node_ = "";

    /**
     * <code>string node = 1;</code>
     */
    public String getNode() {
      Object ref = node_;
      if (!(ref instanceof String)) {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        node_ = s;
        return s;
      } else {
        return (String) ref;
      }
    }

    /**
     * <code>string node = 1;</code>
     */
    public com.google.protobuf.ByteString getNodeBytes() {
      Object ref = node_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b =
            com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        node_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    /**
     * <code>string node = 1;</code>
     */
    public Builder setNode(String value) {
      if (value == null) {
        throw new NullPointerException();
      }

      node_ = value;
      onChanged();
      return this;
    }

    /**
     * <code>string node = 1;</code>
     */
    public Builder clearNode() {

      node_ = getDefaultInstance().getNode();
      onChanged();
      return this;
    }

    /**
     * <code>string node = 1;</code>
     */
    public Builder setNodeBytes(com.google.protobuf.ByteString value) {
      if (value == null) {
        throw new NullPointerException();
      }
      checkByteStringIsUtf8(value);

      node_ = value;
      onChanged();
      return this;
    }

    private Object version_ = "";

    /**
     * <code>string version = 2;</code>
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
     * <code>string version = 2;</code>
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
     * <code>string version = 2;</code>
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
     * <code>string version = 2;</code>
     */
    public Builder clearVersion() {

      version_ = getDefaultInstance().getVersion();
      onChanged();
      return this;
    }

    /**
     * <code>string version = 2;</code>
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
     * <code>string sysdescr = 3;</code>
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
     * <code>string sysdescr = 3;</code>
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
     * <code>string sysdescr = 3;</code>
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
     * <code>string sysdescr = 3;</code>
     */
    public Builder clearSysdescr() {

      sysdescr_ = getDefaultInstance().getSysdescr();
      onChanged();
      return this;
    }

    /**
     * <code>string sysdescr = 3;</code>
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

    private Object clusterName_ = "";

    /**
     * <code>string cluster_name = 4;</code>
     */
    public String getClusterName() {
      Object ref = clusterName_;
      if (!(ref instanceof String)) {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        clusterName_ = s;
        return s;
      } else {
        return (String) ref;
      }
    }

    /**
     * <code>string cluster_name = 4;</code>
     */
    public com.google.protobuf.ByteString getClusterNameBytes() {
      Object ref = clusterName_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b =
            com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        clusterName_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    /**
     * <code>string cluster_name = 4;</code>
     */
    public Builder setClusterName(String value) {
      if (value == null) {
        throw new NullPointerException();
      }

      clusterName_ = value;
      onChanged();
      return this;
    }

    /**
     * <code>string cluster_name = 4;</code>
     */
    public Builder clearClusterName() {

      clusterName_ = getDefaultInstance().getClusterName();
      onChanged();
      return this;
    }

    /**
     * <code>string cluster_name = 4;</code>
     */
    public Builder setClusterNameBytes(com.google.protobuf.ByteString value) {
      if (value == null) {
        throw new NullPointerException();
      }
      checkByteStringIsUtf8(value);

      clusterName_ = value;
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


    // @@protoc_insertion_point(builder_scope:emqx.exhook.v2.RequestMeta)
  }

  // @@protoc_insertion_point(class_scope:emqx.exhook.v2.RequestMeta)
  private static final RequestMeta DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new RequestMeta();
  }

  public static RequestMeta getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<RequestMeta> PARSER =
      new com.google.protobuf.AbstractParser<RequestMeta>() {
        public RequestMeta parsePartialFrom(com.google.protobuf.CodedInputStream input,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
          return new RequestMeta(input, extensionRegistry);
        }
      };

  public static com.google.protobuf.Parser<RequestMeta> parser() {
    return PARSER;
  }

  @Override
  public com.google.protobuf.Parser<RequestMeta> getParserForType() {
    return PARSER;
  }

  public RequestMeta getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

