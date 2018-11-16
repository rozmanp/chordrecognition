package hu.bme.mit.soundtest;

import android.app.Activity;
import android.media.AudioRecord;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.util.Log;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

public class ListeningRunnable implements Runnable {
    private Thread processingThread = null;
    private ListeningActivity mainActivity;

    public ListeningRunnable(ListeningActivity activity){
        mainActivity = activity;
    }

    //@RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void run() {

        Log.e("Window size", Integer.toString(Consts.SAMPLES_PER_WINDOW));

        //Create immediate buffer and window buffer
        //First cast to short without copying because of PCM-16 encoding
        ByteBuffer readBuffer = ByteBuffer.allocateDirect(Consts.BUFFER_SIZE_IN_BYTES);
        readBuffer.order(ByteOrder.nativeOrder());

        FloatBuffer floatBuffer = ByteBuffer.allocateDirect(Consts.SAMPLES_PER_WINDOW * Consts.BYTES_PER_ELEMENT).asFloatBuffer();
        float[] window = new float[floatBuffer.limit()];
        floatBuffer.get(window);
        int buffersInWindow = 0;

        while(ListeningActivity.isRecording) {
            int result = ListeningActivity.recorder.read(readBuffer, Consts.BUFFER_SIZE_IN_BYTES);
            float[] testArray = new float[readBuffer.asFloatBuffer().limit()];
            //int result = ListeningActivity.recorder.read(testArray,0, testArray.length, AudioRecord.READ_BLOCKING);
            if (result < 0) {
                Log.e("ERROR", getBufferReadFailureReason(result));
            }

            if (buffersInWindow * Consts.SAMPLES_PER_BUFFER < Consts.SAMPLES_PER_WINDOW) {
                FloatBuffer tempBuf = readBuffer.asFloatBuffer();
                float[] tempArray = new float[tempBuf.limit()];
                tempBuf.get(tempArray);

                System.arraycopy(tempArray, 0,
                                 window, buffersInWindow * Consts.SAMPLES_PER_BUFFER,
                                 Consts.SAMPLES_PER_BUFFER);

                ++buffersInWindow;
            } else {
                Thread processingThread = new Thread(new ProcessingRunnable(window.clone(),  mainActivity));
                processingThread.start();
                //ProcessingRunnable proc = new ProcessingRunnable(window.clone(),  mainActivity);
                //proc.run();

                //processingThread = new Thread(new..."Processing thread");
                //processingThread.start();

                buffersInWindow = 0;
                readBuffer.clear();
                window = new float[floatBuffer.limit()];
            }
        }
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
}