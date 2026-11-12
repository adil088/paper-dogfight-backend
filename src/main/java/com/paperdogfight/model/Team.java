package com.paperdogfight.model;

/** The two sides of the game. */
public enum Team {
    RED, BLUE;

    /** The other side. */
    public Team opponent() {
        return this == RED ? BLUE : RED;
    }
}
