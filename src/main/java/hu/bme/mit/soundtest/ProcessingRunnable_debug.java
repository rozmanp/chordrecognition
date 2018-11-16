package hu.bme.mit.soundtest;

import android.util.Log;
import android.widget.TextView;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.List;

public class ProcessingRunnable_debug implements Runnable {
    private float[] window = new float[Consts.SAMPLES_PER_WINDOW];
    private ListeningActivity mainActivity;
    private int toleranceInCents = 30;
    private static int runCount = 0;
    FastFourierTransformer fftObj = new FastFourierTransformer(DftNormalization.UNITARY);

    public ProcessingRunnable_debug(float[] shortWindow, ListeningActivity activity) {
        Log.e("src", Integer.toString(shortWindow.length));
        Log.e("dst", Integer.toString(window.length));


        window = shortWindow;
        //for (int i = 0; i < shortWindow.length; ++i) {
        //    window[i] = (float) shortWindow[i];
       // }

        //for (int i = 0; i < 20; ++i) {
        //    Log.i("WINDOW BEFORE-AFTER", shortWindow[i] + ", " + window[i]);
        //}

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

    //For transforming linear axis values to logarithmic
    //For cent representation
    private double[] logspace(double first, double last, int total) {
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

        /*
        //<RECORDING TO FILES>
        String folder = null;

        System.out.println(Environment.getExternalStorageState());
        File file = new File(Environment.getExternalStorageDirectory(), "recording" + runCount + ".pcm");

        try {
            if (!file.createNewFile())
                System.out.println("createNewFile exception");

            FileOutputStream outStream = new FileOutputStream(file, true);
            FloatBuffer sb = FloatBuffer.wrap(window);
            ByteBuffer bb2 = ByteBuffer.allocate(window.length*4);
            bb2.asFloatBuffer().put(sb);
            outStream.write(bb2.array());
            outStream.close();

        } catch (java.io.FileNotFoundException e){
            System.out.println("FileNotFoundException: " + e.getMessage());
        } catch (java.io.IOException e) {
            System.out.println("IOException: " + e.getMessage());
        } catch (java.lang.SecurityException e) {
            System.out.println("SecurityException: " + e.getMessage());
        }
        //</RECORDING TO FILES>

        BufferedWriter bufferedWriter;
        //<RECORDING UNCAST BUFFER>

        try {
            bufferedWriter = new BufferedWriter(new FileWriter(new File(Environment.getExternalStorageDirectory(), "float_buffer" + runCount + ".txt")));
            for (float f : window) {
                bufferedWriter.write(Float.toString(f));
                bufferedWriter.newLine();
            }

            bufferedWriter.flush();
        } catch (IOException e) {
            Log.e("ERROR", e.getMessage());
        }
        //</RECORDING UNCAST BUFFER>


        //<RECORDING CAST BUFFER>
        try {
            bufferedWriter = new BufferedWriter(new FileWriter(new File(Environment.getExternalStorageDirectory(), "double_buffer" + runCount + ".txt")));
            for (double d : db.array()) {
                bufferedWriter.write(Double.toString(d));
                bufferedWriter.newLine();
            }

            bufferedWriter.flush();
        } catch (IOException e) {
            Log.e("ERROR", e.getMessage());
        }

        //</RECORDING CAST BUFFER>

        //<WRITING FFT TO FILES>
        bufferedWriter = null;
        File file2 = new File(Environment.getExternalStorageDirectory(), "fftdata" + runCount + ".txt");
        try {
            bufferedWriter = new BufferedWriter(new FileWriter(file2));
            for (double f : fft){
                bufferedWriter.write(f + " ");
            }

            bufferedWriter.flush();
            bufferedWriter.close();

        } catch (IOException e) {
            Log.e("ERROR", e.getMessage());
        }

        //</WRITING FFT TO FILES>



        //Normalize and take absolute
        /*for (int i = 0; i < fft.length; ++i) {
            //if (fft[i] == Float.NEGATIVE_INFINITY || fft[i] == Float.POSITIVE_INFINITY)
            //    fft[i] = 0;

            fft[i] = Math.abs(fft[i] / window.length);
        }*/

        float NYQUIST = Consts.SAMPLE_RATE / 2;

        //Create vector for axis values
        double[] XAxisVals = logspace(0, NYQUIST, fft.length / 2);

        /*
        //<WRITE LOGSPACE VALUES TO FILE>
        try {
            bufferedWriter = new BufferedWriter(new FileWriter(new File(Environment.getExternalStorageDirectory(), "logspace" + runCount + ".txt")));
            for (double f : XAxisVals) {
                bufferedWriter.write(Double.toString(f));
                bufferedWriter.newLine();
            }

            bufferedWriter.flush();
        } catch (IOException e) {
            Log.e("ERROR", e.getMessage());
        }
        //</WRITE LOGSPACE VALUES TO FILE>
        */

        //Create 48 bins for 4 octaves of notes
        List<List<Double>> bins = new ArrayList<>();
        for (int i = 0; i < 48; ++i){
            bins.add(new ArrayList<Double>());
        }

        //Select and group relevant values into bins
        for (int i = 0; i < XAxisVals.length; ++i) {
            //Ignore all frequencies below C2
            if (XAxisVals[i] < -toleranceInCents)
                continue;

            //Ignore all frequencies over B5
            if (XAxisVals[i] > 4800 - toleranceInCents)
                break;

            double difference = XAxisVals[i] % 100;              //Difference from the closest lower note
            int noteNr = (int) Math.round(XAxisVals[i] / 100);    //Closest note

            //System.out.println("diff: " + Float.toString(difference) + ", freq: " + XAxisVals[i] + ", noteNr: " + noteNr);

            //If it is within the range, add to the corresponding bin
            if (difference < toleranceInCents || difference > 100 - toleranceInCents){
                //if (bins.get(noteNr) == null){
                //    bins.set(noteNr, new ArrayList<Float>());
                //}

                //System.out.println("diff: " + Float.toString(difference) + ", freq: " + XAxisVals[i] + ", noteNr: " + noteNr);

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
/*
        try {
            bufferedWriter = new BufferedWriter(new FileWriter(new File(Environment.getExternalStorageDirectory(), "max_vals" + runCount + ".txt")));
            for (double d : maxVals) {
                bufferedWriter.write(Double.toString(d));
                bufferedWriter.newLine();
            }

            bufferedWriter.flush();
        } catch (IOException e) {
            Log.e("ERROR", e.getMessage());
        }
*/


        /*for (int i = 0; i < maxVals.length; ++i) {
            System.out.println(Chords.getNoteName(i % 12) + ": " + maxVals[i]);
        }*/

        //Creating chroma vector by summing
        float chromaVector[] = new float[12];
        for (int i = 0; i < maxVals.length; ++i) {
            chromaVector[i % 12] += maxVals[i];
        }

        /*for (int i = 0; i < chromaVector.length; ++i) {
            System.out.println(chromaVector[i]);
        }*/

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

        /*for (float ch : chromaVector) {
            System.out.println(ch);
        }*/

        /*
        try {
            bufferedWriter = new BufferedWriter(new FileWriter(new File(Environment.getExternalStorageDirectory(), "chroma" + runCount + ".txt")));
            for (float d : chromaVector) {
                bufferedWriter.write(Double.toString(d));
                bufferedWriter.newLine();
            }

            bufferedWriter.flush();
        } catch (IOException e) {
            Log.e("ERROR", e.getMessage());
        }*/



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
                //Log.e("DELTA #" + i + "->" + shiftBy, Float.toString(deltas[i][shiftBy]));
            }
        }
        //Log.e("", "NEXT-------------------------");

        //Select lowest delta value
        float minDelta = Float.MAX_VALUE;
        int chordIndex = -1, deltaIndex = -1;
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


        /*double maxVal = Float.MIN_VALUE;
        double maxPos = Float.MIN_VALUE;
        for(int i = 0; i < XAxisVals.length && XAxisVals[i] <= 4800f; ++i) {
            if(fft[i] > maxVal){
                maxVal = fft[i];
                maxPos = XAxisVals[i];
            }
        }*/

        //final String note = Chords.getNoteName(Math.abs(Math.round(((maxPos % 1200) - 100) / 100)));  //If starts from B
        //final String note = Chords.getNoteName((int)Math.abs(Math.round((maxPos % 1200) / 100)));

        //final double maxToPrint = maxPos;


        final int finalChordIndex = chordIndex;
        final int finalShiftIndex = deltaIndex;

        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView textView = mainActivity.findViewById(R.id.mainText);
                //textView.setText(note);
                //textView.setText(Float.toString(maxToPrint));
                textView.setText(Chords.getNoteName(finalShiftIndex) +
                        Chords.getType(Chords.values()[finalChordIndex], true));
            }
        });

        ++runCount;
    }
}
