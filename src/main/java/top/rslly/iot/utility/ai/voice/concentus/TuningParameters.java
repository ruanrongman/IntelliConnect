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

class TuningParameters {

  /* Decay time for EntropyCoder.BITREServoir */
  static final int BITRESERVOIR_DECAY_TIME_MS = 500;

  /**
   * ****************
   */
  /* Pitch estimator */
  /**
   * ****************
   */

  /* Level of noise floor for whitening filter LPC analysis in pitch analysis */
  static final float FIND_PITCH_WHITE_NOISE_FRACTION = 1e-3f;

  /* Bandwidth expansion for whitening filter in pitch analysis */
  static final float FIND_PITCH_BANDWIDTH_EXPANSION = 0.99f;

  /**
   * ******************
   */
  /* Linear prediction */
  /**
   * ******************
   */

  /* LPC analysis regularization */
  static final float FIND_LPC_COND_FAC = 1e-5f;

  /* LTP analysis defines */
  static final float FIND_LTP_COND_FAC = 1e-5f;
  static final float LTP_DAMPING = 0.05f;
  static final float LTP_SMOOTHING = 0.1f;

  /* LTP quantization settings */
  static final float MU_LTP_QUANT_NB = 0.03f;
  static final float MU_LTP_QUANT_MB = 0.025f;
  static final float MU_LTP_QUANT_WB = 0.02f;

  /* Max cumulative LTP gain */
  static final float MAX_SUM_LOG_GAIN_DB = 250.0f;

  /**
   * ********************
   */
  /* High pass filtering */
  /**
   * ********************
   */

  /* Smoothing parameters for low end of pitch frequency range estimation */
  static final float VARIABLE_HP_SMTH_COEF1 = 0.1f;
  static final float VARIABLE_HP_SMTH_COEF2 = 0.015f;
  static final float VARIABLE_HP_MAX_DELTA_FREQ = 0.4f;

  /* Min and max cut-off frequency values (-3 dB points) */
  static final int VARIABLE_HP_MIN_CUTOFF_HZ = 60;
  static final int VARIABLE_HP_MAX_CUTOFF_HZ = 100;

  /**
   * ********
   */
  /* Various */
  /**
   * ********
   */

  /* VAD threshold */
  static final float SPEECH_ACTIVITY_DTX_THRES = 0.05f;

  /* Speech Activity LBRR enable threshold */
  static final float LBRR_SPEECH_ACTIVITY_THRES = 0.3f;

  /**
   * **********************
   */
  /* Perceptual parameters */
  /**
   * **********************
   */

  /* reduction in coding SNR during low speech activity */
  static final float BG_SNR_DECR_dB = 2.0f;

  /* factor for reducing quantization noise during voiced speech */
  static final float HARM_SNR_INCR_dB = 2.0f;

  /* factor for reducing quantization noise for unvoiced sparse signals */
  static final float SPARSE_SNR_INCR_dB = 2.0f;

  /*
   * threshold for sparseness measure above which to use lower quantization offset during unvoiced
   */
  static final float SPARSENESS_THRESHOLD_QNT_OFFSET = 0.75f;

  /* warping control */
  static final float WARPING_MULTIPLIER = 0.015f;

  /* fraction added to first autocorrelation value */
  static final float SHAPE_WHITE_NOISE_FRACTION = 5e-5f;

  /* noise shaping filter chirp factor */
  static final float BANDWIDTH_EXPANSION = 0.95f;

  /*
   * difference between chirp factors for analysis and synthesis noise shaping filters at low
   * bitrates
   */
  static final float LOW_RATE_BANDWIDTH_EXPANSION_DELTA = 0.01f;

  /* extra harmonic boosting (signal shaping) at low bitrates */
  static final float LOW_RATE_HARMONIC_BOOST = 0.1f;

  /* extra harmonic boosting (signal shaping) for noisy input signals */
  static final float LOW_INPUT_QUALITY_HARMONIC_BOOST = 0.1f;

  /* harmonic noise shaping */
  static final float HARMONIC_SHAPING = 0.3f;

  /* extra harmonic noise shaping for high bitrates or noisy input */
  static final float HIGH_RATE_OR_LOW_QUALITY_HARMONIC_SHAPING = 0.2f;

  /* parameter for shaping noise towards higher frequencies */
  static final float HP_NOISE_COEF = 0.25f;

  /* parameter for shaping noise even more towards higher frequencies during voiced speech */
  static final float HARM_HP_NOISE_COEF = 0.35f;

  /* parameter for applying a high-pass tilt to the input signal */
  static final float INPUT_TILT = 0.05f;

  /* parameter for extra high-pass tilt to the input signal at high rates */
  static final float HIGH_RATE_INPUT_TILT = 0.1f;

  /* parameter for reducing noise at the very low frequencies */
  static final float LOW_FREQ_SHAPING = 4.0f;

  /*
   * less reduction of noise at the very low frequencies for signals with low SNR at low frequencies
   */
  static final float LOW_QUALITY_LOW_FREQ_SHAPING_DECR = 0.5f;

  /* subframe smoothing coefficient for HarmBoost, HarmShapeGain, Tilt (lower . more smoothing) */
  static final float SUBFR_SMTH_COEF = 0.4f;

  /* parameters defining the R/D tradeoff in the residual quantizer */
  static final float LAMBDA_OFFSET = 1.2f;
  static final float LAMBDA_SPEECH_ACT = -0.2f;
  static final float LAMBDA_DELAYED_DECISIONS = -0.05f;
  static final float LAMBDA_INPUT_QUALITY = -0.1f;
  static final float LAMBDA_CODING_QUALITY = -0.2f;
  static final float LAMBDA_QUANT_OFFSET = 0.8f;

  /* Compensation in bitrate calculations for 10 ms modes */
  static final int REDUCE_BITRATE_10_MS_BPS = 2200;

  /* Maximum time before allowing a bandwidth transition */
  static final int MAX_BANDWIDTH_SWITCH_DELAY_MS = 5000;
}
