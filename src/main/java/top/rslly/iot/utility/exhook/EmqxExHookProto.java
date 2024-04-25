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

public final class EmqxExHookProto {
  private EmqxExHookProto() {}

  public static void registerAllExtensions(com.google.protobuf.ExtensionRegistryLite registry) {}

  public static void registerAllExtensions(com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions((com.google.protobuf.ExtensionRegistryLite) registry);
  }

  static final com.google.protobuf.Descriptors.Descriptor internal_static_emqx_exhook_v2_ProviderLoadedRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_emqx_exhook_v2_ProviderLoadedRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor internal_static_emqx_exhook_v2_ProviderUnloadedRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_emqx_exhook_v2_ProviderUnloadedRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor internal_static_emqx_exhook_v2_ClientConnectRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_emqx_exhook_v2_ClientConnectRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor internal_static_emqx_exhook_v2_ClientConnackRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_emqx_exhook_v2_ClientConnackRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor internal_static_emqx_exhook_v2_ClientConnectedRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_emqx_exhook_v2_ClientConnectedRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor internal_static_emqx_exhook_v2_ClientDisconnectedRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_emqx_exhook_v2_ClientDisconnectedRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor internal_static_emqx_exhook_v2_ClientAuthenticateRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_emqx_exhook_v2_ClientAuthenticateRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor internal_static_emqx_exhook_v2_ClientAuthorizeRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_emqx_exhook_v2_ClientAuthorizeRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor internal_static_emqx_exhook_v2_ClientSubscribeRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_emqx_exhook_v2_ClientSubscribeRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor internal_static_emqx_exhook_v2_ClientUnsubscribeRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_emqx_exhook_v2_ClientUnsubscribeRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor internal_static_emqx_exhook_v2_SessionCreatedRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_emqx_exhook_v2_SessionCreatedRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor internal_static_emqx_exhook_v2_SessionSubscribedRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_emqx_exhook_v2_SessionSubscribedRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor internal_static_emqx_exhook_v2_SessionUnsubscribedRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_emqx_exhook_v2_SessionUnsubscribedRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor internal_static_emqx_exhook_v2_SessionResumedRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_emqx_exhook_v2_SessionResumedRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor internal_static_emqx_exhook_v2_SessionDiscardedRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_emqx_exhook_v2_SessionDiscardedRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor internal_static_emqx_exhook_v2_SessionTakenoverRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_emqx_exhook_v2_SessionTakenoverRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor internal_static_emqx_exhook_v2_SessionTerminatedRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_emqx_exhook_v2_SessionTerminatedRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor internal_static_emqx_exhook_v2_MessagePublishRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_emqx_exhook_v2_MessagePublishRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor internal_static_emqx_exhook_v2_MessageDeliveredRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_emqx_exhook_v2_MessageDeliveredRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor internal_static_emqx_exhook_v2_MessageDroppedRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_emqx_exhook_v2_MessageDroppedRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor internal_static_emqx_exhook_v2_MessageAckedRequest_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_emqx_exhook_v2_MessageAckedRequest_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor internal_static_emqx_exhook_v2_LoadedResponse_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_emqx_exhook_v2_LoadedResponse_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor internal_static_emqx_exhook_v2_ValuedResponse_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_emqx_exhook_v2_ValuedResponse_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor internal_static_emqx_exhook_v2_EmptySuccess_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_emqx_exhook_v2_EmptySuccess_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor internal_static_emqx_exhook_v2_BrokerInfo_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_emqx_exhook_v2_BrokerInfo_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor internal_static_emqx_exhook_v2_HookSpec_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_emqx_exhook_v2_HookSpec_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor internal_static_emqx_exhook_v2_ConnInfo_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_emqx_exhook_v2_ConnInfo_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor internal_static_emqx_exhook_v2_ClientInfo_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_emqx_exhook_v2_ClientInfo_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor internal_static_emqx_exhook_v2_Message_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_emqx_exhook_v2_Message_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor internal_static_emqx_exhook_v2_Message_HeadersEntry_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_emqx_exhook_v2_Message_HeadersEntry_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor internal_static_emqx_exhook_v2_Property_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_emqx_exhook_v2_Property_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor internal_static_emqx_exhook_v2_TopicFilter_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_emqx_exhook_v2_TopicFilter_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor internal_static_emqx_exhook_v2_SubOpts_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_emqx_exhook_v2_SubOpts_fieldAccessorTable;
  static final com.google.protobuf.Descriptors.Descriptor internal_static_emqx_exhook_v2_RequestMeta_descriptor;
  static final com.google.protobuf.GeneratedMessageV3.FieldAccessorTable internal_static_emqx_exhook_v2_RequestMeta_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor getDescriptor() {
    return descriptor;
  }

  private static com.google.protobuf.Descriptors.FileDescriptor descriptor;
  static {
    String[] descriptorData = {"\n\014exhook.proto\022\016emqx.exhook.v2\"n\n\025Provid"
        + "erLoadedRequest\022*\n\006broker\030\001 \001(\0132\032.emqx.e"
        + "xhook.v2.BrokerInfo\022)\n\004meta\030\002 \001(\0132\033.emqx"
        + ".exhook.v2.RequestMeta\"D\n\027ProviderUnload"
        + "edRequest\022)\n\004meta\030\001 \001(\0132\033.emqx.exhook.v2"
        + ".RequestMeta\"\226\001\n\024ClientConnectRequest\022*\n"
        + "\010conninfo\030\001 \001(\0132\030.emqx.exhook.v2.ConnInf"
        + "o\022\'\n\005props\030\002 \003(\0132\030.emqx.exhook.v2.Proper"
        + "ty\022)\n\004meta\030\003 \001(\0132\033.emqx.exhook.v2.Reques"
        + "tMeta\"\253\001\n\024ClientConnackRequest\022*\n\010connin"
        + "fo\030\001 \001(\0132\030.emqx.exhook.v2.ConnInfo\022\023\n\013re"
        + "sult_code\030\002 \001(\t\022\'\n\005props\030\003 \003(\0132\030.emqx.ex"
        + "hook.v2.Property\022)\n\004meta\030\004 \001(\0132\033.emqx.ex"
        + "hook.v2.RequestMeta\"s\n\026ClientConnectedRe"
        + "quest\022.\n\nclientinfo\030\001 \001(\0132\032.emqx.exhook."
        + "v2.ClientInfo\022)\n\004meta\030\002 \001(\0132\033.emqx.exhoo"
        + "k.v2.RequestMeta\"\206\001\n\031ClientDisconnectedR"
        + "equest\022.\n\nclientinfo\030\001 \001(\0132\032.emqx.exhook"
        + ".v2.ClientInfo\022\016\n\006reason\030\002 \001(\t\022)\n\004meta\030\003"
        + " \001(\0132\033.emqx.exhook.v2.RequestMeta\"\206\001\n\031Cl"
        + "ientAuthenticateRequest\022.\n\nclientinfo\030\001 "
        + "\001(\0132\032.emqx.exhook.v2.ClientInfo\022\016\n\006resul"
        + "t\030\002 \001(\010\022)\n\004meta\030\003 \001(\0132\033.emqx.exhook.v2.R"
        + "equestMeta\"\211\002\n\026ClientAuthorizeRequest\022.\n"
        + "\nclientinfo\030\001 \001(\0132\032.emqx.exhook.v2.Clien"
        + "tInfo\022E\n\004type\030\002 \001(\01627.emqx.exhook.v2.Cli"
        + "entAuthorizeRequest.AuthorizeReqType\022\r\n\005"
        + "topic\030\003 \001(\t\022\016\n\006result\030\004 \001(\010\022)\n\004meta\030\005 \001("
        + "\0132\033.emqx.exhook.v2.RequestMeta\".\n\020Author"
        + "izeReqType\022\013\n\007PUBLISH\020\000\022\r\n\tSUBSCRIBE\020\001\"\320"
        + "\001\n\026ClientSubscribeRequest\022.\n\nclientinfo\030"
        + "\001 \001(\0132\032.emqx.exhook.v2.ClientInfo\022\'\n\005pro"
        + "ps\030\002 \003(\0132\030.emqx.exhook.v2.Property\0222\n\rto"
        + "pic_filters\030\003 \003(\0132\033.emqx.exhook.v2.Topic"
        + "Filter\022)\n\004meta\030\004 \001(\0132\033.emqx.exhook.v2.Re"
        + "questMeta\"\322\001\n\030ClientUnsubscribeRequest\022."
        + "\n\nclientinfo\030\001 \001(\0132\032.emqx.exhook.v2.Clie"
        + "ntInfo\022\'\n\005props\030\002 \003(\0132\030.emqx.exhook.v2.P"
        + "roperty\0222\n\rtopic_filters\030\003 \003(\0132\033.emqx.ex"
        + "hook.v2.TopicFilter\022)\n\004meta\030\004 \001(\0132\033.emqx"
        + ".exhook.v2.RequestMeta\"r\n\025SessionCreated"
        + "Request\022.\n\nclientinfo\030\001 \001(\0132\032.emqx.exhoo"
        + "k.v2.ClientInfo\022)\n\004meta\030\002 \001(\0132\033.emqx.exh"
        + "ook.v2.RequestMeta\"\256\001\n\030SessionSubscribed"
        + "Request\022.\n\nclientinfo\030\001 \001(\0132\032.emqx.exhoo"
        + "k.v2.ClientInfo\022\r\n\005topic\030\002 \001(\t\022(\n\007subopt"
        + "s\030\003 \001(\0132\027.emqx.exhook.v2.SubOpts\022)\n\004meta"
        + "\030\004 \001(\0132\033.emqx.exhook.v2.RequestMeta\"\206\001\n\032"
        + "SessionUnsubscribedRequest\022.\n\nclientinfo"
        + "\030\001 \001(\0132\032.emqx.exhook.v2.ClientInfo\022\r\n\005to"
        + "pic\030\002 \001(\t\022)\n\004meta\030\003 \001(\0132\033.emqx.exhook.v2"
        + ".RequestMeta\"r\n\025SessionResumedRequest\022.\n"
        + "\nclientinfo\030\001 \001(\0132\032.emqx.exhook.v2.Clien"
        + "tInfo\022)\n\004meta\030\002 \001(\0132\033.emqx.exhook.v2.Req"
        + "uestMeta\"t\n\027SessionDiscardedRequest\022.\n\nc"
        + "lientinfo\030\001 \001(\0132\032.emqx.exhook.v2.ClientI"
        + "nfo\022)\n\004meta\030\002 \001(\0132\033.emqx.exhook.v2.Reque"
        + "stMeta\"t\n\027SessionTakenoverRequest\022.\n\ncli"
        + "entinfo\030\001 \001(\0132\032.emqx.exhook.v2.ClientInf"
        + "o\022)\n\004meta\030\002 \001(\0132\033.emqx.exhook.v2.Request"
        + "Meta\"\205\001\n\030SessionTerminatedRequest\022.\n\ncli"
        + "entinfo\030\001 \001(\0132\032.emqx.exhook.v2.ClientInf"
        + "o\022\016\n\006reason\030\002 \001(\t\022)\n\004meta\030\003 \001(\0132\033.emqx.e"
        + "xhook.v2.RequestMeta\"l\n\025MessagePublishRe"
        + "quest\022(\n\007message\030\001 \001(\0132\027.emqx.exhook.v2."
        + "Message\022)\n\004meta\030\002 \001(\0132\033.emqx.exhook.v2.R"
        + "equestMeta\"\236\001\n\027MessageDeliveredRequest\022."
        + "\n\nclientinfo\030\001 \001(\0132\032.emqx.exhook.v2.Clie"
        + "ntInfo\022(\n\007message\030\002 \001(\0132\027.emqx.exhook.v2"
        + ".Message\022)\n\004meta\030\003 \001(\0132\033.emqx.exhook.v2."
        + "RequestMeta\"|\n\025MessageDroppedRequest\022(\n\007"
        + "message\030\001 \001(\0132\027.emqx.exhook.v2.Message\022\016"
        + "\n\006reason\030\002 \001(\t\022)\n\004meta\030\003 \001(\0132\033.emqx.exho"
        + "ok.v2.RequestMeta\"\232\001\n\023MessageAckedReques"
        + "t\022.\n\nclientinfo\030\001 \001(\0132\032.emqx.exhook.v2.C"
        + "lientInfo\022(\n\007message\030\002 \001(\0132\027.emqx.exhook"
        + ".v2.Message\022)\n\004meta\030\003 \001(\0132\033.emqx.exhook."
        + "v2.RequestMeta\"9\n\016LoadedResponse\022\'\n\005hook"
        + "s\030\001 \003(\0132\030.emqx.exhook.v2.HookSpec\"\330\001\n\016Va"
        + "luedResponse\022:\n\004type\030\001 \001(\0162,.emqx.exhook"
        + ".v2.ValuedResponse.ResponsedType\022\025\n\013bool"
        + "_result\030\003 \001(\010H\000\022*\n\007message\030\004 \001(\0132\027.emqx."
        + "exhook.v2.MessageH\000\">\n\rResponsedType\022\014\n\010"
        + "CONTINUE\020\000\022\n\n\006IGNORE\020\001\022\023\n\017STOP_AND_RETUR"
        + "N\020\002B\007\n\005value\"\016\n\014EmptySuccess\"Q\n\nBrokerIn"
        + "fo\022\017\n\007version\030\001 \001(\t\022\020\n\010sysdescr\030\002 \001(\t\022\016\n"
        + "\006uptime\030\003 \001(\003\022\020\n\010datetime\030\004 \001(\t\"(\n\010HookS"
        + "pec\022\014\n\004name\030\001 \001(\t\022\016\n\006topics\030\002 \003(\t\"\232\001\n\010Co"
        + "nnInfo\022\014\n\004node\030\001 \001(\t\022\020\n\010clientid\030\002 \001(\t\022\020"
        + "\n\010username\030\003 \001(\t\022\020\n\010peerhost\030\004 \001(\t\022\020\n\010so"
        + "ckport\030\005 \001(\r\022\022\n\nproto_name\030\006 \001(\t\022\021\n\tprot"
        + "o_ver\030\007 \001(\t\022\021\n\tkeepalive\030\010 \001(\r\"\333\001\n\nClien"
        + "tInfo\022\014\n\004node\030\001 \001(\t\022\020\n\010clientid\030\002 \001(\t\022\020\n"
        + "\010username\030\003 \001(\t\022\020\n\010password\030\004 \001(\t\022\020\n\010pee"
        + "rhost\030\005 \001(\t\022\020\n\010sockport\030\006 \001(\r\022\020\n\010protoco"
        + "l\030\007 \001(\t\022\022\n\nmountpoint\030\010 \001(\t\022\024\n\014is_superu"
        + "ser\030\t \001(\010\022\021\n\tanonymous\030\n \001(\010\022\n\n\002cn\030\013 \001(\t"
        + "\022\n\n\002dn\030\014 \001(\t\"\330\001\n\007Message\022\014\n\004node\030\001 \001(\t\022\n"
        + "\n\002id\030\002 \001(\t\022\013\n\003qos\030\003 \001(\r\022\014\n\004from\030\004 \001(\t\022\r\n"
        + "\005topic\030\005 \001(\t\022\017\n\007payload\030\006 \001(\014\022\021\n\ttimesta"
        + "mp\030\007 \001(\004\0225\n\007headers\030\010 \003(\0132$.emqx.exhook."
        + "v2.Message.HeadersEntry\032.\n\014HeadersEntry\022"
        + "\013\n\003key\030\001 \001(\t\022\r\n\005value\030\002 \001(\t:\0028\001\"\'\n\010Prope"
        + "rty\022\014\n\004name\030\001 \001(\t\022\r\n\005value\030\002 \001(\t\"(\n\013Topi"
        + "cFilter\022\014\n\004name\030\001 \001(\t\022\013\n\003qos\030\002 \001(\r\"J\n\007Su"
        + "bOpts\022\013\n\003qos\030\001 \001(\r\022\r\n\005share\030\002 \001(\t\022\n\n\002rh\030"
        + "\003 \001(\r\022\013\n\003rap\030\004 \001(\r\022\n\n\002nl\030\005 \001(\r\"T\n\013Reques"
        + "tMeta\022\014\n\004node\030\001 \001(\t\022\017\n\007version\030\002 \001(\t\022\020\n\010"
        + "sysdescr\030\003 \001(\t\022\024\n\014cluster_name\030\004 \001(\t2\307\017\n"
        + "\014HookProvider\022[\n\020OnProviderLoaded\022%.emqx"
        + ".exhook.v2.ProviderLoadedRequest\032\036.emqx."
        + "exhook.v2.LoadedResponse\"\000\022]\n\022OnProvider"
        + "Unloaded\022\'.emqx.exhook.v2.ProviderUnload"
        + "edRequest\032\034.emqx.exhook.v2.EmptySuccess\""
        + "\000\022W\n\017OnClientConnect\022$.emqx.exhook.v2.Cl"
        + "ientConnectRequest\032\034.emqx.exhook.v2.Empt"
        + "ySuccess\"\000\022W\n\017OnClientConnack\022$.emqx.exh"
        + "ook.v2.ClientConnackRequest\032\034.emqx.exhoo"
        + "k.v2.EmptySuccess\"\000\022[\n\021OnClientConnected"
        + "\022&.emqx.exhook.v2.ClientConnectedRequest"
        + "\032\034.emqx.exhook.v2.EmptySuccess\"\000\022a\n\024OnCl"
        + "ientDisconnected\022).emqx.exhook.v2.Client"
        + "DisconnectedRequest\032\034.emqx.exhook.v2.Emp"
        + "tySuccess\"\000\022c\n\024OnClientAuthenticate\022).em"
        + "qx.exhook.v2.ClientAuthenticateRequest\032\036"
        + ".emqx.exhook.v2.ValuedResponse\"\000\022]\n\021OnCl"
        + "ientAuthorize\022&.emqx.exhook.v2.ClientAut"
        + "horizeRequest\032\036.emqx.exhook.v2.ValuedRes"
        + "ponse\"\000\022[\n\021OnClientSubscribe\022&.emqx.exho"
        + "ok.v2.ClientSubscribeRequest\032\034.emqx.exho"
        + "ok.v2.EmptySuccess\"\000\022_\n\023OnClientUnsubscr"
        + "ibe\022(.emqx.exhook.v2.ClientUnsubscribeRe"
        + "quest\032\034.emqx.exhook.v2.EmptySuccess\"\000\022Y\n"
        + "\020OnSessionCreated\022%.emqx.exhook.v2.Sessi"
        + "onCreatedRequest\032\034.emqx.exhook.v2.EmptyS"
        + "uccess\"\000\022_\n\023OnSessionSubscribed\022(.emqx.e"
        + "xhook.v2.SessionSubscribedRequest\032\034.emqx"
        + ".exhook.v2.EmptySuccess\"\000\022c\n\025OnSessionUn"
        + "subscribed\022*.emqx.exhook.v2.SessionUnsub"
        + "scribedRequest\032\034.emqx.exhook.v2.EmptySuc"
        + "cess\"\000\022Y\n\020OnSessionResumed\022%.emqx.exhook"
        + ".v2.SessionResumedRequest\032\034.emqx.exhook."
        + "v2.EmptySuccess\"\000\022]\n\022OnSessionDiscarded\022"
        + "\'.emqx.exhook.v2.SessionDiscardedRequest"
        + "\032\034.emqx.exhook.v2.EmptySuccess\"\000\022]\n\022OnSe"
        + "ssionTakenover\022\'.emqx.exhook.v2.SessionT"
        + "akenoverRequest\032\034.emqx.exhook.v2.EmptySu"
        + "ccess\"\000\022_\n\023OnSessionTerminated\022(.emqx.ex"
        + "hook.v2.SessionTerminatedRequest\032\034.emqx."
        + "exhook.v2.EmptySuccess\"\000\022[\n\020OnMessagePub"
        + "lish\022%.emqx.exhook.v2.MessagePublishRequ"
        + "est\032\036.emqx.exhook.v2.ValuedResponse\"\000\022]\n"
        + "\022OnMessageDelivered\022\'.emqx.exhook.v2.Mes"
        + "sageDeliveredRequest\032\034.emqx.exhook.v2.Em"
        + "ptySuccess\"\000\022Y\n\020OnMessageDropped\022%.emqx."
        + "exhook.v2.MessageDroppedRequest\032\034.emqx.e"
        + "xhook.v2.EmptySuccess\"\000\022U\n\016OnMessageAcke"
        + "d\022#.emqx.exhook.v2.MessageAckedRequest\032\034"
        + ".emqx.exhook.v2.EmptySuccess\"\000B`\n\034top.rs"
        + "lly.iot.utility.exhookB\017EmqxExHookProtoP"
        + "\001Z\034top.rslly.iot.utility.exhook\252\002\016Emqx.E" + "xhook.V2b\006proto3"};
    com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
        new com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner() {
          public com.google.protobuf.ExtensionRegistry assignDescriptors(
              com.google.protobuf.Descriptors.FileDescriptor root) {
            descriptor = root;
            return null;
          }
        };
    com.google.protobuf.Descriptors.FileDescriptor.internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {}, assigner);
    internal_static_emqx_exhook_v2_ProviderLoadedRequest_descriptor =
        getDescriptor().getMessageTypes().get(0);
    internal_static_emqx_exhook_v2_ProviderLoadedRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_emqx_exhook_v2_ProviderLoadedRequest_descriptor,
            new String[] {"Broker", "Meta",});
    internal_static_emqx_exhook_v2_ProviderUnloadedRequest_descriptor =
        getDescriptor().getMessageTypes().get(1);
    internal_static_emqx_exhook_v2_ProviderUnloadedRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_emqx_exhook_v2_ProviderUnloadedRequest_descriptor,
            new String[] {"Meta",});
    internal_static_emqx_exhook_v2_ClientConnectRequest_descriptor =
        getDescriptor().getMessageTypes().get(2);
    internal_static_emqx_exhook_v2_ClientConnectRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_emqx_exhook_v2_ClientConnectRequest_descriptor,
            new String[] {"Conninfo", "Props", "Meta",});
    internal_static_emqx_exhook_v2_ClientConnackRequest_descriptor =
        getDescriptor().getMessageTypes().get(3);
    internal_static_emqx_exhook_v2_ClientConnackRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_emqx_exhook_v2_ClientConnackRequest_descriptor,
            new String[] {"Conninfo", "ResultCode", "Props", "Meta",});
    internal_static_emqx_exhook_v2_ClientConnectedRequest_descriptor =
        getDescriptor().getMessageTypes().get(4);
    internal_static_emqx_exhook_v2_ClientConnectedRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_emqx_exhook_v2_ClientConnectedRequest_descriptor,
            new String[] {"Clientinfo", "Meta",});
    internal_static_emqx_exhook_v2_ClientDisconnectedRequest_descriptor =
        getDescriptor().getMessageTypes().get(5);
    internal_static_emqx_exhook_v2_ClientDisconnectedRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_emqx_exhook_v2_ClientDisconnectedRequest_descriptor,
            new String[] {"Clientinfo", "Reason", "Meta",});
    internal_static_emqx_exhook_v2_ClientAuthenticateRequest_descriptor =
        getDescriptor().getMessageTypes().get(6);
    internal_static_emqx_exhook_v2_ClientAuthenticateRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_emqx_exhook_v2_ClientAuthenticateRequest_descriptor,
            new String[] {"Clientinfo", "Result", "Meta",});
    internal_static_emqx_exhook_v2_ClientAuthorizeRequest_descriptor =
        getDescriptor().getMessageTypes().get(7);
    internal_static_emqx_exhook_v2_ClientAuthorizeRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_emqx_exhook_v2_ClientAuthorizeRequest_descriptor,
            new String[] {"Clientinfo", "Type", "Topic", "Result", "Meta",});
    internal_static_emqx_exhook_v2_ClientSubscribeRequest_descriptor =
        getDescriptor().getMessageTypes().get(8);
    internal_static_emqx_exhook_v2_ClientSubscribeRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_emqx_exhook_v2_ClientSubscribeRequest_descriptor,
            new String[] {"Clientinfo", "Props", "TopicFilters", "Meta",});
    internal_static_emqx_exhook_v2_ClientUnsubscribeRequest_descriptor =
        getDescriptor().getMessageTypes().get(9);
    internal_static_emqx_exhook_v2_ClientUnsubscribeRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_emqx_exhook_v2_ClientUnsubscribeRequest_descriptor,
            new String[] {"Clientinfo", "Props", "TopicFilters", "Meta",});
    internal_static_emqx_exhook_v2_SessionCreatedRequest_descriptor =
        getDescriptor().getMessageTypes().get(10);
    internal_static_emqx_exhook_v2_SessionCreatedRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_emqx_exhook_v2_SessionCreatedRequest_descriptor,
            new String[] {"Clientinfo", "Meta",});
    internal_static_emqx_exhook_v2_SessionSubscribedRequest_descriptor =
        getDescriptor().getMessageTypes().get(11);
    internal_static_emqx_exhook_v2_SessionSubscribedRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_emqx_exhook_v2_SessionSubscribedRequest_descriptor,
            new String[] {"Clientinfo", "Topic", "Subopts", "Meta",});
    internal_static_emqx_exhook_v2_SessionUnsubscribedRequest_descriptor =
        getDescriptor().getMessageTypes().get(12);
    internal_static_emqx_exhook_v2_SessionUnsubscribedRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_emqx_exhook_v2_SessionUnsubscribedRequest_descriptor,
            new String[] {"Clientinfo", "Topic", "Meta",});
    internal_static_emqx_exhook_v2_SessionResumedRequest_descriptor =
        getDescriptor().getMessageTypes().get(13);
    internal_static_emqx_exhook_v2_SessionResumedRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_emqx_exhook_v2_SessionResumedRequest_descriptor,
            new String[] {"Clientinfo", "Meta",});
    internal_static_emqx_exhook_v2_SessionDiscardedRequest_descriptor =
        getDescriptor().getMessageTypes().get(14);
    internal_static_emqx_exhook_v2_SessionDiscardedRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_emqx_exhook_v2_SessionDiscardedRequest_descriptor,
            new String[] {"Clientinfo", "Meta",});
    internal_static_emqx_exhook_v2_SessionTakenoverRequest_descriptor =
        getDescriptor().getMessageTypes().get(15);
    internal_static_emqx_exhook_v2_SessionTakenoverRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_emqx_exhook_v2_SessionTakenoverRequest_descriptor,
            new String[] {"Clientinfo", "Meta",});
    internal_static_emqx_exhook_v2_SessionTerminatedRequest_descriptor =
        getDescriptor().getMessageTypes().get(16);
    internal_static_emqx_exhook_v2_SessionTerminatedRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_emqx_exhook_v2_SessionTerminatedRequest_descriptor,
            new String[] {"Clientinfo", "Reason", "Meta",});
    internal_static_emqx_exhook_v2_MessagePublishRequest_descriptor =
        getDescriptor().getMessageTypes().get(17);
    internal_static_emqx_exhook_v2_MessagePublishRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_emqx_exhook_v2_MessagePublishRequest_descriptor,
            new String[] {"Message", "Meta",});
    internal_static_emqx_exhook_v2_MessageDeliveredRequest_descriptor =
        getDescriptor().getMessageTypes().get(18);
    internal_static_emqx_exhook_v2_MessageDeliveredRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_emqx_exhook_v2_MessageDeliveredRequest_descriptor,
            new String[] {"Clientinfo", "Message", "Meta",});
    internal_static_emqx_exhook_v2_MessageDroppedRequest_descriptor =
        getDescriptor().getMessageTypes().get(19);
    internal_static_emqx_exhook_v2_MessageDroppedRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_emqx_exhook_v2_MessageDroppedRequest_descriptor,
            new String[] {"Message", "Reason", "Meta",});
    internal_static_emqx_exhook_v2_MessageAckedRequest_descriptor =
        getDescriptor().getMessageTypes().get(20);
    internal_static_emqx_exhook_v2_MessageAckedRequest_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_emqx_exhook_v2_MessageAckedRequest_descriptor,
            new String[] {"Clientinfo", "Message", "Meta",});
    internal_static_emqx_exhook_v2_LoadedResponse_descriptor =
        getDescriptor().getMessageTypes().get(21);
    internal_static_emqx_exhook_v2_LoadedResponse_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_emqx_exhook_v2_LoadedResponse_descriptor, new String[] {"Hooks",});
    internal_static_emqx_exhook_v2_ValuedResponse_descriptor =
        getDescriptor().getMessageTypes().get(22);
    internal_static_emqx_exhook_v2_ValuedResponse_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_emqx_exhook_v2_ValuedResponse_descriptor,
            new String[] {"Type", "BoolResult", "Message", "Value",});
    internal_static_emqx_exhook_v2_EmptySuccess_descriptor =
        getDescriptor().getMessageTypes().get(23);
    internal_static_emqx_exhook_v2_EmptySuccess_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_emqx_exhook_v2_EmptySuccess_descriptor, new String[] {});
    internal_static_emqx_exhook_v2_BrokerInfo_descriptor =
        getDescriptor().getMessageTypes().get(24);
    internal_static_emqx_exhook_v2_BrokerInfo_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_emqx_exhook_v2_BrokerInfo_descriptor,
            new String[] {"Version", "Sysdescr", "Uptime", "Datetime",});
    internal_static_emqx_exhook_v2_HookSpec_descriptor = getDescriptor().getMessageTypes().get(25);
    internal_static_emqx_exhook_v2_HookSpec_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_emqx_exhook_v2_HookSpec_descriptor, new String[] {"Name", "Topics",});
    internal_static_emqx_exhook_v2_ConnInfo_descriptor = getDescriptor().getMessageTypes().get(26);
    internal_static_emqx_exhook_v2_ConnInfo_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_emqx_exhook_v2_ConnInfo_descriptor, new String[] {"Node", "Clientid",
                "Username", "Peerhost", "Sockport", "ProtoName", "ProtoVer", "Keepalive",});
    internal_static_emqx_exhook_v2_ClientInfo_descriptor =
        getDescriptor().getMessageTypes().get(27);
    internal_static_emqx_exhook_v2_ClientInfo_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_emqx_exhook_v2_ClientInfo_descriptor,
            new String[] {"Node", "Clientid", "Username", "Password", "Peerhost", "Sockport",
                "Protocol", "Mountpoint", "IsSuperuser", "Anonymous", "Cn", "Dn",});
    internal_static_emqx_exhook_v2_Message_descriptor = getDescriptor().getMessageTypes().get(28);
    internal_static_emqx_exhook_v2_Message_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_emqx_exhook_v2_Message_descriptor, new String[] {"Node", "Id", "Qos",
                "From", "Topic", "Payload", "Timestamp", "Headers",});
    internal_static_emqx_exhook_v2_Message_HeadersEntry_descriptor =
        internal_static_emqx_exhook_v2_Message_descriptor.getNestedTypes().get(0);
    internal_static_emqx_exhook_v2_Message_HeadersEntry_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_emqx_exhook_v2_Message_HeadersEntry_descriptor,
            new String[] {"Key", "Value",});
    internal_static_emqx_exhook_v2_Property_descriptor = getDescriptor().getMessageTypes().get(29);
    internal_static_emqx_exhook_v2_Property_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_emqx_exhook_v2_Property_descriptor, new String[] {"Name", "Value",});
    internal_static_emqx_exhook_v2_TopicFilter_descriptor =
        getDescriptor().getMessageTypes().get(30);
    internal_static_emqx_exhook_v2_TopicFilter_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_emqx_exhook_v2_TopicFilter_descriptor, new String[] {"Name", "Qos",});
    internal_static_emqx_exhook_v2_SubOpts_descriptor = getDescriptor().getMessageTypes().get(31);
    internal_static_emqx_exhook_v2_SubOpts_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_emqx_exhook_v2_SubOpts_descriptor,
            new String[] {"Qos", "Share", "Rh", "Rap", "Nl",});
    internal_static_emqx_exhook_v2_RequestMeta_descriptor =
        getDescriptor().getMessageTypes().get(32);
    internal_static_emqx_exhook_v2_RequestMeta_fieldAccessorTable =
        new com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
            internal_static_emqx_exhook_v2_RequestMeta_descriptor,
            new String[] {"Node", "Version", "Sysdescr", "ClusterName",});
  }

  // @@protoc_insertion_point(outer_class_scope)
}
