package com.paperdogfight.service;

import java.security.SecureRandom;

/** Source of die rolls; tests plug in a scripted one. */
@FunctionalInterface
public interface DieRoller {

    /** Returns a value between 1 and {@code sides}, inclusive. */
    int roll(int sides);

    /** A fair, server-side random die. */
    static DieRoller secure() {
        SecureRandom random = new SecureRandom();
        return sides -> random.nextInt(sides) + 1;
    }
}
