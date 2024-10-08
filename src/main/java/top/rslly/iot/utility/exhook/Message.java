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
 * Protobuf type {@code emqx.exhook.v2.Message}
 */
public final class Message extends com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:emqx.exhook.v2.Message)
    MessageOrBuilder {
  private static final long serialVersionUID = 0L;

  // Use Message.newBuilder() to construct.
  private Message(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }

  private Message() {
    node_ = "";
    id_ = "";
    qos_ = 0;
    from_ = "";
    topic_ = "";
    payload_ = com.google.protobuf.ByteString.EMPTY;
    timestamp_ = 0L;
  }

  @Override
  public final com.google.protobuf.UnknownFieldSet getUnknownFields() {
    return this.unknownFields;
  }

  private Message(com.google.protobuf.CodedInputStream input,
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

            id_ = s;
            break;
          }
          case 24: {

            qos_ = input.readUInt32();
            break;
          }
          case 34: {
            String s = input.readStringRequireUtf8();

            from_ = s;
            break;
          }
          case 42: {
            String s = input.readStringRequireUtf8();

            topic_ = s;
            break;
          }
          case 50: {

            payload_ = input.readBytes();
            break;
          }
          case 56: {

            timestamp_ = input.readUInt64();
            break;
          }
          case 66: {
            if (!((mutable_bitField0_ & 0x00000080) == 0x00000080)) {
              headers_ =
                  com.google.protobuf.MapField.newMapField(HeadersDefaultEntryHolder.defaultEntry);
              mutable_bitField0_ |= 0x00000080;
            }
            com.google.protobuf.MapEntry<String, String> headers__ = input.readMessage(
                HeadersDefaultEntryHolder.defaultEntry.getParserForType(), extensionRegistry);
            headers_.getMutableMap().put(headers__.getKey(), headers__.getValue());
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
    return EmqxExHookProto.internal_static_emqx_exhook_v2_Message_descriptor;
  }

  @SuppressWarnings({"rawtypes"})
  protected com.google.protobuf.MapField internalGetMapField(int number) {
    switch (number) {
      case 8:
        return internalGetHeaders();
      default:
        throw new RuntimeException("Invalid map field number: " + number);
    }
  }

  protected FieldAccessorTable internalGetFieldAccessorTable() {
    return EmqxExHookProto.internal_static_emqx_exhook_v2_Message_fieldAccessorTable
        .ensureFieldAccessorsInitialized(Message.class, Builder.class);
  }

  private int bitField0_;
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

  public static final int ID_FIELD_NUMBER = 2;
  private volatile Object id_;

  /**
   * <code>string id = 2;</code>
   */
  public String getId() {
    Object ref = id_;
    if (ref instanceof String) {
      return (String) ref;
    } else {
      com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
      String s = bs.toStringUtf8();
      id_ = s;
      return s;
    }
  }

  /**
   * <code>string id = 2;</code>
   */
  public com.google.protobuf.ByteString getIdBytes() {
    Object ref = id_;
    if (ref instanceof String) {
      com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
      id_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  public static final int QOS_FIELD_NUMBER = 3;
  private int qos_;

  /**
   * <code>uint32 qos = 3;</code>
   */
  public int getQos() {
    return qos_;
  }

  public static final int FROM_FIELD_NUMBER = 4;
  private volatile Object from_;

  /**
   * <code>string from = 4;</code>
   */
  public String getFrom() {
    Object ref = from_;
    if (ref instanceof String) {
      return (String) ref;
    } else {
      com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
      String s = bs.toStringUtf8();
      from_ = s;
      return s;
    }
  }

  /**
   * <code>string from = 4;</code>
   */
  public com.google.protobuf.ByteString getFromBytes() {
    Object ref = from_;
    if (ref instanceof String) {
      com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
      from_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  public static final int TOPIC_FIELD_NUMBER = 5;
  private volatile Object topic_;

  /**
   * <code>string topic = 5;</code>
   */
  public String getTopic() {
    Object ref = topic_;
    if (ref instanceof String) {
      return (String) ref;
    } else {
      com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
      String s = bs.toStringUtf8();
      topic_ = s;
      return s;
    }
  }

  /**
   * <code>string topic = 5;</code>
   */
  public com.google.protobuf.ByteString getTopicBytes() {
    Object ref = topic_;
    if (ref instanceof String) {
      com.google.protobuf.ByteString b = com.google.protobuf.ByteString.copyFromUtf8((String) ref);
      topic_ = b;
      return b;
    } else {
      return (com.google.protobuf.ByteString) ref;
    }
  }

  public static final int PAYLOAD_FIELD_NUMBER = 6;
  private com.google.protobuf.ByteString payload_;

  /**
   * <code>bytes payload = 6;</code>
   */
  public com.google.protobuf.ByteString getPayload() {
    return payload_;
  }

  public static final int TIMESTAMP_FIELD_NUMBER = 7;
  private long timestamp_;

  /**
   * <code>uint64 timestamp = 7;</code>
   */
  public long getTimestamp() {
    return timestamp_;
  }

  public static final int HEADERS_FIELD_NUMBER = 8;

  private static final class HeadersDefaultEntryHolder {
    static final com.google.protobuf.MapEntry<String, String> defaultEntry =
        com.google.protobuf.MapEntry.<String, String>newDefaultInstance(
            EmqxExHookProto.internal_static_emqx_exhook_v2_Message_HeadersEntry_descriptor,
            com.google.protobuf.WireFormat.FieldType.STRING, "",
            com.google.protobuf.WireFormat.FieldType.STRING, "");
  }

  private com.google.protobuf.MapField<String, String> headers_;

  private com.google.protobuf.MapField<String, String> internalGetHeaders() {
    if (headers_ == null) {
      return com.google.protobuf.MapField.emptyMapField(HeadersDefaultEntryHolder.defaultEntry);
    }
    return headers_;
  }

  public int getHeadersCount() {
    return internalGetHeaders().getMap().size();
  }

  /**
   * <pre>
   * The key of header can be:
   *  - username:
   *    * Readonly
   *    * The username of sender client
   *    * Value type: utf8 string
   *  - protocol:
   *    * Readonly
   *    * The protocol name of sender client
   *    * Value type: string enum with "mqtt", "mqtt-sn", ...
   *  - peerhost:
   *    * Readonly
   *    * The peerhost of sender client
   *    * Value type: ip address string
   *  - allow_publish:
   *    * Writable
   *    * Whether to allow the message to be published by emqx
   *    * Value type: string enum with "true", "false", default is "true"
   * Notes: All header may be missing, which means that the message does not
   *   carry these headers. We can guarantee that clients coming from MQTT,
   *   MQTT-SN, CoAP, LwM2M and other natively supported protocol clients will
   *   carry these headers, but there is no guarantee that messages published
   *   by other means will do, e.g. messages published by HTTP-API
   * </pre>
   *
   * <code>map&lt;string, string&gt; headers = 8;</code>
   */

  public boolean containsHeaders(String key) {
    if (key == null) {
      throw new NullPointerException();
    }
    return internalGetHeaders().getMap().containsKey(key);
  }

  /**
   * Use {@link #getHeadersMap()} instead.
   */
  @Deprecated
  public java.util.Map<String, String> getHeaders() {
    return getHeadersMap();
  }

  /**
   * <pre>
   * The key of header can be:
   *  - username:
   *    * Readonly
   *    * The username of sender client
   *    * Value type: utf8 string
   *  - protocol:
   *    * Readonly
   *    * The protocol name of sender client
   *    * Value type: string enum with "mqtt", "mqtt-sn", ...
   *  - peerhost:
   *    * Readonly
   *    * The peerhost of sender client
   *    * Value type: ip address string
   *  - allow_publish:
   *    * Writable
   *    * Whether to allow the message to be published by emqx
   *    * Value type: string enum with "true", "false", default is "true"
   * Notes: All header may be missing, which means that the message does not
   *   carry these headers. We can guarantee that clients coming from MQTT,
   *   MQTT-SN, CoAP, LwM2M and other natively supported protocol clients will
   *   carry these headers, but there is no guarantee that messages published
   *   by other means will do, e.g. messages published by HTTP-API
   * </pre>
   *
   * <code>map&lt;string, string&gt; headers = 8;</code>
   */

  public java.util.Map<String, String> getHeadersMap() {
    return internalGetHeaders().getMap();
  }

  /**
   * <pre>
   * The key of header can be:
   *  - username:
   *    * Readonly
   *    * The username of sender client
   *    * Value type: utf8 string
   *  - protocol:
   *    * Readonly
   *    * The protocol name of sender client
   *    * Value type: string enum with "mqtt", "mqtt-sn", ...
   *  - peerhost:
   *    * Readonly
   *    * The peerhost of sender client
   *    * Value type: ip address string
   *  - allow_publish:
   *    * Writable
   *    * Whether to allow the message to be published by emqx
   *    * Value type: string enum with "true", "false", default is "true"
   * Notes: All header may be missing, which means that the message does not
   *   carry these headers. We can guarantee that clients coming from MQTT,
   *   MQTT-SN, CoAP, LwM2M and other natively supported protocol clients will
   *   carry these headers, but there is no guarantee that messages published
   *   by other means will do, e.g. messages published by HTTP-API
   * </pre>
   *
   * <code>map&lt;string, string&gt; headers = 8;</code>
   */

  public String getHeadersOrDefault(String key, String defaultValue) {
    if (key == null) {
      throw new NullPointerException();
    }
    java.util.Map<String, String> map = internalGetHeaders().getMap();
    return map.containsKey(key) ? map.get(key) : defaultValue;
  }

  /**
   * <pre>
   * The key of header can be:
   *  - username:
   *    * Readonly
   *    * The username of sender client
   *    * Value type: utf8 string
   *  - protocol:
   *    * Readonly
   *    * The protocol name of sender client
   *    * Value type: string enum with "mqtt", "mqtt-sn", ...
   *  - peerhost:
   *    * Readonly
   *    * The peerhost of sender client
   *    * Value type: ip address string
   *  - allow_publish:
   *    * Writable
   *    * Whether to allow the message to be published by emqx
   *    * Value type: string enum with "true", "false", default is "true"
   * Notes: All header may be missing, which means that the message does not
   *   carry these headers. We can guarantee that clients coming from MQTT,
   *   MQTT-SN, CoAP, LwM2M and other natively supported protocol clients will
   *   carry these headers, but there is no guarantee that messages published
   *   by other means will do, e.g. messages published by HTTP-API
   * </pre>
   *
   * <code>map&lt;string, string&gt; headers = 8;</code>
   */

  public String getHeadersOrThrow(String key) {
    if (key == null) {
      throw new NullPointerException();
    }
    java.util.Map<String, String> map = internalGetHeaders().getMap();
    if (!map.containsKey(key)) {
      throw new IllegalArgumentException();
    }
    return map.get(key);
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
    if (!getIdBytes().isEmpty()) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 2, id_);
    }
    if (qos_ != 0) {
      output.writeUInt32(3, qos_);
    }
    if (!getFromBytes().isEmpty()) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 4, from_);
    }
    if (!getTopicBytes().isEmpty()) {
      com.google.protobuf.GeneratedMessageV3.writeString(output, 5, topic_);
    }
    if (!payload_.isEmpty()) {
      output.writeBytes(6, payload_);
    }
    if (timestamp_ != 0L) {
      output.writeUInt64(7, timestamp_);
    }
    com.google.protobuf.GeneratedMessageV3.serializeStringMapTo(output, internalGetHeaders(),
        HeadersDefaultEntryHolder.defaultEntry, 8);
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
    if (!getIdBytes().isEmpty()) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(2, id_);
    }
    if (qos_ != 0) {
      size += com.google.protobuf.CodedOutputStream.computeUInt32Size(3, qos_);
    }
    if (!getFromBytes().isEmpty()) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(4, from_);
    }
    if (!getTopicBytes().isEmpty()) {
      size += com.google.protobuf.GeneratedMessageV3.computeStringSize(5, topic_);
    }
    if (!payload_.isEmpty()) {
      size += com.google.protobuf.CodedOutputStream.computeBytesSize(6, payload_);
    }
    if (timestamp_ != 0L) {
      size += com.google.protobuf.CodedOutputStream.computeUInt64Size(7, timestamp_);
    }
    for (java.util.Map.Entry<String, String> entry : internalGetHeaders().getMap().entrySet()) {
      com.google.protobuf.MapEntry<String, String> headers__ =
          HeadersDefaultEntryHolder.defaultEntry.newBuilderForType().setKey(entry.getKey())
              .setValue(entry.getValue()).build();
      size += com.google.protobuf.CodedOutputStream.computeMessageSize(8, headers__);
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
    if (!(obj instanceof Message)) {
      return super.equals(obj);
    }
    Message other = (Message) obj;

    boolean result = true;
    result = result && getNode().equals(other.getNode());
    result = result && getId().equals(other.getId());
    result = result && (getQos() == other.getQos());
    result = result && getFrom().equals(other.getFrom());
    result = result && getTopic().equals(other.getTopic());
    result = result && getPayload().equals(other.getPayload());
    result = result && (getTimestamp() == other.getTimestamp());
    result = result && internalGetHeaders().equals(other.internalGetHeaders());
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
    hash = (37 * hash) + ID_FIELD_NUMBER;
    hash = (53 * hash) + getId().hashCode();
    hash = (37 * hash) + QOS_FIELD_NUMBER;
    hash = (53 * hash) + getQos();
    hash = (37 * hash) + FROM_FIELD_NUMBER;
    hash = (53 * hash) + getFrom().hashCode();
    hash = (37 * hash) + TOPIC_FIELD_NUMBER;
    hash = (53 * hash) + getTopic().hashCode();
    hash = (37 * hash) + PAYLOAD_FIELD_NUMBER;
    hash = (53 * hash) + getPayload().hashCode();
    hash = (37 * hash) + TIMESTAMP_FIELD_NUMBER;
    hash = (53 * hash) + com.google.protobuf.Internal.hashLong(getTimestamp());
    if (!internalGetHeaders().getMap().isEmpty()) {
      hash = (37 * hash) + HEADERS_FIELD_NUMBER;
      hash = (53 * hash) + internalGetHeaders().hashCode();
    }
    hash = (29 * hash) + unknownFields.hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static Message parseFrom(java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static Message parseFrom(java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static Message parseFrom(com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static Message parseFrom(com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static Message parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }

  public static Message parseFrom(byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }

  public static Message parseFrom(java.io.InputStream input) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }

  public static Message parseFrom(java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input,
        extensionRegistry);
  }

  public static Message parseDelimitedFrom(java.io.InputStream input) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input);
  }

  public static Message parseDelimitedFrom(java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseDelimitedWithIOException(PARSER, input,
        extensionRegistry);
  }

  public static Message parseFrom(com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3.parseWithIOException(PARSER, input);
  }

  public static Message parseFrom(com.google.protobuf.CodedInputStream input,
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

  public static Builder newBuilder(Message prototype) {
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
   * Protobuf type {@code emqx.exhook.v2.Message}
   */
  public static final class Builder extends com.google.protobuf.GeneratedMessageV3.Builder<Builder>
      implements
      // @@protoc_insertion_point(builder_implements:emqx.exhook.v2.Message)
      MessageOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor getDescriptor() {
      return EmqxExHookProto.internal_static_emqx_exhook_v2_Message_descriptor;
    }

    @SuppressWarnings({"rawtypes"})
    protected com.google.protobuf.MapField internalGetMapField(int number) {
      switch (number) {
        case 8:
          return internalGetHeaders();
        default:
          throw new RuntimeException("Invalid map field number: " + number);
      }
    }

    @SuppressWarnings({"rawtypes"})
    protected com.google.protobuf.MapField internalGetMutableMapField(int number) {
      switch (number) {
        case 8:
          return internalGetMutableHeaders();
        default:
          throw new RuntimeException("Invalid map field number: " + number);
      }
    }

    protected FieldAccessorTable internalGetFieldAccessorTable() {
      return EmqxExHookProto.internal_static_emqx_exhook_v2_Message_fieldAccessorTable
          .ensureFieldAccessorsInitialized(Message.class, Builder.class);
    }

    // Construct using top.rslly.iot.utility.exhook.Message.newBuilder()
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

      id_ = "";

      qos_ = 0;

      from_ = "";

      topic_ = "";

      payload_ = com.google.protobuf.ByteString.EMPTY;

      timestamp_ = 0L;

      internalGetMutableHeaders().clear();
      return this;
    }

    public com.google.protobuf.Descriptors.Descriptor getDescriptorForType() {
      return EmqxExHookProto.internal_static_emqx_exhook_v2_Message_descriptor;
    }

    public Message getDefaultInstanceForType() {
      return Message.getDefaultInstance();
    }

    public Message build() {
      Message result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    public Message buildPartial() {
      Message result = new Message(this);
      int from_bitField0_ = bitField0_;
      int to_bitField0_ = 0;
      result.node_ = node_;
      result.id_ = id_;
      result.qos_ = qos_;
      result.from_ = from_;
      result.topic_ = topic_;
      result.payload_ = payload_;
      result.timestamp_ = timestamp_;
      result.headers_ = internalGetHeaders();
      result.headers_.makeImmutable();
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
      if (other instanceof Message) {
        return mergeFrom((Message) other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(Message other) {
      if (other == Message.getDefaultInstance())
        return this;
      if (!other.getNode().isEmpty()) {
        node_ = other.node_;
        onChanged();
      }
      if (!other.getId().isEmpty()) {
        id_ = other.id_;
        onChanged();
      }
      if (other.getQos() != 0) {
        setQos(other.getQos());
      }
      if (!other.getFrom().isEmpty()) {
        from_ = other.from_;
        onChanged();
      }
      if (!other.getTopic().isEmpty()) {
        topic_ = other.topic_;
        onChanged();
      }
      if (other.getPayload() != com.google.protobuf.ByteString.EMPTY) {
        setPayload(other.getPayload());
      }
      if (other.getTimestamp() != 0L) {
        setTimestamp(other.getTimestamp());
      }
      internalGetMutableHeaders().mergeFrom(other.internalGetHeaders());
      this.mergeUnknownFields(other.unknownFields);
      onChanged();
      return this;
    }

    public final boolean isInitialized() {
      return true;
    }

    public Builder mergeFrom(com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry) throws java.io.IOException {
      Message parsedMessage = null;
      try {
        parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        parsedMessage = (Message) e.getUnfinishedMessage();
        throw e.unwrapIOException();
      } finally {
        if (parsedMessage != null) {
          mergeFrom(parsedMessage);
        }
      }
      return this;
    }

    private int bitField0_;

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

    private Object id_ = "";

    /**
     * <code>string id = 2;</code>
     */
    public String getId() {
      Object ref = id_;
      if (!(ref instanceof String)) {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        id_ = s;
        return s;
      } else {
        return (String) ref;
      }
    }

    /**
     * <code>string id = 2;</code>
     */
    public com.google.protobuf.ByteString getIdBytes() {
      Object ref = id_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b =
            com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        id_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    /**
     * <code>string id = 2;</code>
     */
    public Builder setId(String value) {
      if (value == null) {
        throw new NullPointerException();
      }

      id_ = value;
      onChanged();
      return this;
    }

    /**
     * <code>string id = 2;</code>
     */
    public Builder clearId() {

      id_ = getDefaultInstance().getId();
      onChanged();
      return this;
    }

    /**
     * <code>string id = 2;</code>
     */
    public Builder setIdBytes(com.google.protobuf.ByteString value) {
      if (value == null) {
        throw new NullPointerException();
      }
      checkByteStringIsUtf8(value);

      id_ = value;
      onChanged();
      return this;
    }

    private int qos_;

    /**
     * <code>uint32 qos = 3;</code>
     */
    public int getQos() {
      return qos_;
    }

    /**
     * <code>uint32 qos = 3;</code>
     */
    public Builder setQos(int value) {

      qos_ = value;
      onChanged();
      return this;
    }

    /**
     * <code>uint32 qos = 3;</code>
     */
    public Builder clearQos() {

      qos_ = 0;
      onChanged();
      return this;
    }

    private Object from_ = "";

    /**
     * <code>string from = 4;</code>
     */
    public String getFrom() {
      Object ref = from_;
      if (!(ref instanceof String)) {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        from_ = s;
        return s;
      } else {
        return (String) ref;
      }
    }

    /**
     * <code>string from = 4;</code>
     */
    public com.google.protobuf.ByteString getFromBytes() {
      Object ref = from_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b =
            com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        from_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    /**
     * <code>string from = 4;</code>
     */
    public Builder setFrom(String value) {
      if (value == null) {
        throw new NullPointerException();
      }

      from_ = value;
      onChanged();
      return this;
    }

    /**
     * <code>string from = 4;</code>
     */
    public Builder clearFrom() {

      from_ = getDefaultInstance().getFrom();
      onChanged();
      return this;
    }

    /**
     * <code>string from = 4;</code>
     */
    public Builder setFromBytes(com.google.protobuf.ByteString value) {
      if (value == null) {
        throw new NullPointerException();
      }
      checkByteStringIsUtf8(value);

      from_ = value;
      onChanged();
      return this;
    }

    private Object topic_ = "";

    /**
     * <code>string topic = 5;</code>
     */
    public String getTopic() {
      Object ref = topic_;
      if (!(ref instanceof String)) {
        com.google.protobuf.ByteString bs = (com.google.protobuf.ByteString) ref;
        String s = bs.toStringUtf8();
        topic_ = s;
        return s;
      } else {
        return (String) ref;
      }
    }

    /**
     * <code>string topic = 5;</code>
     */
    public com.google.protobuf.ByteString getTopicBytes() {
      Object ref = topic_;
      if (ref instanceof String) {
        com.google.protobuf.ByteString b =
            com.google.protobuf.ByteString.copyFromUtf8((String) ref);
        topic_ = b;
        return b;
      } else {
        return (com.google.protobuf.ByteString) ref;
      }
    }

    /**
     * <code>string topic = 5;</code>
     */
    public Builder setTopic(String value) {
      if (value == null) {
        throw new NullPointerException();
      }

      topic_ = value;
      onChanged();
      return this;
    }

    /**
     * <code>string topic = 5;</code>
     */
    public Builder clearTopic() {

      topic_ = getDefaultInstance().getTopic();
      onChanged();
      return this;
    }

    /**
     * <code>string topic = 5;</code>
     */
    public Builder setTopicBytes(com.google.protobuf.ByteString value) {
      if (value == null) {
        throw new NullPointerException();
      }
      checkByteStringIsUtf8(value);

      topic_ = value;
      onChanged();
      return this;
    }

    private com.google.protobuf.ByteString payload_ = com.google.protobuf.ByteString.EMPTY;

    /**
     * <code>bytes payload = 6;</code>
     */
    public com.google.protobuf.ByteString getPayload() {
      return payload_;
    }

    /**
     * <code>bytes payload = 6;</code>
     */
    public Builder setPayload(com.google.protobuf.ByteString value) {
      if (value == null) {
        throw new NullPointerException();
      }

      payload_ = value;
      onChanged();
      return this;
    }

    /**
     * <code>bytes payload = 6;</code>
     */
    public Builder clearPayload() {

      payload_ = getDefaultInstance().getPayload();
      onChanged();
      return this;
    }

    private long timestamp_;

    /**
     * <code>uint64 timestamp = 7;</code>
     */
    public long getTimestamp() {
      return timestamp_;
    }

    /**
     * <code>uint64 timestamp = 7;</code>
     */
    public Builder setTimestamp(long value) {

      timestamp_ = value;
      onChanged();
      return this;
    }

    /**
     * <code>uint64 timestamp = 7;</code>
     */
    public Builder clearTimestamp() {

      timestamp_ = 0L;
      onChanged();
      return this;
    }

    private com.google.protobuf.MapField<String, String> headers_;

    private com.google.protobuf.MapField<String, String> internalGetHeaders() {
      if (headers_ == null) {
        return com.google.protobuf.MapField.emptyMapField(HeadersDefaultEntryHolder.defaultEntry);
      }
      return headers_;
    }

    private com.google.protobuf.MapField<String, String> internalGetMutableHeaders() {
      onChanged();;
      if (headers_ == null) {
        headers_ = com.google.protobuf.MapField.newMapField(HeadersDefaultEntryHolder.defaultEntry);
      }
      if (!headers_.isMutable()) {
        headers_ = headers_.copy();
      }
      return headers_;
    }

    public int getHeadersCount() {
      return internalGetHeaders().getMap().size();
    }

    /**
     * <pre>
     * The key of header can be:
     *  - username:
     *    * Readonly
     *    * The username of sender client
     *    * Value type: utf8 string
     *  - protocol:
     *    * Readonly
     *    * The protocol name of sender client
     *    * Value type: string enum with "mqtt", "mqtt-sn", ...
     *  - peerhost:
     *    * Readonly
     *    * The peerhost of sender client
     *    * Value type: ip address string
     *  - allow_publish:
     *    * Writable
     *    * Whether to allow the message to be published by emqx
     *    * Value type: string enum with "true", "false", default is "true"
     * Notes: All header may be missing, which means that the message does not
     *   carry these headers. We can guarantee that clients coming from MQTT,
     *   MQTT-SN, CoAP, LwM2M and other natively supported protocol clients will
     *   carry these headers, but there is no guarantee that messages published
     *   by other means will do, e.g. messages published by HTTP-API
     * </pre>
     *
     * <code>map&lt;string, string&gt; headers = 8;</code>
     */

    public boolean containsHeaders(String key) {
      if (key == null) {
        throw new NullPointerException();
      }
      return internalGetHeaders().getMap().containsKey(key);
    }

    /**
     * Use {@link #getHeadersMap()} instead.
     */
    @Deprecated
    public java.util.Map<String, String> getHeaders() {
      return getHeadersMap();
    }

    /**
     * <pre>
     * The key of header can be:
     *  - username:
     *    * Readonly
     *    * The username of sender client
     *    * Value type: utf8 string
     *  - protocol:
     *    * Readonly
     *    * The protocol name of sender client
     *    * Value type: string enum with "mqtt", "mqtt-sn", ...
     *  - peerhost:
     *    * Readonly
     *    * The peerhost of sender client
     *    * Value type: ip address string
     *  - allow_publish:
     *    * Writable
     *    * Whether to allow the message to be published by emqx
     *    * Value type: string enum with "true", "false", default is "true"
     * Notes: All header may be missing, which means that the message does not
     *   carry these headers. We can guarantee that clients coming from MQTT,
     *   MQTT-SN, CoAP, LwM2M and other natively supported protocol clients will
     *   carry these headers, but there is no guarantee that messages published
     *   by other means will do, e.g. messages published by HTTP-API
     * </pre>
     *
     * <code>map&lt;string, string&gt; headers = 8;</code>
     */

    public java.util.Map<String, String> getHeadersMap() {
      return internalGetHeaders().getMap();
    }

    /**
     * <pre>
     * The key of header can be:
     *  - username:
     *    * Readonly
     *    * The username of sender client
     *    * Value type: utf8 string
     *  - protocol:
     *    * Readonly
     *    * The protocol name of sender client
     *    * Value type: string enum with "mqtt", "mqtt-sn", ...
     *  - peerhost:
     *    * Readonly
     *    * The peerhost of sender client
     *    * Value type: ip address string
     *  - allow_publish:
     *    * Writable
     *    * Whether to allow the message to be published by emqx
     *    * Value type: string enum with "true", "false", default is "true"
     * Notes: All header may be missing, which means that the message does not
     *   carry these headers. We can guarantee that clients coming from MQTT,
     *   MQTT-SN, CoAP, LwM2M and other natively supported protocol clients will
     *   carry these headers, but there is no guarantee that messages published
     *   by other means will do, e.g. messages published by HTTP-API
     * </pre>
     *
     * <code>map&lt;string, string&gt; headers = 8;</code>
     */

    public String getHeadersOrDefault(String key, String defaultValue) {
      if (key == null) {
        throw new NullPointerException();
      }
      java.util.Map<String, String> map = internalGetHeaders().getMap();
      return map.containsKey(key) ? map.get(key) : defaultValue;
    }

    /**
     * <pre>
     * The key of header can be:
     *  - username:
     *    * Readonly
     *    * The username of sender client
     *    * Value type: utf8 string
     *  - protocol:
     *    * Readonly
     *    * The protocol name of sender client
     *    * Value type: string enum with "mqtt", "mqtt-sn", ...
     *  - peerhost:
     *    * Readonly
     *    * The peerhost of sender client
     *    * Value type: ip address string
     *  - allow_publish:
     *    * Writable
     *    * Whether to allow the message to be published by emqx
     *    * Value type: string enum with "true", "false", default is "true"
     * Notes: All header may be missing, which means that the message does not
     *   carry these headers. We can guarantee that clients coming from MQTT,
     *   MQTT-SN, CoAP, LwM2M and other natively supported protocol clients will
     *   carry these headers, but there is no guarantee that messages published
     *   by other means will do, e.g. messages published by HTTP-API
     * </pre>
     *
     * <code>map&lt;string, string&gt; headers = 8;</code>
     */

    public String getHeadersOrThrow(String key) {
      if (key == null) {
        throw new NullPointerException();
      }
      java.util.Map<String, String> map = internalGetHeaders().getMap();
      if (!map.containsKey(key)) {
        throw new IllegalArgumentException();
      }
      return map.get(key);
    }

    public Builder clearHeaders() {
      internalGetMutableHeaders().getMutableMap().clear();
      return this;
    }

    /**
     * <pre>
     * The key of header can be:
     *  - username:
     *    * Readonly
     *    * The username of sender client
     *    * Value type: utf8 string
     *  - protocol:
     *    * Readonly
     *    * The protocol name of sender client
     *    * Value type: string enum with "mqtt", "mqtt-sn", ...
     *  - peerhost:
     *    * Readonly
     *    * The peerhost of sender client
     *    * Value type: ip address string
     *  - allow_publish:
     *    * Writable
     *    * Whether to allow the message to be published by emqx
     *    * Value type: string enum with "true", "false", default is "true"
     * Notes: All header may be missing, which means that the message does not
     *   carry these headers. We can guarantee that clients coming from MQTT,
     *   MQTT-SN, CoAP, LwM2M and other natively supported protocol clients will
     *   carry these headers, but there is no guarantee that messages published
     *   by other means will do, e.g. messages published by HTTP-API
     * </pre>
     *
     * <code>map&lt;string, string&gt; headers = 8;</code>
     */

    public Builder removeHeaders(String key) {
      if (key == null) {
        throw new NullPointerException();
      }
      internalGetMutableHeaders().getMutableMap().remove(key);
      return this;
    }

    /**
     * Use alternate mutation accessors instead.
     */
    @Deprecated
    public java.util.Map<String, String> getMutableHeaders() {
      return internalGetMutableHeaders().getMutableMap();
    }

    /**
     * <pre>
     * The key of header can be:
     *  - username:
     *    * Readonly
     *    * The username of sender client
     *    * Value type: utf8 string
     *  - protocol:
     *    * Readonly
     *    * The protocol name of sender client
     *    * Value type: string enum with "mqtt", "mqtt-sn", ...
     *  - peerhost:
     *    * Readonly
     *    * The peerhost of sender client
     *    * Value type: ip address string
     *  - allow_publish:
     *    * Writable
     *    * Whether to allow the message to be published by emqx
     *    * Value type: string enum with "true", "false", default is "true"
     * Notes: All header may be missing, which means that the message does not
     *   carry these headers. We can guarantee that clients coming from MQTT,
     *   MQTT-SN, CoAP, LwM2M and other natively supported protocol clients will
     *   carry these headers, but there is no guarantee that messages published
     *   by other means will do, e.g. messages published by HTTP-API
     * </pre>
     *
     * <code>map&lt;string, string&gt; headers = 8;</code>
     */
    public Builder putHeaders(String key, String value) {
      if (key == null) {
        throw new NullPointerException();
      }
      if (value == null) {
        throw new NullPointerException();
      }
      internalGetMutableHeaders().getMutableMap().put(key, value);
      return this;
    }

    /**
     * <pre>
     * The key of header can be:
     *  - username:
     *    * Readonly
     *    * The username of sender client
     *    * Value type: utf8 string
     *  - protocol:
     *    * Readonly
     *    * The protocol name of sender client
     *    * Value type: string enum with "mqtt", "mqtt-sn", ...
     *  - peerhost:
     *    * Readonly
     *    * The peerhost of sender client
     *    * Value type: ip address string
     *  - allow_publish:
     *    * Writable
     *    * Whether to allow the message to be published by emqx
     *    * Value type: string enum with "true", "false", default is "true"
     * Notes: All header may be missing, which means that the message does not
     *   carry these headers. We can guarantee that clients coming from MQTT,
     *   MQTT-SN, CoAP, LwM2M and other natively supported protocol clients will
     *   carry these headers, but there is no guarantee that messages published
     *   by other means will do, e.g. messages published by HTTP-API
     * </pre>
     *
     * <code>map&lt;string, string&gt; headers = 8;</code>
     */

    public Builder putAllHeaders(java.util.Map<String, String> values) {
      internalGetMutableHeaders().getMutableMap().putAll(values);
      return this;
    }

    public final Builder setUnknownFields(final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.setUnknownFieldsProto3(unknownFields);
    }

    public final Builder mergeUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.mergeUnknownFields(unknownFields);
    }


    // @@protoc_insertion_point(builder_scope:emqx.exhook.v2.Message)
  }

  // @@protoc_insertion_point(class_scope:emqx.exhook.v2.Message)
  private static final Message DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new Message();
  }

  public static Message getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<Message> PARSER =
      new com.google.protobuf.AbstractParser<Message>() {
        public Message parsePartialFrom(com.google.protobuf.CodedInputStream input,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
          return new Message(input, extensionRegistry);
        }
      };

  public static com.google.protobuf.Parser<Message> parser() {
    return PARSER;
  }

  @Override
  public com.google.protobuf.Parser<Message> getParserForType() {
    return PARSER;
  }

  public Message getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

