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
 * Protobuf type {@code emqx.exhook.v2.SubOpts}
 */
public final class SubOpts extends com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:emqx.exhook.v2.SubOpts)
    SubOptsOrBuilder {
  private static final long serialVersionUID = 0L;

  // Use SubOpts.newBuilder() to construct.
  private SubOpts(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }

  private SubOpts() {
    qos_ = 0;
    share_ = "";
    rh_ = 0;
    rap_ = 0;
    nl_ = 0;
  }

  @Override
  public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
    return this.unknownFields;
  }

  private SubOpts(com.google.protobuf.CodedInputStream input,
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
          case 8: {

            qos_ = input.readUInt32();
            break;
          }
          case 18: {
            String s = input.readStringRequireUtf8();

            share_ = s;
            break;
          }
          case 24: {

            rh_ = input.readUInt32();
            break;
          }
          case 32: {

            rap_ = input.readUInt32();
            break;
          }
          case 40: {

            nl_ = input.readUInt32();
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
    return EmqxExHookProto.internal_static_emqx_exhook_v2_SubOpts_descriptor;
  }

  protected FieldAccessorTable internalGetFieldAccessorTable() {
    return EmqxExHookProto.internal_static_emqx_exhook_v2_SubOpts_fieldAccessorTable
        .ensureFieldAccessorsInitialized(SubOpts.class, Builder.class);
  }

  public static final int QOS_FIELD_NUMBER = 1;
  private int qos_;

  /**
   * <pre>
   * The QoS level
   * </pre>
   *
   * <code>uint32 qos = 1;</code>
   */
  public int getQos() {
    return qos_;
  }

  public static final int SHARE_FIELD_NUMBER = 2;
  private volatile Object share_;

  /**
   * <pre>
   * The group name for shared subscription
   * </pre>
   *
   * <code>string share = 2;</code>
   */
  public String getShare() {
    Object ref = share_;
    if (ref instanceof String) {
      return (String) ref;
    } else {
      com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
      String s = bs.toStringUtf8();
      share_ = s;
      return s;
    }
  }

  /**
   * <pre>
   * The group name for shared subscription
   * </pre>
   *
   * <code>string share = 2;</code>
   */
  public com.google.protobuf.ByteString getShareBytes() {
    Object ref = share_;
    if (ref instanceof String) {
      com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
      share_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  public static final int RH_FIELD_NUMBER = 3;
  private int rh_;

  /**
   * <pre>
   * The Retain Handling option (MQTT v5.0)
   *  0 = Send retained messages at the time of the subscribe
   *  1 = Send retained messages at subscribe only if the subscription does
   *       not currently exist
   *  2 = Do not send retained messages at the time of the subscribe
   * </pre>
   *
   * <code>uint32 rh = 3;</code>
   */
  public int getRh() {
    return rh_;
  }

  public static final int RAP_FIELD_NUMBER = 4;
  private int rap_;

  /**
   * <pre>
   * The Retain as Published option (MQTT v5.0)
   *  If 1, Application Messages forwarded using this subscription keep the
   *        RETAIN flag they were published with.
   *  If 0, Application Messages forwarded using this subscription have the
   *        RETAIN flag set to 0.
   * Retained messages sent when the subscription is established have the RETAIN flag set to 1.
   * </pre>
   *
   * <code>uint32 rap = 4;</code>
   */
  public int getRap() {
    return rap_;
  }

  public static final int NL_FIELD_NUMBER = 5;
  private int nl_;

  /**
   * <pre>
   * The No Local option (MQTT v5.0)
   * If the value is 1, Application Messages MUST NOT be forwarded to a
   * connection with a ClientID equal to the ClientID of the publishing
   * </pre>
   *
   * <code>uint32 nl = 5;</code>
   */
  public int getNl() {
    return nl_;
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
    if (qos_ != 0) {
      output.writeUInt32(1, qos_);
    }
    if (!getShareBytes().isEmpty()) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 2, share_);
    }
    if (rh_ != 0) {
      output.writeUInt32(3, rh_);
    }
    if (rap_ != 0) {
      output.writeUInt32(4, rap_);
    }
    if (nl_ != 0) {
      output.writeUInt32(5, nl_);
    }
    unknownFields.writeTo(output);
  }

  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1)
      return size;

    size = 0;
    if (qos_ != 0) {
      size += com.google.protobuf.CodedOutputStream.computeUInt32Size(1, qos_);
    }
    if (!getShareBytes().isEmpty()) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(2, share_);
    }
    if (rh_ != 0) {
      size += com.google.protobuf.CodedOutputStream.computeUInt32Size(3, rh_);
    }
    if (rap_ != 0) {
      size += com.google.protobuf.CodedOutputStream.computeUInt32Size(4, rap_);
    }
    if (nl_ != 0) {
      size += com.google.protobuf.CodedOutputStream.computeUInt32Size(5, nl_);
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
    if (!(obj instanceof SubOpts)) {
      return super.equals(obj);
    }
    SubOpts other = (SubOpts) obj;

    boolean result = true;
    result = result && (getQos() == other.getQos());
    result = result && getShare().equals(other.getShare());
    result = result && (getRh() == other.getRh());
    result = result && (getRap() == other.getRap());
    result = result && (getNl() == other.getNl());
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
    hash = (37 * hash) + QOS_FIELD_NUMBER;
    hash = (53 * hash) + getQos();
    hash = (37 * hash) + SHARE_FIELD_NUMBER;
    hash = (53 * hash) + getShare().hashCode();
    hash = (37 * hash) + RH_FIELD_NUMBER;
    hash = (53 * hash) + getRh();
    hash = (37 * hash) + RAP_FIELD_NUMBER;
    hash = (53 * hash) + getRap();
    hash = (37 * hash) + NL_FIELD_NUMBER;
    hash = (53 * hash) + getNl();
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static SubOpts parseFrom(java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static SubOpts parseFrom(java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static SubOpts parseFrom(com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static SubOpts parseFrom(com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static SubOpts parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static SubOpts parseFrom(byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static SubOpts parseFrom(java.io.InputStream input) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }

  public static SubOpts parseFrom(java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input,
        extensionRegistry);
  }

  public static SubOpts parseDelimitedFrom(java.io.InputStream input) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
  }

  public static SubOpts parseDelimitedFrom(java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input,
        extensionRegistry);
  }

  public static SubOpts parseFrom(com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }

  public static SubOpts parseFrom(com.google.protobuf.CodedInputStream input,
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

  public static Builder newBuilder(SubOpts prototype) {
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
   * Protobuf type {@code emqx.exhook.v2.SubOpts}
   */
  public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder>
      implements
      // @@protoc_insertion_point(builder_implements:emqx.exhook.v2.SubOpts)
      SubOptsOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return EmqxExHookProto.internal_static_emqx_exhook_v2_SubOpts_descriptor;
    }

    protected FieldAccessorTable internalGetFieldAccessorTable() {
      return EmqxExHookProto.internal_static_emqx_exhook_v2_SubOpts_fieldAccessorTable
          .ensureFieldAccessorsInitialized(SubOpts.class, Builder.class);
    }

    // Construct using top.rslly.iot.utility.exhook.SubOpts.newBuilder()
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
      qos_ = 0;

      share_ = "";

      rh_ = 0;

      rap_ = 0;

      nl_ = 0;

      return this;
    }

    public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
      return EmqxExHookProto.internal_static_emqx_exhook_v2_SubOpts_descriptor;
    }

    public SubOpts getDefaultInstanceForType() {
      return SubOpts.getDefaultInstance();
    }

    public SubOpts build() {
      SubOpts result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    public SubOpts buildPartial() {
      SubOpts result = new SubOpts(this);
      result.qos_ = qos_;
      result.share_ = share_;
      result.rh_ = rh_;
      result.rap_ = rap_;
      result.nl_ = nl_;
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
      if (other instanceof SubOpts) {
        return mergeFrom((SubOpts) other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(SubOpts other) {
      if (other == SubOpts.getDefaultInstance())
        return this;
      if (other.getQos() != 0) {
        setQos(other.getQos());
      }
      if (!other.getShare().isEmpty()) {
        share_ = other.share_;
        onChanged();
      }
      if (other.getRh() != 0) {
        setRh(other.getRh());
      }
      if (other.getRap() != 0) {
        setRap(other.getRap());
      }
      if (other.getNl() != 0) {
        setNl(other.getNl());
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
      SubOpts parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (SubOpts) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private int qos_;

    /**
     * <pre>
     * The QoS level
     * </pre>
     *
     * <code>uint32 qos = 1;</code>
     */
    public int getQos() {
      return qos_;
    }

    /**
     * <pre>
     * The QoS level
     * </pre>
     *
     * <code>uint32 qos = 1;</code>
     */
    public Builder setQos(int value) {

      qos_ = value;
      onChanged();
      return this;
    }

    /**
     * <pre>
     * The QoS level
     * </pre>
     *
     * <code>uint32 qos = 1;</code>
     */
    public Builder clearQos() {

      qos_ = 0;
      onChanged();
      return this;
    }

    private Object share_ = "";

    /**
     * <pre>
     * The group name for shared subscription
     * </pre>
     *
     * <code>string share = 2;</code>
     */
    public String getShare() {
      Object ref = share_;
      if (!(ref instanceof String)) {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        share_ = s;
        return s;
      } else {
        return (String) ref;
      }
    }

    /**
     * <pre>
     * The group name for shared subscription
     * </pre>
     *
     * <code>string share = 2;</code>
     */
    public com.google.protobuf.ByteString getShareBytes() {
      Object ref = share_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b =
            com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        share_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    /**
     * <pre>
     * The group name for shared subscription
     * </pre>
     *
     * <code>string share = 2;</code>
     */
    public Builder setShare(String value) {
      if (value == null) {
        throw new NullPointerException();
      }

      share_ = value;
      onChanged();
      return this;
    }

    /**
     * <pre>
     * The group name for shared subscription
     * </pre>
     *
     * <code>string share = 2;</code>
     */
    public Builder clearShare() {

      share_ = getDefaultInstance().getShare();
      onChanged();
      return this;
    }

    /**
     * <pre>
     * The group name for shared subscription
     * </pre>
     *
     * <code>string share = 2;</code>
     */
    public Builder setShareBytes(com.google.protobuf.ByteString value) {
      if (value == null) {
        throw new NullPointerException();
      }
      checkByteStringIsUtf8(value);

      share_ = value;
      onChanged();
      return this;
    }

    private int rh_;

    /**
     * <pre>
     * The Retain Handling option (MQTT v5.0)
     *  0 = Send retained messages at the time of the subscribe
     *  1 = Send retained messages at subscribe only if the subscription does
     *       not currently exist
     *  2 = Do not send retained messages at the time of the subscribe
     * </pre>
     *
     * <code>uint32 rh = 3;</code>
     */
    public int getRh() {
      return rh_;
    }

    /**
     * <pre>
     * The Retain Handling option (MQTT v5.0)
     *  0 = Send retained messages at the time of the subscribe
     *  1 = Send retained messages at subscribe only if the subscription does
     *       not currently exist
     *  2 = Do not send retained messages at the time of the subscribe
     * </pre>
     *
     * <code>uint32 rh = 3;</code>
     */
    public Builder setRh(int value) {

      rh_ = value;
      onChanged();
      return this;
    }

    /**
     * <pre>
     * The Retain Handling option (MQTT v5.0)
     *  0 = Send retained messages at the time of the subscribe
     *  1 = Send retained messages at subscribe only if the subscription does
     *       not currently exist
     *  2 = Do not send retained messages at the time of the subscribe
     * </pre>
     *
     * <code>uint32 rh = 3;</code>
     */
    public Builder clearRh() {

      rh_ = 0;
      onChanged();
      return this;
    }

    private int rap_;

    /**
     * <pre>
     * The Retain as Published option (MQTT v5.0)
     *  If 1, Application Messages forwarded using this subscription keep the
     *        RETAIN flag they were published with.
     *  If 0, Application Messages forwarded using this subscription have the
     *        RETAIN flag set to 0.
     * Retained messages sent when the subscription is established have the RETAIN flag set to 1.
     * </pre>
     *
     * <code>uint32 rap = 4;</code>
     */
    public int getRap() {
      return rap_;
    }

    /**
     * <pre>
     * The Retain as Published option (MQTT v5.0)
     *  If 1, Application Messages forwarded using this subscription keep the
     *        RETAIN flag they were published with.
     *  If 0, Application Messages forwarded using this subscription have the
     *        RETAIN flag set to 0.
     * Retained messages sent when the subscription is established have the RETAIN flag set to 1.
     * </pre>
     *
     * <code>uint32 rap = 4;</code>
     */
    public Builder setRap(int value) {

      rap_ = value;
      onChanged();
      return this;
    }

    /**
     * <pre>
     * The Retain as Published option (MQTT v5.0)
     *  If 1, Application Messages forwarded using this subscription keep the
     *        RETAIN flag they were published with.
     *  If 0, Application Messages forwarded using this subscription have the
     *        RETAIN flag set to 0.
     * Retained messages sent when the subscription is established have the RETAIN flag set to 1.
     * </pre>
     *
     * <code>uint32 rap = 4;</code>
     */
    public Builder clearRap() {

      rap_ = 0;
      onChanged();
      return this;
    }

    private int nl_;

    /**
     * <pre>
     * The No Local option (MQTT v5.0)
     * If the value is 1, Application Messages MUST NOT be forwarded to a
     * connection with a ClientID equal to the ClientID of the publishing
     * </pre>
     *
     * <code>uint32 nl = 5;</code>
     */
    public int getNl() {
      return nl_;
    }

    /**
     * <pre>
     * The No Local option (MQTT v5.0)
     * If the value is 1, Application Messages MUST NOT be forwarded to a
     * connection with a ClientID equal to the ClientID of the publishing
     * </pre>
     *
     * <code>uint32 nl = 5;</code>
     */
    public Builder setNl(int value) {

      nl_ = value;
      onChanged();
      return this;
    }

    /**
     * <pre>
     * The No Local option (MQTT v5.0)
     * If the value is 1, Application Messages MUST NOT be forwarded to a
     * connection with a ClientID equal to the ClientID of the publishing
     * </pre>
     *
     * <code>uint32 nl = 5;</code>
     */
    public Builder clearNl() {

      nl_ = 0;
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


    // @@protoc_insertion_point(builder_scope:emqx.exhook.v2.SubOpts)
  }

  // @@protoc_insertion_point(class_scope:emqx.exhook.v2.SubOpts)
  private static final SubOpts DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new SubOpts();
  }

  public static SubOpts getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<SubOpts> PARSER =
      new com.google.protobuf.AbstractParser<SubOpts>() {
        public SubOpts parsePartialFrom(com.google.protobuf.CodedInputStream input,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
          return new SubOpts(input, extensionRegistry);
        }
      };

  public static com.google.protobuf.Parser<SubOpts> parser() {
    return PARSER;
  }

  @Override
  public com.google.protobuf.Parser<SubOpts> getParserForType() {
    return PARSER;
  }

  public SubOpts getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

