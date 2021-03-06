package com.example.imac.alivoicerecognition;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class AudioRecordManager {

    private static AudioRecordManager audioRecordManager;
    // 音频源：音频输入-麦克风
    private final static int AUDIO_INPUT = MediaRecorder.AudioSource.MIC;
    // 采样率
    // 44100是目前的标准，但是某些设备仍然支持22050，16000，11025
    // 采样频率一般共分为22.05KHz、44.1KHz、48KHz三个等级
    private final static int AUDIO_SAMPLE_RATE = 16000;
    // 音频通道 单声道
    private final static int AUDIO_CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    // 音频格式：PCM编码
    private final static int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    // 缓冲区大小：缓冲区字节大小
    private int bufferSizeInBytes = 0;
    // 录音对象
    private AudioRecord audioRecord;

    private static final String TMP_RAW = Environment.getExternalStorageDirectory() + "/ali/tmp.raw";
    // 文件名
    private String mFileWav;

    private File mSpeechFile;

    private boolean mIsRecording = false;

    private AudioRecordManager() {
    }

    //单例模式
    public static AudioRecordManager getInstance() {
        if (audioRecordManager == null) {
            audioRecordManager = new AudioRecordManager();
        }
        return audioRecordManager;
    }

    /**
     * 创建录音对象
     */
    public void createAudio(String fileName, int audioSource, int sampleRateInHz, int channelConfig, int audioFormat) {
        // 获得缓冲区字节大小
        bufferSizeInBytes = AudioRecord.getMinBufferSize(sampleRateInHz,
                channelConfig, audioFormat);
        audioRecord = new AudioRecord(audioSource, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes);
        this.mFileWav = fileName;
    }

    /**
     * 创建默认的录音对象
     *
     * @param fileName 文件名
     */
    public void createDefaultAudio(String fileName) {
        // 获得缓冲区字节大小
        bufferSizeInBytes = AudioRecord.getMinBufferSize(AUDIO_SAMPLE_RATE,
                AUDIO_CHANNEL, AUDIO_ENCODING);
        audioRecord = new AudioRecord(AUDIO_INPUT, AUDIO_SAMPLE_RATE, AUDIO_CHANNEL, AUDIO_ENCODING, bufferSizeInBytes);
        this.mFileWav = fileName;
    }

    /**
     * 开始录音
     */
    public void startRecord() {
        if (OpusJniTool.initOpus() != 0) {
            Log.e("opus", "Opus tool init fail.");
            return;
        }
        // 开启音频文件写入线程
        new Thread(new AudioRecordThread()).start();
    }

    public File getSpeechFile() {
        return mSpeechFile;
    }

    /**
     * 停止录音
     */
    public void stopRecord() {
        Log.d("demo", "stopRecord");
        mIsRecording = false;//停止文件写入
        OpusJniTool.close();
    }

    class AudioRecordThread implements Runnable {
        @Override
        public void run() {
            startRecordByOpus();
//            writeDateTOFile();//往文件中写入裸数据
//            copyWaveFile(TMP_RAW, mFileWav);//给裸数据加上头文件
        }
    }

    private void startRecordByOpus() {
        Log.i("demo", "开始录音");
        //生成PCM文件
        mSpeechFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/opus.pcm");

        //如果存在，就先删除再创建
        if (mSpeechFile.exists())
            mSpeechFile.delete();

        try {
            mSpeechFile.createNewFile();

        } catch (IOException e) {
            throw new IllegalStateException("未能创建" + mSpeechFile.toString());
        }
        try {
            //输出流
            OutputStream os = new FileOutputStream(mSpeechFile);
            BufferedOutputStream bos = new BufferedOutputStream(os);
            DataOutputStream dos = new DataOutputStream(bos);
            int bufferSize = 480;
            AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, AUDIO_SAMPLE_RATE, AUDIO_CHANNEL, AUDIO_ENCODING, bufferSize);

            short[] buffer = new short[bufferSize];
            audioRecord.startRecording();
            // 让录制状态为true
            mIsRecording = true;
            while (mIsRecording) {
                int bufferReadResult = audioRecord.read(buffer, 0, bufferSize);
                if (bufferReadResult >= 0) {
                    //编码
                    short[] encoderShort = OpusJniTool.opusEncoder(buffer, bufferReadResult);
                    //解码
                    short[] decodeShort = OpusJniTool.opusDecode(encoderShort, encoderShort.length, bufferReadResult);
                    for (int i = 0; i < decodeShort.length; i++) {
                        dos.writeShort(decodeShort[i]);
                    }
                }
            }
            audioRecord.stop();
            dos.close();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("demo", "录音失败");
        }
    }

    public void playRecord() {
        Log.d("demo", "播放录音");

        if (mSpeechFile == null) {
            return;
        }
        //读取文件
        int musicLength = (int) (mSpeechFile.length() / 2);
        short[] music = new short[musicLength];
        try {
            InputStream is = new FileInputStream(mSpeechFile);
            BufferedInputStream bis = new BufferedInputStream(is);
            DataInputStream dis = new DataInputStream(bis);
            int i = 0;
            while (dis.available() > 0) {
                music[i] = dis.readShort();
                i++;
            }
            dis.close();
            AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                    16000, AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    musicLength * 2,
                    AudioTrack.MODE_STREAM);
            audioTrack.play();
            audioTrack.write(music, 0, musicLength);
            audioTrack.stop();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("demo", "播放失败");
        }
    }

    /**
     * 这里将数据写入文件，但是并不能播放，因为AudioRecord获得的音频是原始的裸音频，
     * 如果需要播放就必须加入一些格式或者编码的头信息。但是这样的好处就是你可以对音频的 裸数据进行处理，比如你要做一个爱说话的TOM
     * 猫在这里就进行音频的处理，然后重新封装 所以说这样得到的音频比较容易做一些音频的处理。
     */
    private void writeDateTOFile() {
        // new一个byte数组用来存一些字节数据，大小为缓冲区大小
        int bufferSize = 480;
        short[] audioData = new short[bufferSize];
        int readSize = 0;
        DataOutputStream dos = null;
        try {
            File file = new File(mFileWav);
            if (file.exists()) {
                file.delete();
            }
            OutputStream os = new FileOutputStream(file);
            BufferedOutputStream bos = new BufferedOutputStream(os);
            dos = new DataOutputStream(bos);

            while (mIsRecording) {
                readSize = audioRecord.read(audioData, 0, bufferSize);
                if (AudioRecord.ERROR_INVALID_OPERATION != readSize) {
                    //编码
                    short[] encoderShort = OpusJniTool.opusEncoder(audioData, readSize);
                    //解码
                    short[] decodeShort = OpusJniTool.opusDecode(encoderShort, encoderShort.length, readSize);

                    for (int i = 0; i < decodeShort.length; i++) {
                        dos.writeShort(decodeShort[i]);
                    }
                }
            }
            audioRecord.stop();
            audioRecord.release();//释放资源

        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            dos.close();// 关闭写入流
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 这里得到可播放的音频文件
    private void copyWaveFile(String inFilename, String outFilename) {
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        int channels = 2;
        long byteRate = 16 * AUDIO_SAMPLE_RATE * channels / 8;
        byte[] data = new byte[bufferSizeInBytes];
        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;
            WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                    AUDIO_SAMPLE_RATE, channels, byteRate);
            while (in.read(data) != -1) {
                out.write(data);
            }
            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 这里提供一个头信息。插入这些信息就可以得到可以播放的文件。
     * 为我为啥插入这44个字节，这个还真没深入研究，不过你随便打开一个wav
     * 音频的文件，可以发现前面的头文件可以说基本一样哦。每种格式的文件都有
     * 自己特有的头文件。
     */
    private void WriteWaveFileHeader(FileOutputStream out, long totalAudioLen,
                                     long totalDataLen, long longSampleRate, int channels, long byteRate)
            throws IOException {
        byte[] header = new byte[44];
        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8); // block align
        header[33] = 0;
        header[34] = 16; // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        out.write(header, 0, 44);
    }

}
