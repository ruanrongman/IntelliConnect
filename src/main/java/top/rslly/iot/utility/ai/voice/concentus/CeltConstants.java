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
package top.rslly.iot.utility.ai.voice.concentus;

class CeltConstants {

  public static final int Q15ONE = 32767;

  public static final float CELT_SIG_SCALE = 32768.0f;

  public static final int SIG_SHIFT = 12;

  public static final int NORM_SCALING = 16384;

  public static final int DB_SHIFT = 10;

  public static final int EPSILON = 1;
  public static final int VERY_SMALL = 0;
  public static final short VERY_LARGE16 = ((short) 32767);
  public static final short Q15_ONE = ((short) 32767);

  public static final int COMBFILTER_MAXPERIOD = 1024;
  public static final int COMBFILTER_MINPERIOD = 15;

  // from opus_decode.c
  public static final int DECODE_BUFFER_SIZE = 2048;

  // from modes.c
  /* Alternate tuning (partially derived from Vorbis) */
  public static final int BITALLOC_SIZE = 11;
  public static final int MAX_PERIOD = 1024;

  // from static_modes_float.h
  public static final int TOTAL_MODES = 1;

  // from rate.h
  public static final int MAX_PSEUDO = 40;
  public static final int LOG_MAX_PSEUDO = 6;

  public static final int CELT_MAX_PULSES = 128;

  public static final int MAX_FINE_BITS = 8;

  public static final int FINE_OFFSET = 21;
  public static final int QTHETA_OFFSET = 4;
  public static final int QTHETA_OFFSET_TWOPHASE = 16;

  /*
   * The maximum pitch lag to allow in the pitch-based PLC. It's possible to save CPU time in the
   * PLC pitch search by making this smaller than MAX_PERIOD. The current value corresponds to a
   * pitch of 66.67 Hz.
   */
  public static final int PLC_PITCH_LAG_MAX = 720;

  /*
   * The minimum pitch lag to allow in the pitch-based PLC. This corresponds to a pitch of 480 Hz.
   */
  public static final int PLC_PITCH_LAG_MIN = 100;

  public static final int LPC_ORDER = 24;
}
