// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: exhook.proto

package top.rslly.iot.utility.exhook;

public interface SessionCreatedRequestOrBuilder extends
    // @@protoc_insertion_point(interface_extends:emqx.exhook.v2.SessionCreatedRequest)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>.emqx.exhook.v2.ClientInfo clientinfo = 1;</code>
   */
  boolean hasClientinfo();
  /**
   * <code>.emqx.exhook.v2.ClientInfo clientinfo = 1;</code>
   */
  ClientInfo getClientinfo();
  /**
   * <code>.emqx.exhook.v2.ClientInfo clientinfo = 1;</code>
   */
  ClientInfoOrBuilder getClientinfoOrBuilder();

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
