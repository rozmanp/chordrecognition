package hu.bme.mit.soundtest;

import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

public class ListeningRunnable_short implements Runnable {
    private Thread processingThread = null;
    private ListeningActivity mainActivity;

    public ListeningRunnable_short(ListeningActivity activity){
        mainActivity = activity;
    }

    @Override
    public void run() {

        Log.e("Window size", Integer.toString(Consts.SAMPLES_PER_WINDOW));

        //Create immediate buffer and window buffer
        //First cast to short without copying because of PCM-16 encoding
        ByteBuffer readBuffer = ByteBuffer.allocateDirect(Consts.BUFFER_SIZE_IN_BYTES);
        ShortBuffer shortBuffer = ByteBuffer.allocateDirect(Consts.SAMPLES_PER_WINDOW * Consts.BYTES_PER_ELEMENT).asShortBuffer();
        short[] window = new short[shortBuffer.limit()];
        shortBuffer.get(window);
        int buffersInWindow = 0;

        int runCount = 0;

        while(ListeningActivity.isRecording) {
            int result = ListeningActivity.recorder.read(readBuffer, Consts.BUFFER_SIZE_IN_BYTES);
            if (result < 0) {
                throw new RuntimeException("Failed to read from audio buffer");
            }

            if (buffersInWindow * Consts.SAMPLES_PER_BUFFER < Consts.SAMPLES_PER_WINDOW) {
                ShortBuffer tempBuf = readBuffer.asShortBuffer();
                short[] tempArray = new short[tempBuf.limit()];
                tempBuf.get(tempArray);

                System.arraycopy(tempArray, 0,
                                 window, buffersInWindow * Consts.SAMPLES_PER_BUFFER,
                                 Consts.SAMPLES_PER_BUFFER);

                ++buffersInWindow;
            } else {

                ++runCount;

                //RUN FFT
                //ProcessingRunnable proc = new ProcessingRunnable(window.clone(),  mainActivity);
               //proc.run();
                //processingThread = new Thread(new..."Processing thread");
                //processingThread.start();

                buffersInWindow = 0;
                readBuffer.clear();
                window = new short[shortBuffer.limit()];
            }
        }
    }
}