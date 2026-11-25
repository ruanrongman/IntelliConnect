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
package top.rslly.iot.utility.smartVoice.rodak;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public record Rav1Frame(long sequence,byte[]payload){public static final int MAX_PAYLOAD=8192;private static final int HEADER=12;

public static Rav1Frame decode(byte[]bytes,long previousSequence){if(bytes==null||bytes.length<HEADER||bytes[0]!='R'||bytes[1]!='A'||bytes[2]!='V'||bytes[3]!='1')throw new IllegalArgumentException("invalid RAV1 frame");ByteBuffer buffer=ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN);buffer.position(4);long sequence=Integer.toUnsignedLong(buffer.getInt());long length=Integer.toUnsignedLong(buffer.getInt());if(sequence==0||(previousSequence==0&&sequence!=1)||sequence<=previousSequence||length==0||length>MAX_PAYLOAD||length!=bytes.length-HEADER){throw new IllegalArgumentException("invalid RAV1 sequence or length");}byte[]payload=new byte[(int)length];buffer.get(payload);return new Rav1Frame(sequence,payload);}

public byte[]encode(){if(sequence<=0||sequence>0xffffffffL||payload==null||payload.length==0||payload.length>MAX_PAYLOAD)throw new IllegalArgumentException("invalid RAV1 frame");ByteBuffer buffer=ByteBuffer.allocate(HEADER+payload.length).order(ByteOrder.BIG_ENDIAN);buffer.put((byte)'R').put((byte)'A').put((byte)'V').put((byte)'1').putInt((int)sequence).putInt(payload.length).put(payload);return buffer.array();}}
