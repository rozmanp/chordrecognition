package hu.bme.mit.soundtest;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;


public class ListeningActivity extends AppCompatActivity {
    public static AudioRecord recorder = null;
    private Thread recordingThread = null;
    public static volatile boolean isRecording = false;
    public Handler resultHandler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Button btnStartListen = (Button) findViewById(R.id.btnStartListen);
        TextView mainText = (TextView) findViewById(R.id.mainText);

        /*resultHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message inputMessage) {
                TextView mainText = findViewById(R.id.mainText);
                mainText.setText(inputMessage.obj.toString());
            }
        };*/

        btnStartListen.setOnClickListener(btnListenOnClick);
    }

    private View.OnClickListener btnListenOnClick = new View.OnClickListener() {
        public void onClick(View v) {
            String[] permissions = {Manifest.permission.RECORD_AUDIO,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE};

            if (!hasPermissions(ListeningActivity.this, permissions)) {
                ActivityCompat.requestPermissions(ListeningActivity.this,
                        permissions, Consts.PERMISSION_RECORD_AUDIO);
            }

            Button btn = (Button) v;
            if (!isRecording) {
                try {
                    startRecording();
                } catch (IllegalStateException | IllegalArgumentException e) {
                    Log.e("ERROR", e.getMessage());
                    return;
                }

                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                btn.setText(getResources().getString(R.string.stop_listening));
            } else {
                stopRecording();
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                btn.setText(getResources().getString(R.string.start_listening));
            }
        }
        //}
    };

    private void startRecording() {
        //Verify buffer size
        if (AudioRecord.getMinBufferSize(Consts.SAMPLE_RATE, Consts.CHANNEL_CONFIG, Consts.AUDIO_FORMAT) > Consts.BUFFER_SIZE_IN_BYTES) {
            throw new IllegalArgumentException("AudioRead object cannot be created: insufficient window size");
        }

        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                Consts.SAMPLE_RATE, Consts.CHANNEL_CONFIG,
                AudioFormat.ENCODING_PCM_FLOAT, Consts.BUFFER_SIZE_IN_BYTES);

        //Verify initialization
        if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
            throw new IllegalStateException("the initialization of the AudioRecord object was unsuccessful");
        }

        isRecording = true;
        recorder.startRecording();
        recordingThread = new Thread(new ListeningRunnable(this), "Listening thread");
        recordingThread.start();
    }

    private void stopRecording() {
        // stops the recording activity
        if (null != recorder) {
            isRecording = false;
            recorder.stop();
            recorder.release();
            recorder = null;

            recordingThread = null;
        }
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    /*
    public void setDetectedChord (String label){
        Message completeMessage =
                resultHandler.obtainMessage(state, photoTask);
        completeMessage.sendToTarget();
    }
    */

}