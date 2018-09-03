package com.example.imac.alivoicerecognition;

public class OpusJniTool {
    static {
        System.loadLibrary("opus_tool");
    }

    public static native int initOpusCodec();

    public static native int opusCodecEncode(short[] lin, int offset, byte[] encoded);
    public static native int opusCodecDecode(byte[] encoded, int byteNum, short[] lin);

    public static native int opusCodecClose();

}
