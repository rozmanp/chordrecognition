package hu.bme.mit.soundtest;

import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

public class ListeningRunnable implements Runnable {
    private MainActivity mainActivity;
    AudioRecord recorder;
    private static volatile boolean isRecording = false;
    private ProcessingRunnable processingRunnable;

    ListeningRunnable(MainActivity activity){
        mainActivity = activity;

        //Verify buffer size
        if (AudioRecord.getMinBufferSize(Consts.SAMPLE_RATE, Consts.CHANNEL_CONFIG, Consts.AUDIO_FORMAT) > Consts.BUFFER_SIZE_IN_BYTES) {
            throw new IllegalArgumentException("AudioRecord object cannot be created: insufficient window size");
        }

        //Initialize AudioRecord object
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                Consts.SAMPLE_RATE, Consts.CHANNEL_CONFIG,
                Consts.AUDIO_FORMAT, Consts.BUFFER_SIZE_IN_BYTES);

        //Verify initialization
        if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
            throw new IllegalStateException("the initialization of the AudioRecord object was unsuccessful");
        }

        processingRunnable = new ProcessingRunnable(mainActivity);
    }

    @Override
    public void run() {
        startRecording();

        short[] window = new short[Consts.SAMPLES_PER_WINDOW];
        short[] buffer = new short[Consts.SAMPLES_PER_BUFFER];
        int buffersInWindow = 0;

        //Reading from microphone
        while(isRecording) {
            int result = recorder.read(buffer, 0, buffer.length);
            if (result < 0) {
                Log.e("ERROR", getBufferReadErrorCode(result));
            }

            if (buffersInWindow * Consts.SAMPLES_PER_BUFFER < Consts.SAMPLES_PER_WINDOW) {
                System.arraycopy(buffer, 0,
                                window, buffersInWindow * Consts.SAMPLES_PER_BUFFER,
                                Consts.SAMPLES_PER_BUFFER);

                ++buffersInWindow;
            } else {
                processingRunnable.setWindow(window.clone());
                Thread processingThread = new Thread(processingRunnable);
                processingThread.start();

                buffersInWindow = 0;
            }
        }
    }

    void startRecording() {
        isRecording = true;
        recorder.startRecording();
    }

    void stopRecording(){
        isRecording = false;
        recorder.stop();
    }

    boolean isRecording() { return isRecording; }

    private String getBufferReadErrorCode(int errorCode) {
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
}