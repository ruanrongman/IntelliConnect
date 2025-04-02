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

class FindLPC {

  /* Finds LPC vector from correlations, and converts to NLSF */
  static void silk_find_LPC(
      SilkChannelEncoder psEncC, /* I/O Encoder state */
      short[] NLSF_Q15, /* O NLSFs */
      short[] x, /* I Input signal */
      int minInvGain_Q30 /* I Inverse of max prediction gain */
  ) {
    int k, subfr_length;
    int[] a_Q16 = new int[SilkConstants.MAX_LPC_ORDER];
    int isInterpLower, shift;
    BoxedValueInt res_nrg0 = new BoxedValueInt(0);
    BoxedValueInt res_nrg1 = new BoxedValueInt(0);
    BoxedValueInt rshift0 = new BoxedValueInt(0);
    BoxedValueInt rshift1 = new BoxedValueInt(0);
    BoxedValueInt scratch_box1 = new BoxedValueInt(0);
    BoxedValueInt scratch_box2 = new BoxedValueInt(0);

    /* Used only for LSF interpolation */
    int[] a_tmp_Q16 = new int[SilkConstants.MAX_LPC_ORDER];
    int res_nrg_interp, res_nrg, res_tmp_nrg;
    int res_nrg_interp_Q, res_nrg_Q, res_tmp_nrg_Q;
    short[] a_tmp_Q12 = new short[SilkConstants.MAX_LPC_ORDER];
    short[] NLSF0_Q15 = new short[SilkConstants.MAX_LPC_ORDER];

    subfr_length = psEncC.subfr_length + psEncC.predictLPCOrder;

    /* Default: no interpolation */
    psEncC.indices.NLSFInterpCoef_Q2 = 4;

    /* Burg AR analysis for the full frame */
    BurgModified.silk_burg_modified(scratch_box1, scratch_box2, a_Q16, x, 0, minInvGain_Q30,
        subfr_length, psEncC.nb_subfr, psEncC.predictLPCOrder);
    res_nrg = scratch_box1.Val;
    res_nrg_Q = scratch_box2.Val;

    if (psEncC.useInterpolatedNLSFs != 0 && psEncC.first_frame_after_reset == 0
        && psEncC.nb_subfr == SilkConstants.MAX_NB_SUBFR) {
      short[] LPC_res;

      /* Optimal solution for last 10 ms */
      BurgModified.silk_burg_modified(scratch_box1, scratch_box2, a_tmp_Q16, x, (2 * subfr_length),
          minInvGain_Q30, subfr_length, 2, psEncC.predictLPCOrder);
      res_tmp_nrg = scratch_box1.Val;
      res_tmp_nrg_Q = scratch_box2.Val;

      /* subtract residual energy here, as that's easier than adding it to the */
      /* residual energy of the first 10 ms in each iteration of the search below */
      shift = res_tmp_nrg_Q - res_nrg_Q;
      if (shift >= 0) {
        if (shift < 32) {
          res_nrg = res_nrg - Inlines.silk_RSHIFT(res_tmp_nrg, shift);
        }
      } else {
        Inlines.OpusAssert(shift > -32);
        res_nrg = Inlines.silk_RSHIFT(res_nrg, -shift) - res_tmp_nrg;
        res_nrg_Q = res_tmp_nrg_Q;
      }

      /* Convert to NLSFs */
      NLSF.silk_A2NLSF(NLSF_Q15, a_tmp_Q16, psEncC.predictLPCOrder);

      LPC_res = new short[2 * subfr_length];

      /* Search over interpolation indices to find the one with lowest residual energy */
      for (k = 3; k >= 0; k--) {
        /* Interpolate NLSFs for first half */
        Inlines.silk_interpolate(NLSF0_Q15, psEncC.prev_NLSFq_Q15, NLSF_Q15, k,
            psEncC.predictLPCOrder);

        /* Convert to LPC for residual energy evaluation */
        NLSF.silk_NLSF2A(a_tmp_Q12, NLSF0_Q15, psEncC.predictLPCOrder);

        /* Calculate residual energy with NLSF interpolation */
        Filters.silk_LPC_analysis_filter(LPC_res, 0, x, 0, a_tmp_Q12, 0, 2 * subfr_length,
            psEncC.predictLPCOrder);

        SumSqrShift.silk_sum_sqr_shift(res_nrg0, rshift0, LPC_res, psEncC.predictLPCOrder,
            subfr_length - psEncC.predictLPCOrder);

        SumSqrShift.silk_sum_sqr_shift(res_nrg1, rshift1, LPC_res,
            psEncC.predictLPCOrder + subfr_length, subfr_length - psEncC.predictLPCOrder);

        /* Add subframe energies from first half frame */
        shift = rshift0.Val - rshift1.Val;
        if (shift >= 0) {
          res_nrg1.Val = Inlines.silk_RSHIFT(res_nrg1.Val, shift);
          res_nrg_interp_Q = 0 - rshift0.Val;
        } else {
          res_nrg0.Val = Inlines.silk_RSHIFT(res_nrg0.Val, 0 - shift);
          res_nrg_interp_Q = 0 - rshift1.Val;
        }
        res_nrg_interp = Inlines.silk_ADD32(res_nrg0.Val, res_nrg1.Val);

        /*
         * Compare with first half energy without NLSF interpolation, or best interpolated value so
         * far
         */
        shift = res_nrg_interp_Q - res_nrg_Q;
        if (shift >= 0) {
          if (Inlines.silk_RSHIFT(res_nrg_interp, shift) < res_nrg) {
            isInterpLower = (true ? 1 : 0);
          } else {
            isInterpLower = (false ? 1 : 0);
          }
        } else if (-shift < 32) {
          if (res_nrg_interp < Inlines.silk_RSHIFT(res_nrg, -shift)) {
            isInterpLower = (true ? 1 : 0);
          } else {
            isInterpLower = (false ? 1 : 0);
          }
        } else {
          isInterpLower = (false ? 1 : 0);
        }

        /* Determine whether current interpolated NLSFs are best so far */
        if (isInterpLower == (true ? 1 : 0)) {
          /* Interpolation has lower residual energy */
          res_nrg = res_nrg_interp;
          res_nrg_Q = res_nrg_interp_Q;
          psEncC.indices.NLSFInterpCoef_Q2 = (byte) k;
        }
      }
    }

    if (psEncC.indices.NLSFInterpCoef_Q2 == 4) {
      /*
       * NLSF interpolation is currently inactive, calculate NLSFs from full frame AR coefficients
       */
      NLSF.silk_A2NLSF(NLSF_Q15, a_Q16, psEncC.predictLPCOrder);
    }

    Inlines.OpusAssert(psEncC.indices.NLSFInterpCoef_Q2 == 4 || (psEncC.useInterpolatedNLSFs != 0
        && psEncC.first_frame_after_reset == 0 && psEncC.nb_subfr == SilkConstants.MAX_NB_SUBFR));

  }
}
