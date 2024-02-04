// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: exhook.proto

package top.rslly.iot.utility.exhook;

public interface ClientInfoOrBuilder extends
    // @@protoc_insertion_point(interface_extends:emqx.exhook.v2.ClientInfo)
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
   * <code>string clientid = 2;</code>
   */
  String getClientid();
  /**
   * <code>string clientid = 2;</code>
   */
  com.google.protobuf.ByteString
      getClientidBytes();

  /**
   * <code>string username = 3;</code>
   */
  String getUsername();
  /**
   * <code>string username = 3;</code>
   */
  com.google.protobuf.ByteString
      getUsernameBytes();

  /**
   * <code>string password = 4;</code>
   */
  String getPassword();
  /**
   * <code>string password = 4;</code>
   */
  com.google.protobuf.ByteString
      getPasswordBytes();

  /**
   * <code>string peerhost = 5;</code>
   */
  String getPeerhost();
  /**
   * <code>string peerhost = 5;</code>
   */
  com.google.protobuf.ByteString
      getPeerhostBytes();

  /**
   * <code>uint32 sockport = 6;</code>
   */
  int getSockport();

  /**
   * <code>string protocol = 7;</code>
   */
  String getProtocol();
  /**
   * <code>string protocol = 7;</code>
   */
  com.google.protobuf.ByteString
      getProtocolBytes();

  /**
   * <code>string mountpoint = 8;</code>
   */
  String getMountpoint();
  /**
   * <code>string mountpoint = 8;</code>
   */
  com.google.protobuf.ByteString
      getMountpointBytes();

  /**
   * <code>bool is_superuser = 9;</code>
   */
  boolean getIsSuperuser();

  /**
   * <code>bool anonymous = 10;</code>
   */
  boolean getAnonymous();

  /**
   * <pre>
   * common name of client TLS cert
   * </pre>
   *
   * <code>string cn = 11;</code>
   */
  String getCn();
  /**
   * <pre>
   * common name of client TLS cert
   * </pre>
   *
   * <code>string cn = 11;</code>
   */
  com.google.protobuf.ByteString
      getCnBytes();

  /**
   * <pre>
   * subject of client TLS cert
   * </pre>
   *
   * <code>string dn = 12;</code>
   */
  String getDn();
  /**
   * <pre>
   * subject of client TLS cert
   * </pre>
   *
   * <code>string dn = 12;</code>
   */
  com.google.protobuf.ByteString
      getDnBytes();
}