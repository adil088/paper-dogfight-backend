package com.paperdogfight.model;

/** Lifecycle of a game. */
public enum GameStatus {
    /** Created, waiting for the second player to join. */
    WAITING,
    /** Both players present; turns are being played. */
    IN_PROGRESS,
    /** A winner was decided, or the game ended in a draw. */
    FINISHED
}
