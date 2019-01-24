package hu.bme.mit.soundtest;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum Chords {
    MAJOR (new Double[] {1d, 0d, 0d, 0d, 1d, 0d, 0d, 1d, 0d, 0d, 0d, 0d}, "", "major"),
    MINOR (new Double[] {1d, 0d, 0d, 1d, 0d, 0d, 0d, 1d, 0d, 0d, 0d, 0d}, "m", "minor");


    private final List<Double> bitmask;
    private final String notation;
    private final String name;

    Chords(Double[] ch, String notation, String name) {
        if(ch.length != 12){
            throw new InvalidParameterException("Argument does not represent a chroma vector");
        }
        this.bitmask = Arrays.asList(ch);
        this.notation = notation;
        this.name = name;
    }

    private List<Double> getMaskAsList() { return bitmask; }

    public static Double[] shift(Chords chordType, int distance) {
        List<Double> bitmask = new ArrayList<>(chordType.getMaskAsList());


        Collections.rotate(bitmask, distance);
        return bitmask.toArray(new Double[12]);
    }

    //Return inverse of a bitmask
    public static Double[] getInverse(Chords chordType, int shiftBy) {
        Double[] shift = shift(chordType, shiftBy);
        for (int i = 0; i < shift.length; ++i) {
            shift[i] = 1 - shift[i];
        }

        return shift;
    }

    public String getNotation() { return this.notation; }

    public static String getNoteName(int distanceFromC) {
        switch (distanceFromC) {
            case 0:
                return "C";
            case 1:
                return "C#";
            case 2:
                return "D";
            case 3:
                return "D#";
            case 4:
                return "E";
            case 5:
                return "F";
            case 6:
                return "F#";
            case 7:
                return "G";
            case 8:
                return "G#";
            case 9:
                return "A";
            case 10:
                return "A#";
            case 11:
                return "B";
            default:
                throw new IllegalArgumentException("Invalid note ID: " + distanceFromC);
        }
    }
}
