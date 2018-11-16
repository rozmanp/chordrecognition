/*package hu.bme.mit.soundtest;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.paramsen.noise.Noise;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class ListeningActivity_old extends AppCompatActivity {
    private static final int SAMPLE_RATE = 44100;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private static double WINDOW_SIZE_IN_SEC = 1.0;
    private static int SAMPLES_PER_WINDOW = SAMPLE_RATE * (int) WINDOW_SIZE_IN_SEC + 1;
    private static int BYTES_PER_ELEMENT = 2;
    //private static int BUFFER_SIZE_IN_BYTES = SAMPLES_PER_WINDOW * BYTES_PER_ELEMENT;
    private static int BUFFER_SIZE_IN_BYTES = 2*AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);

    private static final float MIN_FREQ = 65.41f;

    private static final int PERMISSION_REQUEST_RECORD_AUDIO = 1;
    private static final int PERMISSION_ALL = 200;

    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    private boolean isRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button btnStartListen = (Button) findViewById(R.id.btnStartListen);
        TextView mainText = (TextView) findViewById(R.id.mainText);

        Log.i("Min buffer size", Integer.toString(AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)));
        Log.i("Buffer size", Integer.toString(BUFFER_SIZE_IN_BYTES));
        btnStartListen.setOnClickListener(btnListenOnClick);
    }

    private View.OnClickListener btnListenOnClick = new View.OnClickListener() {
        public void onClick(View v) {
            String[] permissions = { Manifest.permission.RECORD_AUDIO,
                                     Manifest.permission.WRITE_EXTERNAL_STORAGE };

            if (!hasPermissions(ListeningActivity_old.this, permissions)) {
                ActivityCompat.requestPermissions(ListeningActivity_old.this,
                        permissions, PERMISSION_ALL);
            }


            /*
            if (ContextCompat.checkSelfPermission(ListeningActivity.this,
                    Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {

                if (ActivityCompat.shouldShowRequestPermissionRationale(ListeningActivity.this,
                        Manifest.permission.RECORD_AUDIO)) {
                    ActivityCompat.requestPermissions(ListeningActivity.this,
                            new String[]{Manifest.permission.RECORD_AUDIO},
                            PERMISSION_REQUEST_RECORD_AUDIO);
                } else {
                    ActivityCompat.requestPermissions(ListeningActivity.this,
                            new String[]{Manifest.permission.RECORD_AUDIO},
                            PERMISSION_REQUEST_RECORD_AUDIO);
                }

            } else {
                // Permission already granted
                Button btn = (Button) v;
                if (!isRecording) {
                    try {
                        startRecording();
                    } catch (IllegalStateException | IllegalArgumentException e) {
                        Log.e("", e.getMessage());
                        return;
                    }

                    btn.setText(getResources().getString(R.string.stop_listening));
                } else {
                    stopRecording();
                    btn.setText(getResources().getString(R.string.start_listening));
                }
            }
        //}
    };

    private void startRecording() {
        //Verify buffer size
        if (AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT) > BUFFER_SIZE_IN_BYTES) {
            throw new IllegalArgumentException("AudioRead object cannot be created: insufficient window size");
        }

        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE, CHANNEL_CONFIG,
                AUDIO_FORMAT, BUFFER_SIZE_IN_BYTES);

        //Verify initialization
        if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
            throw new IllegalStateException("the initialization of the AudioRecord object was unsuccessful");
        }

        ArrayList mainBuffer = new ArrayList<>();
        isRecording = true;
        recorder.startRecording();
        recordingThread = new Thread(new ListeningRunnable(), "Listening thread");
        recordingThread.start();
    }

    private void writeToBuffer() {

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

    //Linearly spaced vector between first and last (closed interval)
    public float[] linspace(float first, float last, int total) {
        float[] vec = new float[total];
        for(int i = 0; i < total; ++i){
            vec[i] = first + i * (last - first) / (total - 1);
        }
        return vec;
    }

    private class ListeningRunnable implements Runnable {

        @Override
        public void run() {
            //final File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "recording.pcm");

            //Create ByteBuffer and write audio data into it
            ByteBuffer readBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE_IN_BYTES);
            int result = recorder.read(readBuffer, BUFFER_SIZE_IN_BYTES);
            if (result < 0) {
                throw new RuntimeException("Failed to read from audio buffer");
            }

            //First cast to short without copying because of PCM-16 encoding
            //Then cast to float for the FFT
            short[] shortArrayBuffer = readBuffer.asShortBuffer().array();
            float[] floatArrayBuffer = new float[shortArrayBuffer.length];

            for (int i = 0; i < shortArrayBuffer.length; ++i) {
                floatArrayBuffer[i] = (float) shortArrayBuffer[i];
            }

            //Take FFT and normalize
            float[] fft = Noise.real().optimized().fft(floatArrayBuffer);
            for (int i = 0; i < fft.length; ++i){
                fft[i] = fft[i] / fft.length;
            }

            float NYQUIST = SAMPLE_RATE/2;

            float[] frequencyAxisValues = linspace(0, NYQUIST, Math.round(fft.length / 2));
            for (int i = 0; i < frequencyAxisValues.length; ++i) {
                frequencyAxisValues[i] =
                        (float) (1200 * (Math.log10(frequencyAxisValues[i]) / Math.log10(2) / MIN_FREQ));
            }

            /*
            try {
                final FileOutputStream outStream = new FileOutputStream(file);
                while (isRecording) {
                    int result = recorder.read(readBuffer, 0, BUFFER_SIZE_IN_BYTES);
                    if (result < 0) {
                        throw new RuntimeException("Reading of audio readBuffer failed: " +
                                getBufferReadFailureReason(result));
                    }
                    mainBuffer.addAll(readBuffer)
                    outStream.write(readBuffer.array(), 0, BUFFER_SIZE_IN_BYTES);
                    readBuffer.clear();
                }
            } catch (IOException e){
                Log.e("Listening runnable", e.getMessage());
            }

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

    private String getBufferReadFailureReason(int errorCode) {
        switch (errorCode) {
            case AudioRecord.ERROR_INVALID_OPERATION:
                return "ERROR_INVALID_OPERATION";
            case AudioRecord.ERROR_BAD_VALUE:
                return "ERROR_BAD_VALUE";
            case AudioRecord.ERROR_DEAD_OBJECT:
                return "ERROR_DEAD_OBJECT";
            case AudioRecord.ERROR:
                return "ERROR";
            default:
                return "Unknown (" + errorCode + ")";
        }
    }

/*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
*/