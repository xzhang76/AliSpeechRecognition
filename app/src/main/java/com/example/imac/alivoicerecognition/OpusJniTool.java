package com.example.imac.alivoicerecognition;

public class OpusJniTool {
    static {
        System.loadLibrary("opus_tool");
    }

    public static native int initOpus();

    public static native short[] opusEncoder(short[] buffer, int length);

    public static native short[] opusDecode(short[] buffer, int bufferLength,int pcmLength);

    public static native int close();

}
