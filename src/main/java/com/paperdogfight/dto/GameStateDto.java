package com.paperdogfight.dto;

import com.paperdogfight.config.GameRulesConfig;
import com.paperdogfight.model.EndReason;
import com.paperdogfight.model.Game;
import com.paperdogfight.model.GameStatus;
import com.paperdogfight.model.Jet;
import com.paperdogfight.model.Lane;
import com.paperdogfight.model.Move;
import com.paperdogfight.model.Team;
import java.util.List;

/**
 * Full public snapshot of a game, sent over REST and broadcast over WebSocket. It never contains
 * the player tokens.
 *
 * @param event what just happened: CREATED, JOINED, ROLL, MOVE, HIT, GAME_OVER, REMATCH_REQUESTED,
 *              REMATCH_STARTED or STATE (plain read)
 */
public record GameStateDto(
        String id,
        long version,
        GameStatus status,
        Team currentTurn,
        Team winner,
        EndReason endReason,
        int turnNumber,
        Integer pendingRoll,
        int rollCount,
        Integer lastRollValue,
        Team lastRollTeam,
        List<JetDto> jets,
        List<Move> moves,
        List<String> selectableJetIds,
        List<Team> rematchVotes,
        ConfigDto config,
        String event) {

    /** A jet as seen by clients. */
    public record JetDto(String id, Team team, Lane lane, int number, boolean alive,
                         int lineLengthInSquares, Team destroyedBy) {
        static JetDto of(Jet jet) {
            return new JetDto(jet.getId(), jet.getTeam(), jet.getLane(), jet.getNumber(), jet.isAlive(),
                    jet.getLineLengthInSquares(), jet.getDestroyedBy());
        }
    }

    /** Board geometry and the rules the UI needs to draw lines and explain the game. */
    public record ConfigDto(int cols, int rows, List<Integer> laneGaps, int hitTolerance,
                            int jetsToDestroyToWin, boolean exactRollToHit) {
    }

    public static GameStateDto from(Game game, GameRulesConfig rules, List<String> selectableJetIds,
                                    String event) {
        return new GameStateDto(
                game.getId(),
                game.getVersion(),
                game.getStatus(),
                game.getCurrentTurn(),
                game.getWinner(),
                game.getEndReason(),
                game.getTurnNumber(),
                game.getPendingRoll(),
                game.getRollCount(),
                game.getLastRollValue(),
                game.getLastRollTeam(),
                game.getJets().stream().map(JetDto::of).toList(),
                List.copyOf(game.getMoves()),
                List.copyOf(selectableJetIds),
                List.copyOf(game.getRematchVotes()),
                new ConfigDto(rules.getCols(), rules.getRows(), List.copyOf(rules.getLaneGaps()),
                        rules.getHitTolerance(), rules.getJetsToDestroyToWin(), rules.isExactRollToHit()),
                event);
    }
}
