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

class LinearAlgebra {

  /* Solves Ax = b, assuming A is symmetric */
  static void silk_solve_LDL(
      int[] A, /* I Pointer to symetric square matrix A */
      int A_ptr,
      int M, /* I Size of matrix */
      int[] b, /* I Pointer to b vector */
      int[] x_Q16 /* O Pointer to x solution vector */
  ) {
    Inlines.OpusAssert(M <= SilkConstants.MAX_MATRIX_SIZE);
    int[] L_Q16 = new int[M * M];
    int[] Y = new int[SilkConstants.MAX_MATRIX_SIZE];

    // [Porting note] This is an interleaved array. Formerly it was an array of data structures laid
    // out thus:
    // private struct inv_D_t
    // {
    // int Q36_part;
    // int Q48_part;
    // }
    int[] inv_D = new int[SilkConstants.MAX_MATRIX_SIZE * 2];

    /**
     * ************************************************* Factorize A by LDL such that A = L*D*L',
     * where L is lower triangular with ones on diagonal
     ***************************************************
     */
    silk_LDL_factorize(A, A_ptr, M, L_Q16, inv_D);

    /**
     * ************************************************** substitute D*L'*x = Y. ie: L*D*L'*x = b =>
     * L*Y = b <=> Y = inv(L)*b
     *****************************************************
     */
    silk_LS_SolveFirst(L_Q16, M, b, Y);

    /**
     * ************************************************** D*L'*x = Y <=> L'*x = inv(D)*Y, because D
     * is diagonal just multiply with 1/d_i
     ***************************************************
     */
    silk_LS_divide_Q16(Y, inv_D, M);

    /**
     * ************************************************** x = inv(L') * inv(D) * Y
     ****************************************************
     */
    silk_LS_SolveLast(L_Q16, M, Y, x_Q16);

  }

  /* Factorize square matrix A into LDL form */
  private static void silk_LDL_factorize(
      int[] A, /* I/O Pointer to Symetric Square Matrix */
      int A_ptr,
      int M, /* I Size of Matrix */
      int[] L_Q16, /* I/O Pointer to Square Upper triangular Matrix */
      int[] inv_D /* I/O Pointer to vector holding inverted diagonal elements of D */
  ) {
    int i, j, k, status, loop_count;
    int[] scratch1;
    int scratch1_ptr;
    int[] scratch2;
    int scratch2_ptr;
    int diag_min_value, tmp_32, err;
    int[] v_Q0 = new int[M];
    /* SilkConstants.MAX_MATRIX_SIZE */
    int[] D_Q0 = new int[M];
    /* SilkConstants.MAX_MATRIX_SIZE */
    int one_div_diag_Q36, one_div_diag_Q40, one_div_diag_Q48;

    Inlines.OpusAssert(M <= SilkConstants.MAX_MATRIX_SIZE);

    status = 1;
    diag_min_value =
        Inlines.silk_max_32(
            Inlines.silk_SMMUL(Inlines.silk_ADD_SAT32(A[A_ptr], A[A_ptr + Inlines.silk_SMULBB(M,
                M) - 1]), ((int) ((TuningParameters.FIND_LTP_COND_FAC) * ((long) 1 << (31))
                    + 0.5))/* Inlines.SILK_CONST(TuningParameters.FIND_LTP_COND_FAC, 31) */),
            1 << 9);
    for (loop_count = 0; loop_count < M && status == 1; loop_count++) {
      status = 0;
      for (j = 0; j < M; j++) {
        scratch1 = L_Q16;
        scratch1_ptr = Inlines.MatrixGetPointer(j, 0, M);
        tmp_32 = 0;
        for (i = 0; i < j; i++) {
          v_Q0[i] = Inlines.silk_SMULWW(D_Q0[i], scratch1[scratch1_ptr + i]);
          /* Q0 */
          tmp_32 = Inlines.silk_SMLAWW(tmp_32, v_Q0[i], scratch1[scratch1_ptr + i]);
          /* Q0 */
        }
        tmp_32 = Inlines.silk_SUB32(Inlines.MatrixGet(A, A_ptr, j, j, M), tmp_32);

        if (tmp_32 < diag_min_value) {
          tmp_32 = Inlines.silk_SUB32(Inlines.silk_SMULBB(loop_count + 1, diag_min_value), tmp_32);
          /* Matrix not positive semi-definite, or ill conditioned */
          for (i = 0; i < M; i++) {
            Inlines.MatrixSet(A, A_ptr, i, i, M,
                Inlines.silk_ADD32(Inlines.MatrixGet(A, A_ptr, i, i, M), tmp_32));
          }
          status = 1;
          break;
        }
        D_Q0[j] = tmp_32;
        /* always < max(Correlation) */

        /* two-step division */
        one_div_diag_Q36 = Inlines.silk_INVERSE32_varQ(tmp_32, 36);
        /* Q36 */
        one_div_diag_Q40 = Inlines.silk_LSHIFT(one_div_diag_Q36, 4);
        /* Q40 */
        err = Inlines.silk_SUB32((int) 1 << 24, Inlines.silk_SMULWW(tmp_32, one_div_diag_Q40));
        /* Q24 */
        one_div_diag_Q48 = Inlines.silk_SMULWW(err, one_div_diag_Q40);
        /* Q48 */

        /* Save 1/Ds */
        inv_D[(j * 2) + 0] = one_div_diag_Q36;
        inv_D[(j * 2) + 1] = one_div_diag_Q48;

        Inlines.MatrixSet(L_Q16, j, j, M, 65536);
        /* 1.0 in Q16 */
        scratch1 = A;
        scratch1_ptr = Inlines.MatrixGetPointer(j, 0, M) + A_ptr;
        scratch2 = L_Q16;
        scratch2_ptr = Inlines.MatrixGetPointer(j + 1, 0, M);
        for (i = j + 1; i < M; i++) {
          tmp_32 = 0;
          for (k = 0; k < j; k++) {
            tmp_32 = Inlines.silk_SMLAWW(tmp_32, v_Q0[k], scratch2[scratch2_ptr + k]);
            /* Q0 */
          }
          tmp_32 = Inlines.silk_SUB32(scratch1[scratch1_ptr + i], tmp_32);
          /* always < max(Correlation) */

          /* tmp_32 / D_Q0[j] : Divide to Q16 */
          Inlines.MatrixSet(L_Q16, i, j, M,
              Inlines.silk_ADD32(Inlines.silk_SMMUL(tmp_32, one_div_diag_Q48),
                  Inlines.silk_RSHIFT(Inlines.silk_SMULWW(tmp_32, one_div_diag_Q36), 4)));

          /* go to next column */
          scratch2_ptr += M;
        }
      }
    }

    Inlines.OpusAssert(status == 0);
  }

  private static void silk_LS_divide_Q16(
      int[] T, /* I/O Numenator vector */
      int[] inv_D, /* I 1 / D vector */
      int M /* I dimension */
  ) {
    int i;
    int tmp_32;
    int one_div_diag_Q36, one_div_diag_Q48;

    for (i = 0; i < M; i++) {
      one_div_diag_Q36 = inv_D[(i * 2) + 0];
      one_div_diag_Q48 = inv_D[(i * 2) + 1];

      tmp_32 = T[i];
      T[i] = Inlines.silk_ADD32(Inlines.silk_SMMUL(tmp_32, one_div_diag_Q48),
          Inlines.silk_RSHIFT(Inlines.silk_SMULWW(tmp_32, one_div_diag_Q36), 4));
    }
  }

  /* Solve Lx = b, when L is lower triangular and has ones on the diagonal */
  private static void silk_LS_SolveFirst(
      int[] L_Q16, /* I Pointer to Lower Triangular Matrix */
      int M, /* I Dim of Matrix equation */
      int[] b, /* I b Vector */
      int[] x_Q16 /* O x Vector */
  ) {
    int i, j;
    int ptr32;
    int tmp_32;

    for (i = 0; i < M; i++) {
      ptr32 = Inlines.MatrixGetPointer(i, 0, M);
      tmp_32 = 0;
      for (j = 0; j < i; j++) {
        tmp_32 = Inlines.silk_SMLAWW(tmp_32, L_Q16[ptr32 + j], x_Q16[j]);
      }
      x_Q16[i] = Inlines.silk_SUB32(b[i], tmp_32);
    }
  }

  /* Solve L^t*x = b, where L is lower triangular with ones on the diagonal */
  private static void silk_LS_SolveLast(
      int[] L_Q16, /* I Pointer to Lower Triangular Matrix */
      int M, /* I Dim of Matrix equation */
      int[] b, /* I b Vector */
      int[] x_Q16 /* O x Vector */
  ) {
    int i, j;
    int ptr32;
    int tmp_32;

    for (i = M - 1; i >= 0; i--) {
      ptr32 = Inlines.MatrixGetPointer(0, i, M);
      tmp_32 = 0;
      for (j = M - 1; j > i; j--) {
        tmp_32 = Inlines.silk_SMLAWW(tmp_32, L_Q16[ptr32 + Inlines.silk_SMULBB(j, M)], x_Q16[j]);
      }
      x_Q16[i] = Inlines.silk_SUB32(b[i], tmp_32);
    }
  }
}
