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

/// <summary>
/// Structure containing NLSF codebook
/// </summary>
class NLSFCodebook {

  short nVectors = 0;

  short order = 0;

  /// <summary>
  /// Quantization step size
  /// </summary>
  short quantStepSize_Q16 = 0;

  /// <summary>
  /// Inverse quantization step size
  /// </summary>
  short invQuantStepSize_Q6 = 0;

  /// <summary>
  /// POINTER
  /// </summary>
  short[] CB1_NLSF_Q8 = null;

  /// <summary>
  /// POINTER
  /// </summary>
  short[] CB1_iCDF = null;

  /// <summary>
  /// POINTER to Backward predictor coefs [ order ]
  /// </summary>
  short[] pred_Q8 = null;

  /// <summary>
  /// POINTER to Indices to entropy coding tables [ order ]
  /// </summary>
  short[] ec_sel = null;

  /// <summary>
  /// POINTER
  /// </summary>
  short[] ec_iCDF = null;

  /// <summary>
  /// POINTER
  /// </summary>
  short[] ec_Rates_Q5 = null;

  /// <summary>
  /// POINTER
  /// </summary>
  short[] deltaMin_Q15 = null;

  void Reset() {
    nVectors = 0;
    order = 0;
    quantStepSize_Q16 = 0;
    invQuantStepSize_Q6 = 0;
    CB1_NLSF_Q8 = null;
    CB1_iCDF = null;
    pred_Q8 = null;
    ec_sel = null;
    ec_iCDF = null;
    ec_Rates_Q5 = null;
    deltaMin_Q15 = null;
  }
}
