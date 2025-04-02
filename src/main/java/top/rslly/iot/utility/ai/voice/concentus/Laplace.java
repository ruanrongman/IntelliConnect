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

class Laplace {

  /* The minimum probability of an energy delta (out of 32768). */
  private static final int LAPLACE_LOG_MINP = 0;
  private static final long LAPLACE_MINP = (1 << LAPLACE_LOG_MINP);
  /*
   * The minimum number of guaranteed representable energy deltas (in one direction).
   */
  private static final int LAPLACE_NMIN = 16;

  /* When called, decay is positive and at most 11456. */
  static long ec_laplace_get_freq1(long fs0, int decay) {
    long ft = Inlines.CapToUInt32(32768 - LAPLACE_MINP * (2 * LAPLACE_NMIN) - fs0);
    return (Inlines.CapToUInt32(ft * (16384 - decay)) >> 15);
  }

  static void ec_laplace_encode(EntropyCoder enc, BoxedValueInt value, long fs, int decay) {
    long fl;
    int val = value.Val;
    fl = 0;
    if (val != 0) {
      int s;
      int i;
      s = 0 - (val < 0 ? 1 : 0);
      val = (val + s) ^ s;
      fl = fs;
      fs = ec_laplace_get_freq1(fs, decay);

      /* Search the decaying part of the PDF. */
      for (i = 1; fs > 0 && i < val; i++) {
        fs *= 2;
        fl = Inlines.CapToUInt32(fl + fs + 2 * LAPLACE_MINP);
        fs = Inlines.CapToUInt32((fs * (int) decay) >> 15);
      }

      /* Everything beyond that has probability LAPLACE_MINP. */
      if (fs == 0) {
        int di;
        int ndi_max;
        ndi_max = (int) (32768 - fl + LAPLACE_MINP - 1) >> LAPLACE_LOG_MINP;
        ndi_max = (ndi_max - s) >> 1;
        di = Inlines.IMIN(val - i, ndi_max - 1);
        fl = Inlines.CapToUInt32(fl + (2 * di + 1 + s) * LAPLACE_MINP);
        fs = Inlines.IMIN(LAPLACE_MINP, 32768 - fl);
        value.Val = (i + di + s) ^ s;
      } else {
        fs += LAPLACE_MINP;
        fl = fl + Inlines.CapToUInt32(fs & ~s);
      }
      Inlines.OpusAssert(fl + fs <= 32768);
      Inlines.OpusAssert(fs > 0);
    }

    enc.encode_bin(fl, (fl + fs), 15);
  }

  static int ec_laplace_decode(EntropyCoder dec, long fs, int decay) {
    int val = 0;
    long fl;
    long fm;
    fm = dec.decode_bin(15);
    fl = 0;

    if (fm >= fs) {
      val++;
      fl = fs;
      fs = ec_laplace_get_freq1(fs, decay) + LAPLACE_MINP;
      /* Search the decaying part of the PDF. */
      while (fs > LAPLACE_MINP && fm >= fl + 2 * fs) {
        fs *= 2;
        fl = Inlines.CapToUInt32(fl + fs);
        fs = Inlines.CapToUInt32(((fs - 2 * LAPLACE_MINP) * (int) decay) >> 15);
        fs += LAPLACE_MINP;
        val++;
      }
      /* Everything beyond that has probability LAPLACE_MINP. */
      if (fs <= LAPLACE_MINP) {
        int di;
        di = (int) (fm - fl) >> (LAPLACE_LOG_MINP + 1);
        val += di;
        fl = Inlines.CapToUInt32(fl + Inlines.CapToUInt32(2 * di * LAPLACE_MINP));
      }
      if (fm < fl + fs) {
        val = -val;
      } else {
        fl = Inlines.CapToUInt32(fl + fs);
      }
    }

    Inlines.OpusAssert(fl < 32768);
    Inlines.OpusAssert(fs > 0);
    Inlines.OpusAssert(fl <= fm);
    Inlines.OpusAssert(fm < Inlines.IMIN(fl + fs, 32768));

    dec.dec_update(fl, Inlines.IMIN(fl + fs, 32768), 32768);
    return val;
  }
}
