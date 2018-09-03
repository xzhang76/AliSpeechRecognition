#include <jni.h>
#include "include/opus.h"

OpusDecoder *opusDecoder;
OpusEncoder *opusEncoder;
int channels = 1;
int fs = 16000;

const int FRAME_SAMPLES = 320;

extern "C"
JNIEXPORT jint JNICALL
Java_com_example_imac_alivoicerecognition_OpusJniTool_initOpusCodec(JNIEnv *env, jobject instance) {

    int error;
    OpusEncoder *opusEncoder = opus_encoder_create(16000, 1, OPUS_APPLICATION_VOIP,
                                                &error);
    if (opusEncoder) {
        opus_encoder_ctl(opusEncoder, OPUS_SET_VBR(1));
        opus_encoder_ctl(opusEncoder, OPUS_SET_BITRATE(27800));
        opus_encoder_ctl(opusEncoder, OPUS_SET_COMPLEXITY(8));
        opus_encoder_ctl(opusEncoder, OPUS_SET_SIGNAL(OPUS_SIGNAL_VOICE));
    }
    OpusDecoder *opusDecoder = opus_decoder_create(16000, 1, &error);
    if (opusDecoder != NULL && opusEncoder != NULL) {
        return 0;
    } else {
        return 1;
    }
}

extern "C"
JNIEXPORT jint JNICALL Java_com_example_imac_alivoicerecognition_OpusJniTool_opusCodecEncode
        (JNIEnv* env,jobject thiz, jshortArray samples, jint offset,
         jbyteArray bytes){
    if (!opusEncoder || !samples || !bytes)
        return 0;
    jshort *pSamples = env->GetShortArrayElements(samples, 0);
    jsize nSampleSize = env->GetArrayLength(samples);
    jbyte *pBytes = env->GetByteArrayElements(bytes, 0);
    jsize nByteSize = env->GetArrayLength(bytes);
    if (nSampleSize-offset < FRAME_SAMPLES || nByteSize <= 0)
        return 0;
    int nRet = opus_encode(opusEncoder, pSamples+offset, FRAME_SAMPLES, (unsigned char*)pBytes, nByteSize);
    env->ReleaseShortArrayElements(samples, pSamples, 0);
    env->ReleaseByteArrayElements(bytes, pBytes, 0);
    return nRet;
}

extern "C"
JNIEXPORT jint JNICALL Java_com_example_imac_alivoicerecognition_OpusJniTool_opusCodecDecode
        (JNIEnv* env, jobject thiz, jbyteArray bytes, jint bytelength, jshortArray samples){
    if (!opusDecoder || !samples || !bytes)
        return 0;
    jshort *pSamples = env->GetShortArrayElements(samples, 0);
    jbyte *pBytes = env->GetByteArrayElements(bytes, 0);
    jsize nByteSize = env->GetArrayLength(bytes);
    // jint nbyte = bytes[0];
    if (bytelength<=0)
    {
        return -1;
    }
    int nRet = opus_decode(opusDecoder, (unsigned char*)pBytes, bytelength, pSamples, FRAME_SAMPLES, 0);
    env->ReleaseShortArrayElements(samples, pSamples, 0);
    env->ReleaseByteArrayElements(bytes, pBytes, 0);
    return nRet;
}



extern "C"
JNIEXPORT jint JNICALL
Java_com_example_imac_alivoicerecognition_OpusJniTool_opusCodecClose(JNIEnv *env, jclass type) {
    if (!opusEncoder)
        return -1;
    opus_encoder_destroy(opusEncoder);
    if (!opusDecoder)
        return -1;
    opus_decoder_destroy(opusDecoder);
    return 0;
}