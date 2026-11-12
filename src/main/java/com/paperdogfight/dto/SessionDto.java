package com.paperdogfight.dto;

import com.paperdogfight.model.Team;

/**
 * Returned when a player creates or joins a game. The token proves who the player is and must be
 * sent in the {@code X-Player-Token} header on roll, move and rematch requests.
 */
public record SessionDto(String gameId, Team team, String token, GameStateDto state) {
}
