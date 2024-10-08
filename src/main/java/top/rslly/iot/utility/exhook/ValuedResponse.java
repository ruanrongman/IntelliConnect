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
 * Protobuf type {@code emqx.exhook.v2.ValuedResponse}
 */
public final class ValuedResponse extends com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:emqx.exhook.v2.ValuedResponse)
    ValuedResponseOrBuilder {
  private static final long serialVersionUID = 0L;

  // Use ValuedResponse.newBuilder() to construct.
  private ValuedResponse(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }

  private ValuedResponse() {
    type_ = 0;
  }

  @Override
  public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
    return this.unknownFields;
  }

  private ValuedResponse(com.google.protobuf.CodedInputStream input,
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
            int rawValue = input.readEnum();

            type_ = rawValue;
            break;
          }
          case 24: {
            valueCase_ = 3;
            value_ = input.readBool();
            break;
          }
          case 34: {
            Message.Builder subBuilder = null;
            if (valueCase_ == 4) {
              subBuilder = ((Message) value_).toBuilder();
            }
            value_ = input.readMessage(Message.parser(), extensionRegistry);
            if (subBuilder != null) {
              subBuilder.mergeFrom((Message) value_);
              value_ = subBuilder.buildPartial();
            }
            valueCase_ = 4;
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
    return EmqxExHookProto.internal_static_emqx_exhook_v2_ValuedResponse_descriptor;
  }

  protected FieldAccessorTable internalGetFieldAccessorTable() {
    return EmqxExHookProto.internal_static_emqx_exhook_v2_ValuedResponse_fieldAccessorTable
        .ensureFieldAccessorsInitialized(ValuedResponse.class, Builder.class);
  }

  /**
   * <pre>
   * The responded value type
   *  - contiune: Use the responded value and execute the next hook
   *  - ignore: Ignore the responded value
   *  - stop_and_return: Use the responded value and stop the chain executing
   * </pre>
   *
   * Protobuf enum {@code emqx.exhook.v2.ValuedResponse.ResponsedType}
   */
  public enum ResponsedType implements com.google.protobuf.ProtocolMessageEnum {
    /**
     * <code>CONTINUE = 0;</code>
     */
    CONTINUE(0),
    /**
     * <code>IGNORE = 1;</code>
     */
    IGNORE(1),
    /**
     * <code>STOP_AND_RETURN = 2;</code>
     */
    STOP_AND_RETURN(2), UNRECOGNIZED(-1),;

    /**
     * <code>CONTINUE = 0;</code>
     */
    public static final int CONTINUE_VALUE = 0;
    /**
     * <code>IGNORE = 1;</code>
     */
    public static final int IGNORE_VALUE = 1;
    /**
     * <code>STOP_AND_RETURN = 2;</code>
     */
    public static final int STOP_AND_RETURN_VALUE = 2;


    public final int getNumber() {
      if (this == UNRECOGNIZED) {
        throw new IllegalArgumentException("Can't get the number of an unknown enum value.");
      }
      return value;
    }

    /**
     * @deprecated Use {@link #forNumber(int)} instead.
     */
    @Deprecated
    public static ResponsedType valueOf(int value) {
      return forNumber(value);
    }

    public static ResponsedType forNumber(int value) {
      switch (value) {
        case 0:
          return CONTINUE;
        case 1:
          return IGNORE;
        case 2:
          return STOP_AND_RETURN;
        default:
          return null;
      }
    }

    public static com.google.protobuf.Internal.EnumLiteMap<ResponsedType> internalGetValueMap() {
      return internalValueMap;
    }

    private static final com.google.protobuf.Internal.EnumLiteMap<ResponsedType> internalValueMap =
        new com.google.protobuf.Internal.EnumLiteMap<ResponsedType>() {
          public ResponsedType findValueByNumber(int number) {
            return ResponsedType.forNumber(number);
          }
        };

    public final com.google.protobuf.Descriptors.EnumValueDescriptor getValueDescriptor() {
      return getDescriptor().getValues().get(ordinal());
    }

    public final com.google.protobuf.Descriptors.EnumDescriptor getDescriptorForType() {
      return getDescriptor();
    }

    public static final com.google.protobuf.Descriptors.EnumDescriptor getDescriptor() {
      return ValuedResponse.getDescriptor().getEnumTypes().get(0);
    }

    private static final ResponsedType[] VALUES = values();

    public static ResponsedType valueOf(com.google.protobuf.Descriptors.EnumValueDescriptor desc) {
      if (desc.getType() != getDescriptor()) {
        throw new IllegalArgumentException("EnumValueDescriptor is not for this type.");
      }
      if (desc.getIndex() == -1) {
        return UNRECOGNIZED;
      }
      return VALUES[desc.getIndex()];
    }

    private final int value;

    private ResponsedType(int value) {
      this.value = value;
    }

    // @@protoc_insertion_point(enum_scope:emqx.exhook.v2.ValuedResponse.ResponsedType)
  }

  private int valueCase_ = 0;
  private Object value_;

  public enum ValueCase implements com.google.protobuf.Internal.EnumLite {
    BOOL_RESULT(3), MESSAGE(4), VALUE_NOT_SET(0);

    private final int value;

    private ValueCase(int value) {
      this.value = value;
    }

    /**
     * @deprecated Use {@link #forNumber(int)} instead.
     */
    @Deprecated
    public static ValueCase valueOf(int value) {
      return forNumber(value);
    }

    public static ValueCase forNumber(int value) {
      switch (value) {
        case 3:
          return BOOL_RESULT;
        case 4:
          return MESSAGE;
        case 0:
          return VALUE_NOT_SET;
        default:
          return null;
      }
    }

    public int getNumber() {
      return this.value;
    }
  };

  public ValueCase getValueCase() {
    return ValueCase.forNumber(valueCase_);
  }

  public static final int TYPE_FIELD_NUMBER = 1;
  private int type_;

  /**
   * <code>.emqx.exhook.v2.ValuedResponse.ResponsedType type = 1;</code>
   */
  public int getTypeValue() {
    return type_;
  }

  /**
   * <code>.emqx.exhook.v2.ValuedResponse.ResponsedType type = 1;</code>
   */
  public ResponsedType getType() {
    ResponsedType result = ResponsedType.valueOf(type_);
    return result == null ? ResponsedType.UNRECOGNIZED : result;
  }

  public static final int BOOL_RESULT_FIELD_NUMBER = 3;

  /**
   * <pre>
   * Boolean result, used on the 'client.authenticate', 'client.authorize' hooks
   * </pre>
   *
   * <code>bool bool_result = 3;</code>
   */
  public boolean getBoolResult() {
    if (valueCase_ == 3) {
      return (Boolean) value_;
    }
    return false;
  }

  public static final int MESSAGE_FIELD_NUMBER = 4;

  /**
   * <pre>
   * Message result, used on the 'message.*' hooks
   * </pre>
   *
   * <code>.emqx.exhook.v2.Message message = 4;</code>
   */
  public boolean hasMessage() {
    return valueCase_ == 4;
  }

  /**
   * <pre>
   * Message result, used on the 'message.*' hooks
   * </pre>
   *
   * <code>.emqx.exhook.v2.Message message = 4;</code>
   */
  public Message getMessage() {
    if (valueCase_ == 4) {
      return (Message) value_;
    }
    return Message.getDefaultInstance();
  }

  /**
   * <pre>
   * Message result, used on the 'message.*' hooks
   * </pre>
   *
   * <code>.emqx.exhook.v2.Message message = 4;</code>
   */
  public MessageOrBuilder getMessageOrBuilder() {
    if (valueCase_ == 4) {
      return (Message) value_;
    }
    return Message.getDefaultInstance();
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
    if (type_ != ResponsedType.CONTINUE.getNumber()) {
      output.writeEnum(1, type_);
    }
    if (valueCase_ == 3) {
      output.writeBool(3, (boolean) ((Boolean) value_));
    }
    if (valueCase_ == 4) {
      output.writeMessage(4, (Message) value_);
    }
    unknownFields.writeTo(output);
  }

  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1)
      return size;

    size = 0;
    if (type_ != ResponsedType.CONTINUE.getNumber()) {
      size += com.google.protobuf.CodedOutputStream.computeEnumSize(1, type_);
    }
    if (valueCase_ == 3) {
      size +=
          com.google.protobuf.CodedOutputStream.computeBoolSize(3, (boolean) ((Boolean) value_));
    }
    if (valueCase_ == 4) {
      size += com.google.protobuf.CodedOutputStream.computeMessageSize(4, (Message) value_);
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
    if (!(obj instanceof ValuedResponse)) {
      return super.equals(obj);
    }
    ValuedResponse other = (ValuedResponse) obj;

    boolean result = true;
    result = result && type_ == other.type_;
    result = result && getValueCase().equals(other.getValueCase());
    if (!result)
      return false;
    switch (valueCase_) {
      case 3:
        result = result && (getBoolResult() == other.getBoolResult());
        break;
      case 4:
        result = result && getMessage().equals(other.getMessage());
        break;
      case 0:
      default:
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
    hash = (37 * hash) + TYPE_FIELD_NUMBER;
    hash = (53 * hash) + type_;
    switch (valueCase_) {
      case 3:
        hash = (37 * hash) + BOOL_RESULT_FIELD_NUMBER;
        hash = (53 * hash) + com.google.protobuf.Internal.hashBoolean(getBoolResult());
        break;
      case 4:
        hash = (37 * hash) + MESSAGE_FIELD_NUMBER;
        hash = (53 * hash) + getMessage().hashCode();
        break;
      case 0:
      default:
    }
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static ValuedResponse parseFrom(java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static ValuedResponse parseFrom(java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static ValuedResponse parseFrom(com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static ValuedResponse parseFrom(com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static ValuedResponse parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static ValuedResponse parseFrom(byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static ValuedResponse parseFrom(java.io.InputStream input) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }

  public static ValuedResponse parseFrom(java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input,
        extensionRegistry);
  }

  public static ValuedResponse parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
  }

  public static ValuedResponse parseDelimitedFrom(java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input,
        extensionRegistry);
  }

  public static ValuedResponse parseFrom(com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }

  public static ValuedResponse parseFrom(com.google.protobuf.CodedInputStream input,
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

  public static Builder newBuilder(ValuedResponse prototype) {
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
   * Protobuf type {@code emqx.exhook.v2.ValuedResponse}
   */
  public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder>
      implements
      // @@protoc_insertion_point(builder_implements:emqx.exhook.v2.ValuedResponse)
      ValuedResponseOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return EmqxExHookProto.internal_static_emqx_exhook_v2_ValuedResponse_descriptor;
    }

    protected FieldAccessorTable internalGetFieldAccessorTable() {
      return EmqxExHookProto.internal_static_emqx_exhook_v2_ValuedResponse_fieldAccessorTable
          .ensureFieldAccessorsInitialized(ValuedResponse.class, Builder.class);
    }

    // Construct using top.rslly.iot.utility.exhook.ValuedResponse.newBuilder()
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
      type_ = 0;

      valueCase_ = 0;
      value_ = null;
      return this;
    }

    public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
      return EmqxExHookProto.internal_static_emqx_exhook_v2_ValuedResponse_descriptor;
    }

    public ValuedResponse getDefaultInstanceForType() {
      return ValuedResponse.getDefaultInstance();
    }

    public ValuedResponse build() {
      ValuedResponse result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    public ValuedResponse buildPartial() {
      ValuedResponse result = new ValuedResponse(this);
      result.type_ = type_;
      if (valueCase_ == 3) {
        result.value_ = value_;
      }
      if (valueCase_ == 4) {
        if (messageBuilder_ == null) {
          result.value_ = value_;
        } else {
          result.value_ = messageBuilder_.build();
        }
      }
      result.valueCase_ = valueCase_;
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
      if (other instanceof ValuedResponse) {
        return mergeFrom((ValuedResponse) other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(ValuedResponse other) {
      if (other == ValuedResponse.getDefaultInstance())
        return this;
      if (other.type_ != 0) {
        setTypeValue(other.getTypeValue());
      }
      switch (other.getValueCase()) {
        case BOOL_RESULT: {
          setBoolResult(other.getBoolResult());
          break;
        }
        case MESSAGE: {
          mergeMessage(other.getMessage());
          break;
        }
        case VALUE_NOT_SET: {
          break;
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
      ValuedResponse parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (ValuedResponse) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private int valueCase_ = 0;
    private Object value_;

    public ValueCase getValueCase() {
      return ValueCase.forNumber(valueCase_);
    }

    public Builder clearValue() {
      valueCase_ = 0;
      value_ = null;
      onChanged();
      return this;
    }


    private int type_ = 0;

    /**
     * <code>.emqx.exhook.v2.ValuedResponse.ResponsedType type = 1;</code>
     */
    public int getTypeValue() {
      return type_;
    }

    /**
     * <code>.emqx.exhook.v2.ValuedResponse.ResponsedType type = 1;</code>
     */
    public Builder setTypeValue(int value) {
      type_ = value;
      onChanged();
      return this;
    }

    /**
     * <code>.emqx.exhook.v2.ValuedResponse.ResponsedType type = 1;</code>
     */
    public ResponsedType getType() {
      ResponsedType result = ResponsedType.valueOf(type_);
      return result == null ? ResponsedType.UNRECOGNIZED : result;
    }

    /**
     * <code>.emqx.exhook.v2.ValuedResponse.ResponsedType type = 1;</code>
     */
    public Builder setType(ResponsedType value) {
      if (value == null) {
        throw new NullPointerException();
      }

      type_ = value.getNumber();
      onChanged();
      return this;
    }

    /**
     * <code>.emqx.exhook.v2.ValuedResponse.ResponsedType type = 1;</code>
     */
    public Builder clearType() {

      type_ = 0;
      onChanged();
      return this;
    }

    /**
     * <pre>
     * Boolean result, used on the 'client.authenticate', 'client.authorize' hooks
     * </pre>
     *
     * <code>bool bool_result = 3;</code>
     */
    public boolean getBoolResult() {
      if (valueCase_ == 3) {
        return (Boolean) value_;
      }
      return false;
    }

    /**
     * <pre>
     * Boolean result, used on the 'client.authenticate', 'client.authorize' hooks
     * </pre>
     *
     * <code>bool bool_result = 3;</code>
     */
    public Builder setBoolResult(boolean value) {
      valueCase_ = 3;
      value_ = value;
      onChanged();
      return this;
    }

    /**
     * <pre>
     * Boolean result, used on the 'client.authenticate', 'client.authorize' hooks
     * </pre>
     *
     * <code>bool bool_result = 3;</code>
     */
    public Builder clearBoolResult() {
      if (valueCase_ == 3) {
        valueCase_ = 0;
        value_ = null;
        onChanged();
      }
      return this;
    }

    private com.google.protobuf.SingleFieldBuilderV3<Message, Message.Builder, MessageOrBuilder> messageBuilder_;

    /**
     * <pre>
     * Message result, used on the 'message.*' hooks
     * </pre>
     *
     * <code>.emqx.exhook.v2.Message message = 4;</code>
     */
    public boolean hasMessage() {
      return valueCase_ == 4;
    }

    /**
     * <pre>
     * Message result, used on the 'message.*' hooks
     * </pre>
     *
     * <code>.emqx.exhook.v2.Message message = 4;</code>
     */
    public Message getMessage() {
      if (messageBuilder_ == null) {
        if (valueCase_ == 4) {
          return (Message) value_;
        }
        return Message.getDefaultInstance();
      } else {
        if (valueCase_ == 4) {
          return messageBuilder_.getMessage();
        }
        return Message.getDefaultInstance();
      }
    }

    /**
     * <pre>
     * Message result, used on the 'message.*' hooks
     * </pre>
     *
     * <code>.emqx.exhook.v2.Message message = 4;</code>
     */
    public Builder setMessage(Message value) {
      if (messageBuilder_ == null) {
        if (value == null) {
          throw new NullPointerException();
        }
        value_ = value;
        onChanged();
      } else {
        messageBuilder_.setMessage(value);
      }
      valueCase_ = 4;
      return this;
    }

    /**
     * <pre>
     * Message result, used on the 'message.*' hooks
     * </pre>
     *
     * <code>.emqx.exhook.v2.Message message = 4;</code>
     */
    public Builder setMessage(Message.Builder builderForValue) {
      if (messageBuilder_ == null) {
        value_ = builderForValue.build();
        onChanged();
      } else {
        messageBuilder_.setMessage(builderForValue.build());
      }
      valueCase_ = 4;
      return this;
    }

    /**
     * <pre>
     * Message result, used on the 'message.*' hooks
     * </pre>
     *
     * <code>.emqx.exhook.v2.Message message = 4;</code>
     */
    public Builder mergeMessage(Message value) {
      if (messageBuilder_ == null) {
        if (valueCase_ == 4 && value_ != Message.getDefaultInstance()) {
          value_ = Message.newBuilder((Message) value_).mergeFrom(value).buildPartial();
        } else {
          value_ = value;
        }
        onChanged();
      } else {
        if (valueCase_ == 4) {
          messageBuilder_.mergeFrom(value);
        }
        messageBuilder_.setMessage(value);
      }
      valueCase_ = 4;
      return this;
    }

    /**
     * <pre>
     * Message result, used on the 'message.*' hooks
     * </pre>
     *
     * <code>.emqx.exhook.v2.Message message = 4;</code>
     */
    public Builder clearMessage() {
      if (messageBuilder_ == null) {
        if (valueCase_ == 4) {
          valueCase_ = 0;
          value_ = null;
          onChanged();
        }
      } else {
        if (valueCase_ == 4) {
          valueCase_ = 0;
          value_ = null;
        }
        messageBuilder_.clear();
      }
      return this;
    }

    /**
     * <pre>
     * Message result, used on the 'message.*' hooks
     * </pre>
     *
     * <code>.emqx.exhook.v2.Message message = 4;</code>
     */
    public Message.Builder getMessageBuilder() {
      return getMessageFieldBuilder().getBuilder();
    }

    /**
     * <pre>
     * Message result, used on the 'message.*' hooks
     * </pre>
     *
     * <code>.emqx.exhook.v2.Message message = 4;</code>
     */
    public MessageOrBuilder getMessageOrBuilder() {
      if ((valueCase_ == 4) && (messageBuilder_ != null)) {
        return messageBuilder_.getMessageOrBuilder();
      } else {
        if (valueCase_ == 4) {
          return (Message) value_;
        }
        return Message.getDefaultInstance();
      }
    }

    /**
     * <pre>
     * Message result, used on the 'message.*' hooks
     * </pre>
     *
     * <code>.emqx.exhook.v2.Message message = 4;</code>
     */
    private com.google.protobuf.SingleFieldBuilderV3<Message, Message.Builder, MessageOrBuilder> getMessageFieldBuilder() {
      if (messageBuilder_ == null) {
        if (!(valueCase_ == 4)) {
          value_ = Message.getDefaultInstance();
        }
        messageBuilder_ =
            new com.google.protobuf.SingleFieldBuilderV3<Message, Message.Builder, MessageOrBuilder>(
                (Message) value_, getParentForChildren(), isClean());
        value_ = null;
      }
      valueCase_ = 4;
      onChanged();;
      return messageBuilder_;
    }

    public final Builder setUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.setUnknownFieldsProto3(unknownFields);
    }

    public final Builder mergeUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.mergeUnknownFields(unknownFields);
    }


    // @@protoc_insertion_point(builder_scope:emqx.exhook.v2.ValuedResponse)
  }

  // @@protoc_insertion_point(class_scope:emqx.exhook.v2.ValuedResponse)
  private static final ValuedResponse DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new ValuedResponse();
  }

  public static ValuedResponse getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<ValuedResponse> PARSER =
      new com.google.protobuf.AbstractParser<ValuedResponse>() {
        public ValuedResponse parsePartialFrom(com.google.protobuf.CodedInputStream input,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
          return new ValuedResponse(input, extensionRegistry);
        }
      };

  public static com.google.protobuf.Parser<ValuedResponse> parser() {
    return PARSER;
  }

  @Override
  public com.google.protobuf.Parser<ValuedResponse> getParserForType() {
    return PARSER;
  }

  public ValuedResponse getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

