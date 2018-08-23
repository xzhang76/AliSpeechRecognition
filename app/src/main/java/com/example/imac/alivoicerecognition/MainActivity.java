package com.example.imac.alivoicerecognition;

import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.example.imac.alivoicerecognition.HttpUtil.HttpResponse;
import com.example.imac.alivoicerecognition.HttpUtil.HttpUtil;
import com.example.imac.alivoicerecognition.View.RippleSpeechRecordView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private RippleSpeechRecordView mSpeechRecordView;
    private TextView mContentView;
    private TextView mStatusTv;
    private MediaRecorder mRecorder;
    private static final String SPEECH_PATH = Environment.getExternalStorageDirectory() + "/ali";
    private File mSpeechFile;
    private Handler mHandler;
    private boolean mIsRecording = false;
    private static final String ak_id = "LTAILd8nISBsbxB1";
    private static final String ak_secret = "LpxREKOw4eBk1UJMLKpv40T4zuxM47";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mSpeechRecordView = findViewById(R.id.view_record_speech);
        mHandler = new Handler();
        mSpeechRecordView.setOnClickListener(this);
        mContentView = findViewById(R.id.speech_content_tv);
        mStatusTv = findViewById(R.id.status_tv);

    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.view_record_speech) {
            if (mIsRecording) {
                stopAudioRecord();
                speechRecognition();
            } else {
                startAudioRecord();
            }
        }
    }

    private void startSpeechRecord() {
        try {
            File speechPath = new File(SPEECH_PATH);
            if (!speechPath.exists()) {
                speechPath.mkdirs();
            }
            mSpeechFile = new File(SPEECH_PATH, "/" + System.currentTimeMillis() + ".wav");
            if (mRecorder == null) {
                mRecorder = new MediaRecorder();
                mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);//设置音频采集方式
                mRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);//设置音频输出格式
                mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);//设置音频编码方式
            }
            mRecorder.setOutputFile(mSpeechFile.getAbsolutePath());//设置录音文件输出路径
            mRecorder.prepare();
            mRecorder.start();
            mIsRecording = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        updateMicStatus();
    }

    private void startAudioRecord() {
        File speechPath = new File(SPEECH_PATH);
        if (!speechPath.exists()) {
            speechPath.mkdirs();
        }
        mSpeechFile = new File(SPEECH_PATH, "/" + System.currentTimeMillis() + ".wav");
        AudioRecordManager recordManager = AudioRecordManager.getInstance();
        recordManager.createDefaultAudio(mSpeechFile.getAbsolutePath());
        recordManager.startRecord();
        mSpeechRecordView.startRecording();
        mIsRecording = true;
        mStatusTv.setText("录制中...");
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopSpeechRecord();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mRecorder != null) {
            mRecorder.reset();
            mRecorder.release();
            mRecorder = null;
        }
    }

    private void stopSpeechRecord() {
        if (mRecorder != null) {
            try {
                // 停止录制
                mRecorder.stop();
                mIsRecording = false;
                mSpeechRecordView.setProgressChange(0);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (mRecorder != null) {
                mRecorder.reset();
                mRecorder.release();
                mRecorder = null;
            }
        }
    }

    private void stopAudioRecord() {
        AudioRecordManager manager = AudioRecordManager.getInstance();
        manager.stopRecord();
        mIsRecording = false;
        mSpeechRecordView.startRecognition();
    }

    private void updateMicStatus() {
        if (mRecorder != null) {
            try {
                double ratio = (double) mRecorder.getMaxAmplitude() / 600;
                if (ratio > 1) {
                    int level = (int) (20 * Math.log10(ratio) / 3);
//                    Log.d("demo", "分贝转化后声音等级 = " + level);
                    mSpeechRecordView.setProgressChange(level);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    updateMicStatus();
                }
            }, 100);
        }
    }

    private void speechRecognition() {
        mStatusTv.setText("语音识别中...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (null != mSpeechFile) {
                    //使用对应的ASR模型 详情见文档部分2
                    String model = "chat";
                    final String url = "https://nlsapi.aliyun.com/recognize?model=" + model;
                    byte[] buffer = null;
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                        try {
                            InputStream fis = (MainActivity.this.getResources().getAssets().open("kaihu_voice1.mp3"));
                            ByteArrayOutputStream bos = new ByteArrayOutputStream();
                            byte[] b = new byte[1024];
                            int n;
                            while ((n = fis.read(b)) != -1) {
                                bos.write(b, 0, n);
                            }
                            fis.close();
                            bos.close();
                            buffer = bos.toByteArray();
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        //读取本地的语音文件
                        try {
                            Path path = FileSystems.getDefault().getPath(mSpeechFile.getAbsolutePath());
                            buffer = Files.readAllBytes(path);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    final byte[] finalBuffer = buffer;
                    final HttpResponse response = HttpUtil.sendAsrPost(finalBuffer, "pcm", 16000, url, ak_id, ak_secret);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mContentView.setText(response.getResult());
                            mSpeechRecordView.stopRecognition();
                            mStatusTv.setText("语音识别完成！");
                        }
                    });
                    Log.d("demo", "result: " + response.getResult());
                }
            }
        }).start();
    }
}
