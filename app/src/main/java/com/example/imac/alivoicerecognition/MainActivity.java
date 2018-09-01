package com.example.imac.alivoicerecognition;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.imac.alivoicerecognition.HttpUtil.HttpResponse;
import com.example.imac.alivoicerecognition.HttpUtil.HttpUtil;
import com.example.imac.alivoicerecognition.View.RippleSpeechRecordView;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private RippleSpeechRecordView mSpeechRecordView;
    private TextView mContentView;
    private TextView mStatusTv;
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
        int storagePermission = this.checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (storagePermission != PackageManager.PERMISSION_GRANTED) {
            //这里就会弹出对话框
            this.requestPermissions(
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    0);

        }

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

    private void startAudioRecord() {
        if (Build.VERSION.SDK_INT >= 23) {
            //我是在Fragment里写代码的，因此调用getActivity
            //如果不想判断SDK，可以使用ActivityCompat的接口来检查和申请权限
            int permission = this.checkSelfPermission(Manifest.permission.RECORD_AUDIO);

            if (permission != PackageManager.PERMISSION_GRANTED) {
                //这里就会弹出对话框
                this.requestPermissions(
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        1);

                return;
            }


        }
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
        AudioRecordManager manager = AudioRecordManager.getInstance();
        manager.stopRecord();
        mIsRecording = false;
        super.onPause();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 0) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this,
                        "Storage Permission Denied",
                        Toast.LENGTH_SHORT)
                        .show();
            }
        } else if (requestCode == 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startAudioRecord();
            } else {
                Toast.makeText(this,
                        "Record Audio Permission Denied",
                        Toast.LENGTH_SHORT)
                        .show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    private void stopAudioRecord() {
        AudioRecordManager manager = AudioRecordManager.getInstance();
        manager.stopRecord();
        mIsRecording = false;
        mSpeechRecordView.startRecognition();
    }


    private void speechRecognition() {
        mStatusTv.setText("语音识别中...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (null != mSpeechFile) {
                    //使用对应的ASR模型 详情见文档部分2
                    String model = "chat";
                    final String url = "http://nlsapi.aliyun.com/recognize?model=" + model;
                    byte[] buffer = null;
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                        try {
                            RandomAccessFile rf = new RandomAccessFile(mSpeechFile, "r");
                            buffer = new byte[(int) rf.length()];
                            rf.readFully(buffer);
                        } catch (Exception e) {
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

                    if (null != buffer) {
                        final HttpResponse response = HttpUtil.sendAsrPost(buffer, "pcm", 16000, url, ak_id, ak_secret);
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
            }
        }).start();
    }
}
