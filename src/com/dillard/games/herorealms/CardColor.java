package com.dillard.games.herorealms;

public enum CardColor {
    NONE,
    BLUE,
    GREEN,
    RED,
    WHITE;

    public static int NUM_VALUES = CardColor.values().length;

    public static CardColor forString(String str) {
        switch (str) {
        case "none":
            return NONE;
        case "blue":
            return BLUE;
        case "green":
            return GREEN;
        case "red":
            return RED;
        case "white":
            return WHITE;
        default: throw new RuntimeException(str);
        }
    }
}
