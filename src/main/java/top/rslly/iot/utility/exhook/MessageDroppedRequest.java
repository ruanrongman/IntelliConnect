// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: exhook.proto

package top.rslly.iot.utility.exhook;

/**
 * Protobuf type {@code emqx.exhook.v2.MessageDroppedRequest}
 */
public  final class MessageDroppedRequest extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:emqx.exhook.v2.MessageDroppedRequest)
    MessageDroppedRequestOrBuilder {
private static final long serialVersionUID = 0L;
  // Use MessageDroppedRequest.newBuilder() to construct.
  private MessageDroppedRequest(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private MessageDroppedRequest() {
    reason_ = "";
  }

  @Override
  public final com.google.protobuf.UnknownFieldSet
  getUnknownFields() {
    return this.unknownFields;
  }
  private MessageDroppedRequest(
      com.google.protobuf.CodedInputStream input,
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
            if (!parseUnknownFieldProto3(
                input, unknownFields, extensionRegistry, tag)) {
              done = true;
            }
            break;
          }
          case 10: {
            Message.Builder subBuilder = null;
            if (message_ != null) {
              subBuilder = message_.toBuilder();
            }
            message_ = input.readMessage(Message.parser(), extensionRegistry);
            if (subBuilder != null) {
              subBuilder.mergeFrom(message_);
              message_ = subBuilder.buildPartial();
            }

            break;
          }
          case 18: {
            String s = input.readStringRequireUtf8();

            reason_ = s;
            break;
          }
          case 26: {
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
      throw new com.google.protobuf.InvalidProtocolBufferException(
          e).setUnfinishedMessage(this);
    } finally {
      this.unknownFields = unknownFields.build();
      makeExtensionsImmutable();
    }
  }
  public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
    return EmqxExHookProto.internal_static_emqx_exhook_v2_MessageDroppedRequest_descriptor;
  }

  protected FieldAccessorTable
      internalGetFieldAccessorTable() {
    return EmqxExHookProto.internal_static_emqx_exhook_v2_MessageDroppedRequest_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            MessageDroppedRequest.class, Builder.class);
  }

  public static final int MESSAGE_FIELD_NUMBER = 1;
  private Message message_;
  /**
   * <code>.emqx.exhook.v2.Message message = 1;</code>
   */
  public boolean hasMessage() {
    return message_ != null;
  }
  /**
   * <code>.emqx.exhook.v2.Message message = 1;</code>
   */
  public Message getMessage() {
    return message_ == null ? Message.getDefaultInstance() : message_;
  }
  /**
   * <code>.emqx.exhook.v2.Message message = 1;</code>
   */
  public MessageOrBuilder getMessageOrBuilder() {
    return getMessage();
  }

  public static final int REASON_FIELD_NUMBER = 2;
  private volatile Object reason_;
  /**
   * <code>string reason = 2;</code>
   */
  public String getReason() {
    Object ref = reason_;
    if (ref instanceof String) {
      return (String) ref;
    } else {
      com.google.protobuf.ByteString bs =
          (com.google.protobuf.ByteString) ref;
      String s = bs.toStringUtf8();
      reason_ = s;
      return s;
    }
  }
  /**
   * <code>string reason = 2;</code>
   */
  public com.google.protobuf.ByteString
      getReasonBytes() {
    Object ref = reason_;
    if (ref instanceof String) {
      com.google.protobuf.ByteString b =
          com.google.protobuf.ByteString.copyFromUtf8(
              (String) ref);
      reason_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  public static final int META_FIELD_NUMBER = 3;
  private RequestMeta meta_;
  /**
   * <code>.emqx.exhook.v2.RequestMeta meta = 3;</code>
   */
  public boolean hasMeta() {
    return meta_ != null;
  }
  /**
   * <code>.emqx.exhook.v2.RequestMeta meta = 3;</code>
   */
  public RequestMeta getMeta() {
    return meta_ == null ? RequestMeta.getDefaultInstance() : meta_;
  }
  /**
   * <code>.emqx.exhook.v2.RequestMeta meta = 3;</code>
   */
  public RequestMetaOrBuilder getMetaOrBuilder() {
    return getMeta();
  }

  private byte memoizedIsInitialized = -1;
  public final boolean isInitialized() {
    byte isInitialized = memoizedIsInitialized;
    if (isInitialized == 1) return true;
    if (isInitialized == 0) return false;

    memoizedIsInitialized = 1;
    return true;
  }

  public void writeTo(com.google.protobuf.CodedOutputStream output)
                      throws java.io.IOException {
    if (message_ != null) {
      output.writeMessage(1, getMessage());
    }
    if (!getReasonBytes().isEmpty()) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 2, reason_);
    }
    if (meta_ != null) {
      output.writeMessage(3, getMeta());
    }
    unknownFields.writeTo(output);
  }

  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (message_ != null) {
      size += com.google.protobuf.CodedOutputStream
        .computeMessageSize(1, getMessage());
    }
    if (!getReasonBytes().isEmpty()) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(2, reason_);
    }
    if (meta_ != null) {
      size += com.google.protobuf.CodedOutputStream
        .computeMessageSize(3, getMeta());
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
    if (!(obj instanceof MessageDroppedRequest)) {
      return super.equals(obj);
    }
    MessageDroppedRequest other = (MessageDroppedRequest) obj;

    boolean result = true;
    result = result && (hasMessage() == other.hasMessage());
    if (hasMessage()) {
      result = result && getMessage()
          .equals(other.getMessage());
    }
    result = result && getReason()
        .equals(other.getReason());
    result = result && (hasMeta() == other.hasMeta());
    if (hasMeta()) {
      result = result && getMeta()
          .equals(other.getMeta());
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
    if (hasMessage()) {
      hash = (37 * hash) + MESSAGE_FIELD_NUMBER;
      hash = (53 * hash) + getMessage().hashCode();
    }
    hash = (37 * hash) + REASON_FIELD_NUMBER;
    hash = (53 * hash) + getReason().hashCode();
    if (hasMeta()) {
      hash = (37 * hash) + META_FIELD_NUMBER;
      hash = (53 * hash) + getMeta().hashCode();
    }
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static MessageDroppedRequest parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static MessageDroppedRequest parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static MessageDroppedRequest parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static MessageDroppedRequest parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static MessageDroppedRequest parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static MessageDroppedRequest parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static MessageDroppedRequest parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static MessageDroppedRequest parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }
  public static MessageDroppedRequest parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
  }
  public static MessageDroppedRequest parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static MessageDroppedRequest parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static MessageDroppedRequest parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }

  public Builder newBuilderForType() { return newBuilder(); }
  public static Builder newBuilder() {
    return DEFAULT_INSTANCE.toBuilder();
  }
  public static Builder newBuilder(MessageDroppedRequest prototype) {
    return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
  }
  public Builder toBuilder() {
    return this == DEFAULT_INSTANCE
        ? new Builder() : new Builder().mergeFrom(this);
  }

  @Override
  protected Builder newBuilderForType(
      BuilderParent parent) {
    Builder builder = new Builder(parent);
    return builder;
  }
  /**
   * Protobuf type {@code emqx.exhook.v2.MessageDroppedRequest}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:emqx.exhook.v2.MessageDroppedRequest)
      MessageDroppedRequestOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return EmqxExHookProto.internal_static_emqx_exhook_v2_MessageDroppedRequest_descriptor;
    }

    protected FieldAccessorTable
        internalGetFieldAccessorTable() {
      return EmqxExHookProto.internal_static_emqx_exhook_v2_MessageDroppedRequest_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              MessageDroppedRequest.class, Builder.class);
    }

    // Construct using top.rslly.iot.utility.exhook.MessageDroppedRequest.newBuilder()
    private Builder() {
      maybeForceBuilderInitialization();
    }

    private Builder(
        BuilderParent parent) {
      super(parent);
      maybeForceBuilderInitialization();
    }
    private void maybeForceBuilderInitialization() {
      if (com.google.protobuf.GeneratedMessageV3
              .alwaysUseFieldBuilders) {
      }
    }
    public Builder clear() {
      super.clear();
      if (messageBuilder_ == null) {
        message_ = null;
      } else {
        message_ = null;
        messageBuilder_ = null;
      }
      reason_ = "";

      if (metaBuilder_ == null) {
        meta_ = null;
      } else {
        meta_ = null;
        metaBuilder_ = null;
      }
      return this;
    }

    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return EmqxExHookProto.internal_static_emqx_exhook_v2_MessageDroppedRequest_descriptor;
    }

    public MessageDroppedRequest getDefaultInstanceForType() {
      return MessageDroppedRequest.getDefaultInstance();
    }

    public MessageDroppedRequest build() {
      MessageDroppedRequest result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    public MessageDroppedRequest buildPartial() {
      MessageDroppedRequest result = new MessageDroppedRequest(this);
      if (messageBuilder_ == null) {
        result.message_ = message_;
      } else {
        result.message_ = messageBuilder_.build();
      }
      result.reason_ = reason_;
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
    public Builder setField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        Object value) {
      return (Builder) super.setField(field, value);
    }
    public Builder clearField(
        com.google.protobuf.Descriptors.FieldDescriptor field) {
      return (Builder) super.clearField(field);
    }
    public Builder clearOneof(
        com.google.protobuf.Descriptors.OneofDescriptor oneof) {
      return (Builder) super.clearOneof(oneof);
    }
    public Builder setRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        int index, Object value) {
      return (Builder) super.setRepeatedField(field, index, value);
    }
    public Builder addRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        Object value) {
      return (Builder) super.addRepeatedField(field, value);
    }
    public Builder mergeFrom(com.google.protobuf.Message other) {
      if (other instanceof MessageDroppedRequest) {
        return mergeFrom((MessageDroppedRequest)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(MessageDroppedRequest other) {
      if (other == MessageDroppedRequest.getDefaultInstance()) return this;
      if (other.hasMessage()) {
        mergeMessage(other.getMessage());
      }
      if (!other.getReason().isEmpty()) {
        reason_ = other.reason_;
        onChanged();
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

    public Builder mergeFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      MessageDroppedRequest parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (MessageDroppedRequest) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private Message message_ = null;
    private com.google.protobuf.SingleFieldBuilderV3<
        Message, Message.Builder, MessageOrBuilder> messageBuilder_;
    /**
     * <code>.emqx.exhook.v2.Message message = 1;</code>
     */
    public boolean hasMessage() {
      return messageBuilder_ != null || message_ != null;
    }
    /**
     * <code>.emqx.exhook.v2.Message message = 1;</code>
     */
    public Message getMessage() {
      if (messageBuilder_ == null) {
        return message_ == null ? Message.getDefaultInstance() : message_;
      } else {
        return messageBuilder_.getMessage();
      }
    }
    /**
     * <code>.emqx.exhook.v2.Message message = 1;</code>
     */
    public Builder setMessage(Message value) {
      if (messageBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        message_ = value;
        onChanged();
      } else {
        messageBuilder_.setMessage(value);
      }

      return this;
    }
    /**
     * <code>.emqx.exhook.v2.Message message = 1;</code>
     */
    public Builder setMessage(
        Message.Builder builderForValue) {
      if (messageBuilder_ == null) {
        message_ = builderForValue.build();
        onChanged();
      } else {
        messageBuilder_.setMessage(builderForValue.build());
      }

      return this;
    }
    /**
     * <code>.emqx.exhook.v2.Message message = 1;</code>
     */
    public Builder mergeMessage(Message value) {
      if (messageBuilder_ == null) {
        if (message_ != null) {
          message_ =
            Message.newBuilder(message_).mergeFrom(value).buildPartial();
        } else {
          message_ = value;
        }
        onChanged();
      } else {
        messageBuilder_.mergeFrom(value);
      }

      return this;
    }
    /**
     * <code>.emqx.exhook.v2.Message message = 1;</code>
     */
    public Builder clearMessage() {
      if (messageBuilder_ == null) {
        message_ = null;
        onChanged();
      } else {
        message_ = null;
        messageBuilder_ = null;
      }

      return this;
    }
    /**
     * <code>.emqx.exhook.v2.Message message = 1;</code>
     */
    public Message.Builder getMessageBuilder() {

      onChanged();
      return getMessageFieldBuilder().getBuilder();
    }
    /**
     * <code>.emqx.exhook.v2.Message message = 1;</code>
     */
    public MessageOrBuilder getMessageOrBuilder() {
      if (messageBuilder_ != null) {
        return messageBuilder_.getMessageOrBuilder();
      } else {
        return message_ == null ?
            Message.getDefaultInstance() : message_;
      }
    }
    /**
     * <code>.emqx.exhook.v2.Message message = 1;</code>
     */
    private com.google.protobuf.SingleFieldBuilderV3<
        Message, Message.Builder, MessageOrBuilder>
        getMessageFieldBuilder() {
      if (messageBuilder_ == null) {
        messageBuilder_ = new com.google.protobuf.SingleFieldBuilderV3<
            Message, Message.Builder, MessageOrBuilder>(
                getMessage(),
                getParentForChildren(),
                isClean());
        message_ = null;
      }
      return messageBuilder_;
    }

    private Object reason_ = "";
    /**
     * <code>string reason = 2;</code>
     */
    public String getReason() {
      Object ref = reason_;
      if (!(ref instanceof String)) {
        com.google.protobuf.ByteString bs =
            (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        reason_ = s;
        return s;
      } else {
        return (String) ref;
      }
    }
    /**
     * <code>string reason = 2;</code>
     */
    public com.google.protobuf.ByteString
        getReasonBytes() {
      Object ref = reason_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b =
            com.google.protobuf.ByteString.copyFromUtf8(
                (String) ref);
        reason_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }
    /**
     * <code>string reason = 2;</code>
     */
    public Builder setReason(
        String value) {
      if (value == null) {
    throw new NullPointerException();
  }

      reason_ = value;
      onChanged();
      return this;
    }
    /**
     * <code>string reason = 2;</code>
     */
    public Builder clearReason() {

      reason_ = getDefaultInstance().getReason();
      onChanged();
      return this;
    }
    /**
     * <code>string reason = 2;</code>
     */
    public Builder setReasonBytes(
        com.google.protobuf.ByteString value) {
      if (value == null) {
    throw new NullPointerException();
  }
  checkByteStringIsUtf8(value);

      reason_ = value;
      onChanged();
      return this;
    }

    private RequestMeta meta_ = null;
    private com.google.protobuf.SingleFieldBuilderV3<
        RequestMeta, RequestMeta.Builder, RequestMetaOrBuilder> metaBuilder_;
    /**
     * <code>.emqx.exhook.v2.RequestMeta meta = 3;</code>
     */
    public boolean hasMeta() {
      return metaBuilder_ != null || meta_ != null;
    }
    /**
     * <code>.emqx.exhook.v2.RequestMeta meta = 3;</code>
     */
    public RequestMeta getMeta() {
      if (metaBuilder_ == null) {
        return meta_ == null ? RequestMeta.getDefaultInstance() : meta_;
      } else {
        return metaBuilder_.getMessage();
      }
    }
    /**
     * <code>.emqx.exhook.v2.RequestMeta meta = 3;</code>
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
     * <code>.emqx.exhook.v2.RequestMeta meta = 3;</code>
     */
    public Builder setMeta(
        RequestMeta.Builder builderForValue) {
      if (metaBuilder_ == null) {
        meta_ = builderForValue.build();
        onChanged();
      } else {
        metaBuilder_.setMessage(builderForValue.build());
      }

      return this;
    }
    /**
     * <code>.emqx.exhook.v2.RequestMeta meta = 3;</code>
     */
    public Builder mergeMeta(RequestMeta value) {
      if (metaBuilder_ == null) {
        if (meta_ != null) {
          meta_ =
            RequestMeta.newBuilder(meta_).mergeFrom(value).buildPartial();
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
     * <code>.emqx.exhook.v2.RequestMeta meta = 3;</code>
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
     * <code>.emqx.exhook.v2.RequestMeta meta = 3;</code>
     */
    public RequestMeta.Builder getMetaBuilder() {

      onChanged();
      return getMetaFieldBuilder().getBuilder();
    }
    /**
     * <code>.emqx.exhook.v2.RequestMeta meta = 3;</code>
     */
    public RequestMetaOrBuilder getMetaOrBuilder() {
      if (metaBuilder_ != null) {
        return metaBuilder_.getMessageOrBuilder();
      } else {
        return meta_ == null ?
            RequestMeta.getDefaultInstance() : meta_;
      }
    }
    /**
     * <code>.emqx.exhook.v2.RequestMeta meta = 3;</code>
     */
    private com.google.protobuf.SingleFieldBuilderV3<
        RequestMeta, RequestMeta.Builder, RequestMetaOrBuilder>
        getMetaFieldBuilder() {
      if (metaBuilder_ == null) {
        metaBuilder_ = new com.google.protobuf.SingleFieldBuilderV3<
            RequestMeta, RequestMeta.Builder, RequestMetaOrBuilder>(
                getMeta(),
                getParentForChildren(),
                isClean());
        meta_ = null;
      }
      return metaBuilder_;
    }
    public final Builder setUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.setUnknownFieldsProto3(unknownFields);
    }

    public final Builder mergeUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.mergeUnknownFields(unknownFields);
    }


    // @@protoc_insertion_point(builder_scope:emqx.exhook.v2.MessageDroppedRequest)
  }

  // @@protoc_insertion_point(class_scope:emqx.exhook.v2.MessageDroppedRequest)
  private static final MessageDroppedRequest DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new MessageDroppedRequest();
  }

  public static MessageDroppedRequest getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<MessageDroppedRequest>
      PARSER = new com.google.protobuf.AbstractParser<MessageDroppedRequest>() {
    public MessageDroppedRequest parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return new MessageDroppedRequest(input, extensionRegistry);
    }
  };

  public static com.google.protobuf.Parser<MessageDroppedRequest> parser() {
    return PARSER;
  }

  @Override
  public com.google.protobuf.Parser<MessageDroppedRequest> getParserForType() {
    return PARSER;
  }

  public MessageDroppedRequest getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

