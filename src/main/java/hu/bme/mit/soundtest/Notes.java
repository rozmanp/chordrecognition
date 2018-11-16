package hu.bme.mit.soundtest;

import java.security.InvalidParameterException;

public enum Notes {
    C (0), C_SHARP (1),
    D (2), D_SHARP (3),
    E (4),
    F (5), F_SHARP (6),
    G (7), G_SHARP (8),
    A (9), A_SHARP (10),
    B (11);

    int value;

    Notes(int value){ this.value = value; }

    public int getValue() { return value; }

    public static String getName(int distanceFromC) {
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
                throw new InvalidParameterException("Invalid note ID");
        }
    }
}
