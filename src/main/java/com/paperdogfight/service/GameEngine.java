package com.paperdogfight.service;

import com.paperdogfight.config.GameRulesConfig;
import com.paperdogfight.model.EndReason;
import com.paperdogfight.model.Game;
import com.paperdogfight.model.GameStatus;
import com.paperdogfight.model.Jet;
import com.paperdogfight.model.Move;
import com.paperdogfight.model.MoveType;
import com.paperdogfight.model.Team;
import java.util.List;

/**
 * Every rule of Paper Dogfight, with no Spring, no I/O and no hidden randomness (the die is
 * injected), so it can be unit-tested in isolation.
 *
 * <p>A turn is: {@link #roll} (server-generated die) then {@link #move} (pick a jet; its line grows
 * by the die value). After a move the turn passes to the opponent; if that team has no jet that can
 * still advance its turn is skipped, and if neither team can advance the game ends.
 */
public class GameEngine {

    private final GameRulesConfig rules;
    private final DieRoller die;

    public GameEngine(GameRulesConfig rules, DieRoller die) {
        this.rules = rules;
        this.die = die;
    }

    /** Starts (or restarts) a game: fresh board, first team to move. */
    public void begin(Game game) {
        game.resetBoard();
        game.setStatus(GameStatus.IN_PROGRESS);
        game.setCurrentTurn(rules.getFirstTeam());
        game.setTurnNumber(1);
        startTurn(game);
    }

    /**
     * Rolls the die for the team whose turn it is.
     *
     * @return the rolled value
     */
    public int roll(Game game, Team team) {
        requireActive(game);
        requireTurn(game, team);
        if (game.getPendingRoll() != null) {
            throw new GameRuleException(ErrorCode.ALREADY_ROLLED,
                    "You already rolled this turn. Pick one of your jets.");
        }
        int value = die.roll(rules.getDieSides());
        if (value < 1 || value > rules.getDieSides()) {
            throw new IllegalStateException("Die produced an out-of-range value: " + value);
        }
        game.setPendingRoll(value);
        game.setRollCount(game.getRollCount() + 1);
        game.setLastRollValue(value);
        game.setLastRollTeam(team);
        if (selectableJets(game, team).isEmpty()) {
            // No jet can use this roll (every line is too close to its target): the turn passes.
            game.getMoves().add(new Move(game.getTurnNumber(), team, MoveType.SKIP, null, value, 0, false, null));
            game.setPendingRoll(null);
            game.setCurrentTurn(team.opponent());
            game.setTurnNumber(game.getTurnNumber() + 1);
            startTurn(game);
        }
        return value;
    }

    /**
     * Extends the chosen jet's line by the pending die value, destroys the enemy jet if it is
     * reached, then hands the turn over (or ends the game).
     */
    public Move move(Game game, Team team, String jetId) {
        requireActive(game);
        requireTurn(game, team);
        Integer dieValue = game.getPendingRoll();
        if (dieValue == null) {
            throw new GameRuleException(ErrorCode.ROLL_REQUIRED, "Roll the die before picking a jet.");
        }
        Jet jet = game.findJet(jetId)
                .orElseThrow(() -> new GameRuleException(ErrorCode.INVALID_JET, "Unknown jet: " + jetId));
        if (jet.getTeam() != team) {
            throw new GameRuleException(ErrorCode.JET_NOT_OWNED, "That jet belongs to the other team.");
        }
        if (!jet.isAlive()) {
            throw new GameRuleException(ErrorCode.JET_DESTROYED, "That jet has been destroyed.");
        }
        if (!canAdvance(jet)) {
            throw new GameRuleException(ErrorCode.JET_CANNOT_ADVANCE,
                    "That jet's line already reaches the end of its lane. Pick another jet.");
        }

        int gap = rules.laneGap(jet.getLane());
        int before = jet.getLineLengthInSquares();
        int remaining = gap - before;
        if (rules.isExactRollToHit() && dieValue > remaining) {
            throw new GameRuleException(ErrorCode.ROLL_TOO_HIGH,
                    "That jet only needs " + remaining + " more square" + (remaining == 1 ? "" : "s")
                            + " and you rolled " + dieValue + ". An exact roll is needed to hit; pick another jet.");
        }
        int after = Math.min(before + dieValue, gap); // never draw past the enemy jet's row
        jet.setLineLengthInSquares(after);

        Jet target = game.jet(team.opponent(), jet.getLane());
        boolean hit = target.isAlive() && after >= gap - rules.getHitTolerance();
        if (hit) {
            target.setAlive(false);
            target.setDestroyedBy(team);
        }

        Move move = new Move(game.getTurnNumber(), team, MoveType.ADVANCE, jet.getId(), dieValue,
                after - before, hit, hit ? target.getId() : null);
        game.getMoves().add(move);
        game.setPendingRoll(null);

        if (destroyedCount(game, team.opponent()) >= rules.getJetsToDestroyToWin()) {
            finish(game, team, EndReason.ENEMY_JETS_DESTROYED);
        } else {
            game.setCurrentTurn(team.opponent());
            game.setTurnNumber(game.getTurnNumber() + 1);
            startTurn(game);
        }
        return move;
    }

    /**
     * Called whenever a turn begins. Skips the turn of a team with no jet that can advance, and ends
     * the game when neither team can advance.
     */
    public void startTurn(Game game) {
        if (game.getStatus() != GameStatus.IN_PROGRESS) {
            return;
        }
        Team current = game.getCurrentTurn();
        if (canAnyAdvance(game, current)) {
            return;
        }
        if (!canAnyAdvance(game, current.opponent())) {
            finishStalemate(game);
            return;
        }
        game.getMoves().add(new Move(game.getTurnNumber(), current, MoveType.SKIP, null, 0, 0, false, null));
        game.setCurrentTurn(current.opponent());
        game.setTurnNumber(game.getTurnNumber() + 1);
    }

    /**
     * Records a rematch request. When both teams have asked, the board is reset.
     *
     * @return true if the game was reset
     */
    public boolean requestRematch(Game game, Team team) {
        if (game.getStatus() != GameStatus.FINISHED) {
            throw new GameRuleException(ErrorCode.GAME_NOT_FINISHED, "A rematch is only possible once the game is over.");
        }
        game.getRematchVotes().add(team);
        if (game.getRematchVotes().size() == Team.values().length) {
            begin(game);
            return true;
        }
        return false;
    }

    /** A living jet can advance while its line has not yet reached the end of its lane. */
    public boolean canAdvance(Jet jet) {
        return jet.isAlive() && jet.getLineLengthInSquares() < rules.laneGap(jet.getLane());
    }

    /** The jets of {@code team} that can use the pending die roll right now. */
    public List<Jet> selectableJets(Game game, Team team) {
        Integer die = game.getPendingRoll();
        if (die == null) {
            return List.of();
        }
        return advanceableJets(game, team).stream()
                .filter(j -> !rules.isExactRollToHit() || die <= rules.laneGap(j.getLane()) - j.getLineLengthInSquares())
                .toList();
    }

    /** Living jets of {@code team} whose line has not yet reached the end of its lane. */
    public List<Jet> advanceableJets(Game game, Team team) {
        return game.getJets().stream()
                .filter(j -> j.getTeam() == team && canAdvance(j))
                .toList();
    }

    private boolean canAnyAdvance(Game game, Team team) {
        return !advanceableJets(game, team).isEmpty();
    }

    private int destroyedCount(Game game, Team team) {
        return (int) game.getJets().stream().filter(j -> j.getTeam() == team && !j.isAlive()).count();
    }

    private int aliveCount(Game game, Team team) {
        return (int) game.getJets().stream().filter(j -> j.getTeam() == team && j.isAlive()).count();
    }

    private void finish(Game game, Team winner, EndReason reason) {
        game.setStatus(GameStatus.FINISHED);
        game.setWinner(winner);
        game.setEndReason(reason);
        game.setPendingRoll(null);
    }

    private void finishStalemate(Game game) {
        int red = aliveCount(game, Team.RED);
        int blue = aliveCount(game, Team.BLUE);
        if (rules.isResolveStalemateByJetCount() && red != blue) {
            finish(game, red > blue ? Team.RED : Team.BLUE, EndReason.STALEMATE_MOST_JETS);
        } else {
            finish(game, null, EndReason.STALEMATE_DRAW);
        }
    }

    private void requireActive(Game game) {
        if (game.getStatus() != GameStatus.IN_PROGRESS) {
            throw new GameRuleException(ErrorCode.GAME_NOT_ACTIVE,
                    game.getStatus() == GameStatus.WAITING
                            ? "Waiting for a second player to join."
                            : "The game is over.");
        }
    }

    private void requireTurn(Game game, Team team) {
        if (game.getCurrentTurn() != team) {
            throw new GameRuleException(ErrorCode.NOT_YOUR_TURN, "It is " + game.getCurrentTurn() + "'s turn.");
        }
    }
}
