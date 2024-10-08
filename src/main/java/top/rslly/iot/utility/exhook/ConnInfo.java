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
 * Protobuf type {@code emqx.exhook.v2.ConnInfo}
 */
public final class ConnInfo extends com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:emqx.exhook.v2.ConnInfo)
    ConnInfoOrBuilder {
  private static final long serialVersionUID = 0L;

  // Use ConnInfo.newBuilder() to construct.
  private ConnInfo(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }

  private ConnInfo() {
    node_ = "";
    clientid_ = "";
    username_ = "";
    peerhost_ = "";
    sockport_ = 0;
    protoName_ = "";
    protoVer_ = "";
    keepalive_ = 0;
  }

  @Override
  public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
    return this.unknownFields;
  }

  private ConnInfo(com.google.protobuf.CodedInputStream input,
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

            clientid_ = s;
            break;
          }
          case 26: {
            String s = input.readStringRequireUtf8();

            username_ = s;
            break;
          }
          case 34: {
            String s = input.readStringRequireUtf8();

            peerhost_ = s;
            break;
          }
          case 40: {

            sockport_ = input.readUInt32();
            break;
          }
          case 50: {
            String s = input.readStringRequireUtf8();

            protoName_ = s;
            break;
          }
          case 58: {
            String s = input.readStringRequireUtf8();

            protoVer_ = s;
            break;
          }
          case 64: {

            keepalive_ = input.readUInt32();
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
    return EmqxExHookProto.internal_static_emqx_exhook_v2_ConnInfo_descriptor;
  }

  protected FieldAccessorTable internalGetFieldAccessorTable() {
    return EmqxExHookProto.internal_static_emqx_exhook_v2_ConnInfo_fieldAccessorTable
        .ensureFieldAccessorsInitialized(ConnInfo.class, Builder.class);
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

  public static final int CLIENTID_FIELD_NUMBER = 2;
  private volatile Object clientid_;

  /**
   * <code>string clientid = 2;</code>
   */
  public String getClientid() {
    Object ref = clientid_;
    if (ref instanceof String) {
      return (String) ref;
    } else {
      com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
      String s = bs.toStringUtf8();
      clientid_ = s;
      return s;
    }
  }

  /**
   * <code>string clientid = 2;</code>
   */
  public com.google.protobuf.ByteString getClientidBytes() {
    Object ref = clientid_;
    if (ref instanceof String) {
      com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
      clientid_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  public static final int USERNAME_FIELD_NUMBER = 3;
  private volatile Object username_;

  /**
   * <code>string username = 3;</code>
   */
  public String getUsername() {
    Object ref = username_;
    if (ref instanceof String) {
      return (String) ref;
    } else {
      com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
      String s = bs.toStringUtf8();
      username_ = s;
      return s;
    }
  }

  /**
   * <code>string username = 3;</code>
   */
  public com.google.protobuf.ByteString getUsernameBytes() {
    Object ref = username_;
    if (ref instanceof String) {
      com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
      username_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  public static final int PEERHOST_FIELD_NUMBER = 4;
  private volatile Object peerhost_;

  /**
   * <code>string peerhost = 4;</code>
   */
  public String getPeerhost() {
    Object ref = peerhost_;
    if (ref instanceof String) {
      return (String) ref;
    } else {
      com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
      String s = bs.toStringUtf8();
      peerhost_ = s;
      return s;
    }
  }

  /**
   * <code>string peerhost = 4;</code>
   */
  public com.google.protobuf.ByteString getPeerhostBytes() {
    Object ref = peerhost_;
    if (ref instanceof String) {
      com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
      peerhost_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  public static final int SOCKPORT_FIELD_NUMBER = 5;
  private int sockport_;

  /**
   * <code>uint32 sockport = 5;</code>
   */
  public int getSockport() {
    return sockport_;
  }

  public static final int PROTO_NAME_FIELD_NUMBER = 6;
  private volatile Object protoName_;

  /**
   * <code>string proto_name = 6;</code>
   */
  public String getProtoName() {
    Object ref = protoName_;
    if (ref instanceof String) {
      return (String) ref;
    } else {
      com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
      String s = bs.toStringUtf8();
      protoName_ = s;
      return s;
    }
  }

  /**
   * <code>string proto_name = 6;</code>
   */
  public com.google.protobuf.ByteString getProtoNameBytes() {
    Object ref = protoName_;
    if (ref instanceof String) {
      com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
      protoName_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  public static final int PROTO_VER_FIELD_NUMBER = 7;
  private volatile Object protoVer_;

  /**
   * <code>string proto_ver = 7;</code>
   */
  public String getProtoVer() {
    Object ref = protoVer_;
    if (ref instanceof String) {
      return (String) ref;
    } else {
      com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
      String s = bs.toStringUtf8();
      protoVer_ = s;
      return s;
    }
  }

  /**
   * <code>string proto_ver = 7;</code>
   */
  public com.google.protobuf.ByteString getProtoVerBytes() {
    Object ref = protoVer_;
    if (ref instanceof String) {
      com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
      protoVer_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  public static final int KEEPALIVE_FIELD_NUMBER = 8;
  private int keepalive_;

  /**
   * <code>uint32 keepalive = 8;</code>
   */
  public int getKeepalive() {
    return keepalive_;
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
    if (!getClientidBytes().isEmpty()) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 2, clientid_);
    }
    if (!getUsernameBytes().isEmpty()) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 3, username_);
    }
    if (!getPeerhostBytes().isEmpty()) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 4, peerhost_);
    }
    if (sockport_ != 0) {
      output.writeUInt32(5, sockport_);
    }
    if (!getProtoNameBytes().isEmpty()) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 6, protoName_);
    }
    if (!getProtoVerBytes().isEmpty()) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 7, protoVer_);
    }
    if (keepalive_ != 0) {
      output.writeUInt32(8, keepalive_);
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
    if (!getClientidBytes().isEmpty()) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(2, clientid_);
    }
    if (!getUsernameBytes().isEmpty()) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(3, username_);
    }
    if (!getPeerhostBytes().isEmpty()) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(4, peerhost_);
    }
    if (sockport_ != 0) {
      size += com.google.protobuf.CodedOutputStream.computeUInt32Size(5, sockport_);
    }
    if (!getProtoNameBytes().isEmpty()) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(6, protoName_);
    }
    if (!getProtoVerBytes().isEmpty()) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(7, protoVer_);
    }
    if (keepalive_ != 0) {
      size += com.google.protobuf.CodedOutputStream.computeUInt32Size(8, keepalive_);
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
    if (!(obj instanceof ConnInfo)) {
      return super.equals(obj);
    }
    ConnInfo other = (ConnInfo) obj;

    boolean result = true;
    result = result && getNode().equals(other.getNode());
    result = result && getClientid().equals(other.getClientid());
    result = result && getUsername().equals(other.getUsername());
    result = result && getPeerhost().equals(other.getPeerhost());
    result = result && (getSockport() == other.getSockport());
    result = result && getProtoName().equals(other.getProtoName());
    result = result && getProtoVer().equals(other.getProtoVer());
    result = result && (getKeepalive() == other.getKeepalive());
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
    hash = (37 * hash) + CLIENTID_FIELD_NUMBER;
    hash = (53 * hash) + getClientid().hashCode();
    hash = (37 * hash) + USERNAME_FIELD_NUMBER;
    hash = (53 * hash) + getUsername().hashCode();
    hash = (37 * hash) + PEERHOST_FIELD_NUMBER;
    hash = (53 * hash) + getPeerhost().hashCode();
    hash = (37 * hash) + SOCKPORT_FIELD_NUMBER;
    hash = (53 * hash) + getSockport();
    hash = (37 * hash) + PROTO_NAME_FIELD_NUMBER;
    hash = (53 * hash) + getProtoName().hashCode();
    hash = (37 * hash) + PROTO_VER_FIELD_NUMBER;
    hash = (53 * hash) + getProtoVer().hashCode();
    hash = (37 * hash) + KEEPALIVE_FIELD_NUMBER;
    hash = (53 * hash) + getKeepalive();
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static ConnInfo parseFrom(java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static ConnInfo parseFrom(java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static ConnInfo parseFrom(com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static ConnInfo parseFrom(com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static ConnInfo parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static ConnInfo parseFrom(byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static ConnInfo parseFrom(java.io.InputStream input) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }

  public static ConnInfo parseFrom(java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input,
        extensionRegistry);
  }

  public static ConnInfo parseDelimitedFrom(java.io.InputStream input) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
  }

  public static ConnInfo parseDelimitedFrom(java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input,
        extensionRegistry);
  }

  public static ConnInfo parseFrom(com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }

  public static ConnInfo parseFrom(com.google.protobuf.CodedInputStream input,
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

  public static Builder newBuilder(ConnInfo prototype) {
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
   * Protobuf type {@code emqx.exhook.v2.ConnInfo}
   */
  public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder>
      implements
      // @@protoc_insertion_point(builder_implements:emqx.exhook.v2.ConnInfo)
      ConnInfoOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return EmqxExHookProto.internal_static_emqx_exhook_v2_ConnInfo_descriptor;
    }

    protected FieldAccessorTable internalGetFieldAccessorTable() {
      return EmqxExHookProto.internal_static_emqx_exhook_v2_ConnInfo_fieldAccessorTable
          .ensureFieldAccessorsInitialized(ConnInfo.class, Builder.class);
    }

    // Construct using top.rslly.iot.utility.exhook.ConnInfo.newBuilder()
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

      clientid_ = "";

      username_ = "";

      peerhost_ = "";

      sockport_ = 0;

      protoName_ = "";

      protoVer_ = "";

      keepalive_ = 0;

      return this;
    }

    public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
      return EmqxExHookProto.internal_static_emqx_exhook_v2_ConnInfo_descriptor;
    }

    public ConnInfo getDefaultInstanceForType() {
      return ConnInfo.getDefaultInstance();
    }

    public ConnInfo build() {
      ConnInfo result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    public ConnInfo buildPartial() {
      ConnInfo result = new ConnInfo(this);
      result.node_ = node_;
      result.clientid_ = clientid_;
      result.username_ = username_;
      result.peerhost_ = peerhost_;
      result.sockport_ = sockport_;
      result.protoName_ = protoName_;
      result.protoVer_ = protoVer_;
      result.keepalive_ = keepalive_;
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
      if (other instanceof ConnInfo) {
        return mergeFrom((ConnInfo) other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(ConnInfo other) {
      if (other == ConnInfo.getDefaultInstance())
        return this;
      if (!other.getNode().isEmpty()) {
        node_ = other.node_;
        onChanged();
      }
      if (!other.getClientid().isEmpty()) {
        clientid_ = other.clientid_;
        onChanged();
      }
      if (!other.getUsername().isEmpty()) {
        username_ = other.username_;
        onChanged();
      }
      if (!other.getPeerhost().isEmpty()) {
        peerhost_ = other.peerhost_;
        onChanged();
      }
      if (other.getSockport() != 0) {
        setSockport(other.getSockport());
      }
      if (!other.getProtoName().isEmpty()) {
        protoName_ = other.protoName_;
        onChanged();
      }
      if (!other.getProtoVer().isEmpty()) {
        protoVer_ = other.protoVer_;
        onChanged();
      }
      if (other.getKeepalive() != 0) {
        setKeepalive(other.getKeepalive());
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
      ConnInfo parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (ConnInfo) e.getUnfinishedMessage();
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

    private Object clientid_ = "";

    /**
     * <code>string clientid = 2;</code>
     */
    public String getClientid() {
      Object ref = clientid_;
      if (!(ref instanceof String)) {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        clientid_ = s;
        return s;
      } else {
        return (String) ref;
      }
    }

    /**
     * <code>string clientid = 2;</code>
     */
    public com.google.protobuf.ByteString getClientidBytes() {
      Object ref = clientid_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b =
            com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        clientid_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    /**
     * <code>string clientid = 2;</code>
     */
    public Builder setClientid(String value) {
      if (value == null) {
        throw new NullPointerException();
      }

      clientid_ = value;
      onChanged();
      return this;
    }

    /**
     * <code>string clientid = 2;</code>
     */
    public Builder clearClientid() {

      clientid_ = getDefaultInstance().getClientid();
      onChanged();
      return this;
    }

    /**
     * <code>string clientid = 2;</code>
     */
    public Builder setClientidBytes(com.google.protobuf.ByteString value) {
      if (value == null) {
        throw new NullPointerException();
      }
      checkByteStringIsUtf8(value);

      clientid_ = value;
      onChanged();
      return this;
    }

    private Object username_ = "";

    /**
     * <code>string username = 3;</code>
     */
    public String getUsername() {
      Object ref = username_;
      if (!(ref instanceof String)) {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        username_ = s;
        return s;
      } else {
        return (String) ref;
      }
    }

    /**
     * <code>string username = 3;</code>
     */
    public com.google.protobuf.ByteString getUsernameBytes() {
      Object ref = username_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b =
            com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        username_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    /**
     * <code>string username = 3;</code>
     */
    public Builder setUsername(String value) {
      if (value == null) {
        throw new NullPointerException();
      }

      username_ = value;
      onChanged();
      return this;
    }

    /**
     * <code>string username = 3;</code>
     */
    public Builder clearUsername() {

      username_ = getDefaultInstance().getUsername();
      onChanged();
      return this;
    }

    /**
     * <code>string username = 3;</code>
     */
    public Builder setUsernameBytes(com.google.protobuf.ByteString value) {
      if (value == null) {
        throw new NullPointerException();
      }
      checkByteStringIsUtf8(value);

      username_ = value;
      onChanged();
      return this;
    }

    private Object peerhost_ = "";

    /**
     * <code>string peerhost = 4;</code>
     */
    public String getPeerhost() {
      Object ref = peerhost_;
      if (!(ref instanceof String)) {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        peerhost_ = s;
        return s;
      } else {
        return (String) ref;
      }
    }

    /**
     * <code>string peerhost = 4;</code>
     */
    public com.google.protobuf.ByteString getPeerhostBytes() {
      Object ref = peerhost_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b =
            com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        peerhost_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    /**
     * <code>string peerhost = 4;</code>
     */
    public Builder setPeerhost(String value) {
      if (value == null) {
        throw new NullPointerException();
      }

      peerhost_ = value;
      onChanged();
      return this;
    }

    /**
     * <code>string peerhost = 4;</code>
     */
    public Builder clearPeerhost() {

      peerhost_ = getDefaultInstance().getPeerhost();
      onChanged();
      return this;
    }

    /**
     * <code>string peerhost = 4;</code>
     */
    public Builder setPeerhostBytes(com.google.protobuf.ByteString value) {
      if (value == null) {
        throw new NullPointerException();
      }
      checkByteStringIsUtf8(value);

      peerhost_ = value;
      onChanged();
      return this;
    }

    private int sockport_;

    /**
     * <code>uint32 sockport = 5;</code>
     */
    public int getSockport() {
      return sockport_;
    }

    /**
     * <code>uint32 sockport = 5;</code>
     */
    public Builder setSockport(int value) {

      sockport_ = value;
      onChanged();
      return this;
    }

    /**
     * <code>uint32 sockport = 5;</code>
     */
    public Builder clearSockport() {

      sockport_ = 0;
      onChanged();
      return this;
    }

    private Object protoName_ = "";

    /**
     * <code>string proto_name = 6;</code>
     */
    public String getProtoName() {
      Object ref = protoName_;
      if (!(ref instanceof String)) {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        protoName_ = s;
        return s;
      } else {
        return (String) ref;
      }
    }

    /**
     * <code>string proto_name = 6;</code>
     */
    public com.google.protobuf.ByteString getProtoNameBytes() {
      Object ref = protoName_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b =
            com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        protoName_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    /**
     * <code>string proto_name = 6;</code>
     */
    public Builder setProtoName(String value) {
      if (value == null) {
        throw new NullPointerException();
      }

      protoName_ = value;
      onChanged();
      return this;
    }

    /**
     * <code>string proto_name = 6;</code>
     */
    public Builder clearProtoName() {

      protoName_ = getDefaultInstance().getProtoName();
      onChanged();
      return this;
    }

    /**
     * <code>string proto_name = 6;</code>
     */
    public Builder setProtoNameBytes(com.google.protobuf.ByteString value) {
      if (value == null) {
        throw new NullPointerException();
      }
      checkByteStringIsUtf8(value);

      protoName_ = value;
      onChanged();
      return this;
    }

    private Object protoVer_ = "";

    /**
     * <code>string proto_ver = 7;</code>
     */
    public String getProtoVer() {
      Object ref = protoVer_;
      if (!(ref instanceof String)) {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        protoVer_ = s;
        return s;
      } else {
        return (String) ref;
      }
    }

    /**
     * <code>string proto_ver = 7;</code>
     */
    public com.google.protobuf.ByteString getProtoVerBytes() {
      Object ref = protoVer_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b =
            com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        protoVer_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    /**
     * <code>string proto_ver = 7;</code>
     */
    public Builder setProtoVer(String value) {
      if (value == null) {
        throw new NullPointerException();
      }

      protoVer_ = value;
      onChanged();
      return this;
    }

    /**
     * <code>string proto_ver = 7;</code>
     */
    public Builder clearProtoVer() {

      protoVer_ = getDefaultInstance().getProtoVer();
      onChanged();
      return this;
    }

    /**
     * <code>string proto_ver = 7;</code>
     */
    public Builder setProtoVerBytes(com.google.protobuf.ByteString value) {
      if (value == null) {
        throw new NullPointerException();
      }
      checkByteStringIsUtf8(value);

      protoVer_ = value;
      onChanged();
      return this;
    }

    private int keepalive_;

    /**
     * <code>uint32 keepalive = 8;</code>
     */
    public int getKeepalive() {
      return keepalive_;
    }

    /**
     * <code>uint32 keepalive = 8;</code>
     */
    public Builder setKeepalive(int value) {

      keepalive_ = value;
      onChanged();
      return this;
    }

    /**
     * <code>uint32 keepalive = 8;</code>
     */
    public Builder clearKeepalive() {

      keepalive_ = 0;
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


    // @@protoc_insertion_point(builder_scope:emqx.exhook.v2.ConnInfo)
  }

  // @@protoc_insertion_point(class_scope:emqx.exhook.v2.ConnInfo)
  private static final ConnInfo DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new ConnInfo();
  }

  public static ConnInfo getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<ConnInfo> PARSER =
      new com.google.protobuf.AbstractParser<ConnInfo>() {
        public ConnInfo parsePartialFrom(com.google.protobuf.CodedInputStream input,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
          return new ConnInfo(input, extensionRegistry);
        }
      };

  public static com.google.protobuf.Parser<ConnInfo> parser() {
    return PARSER;
  }

  @Override
  public com.google.protobuf.Parser<ConnInfo> getParserForType() {
    return PARSER;
  }

  public ConnInfo getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

