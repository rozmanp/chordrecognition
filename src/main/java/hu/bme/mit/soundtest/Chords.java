package hu.bme.mit.soundtest;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum Chords {
    MAJOR (new Float[] {1f, 0f, 0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 0f, 0f}),
    MINOR (new Float[] {1f, 0f, 0f, 1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f});
    /*
    DIM   (new Float[] {1f, 0f, 0f, 1f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f}),
    AUG   (new Float[] {1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f}),
    SUS2  (new Float[] {1f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f}),
    SUS4  (new Float[] {1f, 0f, 0f, 0f, 0f, 1f, 0f, 1f, 0f, 0f, 0f, 0f});
    */
    private final List<Float> bitmask;

    Chords(Float[] ch) {
        if(ch.length != 12){
            throw new InvalidParameterException("Argument does not represent a chroma vector");
        }
        this.bitmask = Arrays.asList(ch);
    }

    private List<Float> getMaskAsList() { return bitmask; }

    public static Float[] shift(Chords chordType, int distance) {
        List<Float> bitmask = new ArrayList<>(chordType.getMaskAsList());

        Collections.rotate(bitmask, distance);
        return bitmask.toArray(new Float[12]);
    }

    //Return inverse of a bitmask
    public static Float[] getInverse(Chords chordType, int shiftBy) {
        Float[] shift = shift(chordType, shiftBy);
        for (int i = 0; i < shift.length; ++i) {
            shift[i] = 1 - shift[i];
        }

        return shift;
    }

    public static String getType(Chords chord, boolean abbreviate) {
        switch (chord) {
            case MAJOR:
                return (abbreviate) ? "" : "major";
            case MINOR:
                return (abbreviate) ? "m" : "minor";
            /*
            case DIM:
                return (abbreviate) ? "dim" : "diminished";
            case AUG:
                return (abbreviate) ? "aug" : "augmented";
            case SUS2:
                return (abbreviate) ? "sus2" : "suspended 2nd";
            case SUS4:
                return (abbreviate) ? "sus4" : "suspended 4th";
             */
            default:
                throw new IllegalArgumentException("Invalid chord type");
        }
    }

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
