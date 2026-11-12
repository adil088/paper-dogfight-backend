package com.paperdogfight.model;

/** Kind of entry in the move log. */
public enum MoveType {
    /** A normal turn: a die was rolled and a jet's line was extended. */
    ADVANCE,
    /** A team had no jet that could advance, so its turn was skipped. */
    SKIP
}
