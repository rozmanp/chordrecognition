package hu.bme.mit.soundtest;

import android.media.AudioFormat;
import android.media.AudioRecord;

public final class Consts {
    //Configuration values
    public static final int SAMPLE_RATE = 44100;
    public static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    public static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    //Calculated config values
    public static final int MIN_BUFFER_SIZE_IN_BYTES =
            AudioRecord.getMinBufferSize(Consts.SAMPLE_RATE,
                    Consts.CHANNEL_CONFIG, Consts.AUDIO_FORMAT);
    public static final float WINDOW_SIZE_IN_SEC = 0.3f;
    public static final int BUFFER_SIZE_IN_BYTES = 2 * MIN_BUFFER_SIZE_IN_BYTES;
    public static final int BYTES_PER_ELEMENT = 2;      //PCM-16 encoding
    public static final int SAMPLES_PER_BUFFER = BUFFER_SIZE_IN_BYTES / BYTES_PER_ELEMENT;

    private static final int MIN_SAMPLES_PER_WINDOW = (int) (Consts.SAMPLE_RATE * Consts.WINDOW_SIZE_IN_SEC + 1);
    public static final int SAMPLES_PER_WINDOW = MIN_SAMPLES_PER_WINDOW + (Consts.SAMPLES_PER_BUFFER - (MIN_SAMPLES_PER_WINDOW % Consts.SAMPLES_PER_BUFFER));

    public static final float NYQUIST = Consts.SAMPLE_RATE / 2;
    public static final float MIN_FREQ = 65.41f;        //note C2
    public static final int TOLERANCE_IN_CENTS = 20;

    //Permissions
    public static final int PERMISSION_RECORD_AUDIO = 1;

}
