//
// Created by xiaofeng on 2018/4/13.
//


#include "opus-build.h"
#include "../include/opus.h"


OpusDecoder *initDecoderCreate(int fs, int channels) {
    int error;
    OpusDecoder *opusDecoder =  opus_decoder_create(fs, channels, &error);
    if (error < 0) {
        return NULL;
    } else {
        return opusDecoder;
    }
}

//fs = 1600, channels = 1
OpusEncoder *initEncoderCreate(int fs, int channels) {
    int error;
    OpusEncoder *opusEncoder = opus_encoder_create(fs, channels, OPUS_APPLICATION_VOIP, &error);
    if (error < 0) {
        return NULL;
    } else {
        opus_encoder_ctl(opusEncoder, OPUS_SET_VBR(1));
        opus_encoder_ctl(opusEncoder, OPUS_SET_BITRATE(27800));
        opus_encoder_ctl(opusEncoder, OPUS_SET_COMPLEXITY(8));
        opus_encoder_ctl(opusEncoder, OPUS_SET_SIGNAL(OPUS_SIGNAL_VOICE));
        return opusEncoder;
    }
}

int opus_encodes(OpusEncoder *opusEncoder, opus_int16 buffer[], int size, short *out) {
    int length;
    if (opusEncoder != NULL) {
        length = opus_encode(opusEncoder, buffer, size, out, 480);
    } else {
        length = 0;
    }
    return length;
}

int opus_decodes(OpusDecoder *opusDecoder, short buffer[], int bufferSize, opus_int16 *pcmBuffer) {
    int length;
    if (opusDecoder != NULL) {
        length = opus_decode(opusDecoder, buffer, bufferSize, pcmBuffer, 480 * 4, 0);
    } else {
        length = 0;
    }
    return length;
}

int close(OpusEncoder *opusEncoder, OpusDecoder *opusDecoder) {
    if (opusEncoder != NULL) {
        opus_encoder_destroy(opusEncoder);
    }
    if (opusDecoder != NULL) {
        opus_decoder_destroy(opusDecoder);
    }
}

