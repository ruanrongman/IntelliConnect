// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: exhook.proto

package top.rslly.iot.utility.exhook;

public interface MessagePublishRequestOrBuilder extends
    // @@protoc_insertion_point(interface_extends:emqx.exhook.v2.MessagePublishRequest)
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
   * <code>.emqx.exhook.v2.RequestMeta meta = 2;</code>
   */
  boolean hasMeta();
  /**
   * <code>.emqx.exhook.v2.RequestMeta meta = 2;</code>
   */
  RequestMeta getMeta();
  /**
   * <code>.emqx.exhook.v2.RequestMeta meta = 2;</code>
   */
  RequestMetaOrBuilder getMetaOrBuilder();
}
