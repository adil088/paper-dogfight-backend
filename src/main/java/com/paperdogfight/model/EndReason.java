package com.paperdogfight.model;

/** Why a finished game ended. */
public enum EndReason {
    /** A team crossed out the required number of enemy jets. */
    ENEMY_JETS_DESTROYED,
    /** Nobody could advance any more; the team with more surviving jets won. */
    STALEMATE_MOST_JETS,
    /** Nobody could advance any more and the survivors were level (or the tie-break is disabled). */
    STALEMATE_DRAW
}
