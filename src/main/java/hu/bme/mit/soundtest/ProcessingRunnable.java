package hu.bme.mit.soundtest;

import android.util.Log;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ProcessingRunnable implements Runnable {
    private short[] window;
    private MainActivity mainActivity;

    private FastFourierTransformer fftObj = new FastFourierTransformer(DftNormalization.UNITARY);
    private double[] hammingWin;
    private double[] XAxisVals;
    private int windowSizeWithPadding;

    //Constructor is used to compute arrays beforehand that will remain the same with every window
    ProcessingRunnable(MainActivity activity) {
        mainActivity = activity;

        //Computing the values of the Hamming window
        hammingWin = hamming(Consts.SAMPLES_PER_WINDOW);

        //Determine the nearest greater power of 2 to the window size
        //FFT only accepts input arrays with the length of a power of 2
        //So the window needs to be padded with 0s until its length is a power of 2
        windowSizeWithPadding = 0;
        int iter = 0;
        while (windowSizeWithPadding < Consts.SAMPLES_PER_WINDOW) {
            windowSizeWithPadding = (int) Math.pow(2, iter);
            ++iter;
        }

        //As long as the windows are of the same length,
        //the size of the output of the FFT will remain the same
        XAxisVals = log2space(0, Consts.NYQUIST, windowSizeWithPadding / 2);
    }

    //Window needs to be set every time a thread starts
    void setWindow(short[] win) {
        window = win;
    }

    //Linearly spaced vector between first and last (closed interval)
    private double[] linspace(double first, double last, int total) {
        double[] vec = new double[total];
        for (int i = 0; i < total; ++i) {
            vec[i] = first + i * (last - first) / (total - 1);
        }
        return vec;
    }

    //Generate n-point Hamming window
    private double[] hamming(int n){
        double[] w = new double[n];
        for (int i = 0; i < n; ++i) {
            w[i] = 0.54 - 0.46*Math.cos((2 * Math.PI * i) / (n - 1));
        }
        return w;
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
        // Apply Hamming window
        for(int i = 0; i < window.length; ++i){
            window[i] *= hammingWin[i];
        }

        //APACHE FFT
        //Cast window content to doubles for the FFT
        //Unset values will remain 0 (padding)
        double[] windowInDoubles = new double[windowSizeWithPadding];
        for(int i = 0; i < window.length; ++i) {
            windowInDoubles[i] = (double) window[i];
        }

        Complex[] fftComp = fftObj.transform(windowInDoubles, TransformType.FORWARD);
        double[] fft = new double[fftComp.length];
        for (int i = 0; i < fft.length; ++i) {
            fft[i] = Math.pow(fftComp[i].abs(), 2);
        }

        //Create 48 bins for 4 octaves of notes
        List<List<Double>> bins = new ArrayList<>();
        for (int i = 0; i < 48; ++i) {
            bins.add(new ArrayList<Double>());
        }

        //Select and group relevant values into bins
        for (int i = 0; i < XAxisVals.length; ++i) {
            //Ignore all frequencies below and including C2
            if (XAxisVals[i] < Consts.TOLERANCE_IN_CENTS)
                continue;

            //Ignore all frequencies over C6 (+tolerance)
            if (XAxisVals[i] > 4800 + Consts.TOLERANCE_IN_CENTS)
                break;

            double difference = XAxisVals[i] % 100;               //Difference from the closest lower note
            int noteNr = (int) Math.round(XAxisVals[i] / 100) - 1;    //Closest note

            //If it is within the range, add to the corresponding bin
            if (difference < Consts.TOLERANCE_IN_CENTS || difference > 100 - Consts.TOLERANCE_IN_CENTS){
                bins.get(noteNr).add(fft[i]);
            }
        }

        //Maximum selection in each bin
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
                Log.w("WARNING", "Empty bin: #" + i);
            }

            maxVals[i] = thisMax;
        }

        //Creating chroma vector by summing
        double chromaVector[] = new double[12];
        for (int i = 0; i < maxVals.length; ++i) {
            chromaVector[(i+1) % 12] += maxVals[i];
        }

        //Normalize chroma vector
        double maxAggregatedVal = Float.NEGATIVE_INFINITY;
        for (double chVal : chromaVector) {
            if (chVal > maxAggregatedVal) {
                maxAggregatedVal = chVal;
            }
        }
        for (int i = 0; i < chromaVector.length; ++i){
            chromaVector[i] /= maxAggregatedVal;
        }

        //Pattern matching
        ArrayList<ChordData> chordDiffs = new ArrayList<>();
        for (int i = 0; i < Chords.values().length; ++i) {
            Chords thisChord = Chords.values()[i];

            for (int shiftBy = 0; shiftBy < 12; ++shiftBy) {
                ChordData newChordData = new ChordData(shiftBy, thisChord);
                Double[] invMask = Chords.getInverse(thisChord, shiftBy);

                for (int bit = 0; bit < invMask.length; ++bit) {
                    newChordData.delta += invMask[bit] *  Math.pow(chromaVector[bit], 2);
                }

                newChordData.delta = Math.sqrt(newChordData.delta);
                chordDiffs.add(newChordData);
            }
        }

        //Sorting the chords by their score at pattern matching
        Collections.sort(chordDiffs, new Comparator<ChordData>() {
            @Override
            public int compare(ChordData o1, ChordData o2) {
                return o1.delta.compareTo(o2.delta);
            }
        });

        mainActivity.runOnUiThread(new Runnable() {
            ArrayList<ChordData> chordDiffs;

            //Save variables to local attributes to make them accessible in run()
            //Runnable return type for method chaining
            Runnable setArgs(ArrayList<ChordData> cData) {
                chordDiffs = cData;
                return this;
            }

            @Override
            public void run() {
                mainActivity.updateUI(chordDiffs);
            }
        }.setArgs(chordDiffs));
    }

    //Helper class for preserving the name of the chords after sorting
    class ChordData {
        int note;
        Chords chordType;
        Double delta;

        ChordData (int note, Chords chordType) {
            this.note = note;
            this.chordType = chordType;
            this.delta = 0d;
        }
    }
}
