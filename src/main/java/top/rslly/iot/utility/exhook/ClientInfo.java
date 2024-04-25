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
 * Protobuf type {@code emqx.exhook.v2.ClientInfo}
 */
public final class ClientInfo extends com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:emqx.exhook.v2.ClientInfo)
    ClientInfoOrBuilder {
  private static final long serialVersionUID = 0L;

  // Use ClientInfo.newBuilder() to construct.
  private ClientInfo(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }

  private ClientInfo() {
    node_ = "";
    clientid_ = "";
    username_ = "";
    password_ = "";
    peerhost_ = "";
    sockport_ = 0;
    protocol_ = "";
    mountpoint_ = "";
    isSuperuser_ = false;
    anonymous_ = false;
    cn_ = "";
    dn_ = "";
  }

  @Override
  public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
    return this.unknownFields;
  }

  private ClientInfo(com.google.protobuf.CodedInputStream input,
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

            password_ = s;
            break;
          }
          case 42: {
            String s = input.readStringRequireUtf8();

            peerhost_ = s;
            break;
          }
          case 48: {

            sockport_ = input.readUInt32();
            break;
          }
          case 58: {
            String s = input.readStringRequireUtf8();

            protocol_ = s;
            break;
          }
          case 66: {
            String s = input.readStringRequireUtf8();

            mountpoint_ = s;
            break;
          }
          case 72: {

            isSuperuser_ = input.readBool();
            break;
          }
          case 80: {

            anonymous_ = input.readBool();
            break;
          }
          case 90: {
            String s = input.readStringRequireUtf8();

            cn_ = s;
            break;
          }
          case 98: {
            String s = input.readStringRequireUtf8();

            dn_ = s;
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
    return EmqxExHookProto.internal_static_emqx_exhook_v2_ClientInfo_descriptor;
  }

  protected FieldAccessorTable internalGetFieldAccessorTable() {
    return EmqxExHookProto.internal_static_emqx_exhook_v2_ClientInfo_fieldAccessorTable
        .ensureFieldAccessorsInitialized(ClientInfo.class, Builder.class);
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

  public static final int PASSWORD_FIELD_NUMBER = 4;
  private volatile Object password_;

  /**
   * <code>string password = 4;</code>
   */
  public String getPassword() {
    Object ref = password_;
    if (ref instanceof String) {
      return (String) ref;
    } else {
      com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
      String s = bs.toStringUtf8();
      password_ = s;
      return s;
    }
  }

  /**
   * <code>string password = 4;</code>
   */
  public com.google.protobuf.ByteString getPasswordBytes() {
    Object ref = password_;
    if (ref instanceof String) {
      com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
      password_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  public static final int PEERHOST_FIELD_NUMBER = 5;
  private volatile Object peerhost_;

  /**
   * <code>string peerhost = 5;</code>
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
   * <code>string peerhost = 5;</code>
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

  public static final int SOCKPORT_FIELD_NUMBER = 6;
  private int sockport_;

  /**
   * <code>uint32 sockport = 6;</code>
   */
  public int getSockport() {
    return sockport_;
  }

  public static final int PROTOCOL_FIELD_NUMBER = 7;
  private volatile Object protocol_;

  /**
   * <code>string protocol = 7;</code>
   */
  public String getProtocol() {
    Object ref = protocol_;
    if (ref instanceof String) {
      return (String) ref;
    } else {
      com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
      String s = bs.toStringUtf8();
      protocol_ = s;
      return s;
    }
  }

  /**
   * <code>string protocol = 7;</code>
   */
  public com.google.protobuf.ByteString getProtocolBytes() {
    Object ref = protocol_;
    if (ref instanceof String) {
      com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
      protocol_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  public static final int MOUNTPOINT_FIELD_NUMBER = 8;
  private volatile Object mountpoint_;

  /**
   * <code>string mountpoint = 8;</code>
   */
  public String getMountpoint() {
    Object ref = mountpoint_;
    if (ref instanceof String) {
      return (String) ref;
    } else {
      com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
      String s = bs.toStringUtf8();
      mountpoint_ = s;
      return s;
    }
  }

  /**
   * <code>string mountpoint = 8;</code>
   */
  public com.google.protobuf.ByteString getMountpointBytes() {
    Object ref = mountpoint_;
    if (ref instanceof String) {
      com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
      mountpoint_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  public static final int IS_SUPERUSER_FIELD_NUMBER = 9;
  private boolean isSuperuser_;

  /**
   * <code>bool is_superuser = 9;</code>
   */
  public boolean getIsSuperuser() {
    return isSuperuser_;
  }

  public static final int ANONYMOUS_FIELD_NUMBER = 10;
  private boolean anonymous_;

  /**
   * <code>bool anonymous = 10;</code>
   */
  public boolean getAnonymous() {
    return anonymous_;
  }

  public static final int CN_FIELD_NUMBER = 11;
  private volatile Object cn_;

  /**
   * <pre>
   * common name of client TLS cert
   * </pre>
   *
   * <code>string cn = 11;</code>
   */
  public String getCn() {
    Object ref = cn_;
    if (ref instanceof String) {
      return (String) ref;
    } else {
      com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
      String s = bs.toStringUtf8();
      cn_ = s;
      return s;
    }
  }

  /**
   * <pre>
   * common name of client TLS cert
   * </pre>
   *
   * <code>string cn = 11;</code>
   */
  public com.google.protobuf.ByteString getCnBytes() {
    Object ref = cn_;
    if (ref instanceof String) {
      com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
      cn_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  public static final int DN_FIELD_NUMBER = 12;
  private volatile Object dn_;

  /**
   * <pre>
   * subject of client TLS cert
   * </pre>
   *
   * <code>string dn = 12;</code>
   */
  public String getDn() {
    Object ref = dn_;
    if (ref instanceof String) {
      return (String) ref;
    } else {
      com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
      String s = bs.toStringUtf8();
      dn_ = s;
      return s;
    }
  }

  /**
   * <pre>
   * subject of client TLS cert
   * </pre>
   *
   * <code>string dn = 12;</code>
   */
  public com.google.protobuf.ByteString getDnBytes() {
    Object ref = dn_;
    if (ref instanceof String) {
      com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
      dn_ = b;
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
    if (!getClientidBytes().isEmpty()) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 2, clientid_);
    }
    if (!getUsernameBytes().isEmpty()) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 3, username_);
    }
    if (!getPasswordBytes().isEmpty()) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 4, password_);
    }
    if (!getPeerhostBytes().isEmpty()) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 5, peerhost_);
    }
    if (sockport_ != 0) {
      output.writeUInt32(6, sockport_);
    }
    if (!getProtocolBytes().isEmpty()) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 7, protocol_);
    }
    if (!getMountpointBytes().isEmpty()) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 8, mountpoint_);
    }
    if (isSuperuser_ != false) {
      output.writeBool(9, isSuperuser_);
    }
    if (anonymous_ != false) {
      output.writeBool(10, anonymous_);
    }
    if (!getCnBytes().isEmpty()) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 11, cn_);
    }
    if (!getDnBytes().isEmpty()) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 12, dn_);
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
    if (!getPasswordBytes().isEmpty()) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(4, password_);
    }
    if (!getPeerhostBytes().isEmpty()) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(5, peerhost_);
    }
    if (sockport_ != 0) {
      size += com.google.protobuf.CodedOutputStream.computeUInt32Size(6, sockport_);
    }
    if (!getProtocolBytes().isEmpty()) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(7, protocol_);
    }
    if (!getMountpointBytes().isEmpty()) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(8, mountpoint_);
    }
    if (isSuperuser_ != false) {
      size += com.google.protobuf.CodedOutputStream.computeBoolSize(9, isSuperuser_);
    }
    if (anonymous_ != false) {
      size += com.google.protobuf.CodedOutputStream.computeBoolSize(10, anonymous_);
    }
    if (!getCnBytes().isEmpty()) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(11, cn_);
    }
    if (!getDnBytes().isEmpty()) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(12, dn_);
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
    if (!(obj instanceof ClientInfo)) {
      return super.equals(obj);
    }
    ClientInfo other = (ClientInfo) obj;

    boolean result = true;
    result = result && getNode().equals(other.getNode());
    result = result && getClientid().equals(other.getClientid());
    result = result && getUsername().equals(other.getUsername());
    result = result && getPassword().equals(other.getPassword());
    result = result && getPeerhost().equals(other.getPeerhost());
    result = result && (getSockport() == other.getSockport());
    result = result && getProtocol().equals(other.getProtocol());
    result = result && getMountpoint().equals(other.getMountpoint());
    result = result && (getIsSuperuser() == other.getIsSuperuser());
    result = result && (getAnonymous() == other.getAnonymous());
    result = result && getCn().equals(other.getCn());
    result = result && getDn().equals(other.getDn());
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
    hash = (37 * hash) + PASSWORD_FIELD_NUMBER;
    hash = (53 * hash) + getPassword().hashCode();
    hash = (37 * hash) + PEERHOST_FIELD_NUMBER;
    hash = (53 * hash) + getPeerhost().hashCode();
    hash = (37 * hash) + SOCKPORT_FIELD_NUMBER;
    hash = (53 * hash) + getSockport();
    hash = (37 * hash) + PROTOCOL_FIELD_NUMBER;
    hash = (53 * hash) + getProtocol().hashCode();
    hash = (37 * hash) + MOUNTPOINT_FIELD_NUMBER;
    hash = (53 * hash) + getMountpoint().hashCode();
    hash = (37 * hash) + IS_SUPERUSER_FIELD_NUMBER;
    hash = (53 * hash) + com.google.protobuf.Internal.hashBoolean(getIsSuperuser());
    hash = (37 * hash) + ANONYMOUS_FIELD_NUMBER;
    hash = (53 * hash) + com.google.protobuf.Internal.hashBoolean(getAnonymous());
    hash = (37 * hash) + CN_FIELD_NUMBER;
    hash = (53 * hash) + getCn().hashCode();
    hash = (37 * hash) + DN_FIELD_NUMBER;
    hash = (53 * hash) + getDn().hashCode();
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static ClientInfo parseFrom(java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static ClientInfo parseFrom(java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static ClientInfo parseFrom(com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static ClientInfo parseFrom(com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static ClientInfo parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static ClientInfo parseFrom(byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static ClientInfo parseFrom(java.io.InputStream input) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }

  public static ClientInfo parseFrom(java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input,
        extensionRegistry);
  }

  public static ClientInfo parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
  }

  public static ClientInfo parseDelimitedFrom(java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input,
        extensionRegistry);
  }

  public static ClientInfo parseFrom(com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }

  public static ClientInfo parseFrom(com.google.protobuf.CodedInputStream input,
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

  public static Builder newBuilder(ClientInfo prototype) {
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
   * Protobuf type {@code emqx.exhook.v2.ClientInfo}
   */
  public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder>
      implements
      // @@protoc_insertion_point(builder_implements:emqx.exhook.v2.ClientInfo)
      ClientInfoOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return EmqxExHookProto.internal_static_emqx_exhook_v2_ClientInfo_descriptor;
    }

    protected FieldAccessorTable internalGetFieldAccessorTable() {
      return EmqxExHookProto.internal_static_emqx_exhook_v2_ClientInfo_fieldAccessorTable
          .ensureFieldAccessorsInitialized(ClientInfo.class, Builder.class);
    }

    // Construct using top.rslly.iot.utility.exhook.ClientInfo.newBuilder()
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

      password_ = "";

      peerhost_ = "";

      sockport_ = 0;

      protocol_ = "";

      mountpoint_ = "";

      isSuperuser_ = false;

      anonymous_ = false;

      cn_ = "";

      dn_ = "";

      return this;
    }

    public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
      return EmqxExHookProto.internal_static_emqx_exhook_v2_ClientInfo_descriptor;
    }

    public ClientInfo getDefaultInstanceForType() {
      return ClientInfo.getDefaultInstance();
    }

    public ClientInfo build() {
      ClientInfo result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    public ClientInfo buildPartial() {
      ClientInfo result = new ClientInfo(this);
      result.node_ = node_;
      result.clientid_ = clientid_;
      result.username_ = username_;
      result.password_ = password_;
      result.peerhost_ = peerhost_;
      result.sockport_ = sockport_;
      result.protocol_ = protocol_;
      result.mountpoint_ = mountpoint_;
      result.isSuperuser_ = isSuperuser_;
      result.anonymous_ = anonymous_;
      result.cn_ = cn_;
      result.dn_ = dn_;
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
      if (other instanceof ClientInfo) {
        return mergeFrom((ClientInfo) other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(ClientInfo other) {
      if (other == ClientInfo.getDefaultInstance())
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
      if (!other.getPassword().isEmpty()) {
        password_ = other.password_;
        onChanged();
      }
      if (!other.getPeerhost().isEmpty()) {
        peerhost_ = other.peerhost_;
        onChanged();
      }
      if (other.getSockport() != 0) {
        setSockport(other.getSockport());
      }
      if (!other.getProtocol().isEmpty()) {
        protocol_ = other.protocol_;
        onChanged();
      }
      if (!other.getMountpoint().isEmpty()) {
        mountpoint_ = other.mountpoint_;
        onChanged();
      }
      if (other.getIsSuperuser() != false) {
        setIsSuperuser(other.getIsSuperuser());
      }
      if (other.getAnonymous() != false) {
        setAnonymous(other.getAnonymous());
      }
      if (!other.getCn().isEmpty()) {
        cn_ = other.cn_;
        onChanged();
      }
      if (!other.getDn().isEmpty()) {
        dn_ = other.dn_;
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
      ClientInfo parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (ClientInfo) e.getUnfinishedMessage();
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

    private Object password_ = "";

    /**
     * <code>string password = 4;</code>
     */
    public String getPassword() {
      Object ref = password_;
      if (!(ref instanceof String)) {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        password_ = s;
        return s;
      } else {
        return (String) ref;
      }
    }

    /**
     * <code>string password = 4;</code>
     */
    public com.google.protobuf.ByteString getPasswordBytes() {
      Object ref = password_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b =
            com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        password_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    /**
     * <code>string password = 4;</code>
     */
    public Builder setPassword(String value) {
      if (value == null) {
        throw new NullPointerException();
      }

      password_ = value;
      onChanged();
      return this;
    }

    /**
     * <code>string password = 4;</code>
     */
    public Builder clearPassword() {

      password_ = getDefaultInstance().getPassword();
      onChanged();
      return this;
    }

    /**
     * <code>string password = 4;</code>
     */
    public Builder setPasswordBytes(com.google.protobuf.ByteString value) {
      if (value == null) {
        throw new NullPointerException();
      }
      checkByteStringIsUtf8(value);

      password_ = value;
      onChanged();
      return this;
    }

    private Object peerhost_ = "";

    /**
     * <code>string peerhost = 5;</code>
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
     * <code>string peerhost = 5;</code>
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
     * <code>string peerhost = 5;</code>
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
     * <code>string peerhost = 5;</code>
     */
    public Builder clearPeerhost() {

      peerhost_ = getDefaultInstance().getPeerhost();
      onChanged();
      return this;
    }

    /**
     * <code>string peerhost = 5;</code>
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
     * <code>uint32 sockport = 6;</code>
     */
    public int getSockport() {
      return sockport_;
    }

    /**
     * <code>uint32 sockport = 6;</code>
     */
    public Builder setSockport(int value) {

      sockport_ = value;
      onChanged();
      return this;
    }

    /**
     * <code>uint32 sockport = 6;</code>
     */
    public Builder clearSockport() {

      sockport_ = 0;
      onChanged();
      return this;
    }

    private Object protocol_ = "";

    /**
     * <code>string protocol = 7;</code>
     */
    public String getProtocol() {
      Object ref = protocol_;
      if (!(ref instanceof String)) {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        protocol_ = s;
        return s;
      } else {
        return (String) ref;
      }
    }

    /**
     * <code>string protocol = 7;</code>
     */
    public com.google.protobuf.ByteString getProtocolBytes() {
      Object ref = protocol_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b =
            com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        protocol_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    /**
     * <code>string protocol = 7;</code>
     */
    public Builder setProtocol(String value) {
      if (value == null) {
        throw new NullPointerException();
      }

      protocol_ = value;
      onChanged();
      return this;
    }

    /**
     * <code>string protocol = 7;</code>
     */
    public Builder clearProtocol() {

      protocol_ = getDefaultInstance().getProtocol();
      onChanged();
      return this;
    }

    /**
     * <code>string protocol = 7;</code>
     */
    public Builder setProtocolBytes(com.google.protobuf.ByteString value) {
      if (value == null) {
        throw new NullPointerException();
      }
      checkByteStringIsUtf8(value);

      protocol_ = value;
      onChanged();
      return this;
    }

    private Object mountpoint_ = "";

    /**
     * <code>string mountpoint = 8;</code>
     */
    public String getMountpoint() {
      Object ref = mountpoint_;
      if (!(ref instanceof String)) {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        mountpoint_ = s;
        return s;
      } else {
        return (String) ref;
      }
    }

    /**
     * <code>string mountpoint = 8;</code>
     */
    public com.google.protobuf.ByteString getMountpointBytes() {
      Object ref = mountpoint_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b =
            com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        mountpoint_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    /**
     * <code>string mountpoint = 8;</code>
     */
    public Builder setMountpoint(String value) {
      if (value == null) {
        throw new NullPointerException();
      }

      mountpoint_ = value;
      onChanged();
      return this;
    }

    /**
     * <code>string mountpoint = 8;</code>
     */
    public Builder clearMountpoint() {

      mountpoint_ = getDefaultInstance().getMountpoint();
      onChanged();
      return this;
    }

    /**
     * <code>string mountpoint = 8;</code>
     */
    public Builder setMountpointBytes(com.google.protobuf.ByteString value) {
      if (value == null) {
        throw new NullPointerException();
      }
      checkByteStringIsUtf8(value);

      mountpoint_ = value;
      onChanged();
      return this;
    }

    private boolean isSuperuser_;

    /**
     * <code>bool is_superuser = 9;</code>
     */
    public boolean getIsSuperuser() {
      return isSuperuser_;
    }

    /**
     * <code>bool is_superuser = 9;</code>
     */
    public Builder setIsSuperuser(boolean value) {

      isSuperuser_ = value;
      onChanged();
      return this;
    }

    /**
     * <code>bool is_superuser = 9;</code>
     */
    public Builder clearIsSuperuser() {

      isSuperuser_ = false;
      onChanged();
      return this;
    }

    private boolean anonymous_;

    /**
     * <code>bool anonymous = 10;</code>
     */
    public boolean getAnonymous() {
      return anonymous_;
    }

    /**
     * <code>bool anonymous = 10;</code>
     */
    public Builder setAnonymous(boolean value) {

      anonymous_ = value;
      onChanged();
      return this;
    }

    /**
     * <code>bool anonymous = 10;</code>
     */
    public Builder clearAnonymous() {

      anonymous_ = false;
      onChanged();
      return this;
    }

    private Object cn_ = "";

    /**
     * <pre>
     * common name of client TLS cert
     * </pre>
     *
     * <code>string cn = 11;</code>
     */
    public String getCn() {
      Object ref = cn_;
      if (!(ref instanceof String)) {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        cn_ = s;
        return s;
      } else {
        return (String) ref;
      }
    }

    /**
     * <pre>
     * common name of client TLS cert
     * </pre>
     *
     * <code>string cn = 11;</code>
     */
    public com.google.protobuf.ByteString getCnBytes() {
      Object ref = cn_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b =
            com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        cn_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    /**
     * <pre>
     * common name of client TLS cert
     * </pre>
     *
     * <code>string cn = 11;</code>
     */
    public Builder setCn(String value) {
      if (value == null) {
        throw new NullPointerException();
      }

      cn_ = value;
      onChanged();
      return this;
    }

    /**
     * <pre>
     * common name of client TLS cert
     * </pre>
     *
     * <code>string cn = 11;</code>
     */
    public Builder clearCn() {

      cn_ = getDefaultInstance().getCn();
      onChanged();
      return this;
    }

    /**
     * <pre>
     * common name of client TLS cert
     * </pre>
     *
     * <code>string cn = 11;</code>
     */
    public Builder setCnBytes(com.google.protobuf.ByteString value) {
      if (value == null) {
        throw new NullPointerException();
      }
      checkByteStringIsUtf8(value);

      cn_ = value;
      onChanged();
      return this;
    }

    private Object dn_ = "";

    /**
     * <pre>
     * subject of client TLS cert
     * </pre>
     *
     * <code>string dn = 12;</code>
     */
    public String getDn() {
      Object ref = dn_;
      if (!(ref instanceof String)) {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        dn_ = s;
        return s;
      } else {
        return (String) ref;
      }
    }

    /**
     * <pre>
     * subject of client TLS cert
     * </pre>
     *
     * <code>string dn = 12;</code>
     */
    public com.google.protobuf.ByteString getDnBytes() {
      Object ref = dn_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b =
            com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        dn_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    /**
     * <pre>
     * subject of client TLS cert
     * </pre>
     *
     * <code>string dn = 12;</code>
     */
    public Builder setDn(String value) {
      if (value == null) {
        throw new NullPointerException();
      }

      dn_ = value;
      onChanged();
      return this;
    }

    /**
     * <pre>
     * subject of client TLS cert
     * </pre>
     *
     * <code>string dn = 12;</code>
     */
    public Builder clearDn() {

      dn_ = getDefaultInstance().getDn();
      onChanged();
      return this;
    }

    /**
     * <pre>
     * subject of client TLS cert
     * </pre>
     *
     * <code>string dn = 12;</code>
     */
    public Builder setDnBytes(com.google.protobuf.ByteString value) {
      if (value == null) {
        throw new NullPointerException();
      }
      checkByteStringIsUtf8(value);

      dn_ = value;
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


    // @@protoc_insertion_point(builder_scope:emqx.exhook.v2.ClientInfo)
  }

  // @@protoc_insertion_point(class_scope:emqx.exhook.v2.ClientInfo)
  private static final ClientInfo DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new ClientInfo();
  }

  public static ClientInfo getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<ClientInfo> PARSER =
      new com.google.protobuf.AbstractParser<ClientInfo>() {
        public ClientInfo parsePartialFrom(com.google.protobuf.CodedInputStream input,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
          return new ClientInfo(input, extensionRegistry);
        }
      };

  public static com.google.protobuf.Parser<ClientInfo> parser() {
    return PARSER;
  }

  @Override
  public com.google.protobuf.Parser<ClientInfo> getParserForType() {
    return PARSER;
  }

  public ClientInfo getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

