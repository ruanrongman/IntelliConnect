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
/// Encoder state
/// </summary>
class SilkChannelEncoder {

  final int[] In_HP_State = new int[2];
  /* High pass filter state */
  int variable_HP_smth1_Q15 = 0;
  /* State of first smoother */
  int variable_HP_smth2_Q15 = 0;
  /* State of second smoother */
  final SilkLPState sLP = new SilkLPState();
  /* Low pass filter state */
  final SilkVADState sVAD = new SilkVADState();
  /* Voice activity detector state */
  final SilkNSQState sNSQ = new SilkNSQState();
  /* Noise Shape Quantizer State */
  final short[] prev_NLSFq_Q15 = new short[SilkConstants.MAX_LPC_ORDER];
  /* Previously quantized NLSF vector */
  int speech_activity_Q8 = 0;
  /* Speech activity */
  int allow_bandwidth_switch = 0;
  /* Flag indicating that switching of bandwidth is allowed */
  byte LBRRprevLastGainIndex = 0;
  byte prevSignalType = 0;
  int prevLag = 0;
  int pitch_LPC_win_length = 0;
  int max_pitch_lag = 0;
  /* Highest possible pitch lag (samples) */
  int API_fs_Hz = 0;
  /* API sampling frequency (Hz) */
  int prev_API_fs_Hz = 0;
  /* Previous API sampling frequency (Hz) */
  int maxInternal_fs_Hz = 0;
  /* Maximum sampling frequency (Hz) */
  int minInternal_fs_Hz = 0;
  /* Minimum sampling frequency (Hz) */
  int desiredInternal_fs_Hz = 0;
  /* Soft request for sampling frequency (Hz) */
  int fs_kHz = 0;
  /* Internal sampling frequency (kHz) */
  int nb_subfr = 0;
  /* Number of 5 ms subframes in a frame */
  int frame_length = 0;
  /* Frame length (samples) */
  int subfr_length = 0;
  /* Subframe length (samples) */
  int ltp_mem_length = 0;
  /* Length of LTP memory */
  int la_pitch = 0;
  /* Look-ahead for pitch analysis (samples) */
  int la_shape = 0;
  /* Look-ahead for noise shape analysis (samples) */
  int shapeWinLength = 0;
  /* Window length for noise shape analysis (samples) */
  int TargetRate_bps = 0;
  /* Target bitrate (bps) */
  int PacketSize_ms = 0;
  /* Number of milliseconds to put in each packet */
  int PacketLoss_perc = 0;
  /* Packet loss rate measured by farend */
  int frameCounter = 0;
  int Complexity = 0;
  /* Complexity setting */
  int nStatesDelayedDecision = 0;
  /* Number of states in delayed decision quantization */
  int useInterpolatedNLSFs = 0;
  /* Flag for using NLSF interpolation */
  int shapingLPCOrder = 0;
  /* Filter order for noise shaping filters */
  int predictLPCOrder = 0;
  /* Filter order for prediction filters */
  int pitchEstimationComplexity = 0;
  /* Complexity level for pitch estimator */
  int pitchEstimationLPCOrder = 0;
  /* Whitening filter order for pitch estimator */
  int pitchEstimationThreshold_Q16 = 0;
  /* Threshold for pitch estimator */
  int LTPQuantLowComplexity = 0;
  /* Flag for low complexity LTP quantization */
  int mu_LTP_Q9 = 0;
  /* Rate-distortion tradeoff in LTP quantization */
  int sum_log_gain_Q7 = 0;
  /* Cumulative max prediction gain */
  int NLSF_MSVQ_Survivors = 0;
  /* Number of survivors in NLSF MSVQ */
  int first_frame_after_reset = 0;
  /* Flag for deactivating NLSF interpolation, pitch prediction */
  int controlled_since_last_payload = 0;
  /* Flag for ensuring codec_control only runs once per packet */
  int warping_Q16 = 0;
  /* Warping parameter for warped noise shaping */
  int useCBR = 0;
  /* Flag to enable constant bitrate */
  int prefillFlag = 0;
  /* Flag to indicate that only buffers are prefilled, no coding */
  short[] pitch_lag_low_bits_iCDF = null;
  /* Pointer to iCDF table for low bits of pitch lag index */
  short[] pitch_contour_iCDF = null;
  /* Pointer to iCDF table for pitch contour index */
  NLSFCodebook psNLSF_CB = null;
  /* Pointer to NLSF codebook */
  final int[] input_quality_bands_Q15 = new int[SilkConstants.VAD_N_BANDS];
  int input_tilt_Q15 = 0;
  int SNR_dB_Q7 = 0;
  /* Quality setting */

  final byte[] VAD_flags = new byte[SilkConstants.MAX_FRAMES_PER_PACKET];
  byte LBRR_flag = 0;
  final int[] LBRR_flags = new int[SilkConstants.MAX_FRAMES_PER_PACKET];

  final SideInfoIndices indices = new SideInfoIndices();
  final byte[] pulses = new byte[SilkConstants.MAX_FRAME_LENGTH];

  /* Input/output buffering */
  final short[] inputBuf = new short[SilkConstants.MAX_FRAME_LENGTH + 2];
  /* Buffer containing input signal */
  int inputBufIx = 0;
  int nFramesPerPacket = 0;
  int nFramesEncoded = 0;
  /* Number of frames analyzed in current packet */

  int nChannelsAPI = 0;
  int nChannelsInternal = 0;
  int channelNb = 0;

  /* Parameters For LTP scaling Control */
  int frames_since_onset = 0;

  /* Specifically for entropy coding */
  int ec_prevSignalType = 0;
  short ec_prevLagIndex = 0;

  final SilkResamplerState resampler_state = new SilkResamplerState();

  /* DTX */
  int useDTX = 0;
  /* Flag to enable DTX */
  int inDTX = 0;
  /* Flag to signal DTX period */
  int noSpeechCounter = 0;
  /* Counts concecutive nonactive frames, used by DTX */

  /* Inband Low Bitrate Redundancy (LBRR) data */
  int useInBandFEC = 0;
  /* Saves the API setting for query */
  int LBRR_enabled = 0;
  /* Depends on useInBandFRC, bitrate and packet loss rate */
  int LBRR_GainIncreases = 0;
  /* Gains increment for coding LBRR frames */
  final SideInfoIndices[] indices_LBRR = new SideInfoIndices[SilkConstants.MAX_FRAMES_PER_PACKET];
  final byte[][] pulses_LBRR = Arrays.InitTwoDimensionalArrayByte(
      SilkConstants.MAX_FRAMES_PER_PACKET, SilkConstants.MAX_FRAME_LENGTH);

  /* Noise shaping state */
  final SilkShapeState sShape = new SilkShapeState();

  /* Prefilter State */
  final SilkPrefilterState sPrefilt = new SilkPrefilterState();

  /* Buffer for find pitch and noise shape analysis */
  final short[] x_buf = new short[2 * SilkConstants.MAX_FRAME_LENGTH + SilkConstants.LA_SHAPE_MAX];

  /* Normalized correlation from pitch lag estimator */
  int LTPCorr_Q15 = 0;

  SilkChannelEncoder() {
    for (int c = 0; c < SilkConstants.MAX_FRAMES_PER_PACKET; c++) {
      indices_LBRR[c] = new SideInfoIndices();
    }
  }

  void Reset() {
    Arrays.MemSet(In_HP_State, 0, 2);
    variable_HP_smth1_Q15 = 0;
    variable_HP_smth2_Q15 = 0;
    sLP.Reset();
    sVAD.Reset();
    sNSQ.Reset();
    Arrays.MemSet(prev_NLSFq_Q15, (short) 0, SilkConstants.MAX_LPC_ORDER);
    speech_activity_Q8 = 0;
    allow_bandwidth_switch = 0;
    LBRRprevLastGainIndex = 0;
    prevSignalType = 0;
    prevLag = 0;
    pitch_LPC_win_length = 0;
    max_pitch_lag = 0;
    API_fs_Hz = 0;
    prev_API_fs_Hz = 0;
    maxInternal_fs_Hz = 0;
    minInternal_fs_Hz = 0;
    desiredInternal_fs_Hz = 0;
    fs_kHz = 0;
    nb_subfr = 0;
    frame_length = 0;
    subfr_length = 0;
    ltp_mem_length = 0;
    la_pitch = 0;
    la_shape = 0;
    shapeWinLength = 0;
    TargetRate_bps = 0;
    PacketSize_ms = 0;
    PacketLoss_perc = 0;
    frameCounter = 0;
    Complexity = 0;
    nStatesDelayedDecision = 0;
    useInterpolatedNLSFs = 0;
    shapingLPCOrder = 0;
    predictLPCOrder = 0;
    pitchEstimationComplexity = 0;
    pitchEstimationLPCOrder = 0;
    pitchEstimationThreshold_Q16 = 0;
    LTPQuantLowComplexity = 0;
    mu_LTP_Q9 = 0;
    sum_log_gain_Q7 = 0;
    NLSF_MSVQ_Survivors = 0;
    first_frame_after_reset = 0;
    controlled_since_last_payload = 0;
    warping_Q16 = 0;
    useCBR = 0;
    prefillFlag = 0;
    pitch_lag_low_bits_iCDF = null;
    pitch_contour_iCDF = null;
    psNLSF_CB = null;
    Arrays.MemSet(input_quality_bands_Q15, 0, SilkConstants.VAD_N_BANDS);
    input_tilt_Q15 = 0;
    SNR_dB_Q7 = 0;
    Arrays.MemSet(VAD_flags, (byte) 0, SilkConstants.MAX_FRAMES_PER_PACKET);
    LBRR_flag = 0;
    Arrays.MemSet(LBRR_flags, 0, SilkConstants.MAX_FRAMES_PER_PACKET);
    indices.Reset();
    Arrays.MemSet(pulses, (byte) 0, SilkConstants.MAX_FRAME_LENGTH);
    Arrays.MemSet(inputBuf, (short) 0, SilkConstants.MAX_FRAME_LENGTH + 2);
    inputBufIx = 0;
    nFramesPerPacket = 0;
    nFramesEncoded = 0;
    nChannelsAPI = 0;
    nChannelsInternal = 0;
    channelNb = 0;
    frames_since_onset = 0;
    ec_prevSignalType = 0;
    ec_prevLagIndex = 0;
    resampler_state.Reset();
    useDTX = 0;
    inDTX = 0;
    noSpeechCounter = 0;
    useInBandFEC = 0;
    LBRR_enabled = 0;
    LBRR_GainIncreases = 0;
    for (int c = 0; c < SilkConstants.MAX_FRAMES_PER_PACKET; c++) {
      indices_LBRR[c].Reset();
      Arrays.MemSet(pulses_LBRR[c], (byte) 0, SilkConstants.MAX_FRAME_LENGTH);
    }
    sShape.Reset();
    sPrefilt.Reset();
    Arrays.MemSet(x_buf, (short) 0,
        2 * SilkConstants.MAX_FRAME_LENGTH + SilkConstants.LA_SHAPE_MAX);
    LTPCorr_Q15 = 0;
  }

  /// <summary>
  /// Control encoder
  /// </summary>
  /// <param name="this">I/O Pointer to Silk encoder state</param>
  /// <param name="encControl">I Control structure</param>
  /// <param name="TargetRate_bps">I Target max bitrate (bps)</param>
  /// <param name="allow_bw_switch">I Flag to allow switching audio bandwidth</param>
  /// <param name="channelNb">I Channel number</param>
  /// <param name="force_fs_kHz"></param>
  /// <returns></returns>
  int silk_control_encoder(
      EncControlState encControl,
      int TargetRate_bps,
      int allow_bw_switch,
      int channelNb,
      int force_fs_kHz) {
    int fs_kHz;
    int ret = SilkError.SILK_NO_ERROR;

    this.useDTX = encControl.useDTX;
    this.useCBR = encControl.useCBR;
    this.API_fs_Hz = encControl.API_sampleRate;
    this.maxInternal_fs_Hz = encControl.maxInternalSampleRate;
    this.minInternal_fs_Hz = encControl.minInternalSampleRate;
    this.desiredInternal_fs_Hz = encControl.desiredInternalSampleRate;
    this.useInBandFEC = encControl.useInBandFEC;
    this.nChannelsAPI = encControl.nChannelsAPI;
    this.nChannelsInternal = encControl.nChannelsInternal;
    this.allow_bandwidth_switch = allow_bw_switch;
    this.channelNb = channelNb;

    if (this.controlled_since_last_payload != 0 && this.prefillFlag == 0) {
      if (this.API_fs_Hz != this.prev_API_fs_Hz && this.fs_kHz > 0) {
        /* Change in API sampling rate in the middle of encoding a packet */
        ret = silk_setup_resamplers(this.fs_kHz);
      }
      return ret;
    }

    /* Beyond this point we know that there are no previously coded frames in the payload buffer */
    /**
     * *****************************************
     */
    /* Determine sampling rate */
    /**
     * *****************************************
     */
    fs_kHz = silk_control_audio_bandwidth(encControl);
    if (force_fs_kHz != 0) {
      fs_kHz = force_fs_kHz;
    }
    /**
     * *****************************************
     */
    /* Prepare resampler and buffered data */
    /**
     * *****************************************
     */
    ret = silk_setup_resamplers(fs_kHz);

    /**
     * *****************************************
     */
    /* Set sampling frequency */
    /**
     * *****************************************
     */
    ret = silk_setup_fs(fs_kHz, encControl.payloadSize_ms);

    /**
     * *****************************************
     */
    /* Set encoding complexity */
    /**
     * *****************************************
     */
    ret = silk_setup_complexity(encControl.complexity);

    /**
     * *****************************************
     */
    /* Set packet loss rate measured by farend */
    /**
     * *****************************************
     */
    this.PacketLoss_perc = encControl.packetLossPercentage;

    /**
     * *****************************************
     */
    /* Set LBRR usage */
    /**
     * *****************************************
     */
    ret = silk_setup_LBRR(TargetRate_bps);

    this.controlled_since_last_payload = 1;

    return ret;
  }

  /// <summary>
  ///
  /// </summary>
  /// <param name="this">I/O</param>
  /// <param name="fs_kHz">I</param>
  /// <returns></returns>
  private int silk_setup_resamplers(int fs_kHz) {
    int ret = 0;

    if (this.fs_kHz != fs_kHz || this.prev_API_fs_Hz != this.API_fs_Hz) {
      if (this.fs_kHz == 0) {
        /* Initialize the resampler for enc_API.c preparing resampling from API_fs_Hz to fs_kHz */
        ret +=
            Resampler.silk_resampler_init(this.resampler_state, this.API_fs_Hz, fs_kHz * 1000, 1);
      } else {
        short[] x_buf_API_fs_Hz;
        SilkResamplerState temp_resampler_state = null;

        int api_buf_samples;
        int old_buf_samples;
        int buf_length_ms;

        buf_length_ms = Inlines.silk_LSHIFT(this.nb_subfr * 5, 1) + SilkConstants.LA_SHAPE_MS;
        old_buf_samples = buf_length_ms * this.fs_kHz;

        /* Initialize resampler for temporary resampling of x_buf data to API_fs_Hz */
        temp_resampler_state = new SilkResamplerState();
        ret += Resampler.silk_resampler_init(temp_resampler_state,
            Inlines.silk_SMULBB(this.fs_kHz, 1000), this.API_fs_Hz, 0);

        /* Calculate number of samples to temporarily upsample */
        api_buf_samples = buf_length_ms * Inlines.silk_DIV32_16(this.API_fs_Hz, 1000);

        /* Temporary resampling of x_buf data to API_fs_Hz */
        x_buf_API_fs_Hz = new short[api_buf_samples];
        ret += Resampler.silk_resampler(temp_resampler_state, x_buf_API_fs_Hz, 0, this.x_buf, 0,
            old_buf_samples);

        /* Initialize the resampler for enc_API.c preparing resampling from API_fs_Hz to fs_kHz */
        ret += Resampler.silk_resampler_init(this.resampler_state, this.API_fs_Hz,
            Inlines.silk_SMULBB(fs_kHz, 1000), 1);

        /* Correct resampler state by resampling buffered data from API_fs_Hz to fs_kHz */
        ret += Resampler.silk_resampler(this.resampler_state, this.x_buf, 0, x_buf_API_fs_Hz, 0,
            api_buf_samples);
      }
    }

    this.prev_API_fs_Hz = this.API_fs_Hz;

    return ret;
  }

  /// <summary>
  ///
  /// </summary>
  /// <param name="this">I/O</param>
  /// <param name="fs_kHz">I</param>
  /// <param name="PacketSize_ms">I</param>
  /// <returns></returns>
  private int silk_setup_fs(
      int fs_kHz,
      int PacketSize_ms) {
    int ret = SilkError.SILK_NO_ERROR;

    /* Set packet size */
    if (PacketSize_ms != this.PacketSize_ms) {
      if ((PacketSize_ms != 10)
          && (PacketSize_ms != 20)
          && (PacketSize_ms != 40)
          && (PacketSize_ms != 60)) {
        ret = SilkError.SILK_ENC_PACKET_SIZE_NOT_SUPPORTED;
      }
      if (PacketSize_ms <= 10) {
        this.nFramesPerPacket = 1;
        this.nb_subfr = PacketSize_ms == 10 ? 2 : 1;
        this.frame_length = Inlines.silk_SMULBB(PacketSize_ms, fs_kHz);
        this.pitch_LPC_win_length =
            Inlines.silk_SMULBB(SilkConstants.FIND_PITCH_LPC_WIN_MS_2_SF, fs_kHz);
        if (this.fs_kHz == 8) {
          this.pitch_contour_iCDF = SilkTables.silk_pitch_contour_10_ms_NB_iCDF;
        } else {
          this.pitch_contour_iCDF = SilkTables.silk_pitch_contour_10_ms_iCDF;
        }
      } else {
        this.nFramesPerPacket =
            Inlines.silk_DIV32_16(PacketSize_ms, SilkConstants.MAX_FRAME_LENGTH_MS);
        this.nb_subfr = SilkConstants.MAX_NB_SUBFR;
        this.frame_length = Inlines.silk_SMULBB(20, fs_kHz);
        this.pitch_LPC_win_length =
            Inlines.silk_SMULBB(SilkConstants.FIND_PITCH_LPC_WIN_MS, fs_kHz);
        if (this.fs_kHz == 8) {
          this.pitch_contour_iCDF = SilkTables.silk_pitch_contour_NB_iCDF;
        } else {
          this.pitch_contour_iCDF = SilkTables.silk_pitch_contour_iCDF;
        }
      }
      this.PacketSize_ms = PacketSize_ms;
      this.TargetRate_bps = 0;
      /* trigger new SNR computation */
    }

    /* Set sampling frequency */
    Inlines.OpusAssert(fs_kHz == 8 || fs_kHz == 12 || fs_kHz == 16);
    Inlines.OpusAssert(this.nb_subfr == 2 || this.nb_subfr == 4);
    if (this.fs_kHz != fs_kHz) {
      /* reset part of the state */
      this.sShape.Reset();
      this.sPrefilt.Reset();
      this.sNSQ.Reset();
      Arrays.MemSet(this.prev_NLSFq_Q15, (short) 0, SilkConstants.MAX_LPC_ORDER);
      Arrays.MemSet(this.sLP.In_LP_State, 0, 2);
      this.inputBufIx = 0;
      this.nFramesEncoded = 0;
      this.TargetRate_bps = 0;
      /* trigger new SNR computation */

      /* Initialize non-zero parameters */
      this.prevLag = 100;
      this.first_frame_after_reset = 1;
      this.sPrefilt.lagPrev = 100;
      this.sShape.LastGainIndex = 10;
      this.sNSQ.lagPrev = 100;
      this.sNSQ.prev_gain_Q16 = 65536;
      this.prevSignalType = SilkConstants.TYPE_NO_VOICE_ACTIVITY;

      this.fs_kHz = fs_kHz;
      if (this.fs_kHz == 8) {
        if (this.nb_subfr == SilkConstants.MAX_NB_SUBFR) {
          this.pitch_contour_iCDF = SilkTables.silk_pitch_contour_NB_iCDF;
        } else {
          this.pitch_contour_iCDF = SilkTables.silk_pitch_contour_10_ms_NB_iCDF;
        }
      } else if (this.nb_subfr == SilkConstants.MAX_NB_SUBFR) {
        this.pitch_contour_iCDF = SilkTables.silk_pitch_contour_iCDF;
      } else {
        this.pitch_contour_iCDF = SilkTables.silk_pitch_contour_10_ms_iCDF;
      }

      if (this.fs_kHz == 8 || this.fs_kHz == 12) {
        this.predictLPCOrder = SilkConstants.MIN_LPC_ORDER;
        this.psNLSF_CB = SilkTables.silk_NLSF_CB_NB_MB;
      } else {
        this.predictLPCOrder = SilkConstants.MAX_LPC_ORDER;
        this.psNLSF_CB = SilkTables.silk_NLSF_CB_WB;
      }

      this.subfr_length = SilkConstants.SUB_FRAME_LENGTH_MS * fs_kHz;
      this.frame_length = Inlines.silk_SMULBB(this.subfr_length, this.nb_subfr);
      this.ltp_mem_length = Inlines.silk_SMULBB(SilkConstants.LTP_MEM_LENGTH_MS, fs_kHz);
      this.la_pitch = Inlines.silk_SMULBB(SilkConstants.LA_PITCH_MS, fs_kHz);
      this.max_pitch_lag = Inlines.silk_SMULBB(18, fs_kHz);

      if (this.nb_subfr == SilkConstants.MAX_NB_SUBFR) {
        this.pitch_LPC_win_length =
            Inlines.silk_SMULBB(SilkConstants.FIND_PITCH_LPC_WIN_MS, fs_kHz);
      } else {
        this.pitch_LPC_win_length =
            Inlines.silk_SMULBB(SilkConstants.FIND_PITCH_LPC_WIN_MS_2_SF, fs_kHz);
      }

      if (this.fs_kHz == 16) {
        this.mu_LTP_Q9 = ((int) ((TuningParameters.MU_LTP_QUANT_WB) * ((long) 1 << (9)) + 0.5))/*
                                                                                                * Inlines
                                                                                                * .
                                                                                                * SILK_CONST
                                                                                                * (
                                                                                                * TuningParameters
                                                                                                * .
                                                                                                * MU_LTP_QUANT_WB,
                                                                                                * 9)
                                                                                                */;
        this.pitch_lag_low_bits_iCDF = SilkTables.silk_uniform8_iCDF;
      } else if (this.fs_kHz == 12) {
        this.mu_LTP_Q9 = ((int) ((TuningParameters.MU_LTP_QUANT_MB) * ((long) 1 << (9)) + 0.5))/*
                                                                                                * Inlines
                                                                                                * .
                                                                                                * SILK_CONST
                                                                                                * (
                                                                                                * TuningParameters
                                                                                                * .
                                                                                                * MU_LTP_QUANT_MB,
                                                                                                * 9)
                                                                                                */;
        this.pitch_lag_low_bits_iCDF = SilkTables.silk_uniform6_iCDF;
      } else {
        this.mu_LTP_Q9 = ((int) ((TuningParameters.MU_LTP_QUANT_NB) * ((long) 1 << (9)) + 0.5))/*
                                                                                                * Inlines
                                                                                                * .
                                                                                                * SILK_CONST
                                                                                                * (
                                                                                                * TuningParameters
                                                                                                * .
                                                                                                * MU_LTP_QUANT_NB,
                                                                                                * 9)
                                                                                                */;
        this.pitch_lag_low_bits_iCDF = SilkTables.silk_uniform4_iCDF;
      }
    }

    /* Check that settings are valid */
    Inlines.OpusAssert((this.subfr_length * this.nb_subfr) == this.frame_length);

    return ret;
  }

  /// <summary>
  ///
  /// </summary>
  /// <param name="this">I/O</param>
  /// <param name="Complexity">O</param>
  /// <returns></returns>
  private int silk_setup_complexity(int Complexity) {
    int ret = 0;

    /* Set encoding complexity */
    Inlines.OpusAssert(Complexity >= 0 && Complexity <= 10);
    if (Complexity < 2) {
      this.pitchEstimationComplexity = SilkConstants.SILK_PE_MIN_COMPLEX;
      this.pitchEstimationThreshold_Q16 = ((int) ((0.8f) * ((long) 1 << (16)) + 0.5))/*
                                                                                      * Inlines.
                                                                                      * SILK_CONST(0
                                                                                      * .8f, 16)
                                                                                      */;
      this.pitchEstimationLPCOrder = 6;
      this.shapingLPCOrder = 8;
      this.la_shape = 3 * this.fs_kHz;
      this.nStatesDelayedDecision = 1;
      this.useInterpolatedNLSFs = 0;
      this.LTPQuantLowComplexity = 1;
      this.NLSF_MSVQ_Survivors = 2;
      this.warping_Q16 = 0;
    } else if (Complexity < 4) {
      this.pitchEstimationComplexity = SilkConstants.SILK_PE_MID_COMPLEX;
      this.pitchEstimationThreshold_Q16 = ((int) ((0.76f) * ((long) 1 << (16)) + 0.5))/*
                                                                                       * Inlines.
                                                                                       * SILK_CONST(
                                                                                       * 0.76f, 16)
                                                                                       */;
      this.pitchEstimationLPCOrder = 8;
      this.shapingLPCOrder = 10;
      this.la_shape = 5 * this.fs_kHz;
      this.nStatesDelayedDecision = 1;
      this.useInterpolatedNLSFs = 0;
      this.LTPQuantLowComplexity = 0;
      this.NLSF_MSVQ_Survivors = 4;
      this.warping_Q16 = 0;
    } else if (Complexity < 6) {
      this.pitchEstimationComplexity = SilkConstants.SILK_PE_MID_COMPLEX;
      this.pitchEstimationThreshold_Q16 = ((int) ((0.74f) * ((long) 1 << (16)) + 0.5))/*
                                                                                       * Inlines.
                                                                                       * SILK_CONST(
                                                                                       * 0.74f, 16)
                                                                                       */;
      this.pitchEstimationLPCOrder = 10;
      this.shapingLPCOrder = 12;
      this.la_shape = 5 * this.fs_kHz;
      this.nStatesDelayedDecision = 2;
      this.useInterpolatedNLSFs = 1;
      this.LTPQuantLowComplexity = 0;
      this.NLSF_MSVQ_Survivors = 8;
      this.warping_Q16 =
          this.fs_kHz * ((int) ((TuningParameters.WARPING_MULTIPLIER) * ((long) 1 << (16))
              + 0.5))/* Inlines.SILK_CONST(TuningParameters.WARPING_MULTIPLIER, 16) */;
    } else if (Complexity < 8) {
      this.pitchEstimationComplexity = SilkConstants.SILK_PE_MID_COMPLEX;
      this.pitchEstimationThreshold_Q16 = ((int) ((0.72f) * ((long) 1 << (16)) + 0.5))/*
                                                                                       * Inlines.
                                                                                       * SILK_CONST(
                                                                                       * 0.72f, 16)
                                                                                       */;
      this.pitchEstimationLPCOrder = 12;
      this.shapingLPCOrder = 14;
      this.la_shape = 5 * this.fs_kHz;
      this.nStatesDelayedDecision = 3;
      this.useInterpolatedNLSFs = 1;
      this.LTPQuantLowComplexity = 0;
      this.NLSF_MSVQ_Survivors = 16;
      this.warping_Q16 =
          this.fs_kHz * ((int) ((TuningParameters.WARPING_MULTIPLIER) * ((long) 1 << (16))
              + 0.5))/* Inlines.SILK_CONST(TuningParameters.WARPING_MULTIPLIER, 16) */;
    } else {
      this.pitchEstimationComplexity = SilkConstants.SILK_PE_MAX_COMPLEX;
      this.pitchEstimationThreshold_Q16 = ((int) ((0.7f) * ((long) 1 << (16)) + 0.5))/*
                                                                                      * Inlines.
                                                                                      * SILK_CONST(0
                                                                                      * .7f, 16)
                                                                                      */;
      this.pitchEstimationLPCOrder = 16;
      this.shapingLPCOrder = 16;
      this.la_shape = 5 * this.fs_kHz;
      this.nStatesDelayedDecision = SilkConstants.MAX_DEL_DEC_STATES;
      this.useInterpolatedNLSFs = 1;
      this.LTPQuantLowComplexity = 0;
      this.NLSF_MSVQ_Survivors = 32;
      this.warping_Q16 =
          this.fs_kHz * ((int) ((TuningParameters.WARPING_MULTIPLIER) * ((long) 1 << (16))
              + 0.5))/* Inlines.SILK_CONST(TuningParameters.WARPING_MULTIPLIER, 16) */;
    }

    /* Do not allow higher pitch estimation LPC order than predict LPC order */
    this.pitchEstimationLPCOrder =
        Inlines.silk_min_int(this.pitchEstimationLPCOrder, this.predictLPCOrder);
    this.shapeWinLength = SilkConstants.SUB_FRAME_LENGTH_MS * this.fs_kHz + 2 * this.la_shape;
    this.Complexity = Complexity;

    Inlines.OpusAssert(this.pitchEstimationLPCOrder <= SilkConstants.MAX_FIND_PITCH_LPC_ORDER);
    Inlines.OpusAssert(this.shapingLPCOrder <= SilkConstants.MAX_SHAPE_LPC_ORDER);
    Inlines.OpusAssert(this.nStatesDelayedDecision <= SilkConstants.MAX_DEL_DEC_STATES);
    Inlines.OpusAssert(this.warping_Q16 <= 32767);
    Inlines.OpusAssert(this.la_shape <= SilkConstants.LA_SHAPE_MAX);
    Inlines.OpusAssert(this.shapeWinLength <= SilkConstants.SHAPE_LPC_WIN_MAX);
    Inlines.OpusAssert(this.NLSF_MSVQ_Survivors <= SilkConstants.NLSF_VQ_MAX_SURVIVORS);

    return ret;
  }

  /// <summary>
  ///
  /// </summary>
  /// <param name="this">I/O</param>
  /// <param name="TargetRate_bps">I</param>
  /// <returns></returns>
  private int silk_setup_LBRR(int TargetRate_bps) {
    int LBRR_in_previous_packet;
    int ret = SilkError.SILK_NO_ERROR;
    int LBRR_rate_thres_bps;

    LBRR_in_previous_packet = this.LBRR_enabled;
    this.LBRR_enabled = 0;
    if (this.useInBandFEC != 0 && this.PacketLoss_perc > 0) {
      if (this.fs_kHz == 8) {
        LBRR_rate_thres_bps = SilkConstants.LBRR_NB_MIN_RATE_BPS;
      } else if (this.fs_kHz == 12) {
        LBRR_rate_thres_bps = SilkConstants.LBRR_MB_MIN_RATE_BPS;
      } else {
        LBRR_rate_thres_bps = SilkConstants.LBRR_WB_MIN_RATE_BPS;
      }

      LBRR_rate_thres_bps = Inlines.silk_SMULWB(
          Inlines.silk_MUL(LBRR_rate_thres_bps, 125 - Inlines.silk_min(this.PacketLoss_perc, 25)),
          ((int) ((0.01f) * ((long) 1 << (16)) + 0.5))/* Inlines.SILK_CONST(0.01f, 16) */);

      if (TargetRate_bps > LBRR_rate_thres_bps) {
        /* Set gain increase for coding LBRR excitation */
        if (LBRR_in_previous_packet == 0) {
          /* Previous packet did not have LBRR, and was therefore coded at a higher bitrate */
          this.LBRR_GainIncreases = 7;
        } else {
          this.LBRR_GainIncreases = Inlines.silk_max_int(
              7 - Inlines.silk_SMULWB((int) this.PacketLoss_perc,
                  ((int) ((0.4f) * ((long) 1 << (16)) + 0.5))/* Inlines.SILK_CONST(0.4f, 16) */),
              2);
        }
        this.LBRR_enabled = 1;
      }
    }

    return ret;
  }

  /// <summary>
  /// Control sampling rate
  /// </summary>
  /// <param name="this">I/O Pointer to Silk encoder state</param>
  /// <param name="encControl">I Control structure</param>
  /// <returns></returns>
  int silk_control_audio_bandwidth(EncControlState encControl) {
    int fs_kHz;
    int fs_Hz;

    fs_kHz = this.fs_kHz;
    fs_Hz = Inlines.silk_SMULBB(fs_kHz, 1000);

    if (fs_Hz == 0) {
      /* Encoder has just been initialized */
      fs_Hz = Inlines.silk_min(this.desiredInternal_fs_Hz, this.API_fs_Hz);
      fs_kHz = Inlines.silk_DIV32_16(fs_Hz, 1000);
    } else if (fs_Hz > this.API_fs_Hz || fs_Hz > this.maxInternal_fs_Hz
        || fs_Hz < this.minInternal_fs_Hz) {
      /*
       * Make sure rate is not higher than external rate or maximum allowed, or lower than minimum
       * allowed
       */
      fs_Hz = this.API_fs_Hz;
      fs_Hz = Inlines.silk_min(fs_Hz, this.maxInternal_fs_Hz);
      fs_Hz = Inlines.silk_max(fs_Hz, this.minInternal_fs_Hz);
      fs_kHz = Inlines.silk_DIV32_16(fs_Hz, 1000);
    } else {
      /* State machine for the sampling rate switching */
      if (this.sLP.transition_frame_no >= SilkConstants.TRANSITION_FRAMES) {
        /* Stop transition phase */
        this.sLP.mode = 0;
      }

      if (this.allow_bandwidth_switch != 0 || encControl.opusCanSwitch != 0) {
        /* Check if we should switch down */
        if (Inlines.silk_SMULBB(this.fs_kHz, 1000) > this.desiredInternal_fs_Hz) {
          /* Switch down */
          if (this.sLP.mode == 0) {
            /* New transition */
            this.sLP.transition_frame_no = SilkConstants.TRANSITION_FRAMES;

            /* Reset transition filter state */
            Arrays.MemSet(this.sLP.In_LP_State, 0, 2);
          }

          if (encControl.opusCanSwitch != 0) {
            /* Stop transition phase */
            this.sLP.mode = 0;

            /* Switch to a lower sample frequency */
            fs_kHz = this.fs_kHz == 16 ? 12 : 8;
          } else if (this.sLP.transition_frame_no <= 0) {
            encControl.switchReady = 1;
            /* Make room for redundancy */
            encControl.maxBits -= encControl.maxBits * 5 / (encControl.payloadSize_ms + 5);
          } else {
            /* Direction: down (at double speed) */
            this.sLP.mode = -2;
          }
        } else /* Check if we should switch up */ if (Inlines.silk_SMULBB(this.fs_kHz,
            1000) < this.desiredInternal_fs_Hz) {
          /* Switch up */
          if (encControl.opusCanSwitch != 0) {
            /* Switch to a higher sample frequency */
            fs_kHz = this.fs_kHz == 8 ? 12 : 16;

            /* New transition */
            this.sLP.transition_frame_no = 0;

            /* Reset transition filter state */
            Arrays.MemSet(this.sLP.In_LP_State, 0, 2);

            /* Direction: up */
            this.sLP.mode = 1;
          } else if (this.sLP.mode == 0) {
            encControl.switchReady = 1;
            /* Make room for redundancy */
            encControl.maxBits -= encControl.maxBits * 5 / (encControl.payloadSize_ms + 5);
          } else {
            /* Direction: up */
            this.sLP.mode = 1;
          }
        } else if (this.sLP.mode < 0) {
          this.sLP.mode = 1;
        }
      }
    }

    return fs_kHz;
  }

  /* Control SNR of residual quantizer */
  int silk_control_SNR(
      int TargetRate_bps /* I Target max bitrate (bps) */
  ) {
    int k, ret = SilkError.SILK_NO_ERROR;
    int frac_Q6;
    int[] rateTable;

    /* Set bitrate/coding quality */
    TargetRate_bps = Inlines.silk_LIMIT(TargetRate_bps, SilkConstants.MIN_TARGET_RATE_BPS,
        SilkConstants.MAX_TARGET_RATE_BPS);
    if (TargetRate_bps != this.TargetRate_bps) {
      this.TargetRate_bps = TargetRate_bps;

      /* If new TargetRate_bps, translate to SNR_dB value */
      if (this.fs_kHz == 8) {
        rateTable = SilkTables.silk_TargetRate_table_NB;
      } else if (this.fs_kHz == 12) {
        rateTable = SilkTables.silk_TargetRate_table_MB;
      } else {
        rateTable = SilkTables.silk_TargetRate_table_WB;
      }

      /* Reduce bitrate for 10 ms modes in these calculations */
      if (this.nb_subfr == 2) {
        TargetRate_bps -= TuningParameters.REDUCE_BITRATE_10_MS_BPS;
      }

      /* Find bitrate interval in table and interpolate */
      for (k = 1; k < SilkConstants.TARGET_RATE_TAB_SZ; k++) {
        if (TargetRate_bps <= rateTable[k]) {
          frac_Q6 = Inlines.silk_DIV32(Inlines.silk_LSHIFT(TargetRate_bps - rateTable[k - 1], 6),
              rateTable[k] - rateTable[k - 1]);
          this.SNR_dB_Q7 =
              Inlines.silk_LSHIFT(SilkTables.silk_SNR_table_Q1[k - 1], 6) + Inlines.silk_MUL(
                  frac_Q6, SilkTables.silk_SNR_table_Q1[k] - SilkTables.silk_SNR_table_Q1[k - 1]);
          break;
        }
      }
    }

    return ret;
  }

  void silk_encode_do_VAD() {
    /**
     * *************************
     */
    /* Voice Activity Detection */
    /**
     * *************************
     */
    VoiceActivityDetection.silk_VAD_GetSA_Q8(this, this.inputBuf, 1);

    /**
     * ***********************************************
     */
    /* Convert speech activity into VAD and DTX flags */
    /**
     * ***********************************************
     */
    if (this.speech_activity_Q8 < ((int) ((TuningParameters.SPEECH_ACTIVITY_DTX_THRES)
        * ((long) 1 << (8)) + 0.5))/*
                                    * Inlines.SILK_CONST(TuningParameters.SPEECH_ACTIVITY_DTX_THRES,
                                    * 8)
                                    */) {
      this.indices.signalType = SilkConstants.TYPE_NO_VOICE_ACTIVITY;
      this.noSpeechCounter++;
      if (this.noSpeechCounter < SilkConstants.NB_SPEECH_FRAMES_BEFORE_DTX) {
        this.inDTX = 0;
      } else if (this.noSpeechCounter > SilkConstants.MAX_CONSECUTIVE_DTX
          + SilkConstants.NB_SPEECH_FRAMES_BEFORE_DTX) {
        this.noSpeechCounter = SilkConstants.NB_SPEECH_FRAMES_BEFORE_DTX;
        this.inDTX = 0;
      }
      this.VAD_flags[this.nFramesEncoded] = 0;
    } else {
      this.noSpeechCounter = 0;
      this.inDTX = 0;
      this.indices.signalType = SilkConstants.TYPE_UNVOICED;
      this.VAD_flags[this.nFramesEncoded] = 1;
    }
  }

  /**
   * *************
   */
  /* Encode frame */
  /**
   * *************
   */
  int silk_encode_frame(
      BoxedValueInt pnBytesOut, /* O Pointer to number of payload bytes; */
      EntropyCoder psRangeEnc, /* I/O compressor data structure */
      int condCoding, /* I The type of conditional coding to use */
      int maxBits, /* I If > 0: maximum number of output bits */
      int useCBR /* I Flag to force constant-bitrate operation */
  ) {
    SilkEncoderControl sEncCtrl = new SilkEncoderControl();
    int i, iter, maxIter, found_upper, found_lower, ret = 0;
    int x_frame;
    EntropyCoder sRangeEnc_copy = new EntropyCoder();
    EntropyCoder sRangeEnc_copy2 = new EntropyCoder();
    SilkNSQState sNSQ_copy = new SilkNSQState();
    SilkNSQState sNSQ_copy2 = new SilkNSQState();
    int nBits, nBits_lower, nBits_upper, gainMult_lower, gainMult_upper;
    int gainsID, gainsID_lower, gainsID_upper;
    short gainMult_Q8;
    short ec_prevLagIndex_copy;
    int ec_prevSignalType_copy;
    byte LastGainIndex_copy2;
    byte seed_copy;

    /* This is totally unnecessary but many compilers (including gcc) are too dumb to realise it */
    LastGainIndex_copy2 = 0;
    nBits_lower = nBits_upper = gainMult_lower = gainMult_upper = 0;

    this.indices.Seed = (byte) (this.frameCounter++ & 3);

    /**
     * ***********************************************************
     */
    /* Set up Input Pointers, and insert frame in input buffer */
    /**
     * **********************************************************
     */
    /* start of frame to encode */
    x_frame = this.ltp_mem_length;

    /**
     * ************************************
     */
    /* Ensure smooth bandwidth transitions */
    /**
     * ************************************
     */
    this.sLP.silk_LP_variable_cutoff(this.inputBuf, 1, this.frame_length);

    /**
     * ****************************************
     */
    /* Copy new frame to front of input buffer */
    /**
     * ****************************************
     */
    System.arraycopy(this.inputBuf, 1, this.x_buf,
        x_frame + SilkConstants.LA_SHAPE_MS * this.fs_kHz, this.frame_length);

    if (this.prefillFlag == 0) {
      int[] xfw_Q3;
      short[] res_pitch;
      byte[] ec_buf_copy;
      int res_pitch_frame;

      res_pitch = new short[this.la_pitch + this.frame_length + this.ltp_mem_length];
      /* start of pitch LPC residual frame */
      res_pitch_frame = this.ltp_mem_length;

      /**
       * **************************************
       */
      /* Find pitch lags, initial LPC analysis */
      /**
       * **************************************
       */
      FindPitchLags.silk_find_pitch_lags(this, sEncCtrl, res_pitch, this.x_buf, x_frame);

      /**
       * *********************
       */
      /* Noise shape analysis */
      /**
       * *********************
       */
      NoiseShapeAnalysis.silk_noise_shape_analysis(this, sEncCtrl, res_pitch, res_pitch_frame,
          this.x_buf, x_frame);

      /**
       * ************************************************
       */
      /* Find linear prediction coefficients (LPC + LTP) */
      /**
       * ************************************************
       */
      FindPredCoefs.silk_find_pred_coefs(this, sEncCtrl, res_pitch, this.x_buf, x_frame,
          condCoding);

      /**
       * *************************************
       */
      /* Process gains */
      /**
       * *************************************
       */
      ProcessGains.silk_process_gains(this, sEncCtrl, condCoding);

      /**
       * **************************************
       */
      /* Prefiltering for noise shaper */
      /**
       * **************************************
       */
      xfw_Q3 = new int[this.frame_length];
      Filters.silk_prefilter(this, sEncCtrl, xfw_Q3, this.x_buf, x_frame);

      /**
       * *************************************
       */
      /* Low Bitrate Redundant Encoding */
      /**
       * *************************************
       */
      silk_LBRR_encode(sEncCtrl, xfw_Q3, condCoding);

      /* Loop over quantizer and entropy coding to control bitrate */
      maxIter = 6;
      gainMult_Q8 = (short) (((int) ((1) * ((long) 1 << (8)) + 0.5))/* Inlines.SILK_CONST(1, 8) */);
      found_lower = 0;
      found_upper = 0;
      gainsID = GainQuantization.silk_gains_ID(this.indices.GainsIndices, this.nb_subfr);
      gainsID_lower = -1;
      gainsID_upper = -1;
      /* Copy part of the input state */
      sRangeEnc_copy.Assign(psRangeEnc);
      sNSQ_copy.Assign(this.sNSQ);
      seed_copy = this.indices.Seed;
      ec_prevLagIndex_copy = this.ec_prevLagIndex;
      ec_prevSignalType_copy = this.ec_prevSignalType;
      ec_buf_copy = new byte[1275]; // fixme: this size might be optimized to the actual size
      for (iter = 0;; iter++) {
        if (gainsID == gainsID_lower) {
          nBits = nBits_lower;
        } else if (gainsID == gainsID_upper) {
          nBits = nBits_upper;
        } else {
          /* Restore part of the input state */
          if (iter > 0) {
            psRangeEnc.Assign(sRangeEnc_copy);
            this.sNSQ.Assign(sNSQ_copy);
            this.indices.Seed = seed_copy;
            this.ec_prevLagIndex = ec_prevLagIndex_copy;
            this.ec_prevSignalType = ec_prevSignalType_copy;
          }

          /**
           * **************************************
           */
          /* Noise shaping quantization */
          /**
           * **************************************
           */
          if (this.nStatesDelayedDecision > 1 || this.warping_Q16 > 0) {
            sNSQ.silk_NSQ_del_dec(
                this,
                this.indices,
                xfw_Q3,
                pulses,
                sEncCtrl.PredCoef_Q12,
                sEncCtrl.LTPCoef_Q14,
                sEncCtrl.AR2_Q13,
                sEncCtrl.HarmShapeGain_Q14,
                sEncCtrl.Tilt_Q14,
                sEncCtrl.LF_shp_Q14,
                sEncCtrl.Gains_Q16,
                sEncCtrl.pitchL,
                sEncCtrl.Lambda_Q10,
                sEncCtrl.LTP_scale_Q14);
          } else {
            sNSQ.silk_NSQ(
                this,
                this.indices,
                xfw_Q3,
                pulses,
                sEncCtrl.PredCoef_Q12,
                sEncCtrl.LTPCoef_Q14,
                sEncCtrl.AR2_Q13,
                sEncCtrl.HarmShapeGain_Q14,
                sEncCtrl.Tilt_Q14,
                sEncCtrl.LF_shp_Q14,
                sEncCtrl.Gains_Q16,
                sEncCtrl.pitchL,
                sEncCtrl.Lambda_Q10,
                sEncCtrl.LTP_scale_Q14);
          }

          /**
           * *************************************
           */
          /* Encode Parameters */
          /**
           * *************************************
           */
          EncodeIndices.silk_encode_indices(this, psRangeEnc, this.nFramesEncoded, 0, condCoding);

          /**
           * *************************************
           */
          /* Encode Excitation Signal */
          /**
           * *************************************
           */
          EncodePulses.silk_encode_pulses(psRangeEnc, this.indices.signalType,
              this.indices.quantOffsetType,
              this.pulses, this.frame_length);

          nBits = psRangeEnc.tell();

          if (useCBR == 0 && iter == 0 && nBits <= maxBits) {
            break;
          }
        }

        if (iter == maxIter) {
          if (found_lower != 0 && (gainsID == gainsID_lower || nBits > maxBits)) {
            /* Restore output state from earlier iteration that did meet the bitrate budget */
            psRangeEnc.Assign(sRangeEnc_copy2);
            Inlines.OpusAssert(sRangeEnc_copy2.offs <= 1275);
            psRangeEnc.write_buffer(ec_buf_copy, 0, 0, (int) sRangeEnc_copy2.offs);
            this.sNSQ.Assign(sNSQ_copy2);
            this.sShape.LastGainIndex = LastGainIndex_copy2;
          }
          break;
        }

        if (nBits > maxBits) {
          if (found_lower == 0 && iter >= 2) {
            /*
             * Adjust the quantizer's rate/distortion tradeoff and discard previous "upper" results
             */
            sEncCtrl.Lambda_Q10 =
                Inlines.silk_ADD_RSHIFT32(sEncCtrl.Lambda_Q10, sEncCtrl.Lambda_Q10, 1);
            found_upper = 0;
            gainsID_upper = -1;
          } else {
            found_upper = 1;
            nBits_upper = nBits;
            gainMult_upper = gainMult_Q8;
            gainsID_upper = gainsID;
          }
        } else if (nBits < maxBits - 5) {
          found_lower = 1;
          nBits_lower = nBits;
          gainMult_lower = gainMult_Q8;
          if (gainsID != gainsID_lower) {
            gainsID_lower = gainsID;
            /* Copy part of the output state */
            sRangeEnc_copy2.Assign(psRangeEnc);
            Inlines.OpusAssert(psRangeEnc.offs <= 1275);
            System.arraycopy(psRangeEnc.get_buffer(), 0, ec_buf_copy, 0, (int) psRangeEnc.offs);
            sNSQ_copy2.Assign(this.sNSQ);
            LastGainIndex_copy2 = this.sShape.LastGainIndex;
          }
        } else {
          /* Within 5 bits of budget: close enough */
          break;
        }

        if ((found_lower & found_upper) == 0) {
          /* Adjust gain according to high-rate rate/distortion curve */
          int gain_factor_Q16;
          gain_factor_Q16 =
              Inlines.silk_log2lin(Inlines.silk_LSHIFT(nBits - maxBits, 7) / this.frame_length
                  + ((int) ((16) * ((long) 1 << (7)) + 0.5))/* Inlines.SILK_CONST(16, 7) */);
          gain_factor_Q16 = Inlines.silk_min_32(gain_factor_Q16,
              ((int) ((2) * ((long) 1 << (16)) + 0.5))/* Inlines.SILK_CONST(2, 16) */);
          if (nBits > maxBits) {
            gain_factor_Q16 = Inlines.silk_max_32(gain_factor_Q16,
                ((int) ((1.3f) * ((long) 1 << (16)) + 0.5))/* Inlines.SILK_CONST(1.3f, 16) */);
          }

          gainMult_Q8 = (short) (Inlines.silk_SMULWB(gain_factor_Q16, (int) gainMult_Q8));
        } else {
          /* Adjust gain by interpolating */
          gainMult_Q8 = (short) (gainMult_lower + Inlines.silk_DIV32_16(
              Inlines.silk_MUL(gainMult_upper - gainMult_lower, maxBits - nBits_lower),
              nBits_upper - nBits_lower));
          /*
           * New gain multplier must be between 25% and 75% of old range (note that gainMult_upper <
           * gainMult_lower)
           */
          if (gainMult_Q8 > Inlines.silk_ADD_RSHIFT32(gainMult_lower,
              gainMult_upper - gainMult_lower, 2)) {
            gainMult_Q8 = (short) (Inlines.silk_ADD_RSHIFT32(gainMult_lower,
                gainMult_upper - gainMult_lower, 2));
          } else if (gainMult_Q8 < Inlines.silk_SUB_RSHIFT32(gainMult_upper,
              gainMult_upper - gainMult_lower, 2)) {
            gainMult_Q8 = (short) (Inlines.silk_SUB_RSHIFT32(gainMult_upper,
                gainMult_upper - gainMult_lower, 2));
          }
        }

        for (i = 0; i < this.nb_subfr; i++) {
          sEncCtrl.Gains_Q16[i] = Inlines
              .silk_LSHIFT_SAT32(Inlines.silk_SMULWB(sEncCtrl.GainsUnq_Q16[i], gainMult_Q8), 8);
        }

        /* Quantize gains */
        this.sShape.LastGainIndex = sEncCtrl.lastGainIndexPrev;
        BoxedValueByte boxed_gainIndex = new BoxedValueByte(this.sShape.LastGainIndex);
        GainQuantization.silk_gains_quant(this.indices.GainsIndices, sEncCtrl.Gains_Q16,
            boxed_gainIndex, condCoding == SilkConstants.CODE_CONDITIONALLY ? 1 : 0, this.nb_subfr);
        this.sShape.LastGainIndex = boxed_gainIndex.Val;

        /* Unique identifier of gains vector */
        gainsID = GainQuantization.silk_gains_ID(this.indices.GainsIndices, this.nb_subfr);
      }
    }

    /* Update input buffer */
    Arrays.MemMove(this.x_buf, this.frame_length, 0,
        this.ltp_mem_length + SilkConstants.LA_SHAPE_MS * this.fs_kHz);

    /* Exit without entropy coding */
    if (this.prefillFlag != 0) {
      /* No payload */
      pnBytesOut.Val = 0;

      return ret;
    }

    /* Parameters needed for next frame */
    this.prevLag = sEncCtrl.pitchL[this.nb_subfr - 1];
    this.prevSignalType = this.indices.signalType;

    /**
     * *************************************
     */
    /* Finalize payload */
    /**
     * *************************************
     */
    this.first_frame_after_reset = 0;
    /* Payload size */
    pnBytesOut.Val = Inlines.silk_RSHIFT(psRangeEnc.tell() + 7, 3);

    return ret;
  }

  /*
   * Low-Bitrate Redundancy (LBRR) encoding. Reuse all parameters but encode excitation at lower
   * bitrate
   */
  void silk_LBRR_encode(
      SilkEncoderControl thisCtrl, /* I/O Pointer to Silk FIX encoder control struct */
      int[] xfw_Q3, /* I Input signal */
      int condCoding /* I The type of conditional coding used so far for this frame */
  ) {
    int[] TempGains_Q16 = new int[/* SilkConstants.MAX_NB_SUBFR */this.nb_subfr];
    SideInfoIndices psIndices_LBRR = this.indices_LBRR[this.nFramesEncoded];
    SilkNSQState sNSQ_LBRR = new SilkNSQState();

    /**
     * ****************************************
     */
    /* Control use of inband LBRR */
    /**
     * ****************************************
     */
    if (this.LBRR_enabled != 0
        && this.speech_activity_Q8 > ((int) ((TuningParameters.LBRR_SPEECH_ACTIVITY_THRES)
            * ((long) 1 << (8)) + 0.5))/*
                                        * Inlines.SILK_CONST(TuningParameters.
                                        * LBRR_SPEECH_ACTIVITY_THRES, 8)
                                        */) {
      this.LBRR_flags[this.nFramesEncoded] = 1;

      /* Copy noise shaping quantizer state and quantization indices from regular encoding */
      sNSQ_LBRR.Assign(this.sNSQ);
      psIndices_LBRR.Assign(this.indices);

      /* Save original gains */
      System.arraycopy(thisCtrl.Gains_Q16, 0, TempGains_Q16, 0, this.nb_subfr);

      if (this.nFramesEncoded == 0 || this.LBRR_flags[this.nFramesEncoded - 1] == 0) {
        /* First frame in packet or previous frame not LBRR coded */
        this.LBRRprevLastGainIndex = this.sShape.LastGainIndex;

        /* Increase Gains to get target LBRR rate */
        psIndices_LBRR.GainsIndices[0] =
            (byte) (psIndices_LBRR.GainsIndices[0] + this.LBRR_GainIncreases);
        psIndices_LBRR.GainsIndices[0] = (byte) (Inlines
            .silk_min_int(psIndices_LBRR.GainsIndices[0], SilkConstants.N_LEVELS_QGAIN - 1));
      }

      /* Decode to get gains in sync with decoder */
      /* Overwrite unquantized gains with quantized gains */
      BoxedValueByte boxed_gainIndex = new BoxedValueByte(this.LBRRprevLastGainIndex);
      GainQuantization.silk_gains_dequant(thisCtrl.Gains_Q16, psIndices_LBRR.GainsIndices,
          boxed_gainIndex, condCoding == SilkConstants.CODE_CONDITIONALLY ? 1 : 0, this.nb_subfr);
      this.LBRRprevLastGainIndex = boxed_gainIndex.Val;

      /**
       * **************************************
       */
      /* Noise shaping quantization */
      /**
       * **************************************
       */
      if (this.nStatesDelayedDecision > 1 || this.warping_Q16 > 0) {
        sNSQ_LBRR.silk_NSQ_del_dec(this,
            psIndices_LBRR,
            xfw_Q3,
            this.pulses_LBRR[this.nFramesEncoded],
            thisCtrl.PredCoef_Q12,
            thisCtrl.LTPCoef_Q14,
            thisCtrl.AR2_Q13,
            thisCtrl.HarmShapeGain_Q14,
            thisCtrl.Tilt_Q14,
            thisCtrl.LF_shp_Q14,
            thisCtrl.Gains_Q16,
            thisCtrl.pitchL,
            thisCtrl.Lambda_Q10,
            thisCtrl.LTP_scale_Q14);
      } else {
        sNSQ_LBRR.silk_NSQ(this,
            psIndices_LBRR,
            xfw_Q3,
            this.pulses_LBRR[this.nFramesEncoded],
            thisCtrl.PredCoef_Q12,
            thisCtrl.LTPCoef_Q14,
            thisCtrl.AR2_Q13,
            thisCtrl.HarmShapeGain_Q14,
            thisCtrl.Tilt_Q14,
            thisCtrl.LF_shp_Q14,
            thisCtrl.Gains_Q16,
            thisCtrl.pitchL,
            thisCtrl.Lambda_Q10,
            thisCtrl.LTP_scale_Q14);
      }

      /* Restore original gains */
      System.arraycopy(TempGains_Q16, 0, thisCtrl.Gains_Q16, 0, this.nb_subfr);
    }
  }
}
