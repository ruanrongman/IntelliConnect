// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: exhook.proto

package top.rslly.iot.utility.exhook;

public interface MessageDroppedRequestOrBuilder extends
    // @@protoc_insertion_point(interface_extends:emqx.exhook.v2.MessageDroppedRequest)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>.emqx.exhook.v2.Message message = 1;</code>
   */
  boolean hasMessage();
  /**
   * <code>.emqx.exhook.v2.Message message = 1;</code>
   */
  Message getMessage();
  /**
   * <code>.emqx.exhook.v2.Message message = 1;</code>
   */
  MessageOrBuilder getMessageOrBuilder();

  /**
   * <code>string reason = 2;</code>
   */
  String getReason();
  /**
   * <code>string reason = 2;</code>
   */
  com.google.protobuf.ByteString
      getReasonBytes();

  /**
   * <code>.emqx.exhook.v2.RequestMeta meta = 3;</code>
   */
  boolean hasMeta();
  /**
   * <code>.emqx.exhook.v2.RequestMeta meta = 3;</code>
   */
  RequestMeta getMeta();
  /**
   * <code>.emqx.exhook.v2.RequestMeta meta = 3;</code>
   */
  RequestMetaOrBuilder getMetaOrBuilder();
}