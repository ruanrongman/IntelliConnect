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

class ResidualEnergy {

  /* Calculates residual energies of input subframes where all subframes have LPC_order */
  /* of preceding samples */
  static void silk_residual_energy(
      int[] nrgs, /* O Residual energy per subframe [MAX_NB_SUBFR] */
      int[] nrgsQ, /* O Q value per subframe [MAX_NB_SUBFR] */
      short[] x, /* I Input signal */
      short[][] a_Q12, /* I AR coefs for each frame half [2][MAX_LPC_ORDER] */
      int[] gains, /* I Quantization gains [SilkConstants.MAX_NB_SUBFR] */
      int subfr_length, /* I Subframe length */
      int nb_subfr, /* I Number of subframes */
      int LPC_order /* I LPC order */
  ) {
    int offset, i, j, lz1, lz2;
    BoxedValueInt rshift = new BoxedValueInt(0);
    BoxedValueInt energy = new BoxedValueInt(0);
    int LPC_res_ptr;
    short[] LPC_res;
    int x_ptr;
    int tmp32;

    x_ptr = 0;
    offset = LPC_order + subfr_length;

    /* Filter input to create the LPC residual for each frame half, and measure subframe energies */
    LPC_res = new short[(SilkConstants.MAX_NB_SUBFR >> 1) * offset];
    Inlines.OpusAssert((nb_subfr >> 1) * (SilkConstants.MAX_NB_SUBFR >> 1) == nb_subfr);
    for (i = 0; i < nb_subfr >> 1; i++) {
      /* Calculate half frame LPC residual signal including preceding samples */
      Filters.silk_LPC_analysis_filter(LPC_res, 0, x, x_ptr, a_Q12[i], 0,
          (SilkConstants.MAX_NB_SUBFR >> 1) * offset, LPC_order);

      /* Point to first subframe of the just calculated LPC residual signal */
      LPC_res_ptr = LPC_order;
      for (j = 0; j < (SilkConstants.MAX_NB_SUBFR >> 1); j++) {
        /* Measure subframe energy */
        SumSqrShift.silk_sum_sqr_shift(energy, rshift, LPC_res, LPC_res_ptr, subfr_length);
        nrgs[i * (SilkConstants.MAX_NB_SUBFR >> 1) + j] = energy.Val;

        /* Set Q values for the measured energy */
        nrgsQ[i * (SilkConstants.MAX_NB_SUBFR >> 1) + j] = 0 - rshift.Val;

        /* Move to next subframe */
        LPC_res_ptr += offset;
      }
      /* Move to next frame half */
      x_ptr += (SilkConstants.MAX_NB_SUBFR >> 1) * offset;
    }

    /* Apply the squared subframe gains */
    for (i = 0; i < nb_subfr; i++) {
      /* Fully upscale gains and energies */
      lz1 = Inlines.silk_CLZ32(nrgs[i]) - 1;
      lz2 = Inlines.silk_CLZ32(gains[i]) - 1;

      tmp32 = Inlines.silk_LSHIFT32(gains[i], lz2);

      /* Find squared gains */
      tmp32 = Inlines.silk_SMMUL(tmp32, tmp32);
      /* Q( 2 * lz2 - 32 ) */

      /* Scale energies */
      nrgs[i] = Inlines.silk_SMMUL(tmp32, Inlines.silk_LSHIFT32(nrgs[i], lz1));
      /* Q( nrgsQ[ i ] + lz1 + 2 * lz2 - 32 - 32 ) */
      nrgsQ[i] += lz1 + 2 * lz2 - 32 - 32;
    }

  }

  /* Residual energy: nrg = wxx - 2 * wXx * c + c' * wXX * c */
  static int silk_residual_energy16_covar(
      short[] c, /* I Prediction vector */
      int c_ptr,
      int[] wXX, /* I Correlation matrix */
      int wXX_ptr,
      int[] wXx, /* I Correlation vector */
      int wxx, /* I Signal energy */
      int D, /* I Dimension */
      int cQ /* I Q value for c vector 0 - 15 */
  ) {
    int i, j, lshifts, Qxtra;
    int c_max, w_max, tmp, tmp2, nrg;
    int[] cn = new int[D]; // SilkConstants.MAX_MATRIX_SIZE
    int pRow;

    /* Safety checks */
    Inlines.OpusAssert(D >= 0);
    Inlines.OpusAssert(D <= 16);
    Inlines.OpusAssert(cQ > 0);
    Inlines.OpusAssert(cQ < 16);

    lshifts = 16 - cQ;
    Qxtra = lshifts;

    c_max = 0;
    for (i = c_ptr; i < c_ptr + D; i++) {
      c_max = Inlines.silk_max_32(c_max, Inlines.silk_abs((int) c[i]));
    }
    Qxtra = Inlines.silk_min_int(Qxtra, Inlines.silk_CLZ32(c_max) - 17);

    w_max = Inlines.silk_max_32(wXX[wXX_ptr], wXX[wXX_ptr + (D * D) - 1]);
    Qxtra =
        Inlines.silk_min_int(Qxtra,
            Inlines.silk_CLZ32(
                Inlines.silk_MUL(D, Inlines.silk_RSHIFT(Inlines.silk_SMULWB(w_max, c_max), 4)))
                - 5);
    Qxtra = Inlines.silk_max_int(Qxtra, 0);
    for (i = 0; i < D; i++) {
      cn[i] = Inlines.silk_LSHIFT((int) c[c_ptr + i], Qxtra);
      Inlines.OpusAssert(Inlines.silk_abs(cn[i]) <= (Short.MAX_VALUE + 1));
      /* Check that Inlines.silk_SMLAWB can be used */
    }
    lshifts -= Qxtra;

    /* Compute wxx - 2 * wXx * c */
    tmp = 0;
    for (i = 0; i < D; i++) {
      tmp = Inlines.silk_SMLAWB(tmp, wXx[i], cn[i]);
    }
    nrg = Inlines.silk_RSHIFT(wxx, 1 + lshifts) - tmp;
    /* Q: -lshifts - 1 */

    /* Add c' * wXX * c, assuming wXX is symmetric */
    tmp2 = 0;
    for (i = 0; i < D; i++) {
      tmp = 0;
      pRow = wXX_ptr + (i * D);
      for (j = i + 1; j < D; j++) {
        tmp = Inlines.silk_SMLAWB(tmp, wXX[pRow + j], cn[j]);
      }
      tmp = Inlines.silk_SMLAWB(tmp, Inlines.silk_RSHIFT(wXX[pRow + i], 1), cn[i]);
      tmp2 = Inlines.silk_SMLAWB(tmp2, tmp, cn[i]);
    }
    nrg = Inlines.silk_ADD_LSHIFT32(nrg, tmp2, lshifts);
    /* Q: -lshifts - 1 */

    /* Keep one bit free always, because we add them for LSF interpolation */
    if (nrg < 1) {
      nrg = 1;
    } else if (nrg > Inlines.silk_RSHIFT(Integer.MAX_VALUE, lshifts + 2)) {
      nrg = Integer.MAX_VALUE >> 1;
    } else {
      nrg = Inlines.silk_LSHIFT(nrg, lshifts + 1);
      /* Q0 */
    }
    return nrg;

  }
}
