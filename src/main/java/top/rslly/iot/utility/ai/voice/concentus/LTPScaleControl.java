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

class LTPScaleControl {

  /* Calculation of LTP state scaling */
  static void silk_LTP_scale_ctrl(
      SilkChannelEncoder psEnc, /* I/O encoder state */
      SilkEncoderControl psEncCtrl, /* I/O encoder control */
      int condCoding /* I The type of conditional coding to use */
  ) {
    int round_loss;

    if (condCoding == SilkConstants.CODE_INDEPENDENTLY) {
      /* Only scale if first frame in packet */
      round_loss = psEnc.PacketLoss_perc + psEnc.nFramesPerPacket;
      psEnc.indices.LTP_scaleIndex = (byte) Inlines.silk_LIMIT(
          Inlines.silk_SMULWB(Inlines.silk_SMULBB(round_loss, psEncCtrl.LTPredCodGain_Q7),
              ((int) ((0.1f) * ((long) 1 << (9)) + 0.5))/* Inlines.SILK_CONST(0.1f, 9) */),
          0, 2);
    } else {
      /* Default is minimum scaling */
      psEnc.indices.LTP_scaleIndex = 0;
    }
    psEncCtrl.LTP_scale_Q14 = SilkTables.silk_LTPScales_table_Q14[psEnc.indices.LTP_scaleIndex];
  }
}
