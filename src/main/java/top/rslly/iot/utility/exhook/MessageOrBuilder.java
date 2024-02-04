// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: exhook.proto

package top.rslly.iot.utility.exhook;

public interface MessageOrBuilder extends
    // @@protoc_insertion_point(interface_extends:emqx.exhook.v2.Message)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>string node = 1;</code>
   */
  String getNode();
  /**
   * <code>string node = 1;</code>
   */
  com.google.protobuf.ByteString
      getNodeBytes();

  /**
   * <code>string id = 2;</code>
   */
  String getId();
  /**
   * <code>string id = 2;</code>
   */
  com.google.protobuf.ByteString
      getIdBytes();

  /**
   * <code>uint32 qos = 3;</code>
   */
  int getQos();

  /**
   * <code>string from = 4;</code>
   */
  String getFrom();
  /**
   * <code>string from = 4;</code>
   */
  com.google.protobuf.ByteString
      getFromBytes();

  /**
   * <code>string topic = 5;</code>
   */
  String getTopic();
  /**
   * <code>string topic = 5;</code>
   */
  com.google.protobuf.ByteString
      getTopicBytes();

  /**
   * <code>bytes payload = 6;</code>
   */
  com.google.protobuf.ByteString getPayload();

  /**
   * <code>uint64 timestamp = 7;</code>
   */
  long getTimestamp();

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
  int getHeadersCount();
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
  boolean containsHeaders(
      String key);
  /**
   * Use {@link #getHeadersMap()} instead.
   */
  @Deprecated
  java.util.Map<String, String>
  getHeaders();
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
  java.util.Map<String, String>
  getHeadersMap();
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

  String getHeadersOrDefault(
      String key,
      String defaultValue);
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

  String getHeadersOrThrow(
      String key);
}