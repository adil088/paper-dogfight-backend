package com.paperdogfight.model;

/**
 * One entry of the move log.
 *
 * @param turnNumber      turn counter at the time of the move (starts at 1)
 * @param team            team that moved
 * @param type            {@link MoveType#ADVANCE} or {@link MoveType#SKIP}
 * @param jetId           jet that advanced (null for a skip)
 * @param dieValue        the die roll (0 for a skip)
 * @param advanced        squares the line actually grew (can be less than the die when capped)
 * @param hit             whether this move destroyed an enemy jet
 * @param destroyedJetId  id of the destroyed enemy jet, or null
 */
public record Move(int turnNumber, Team team, MoveType type, String jetId, int dieValue,
                   int advanced, boolean hit, String destroyedJetId) {
}
