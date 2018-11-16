package hu.bme.mit.soundtest;

import android.util.Log;
import android.widget.TextView;

import com.paramsen.noise.Noise;
import com.paramsen.noise.NoiseOptimized;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.List;

public class ProcessingRunnable implements Runnable {
    private float[] window = new float[Consts.SAMPLES_PER_WINDOW];
    private ListeningActivity mainActivity;
    private int toleranceInCents = 30;
    private static int runCount = 0;
    FastFourierTransformer fftObj = new FastFourierTransformer(DftNormalization.UNITARY);

    public ProcessingRunnable(float[] shortWindow, ListeningActivity activity) {
        Log.e("src", Integer.toString(shortWindow.length));
        Log.e("dst", Integer.toString(window.length));


        window = shortWindow;
        //for (int i = 0; i < shortWindow.length; ++i) {
        //    window[i] = (float) shortWindow[i];
       // }

        this.mainActivity = activity;
    }

    //Linearly spaced vector between first and last (closed interval)
    private double[] linspace(double first, double last, int total) {
        double[] vec = new double[total];
        for (int i = 0; i < total; ++i) {
            vec[i] = first + i * (last - first) / (total - 1);
        }
        return vec;
    }

    //For transforming linear axis values to base 2 logarithmic
    //For cent representation
    private double[] log2space(double first, double last, int total) {
        double[] res = linspace(first, last, total);
        for (int i = 0; i < res.length; ++i) {
            res[i] = 1200 * (Math.log10(res[i] / Consts.MIN_FREQ) / Math.log10(2));
        }
        return res;
    }

    public void run() {
        //System.out.println(ByteOrder.nativeOrder());


        //NOISE FFT
        /*float[] fft = new float[window.length + 2];
        NoiseOptimized noise = Noise.real().optimized().init(window.length, true); //.fft(window, fft);
        fft = noise.fft(window);*/

        //APACHE FFT
        int windowSizeWithPadding = 0;
        int iter = 10;                                      //Buffer size won't be lower than 2^10
        while (windowSizeWithPadding < Consts.SAMPLES_PER_WINDOW) {
            windowSizeWithPadding = (int) Math.pow(2, iter);
            ++iter;
        }

        double[] windowInDoubles = new double[windowSizeWithPadding];
        for(int i = 0; i < window.length; ++i) {
            windowInDoubles[i] = (double) window[i];
        }

        DoubleBuffer db = DoubleBuffer.wrap(windowInDoubles);
        ByteBuffer bb = ByteBuffer.allocate(windowInDoubles.length*8);
        bb.asDoubleBuffer().put(db);

        Complex[] fftComp = fftObj.transform(db.array(), TransformType.FORWARD);
        double[] fft = new double[fftComp.length];
        for (int i = 0; i < fft.length; ++i) {
            fft[i] = fftComp[i].abs();
        }

        //Normalize and take absolute
        /*for (int i = 0; i < fft.length; ++i) {
            //if (fft[i] == Float.NEGATIVE_INFINITY || fft[i] == Float.POSITIVE_INFINITY)
            //    fft[i] = 0;

            fft[i] = Math.abs(fft[i] / window.length);
        }*/

        float NYQUIST = Consts.SAMPLE_RATE / 2;

        //Create vector for axis values
        double[] XAxisVals = log2space(0, NYQUIST, fft.length / 2);


        //Create 48 bins for 4 octaves of notes
        List<List<Double>> bins = new ArrayList<>();
        for (int i = 0; i < 48; ++i) {
            bins.add(new ArrayList<Double>());
        }

        //Select and group relevant values into bins
        for (int i = 0; i < XAxisVals.length; ++i) {
            //Ignore all frequencies below C2
            if (XAxisVals[i] < -toleranceInCents)
                continue;

            //Ignore all frequencies over B5 (+tolerance)
            if (XAxisVals[i] > 4800 - toleranceInCents)
                break;

            double difference = XAxisVals[i] % 100;               //Difference from the closest lower note
            int noteNr = (int) Math.round(XAxisVals[i] / 100);    //Closest note

            //If it is within the range, add to the corresponding bin
            if (difference < toleranceInCents || difference > 100 - toleranceInCents){

                bins.get(noteNr).add(fft[i]);
            }
        }

        //Maximum selection in each bit
        double[] maxVals = new double[48];
        for (int i = 0; i < bins.size(); ++i){
            double thisMax = 0;

            if (bins.get(i) != null) {
                for (int j = 0; j < bins.get(i).size(); ++j) {
                    double thisVal = bins.get(i).get(j);

                    if (thisVal > thisMax) {
                        thisMax = thisVal;
                    }
                }
            } else {
                Log.e("WARNING", "Empty bin: #" + i);
            }

            maxVals[i] = thisMax;
        }

        //Creating chroma vector by summing
        float chromaVector[] = new float[12];
        for (int i = 0; i < maxVals.length; ++i) {
            chromaVector[i % 12] += maxVals[i];
        }

        //Normalize chroma vector
        float maxAggregatedVal = Float.NEGATIVE_INFINITY;
        for (float chVal : chromaVector) {
            if (chVal > maxAggregatedVal) {
                maxAggregatedVal = chVal;
            }
        }
        for (int i = 0; i < chromaVector.length; ++i){
            chromaVector[i] /= maxAggregatedVal;
        }

        //Pattern matching
        float[][] deltas = new float[Chords.values().length][12];
        for (int i = 0; i < Chords.values().length; ++i) {
            Chords thisChord = Chords.values()[i];

            for (int shiftBy = 0; shiftBy < 12; ++shiftBy) {
                Float[] invMask = Chords.getInverse(thisChord, shiftBy);

                for (int bit = 0; bit < invMask.length; ++bit) {
                    deltas[i][shiftBy] += invMask[bit] * Math.pow(chromaVector[bit], 2);
                }
                deltas[i][shiftBy] = (float) Math.sqrt(deltas[i][shiftBy]);
            }
        }

        //Select lowest delta value
        ArrayList<Float> vals;
        ArrayList<Chords> chords;
        ArrayList<Integer> notes;
/*
        for (int i = 0; i < Chords.values().length; ++i){
            for (int j = 0; j < 12; ++j){
                Log.i("Current",Chords.getNoteName(j) +
                        Chords.getType(Chords.values()[i], true) + ", value: " + deltas[i][j]);
                if (deltas[i][j] < minDelta) {
                    minDelta = deltas[i][j];
                    chordIndex = i;
                    deltaIndex = j;
                }
            }
        }
*/

        //Select lowest delta value
        float minDelta = Float.MAX_VALUE;
        int chordIndex = -1, deltaIndex = -1;
        for (int i = 0; i < Chords.values().length; ++i){
            for (int j = 0; j < 12; ++j){
                if (deltas[i][j] < minDelta) {
                    minDelta = deltas[i][j];
                    chordIndex = i;
                    deltaIndex = j;
                }
            }
        }

        mainActivity.runOnUiThread(new Runnable() {
            int finalChordIndex = -1, finalShiftIndex = -1;

            //Save variables to local attributes to make them accessible in run()
            //Runnable return type for method linking
            Runnable setArgs(int chord, int note) {
                finalChordIndex = chord;
                finalShiftIndex = note;
                return this;
            }

            @Override
            public void run() {
                TextView textView = mainActivity.findViewById(R.id.mainText);

                textView.setText(String.format(
                        "%s%s",
                        Chords.getNoteName(finalShiftIndex),
                        Chords.getType(Chords.values()[finalChordIndex], true)));
            }
        }.setArgs(chordIndex, deltaIndex));

        //++runCount;
    }
}
