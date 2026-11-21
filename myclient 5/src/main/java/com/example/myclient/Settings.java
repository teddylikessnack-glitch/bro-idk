package com.example.myclient;

import java.util.Arrays;

public final class Settings {
    public static final String[] NAMES = {
            "FPS", "CPS", "Coordinates", "Direction", "Speed",
            "Clock", "Memory", "Armor & Item", "Keystrokes"
    };
    public static final int FPS = 0, CPS = 1, COORDS = 2, DIRECTION = 3, SPEED = 4,
            CLOCK = 5, MEMORY = 6, ARMOR = 7, KEYSTROKES = 8;

    public static final boolean[] ON = new boolean[NAMES.length];

    static {
        Arrays.fill(ON, true);
    }

    private Settings() {}

    public static boolean on(int i) {
        return ON[i];
    }
}
