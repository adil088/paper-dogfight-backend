package com.paperdogfight.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.paperdogfight.config.GameRulesConfig;
import com.paperdogfight.model.EndReason;
import com.paperdogfight.model.Game;
import com.paperdogfight.model.GameStatus;
import com.paperdogfight.model.Jet;
import com.paperdogfight.model.Move;
import com.paperdogfight.model.MoveType;
import com.paperdogfight.model.Team;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for the pure rules in {@link GameEngine}. The die is scripted, so nothing is random. */
class GameEngineTest {

    private GameRulesConfig rules;
    private Deque<Integer> scriptedRolls;
    private GameEngine engine;

    @BeforeEach
    void setUp() {
        rules = new GameRulesConfig();
        scriptedRolls = new ArrayDeque<>();
        engine = new GameEngine(rules, sides -> scriptedRolls.removeFirst());
    }

    private Game startedGame() {
        Game game = new Game("TEST1");
        engine.begin(game);
        return game;
    }

    /** Rolls the given value for the team, then moves the given jet. */
    private Move play(Game game, Team team, int die, String jetId) {
        scriptedRolls.addLast(die);
        engine.roll(game, team);
        return engine.move(game, team, jetId);
    }

    private GameRuleException failure(Runnable action) {
        return assertThrows(GameRuleException.class, action::run);
    }

    // ---------------------------------------------------------------- turn order

    @Test
    void redMovesFirstAndTurnsAlternate() {
        Game game = startedGame();
        assertEquals(GameStatus.IN_PROGRESS, game.getStatus());
        assertEquals(Team.RED, game.getCurrentTurn());

        play(game, Team.RED, 2, "R1");
        assertEquals(Team.BLUE, game.getCurrentTurn());

        play(game, Team.BLUE, 3, "B1");
        assertEquals(Team.RED, game.getCurrentTurn());

        play(game, Team.RED, 1, "R3");
        assertEquals(Team.BLUE, game.getCurrentTurn());
        assertEquals(4, game.getTurnNumber());
    }

    @Test
    void blueCannotRollOnRedsTurn() {
        Game game = startedGame();
        scriptedRolls.addLast(4);
        assertEquals(ErrorCode.NOT_YOUR_TURN, failure(() -> engine.roll(game, Team.BLUE)).getCode());
    }

    @Test
    void cannotRollTwiceInOneTurn() {
        Game game = startedGame();
        scriptedRolls.addLast(4);
        scriptedRolls.addLast(5);
        engine.roll(game, Team.RED);
        assertEquals(ErrorCode.ALREADY_ROLLED, failure(() -> engine.roll(game, Team.RED)).getCode());
        assertEquals(4, game.getPendingRoll());
    }

    @Test
    void mustRollBeforeMoving() {
        Game game = startedGame();
        assertEquals(ErrorCode.ROLL_REQUIRED, failure(() -> engine.move(game, Team.RED, "R1")).getCode());
    }

    @Test
    void cannotPickTheOtherTeamsJetOrAnUnknownJet() {
        Game game = startedGame();
        scriptedRolls.addLast(3);
        engine.roll(game, Team.RED);
        assertEquals(ErrorCode.JET_NOT_OWNED, failure(() -> engine.move(game, Team.RED, "B1")).getCode());
        assertEquals(ErrorCode.INVALID_JET, failure(() -> engine.move(game, Team.RED, "X9")).getCode());
        assertEquals(3, game.getPendingRoll(), "a rejected move must not consume the roll");
    }

    // ------------------------------------------------------------ line extension

    @Test
    void lineGrowsByExactlyTheDieValueEachTurn() {
        Game game = startedGame();

        Move first = play(game, Team.RED, 4, "R2");
        assertEquals(4, game.jet(Team.RED, com.paperdogfight.model.Lane.CENTER).getLineLengthInSquares());
        assertEquals(4, first.advanced());
        assertEquals(4, first.dieValue());

        play(game, Team.BLUE, 2, "B1");
        play(game, Team.RED, 3, "R2");
        assertEquals(7, game.findJet("R2").orElseThrow().getLineLengthInSquares());
        assertEquals(2, game.findJet("B1").orElseThrow().getLineLengthInSquares());
        assertEquals(0, game.findJet("R1").orElseThrow().getLineLengthInSquares());
    }

    @Test
    void lineIsCappedAtTheEnemyJetsRowWhenExactRollIsNotRequired() {
        rules.setExactRollToHit(false);
        rules.setLaneGaps(List.of(6, 4, 6));
        Game game = startedGame();

        Move move = play(game, Team.RED, 6, "R2"); // centre gap is only 4
        assertEquals(4, game.findJet("R2").orElseThrow().getLineLengthInSquares());
        assertEquals(4, move.advanced(), "only the squares actually drawn are reported");
        assertEquals(6, move.dieValue());
    }

    // ------------------------------------------------------------ hit detection

    @Test
    void lineReachingTheEnemyJetDestroysIt() {
        rules.setLaneGaps(List.of(6, 5, 6));
        Game game = startedGame();

        Move move = play(game, Team.RED, 5, "R2");
        Jet target = game.findJet("B2").orElseThrow();
        assertTrue(move.hit());
        assertEquals("B2", move.destroyedJetId());
        assertFalse(target.isAlive());
        assertEquals(Team.RED, target.getDestroyedBy());
    }

    @Test
    void lineStoppingShortOfTheEnemyJetIsNotAHit() {
        rules.setLaneGaps(List.of(6, 5, 6));
        Game game = startedGame();

        Move move = play(game, Team.RED, 4, "R2");
        assertFalse(move.hit());
        assertNull(move.destroyedJetId());
        assertTrue(game.findJet("B2").orElseThrow().isAlive());
    }

    @Test
    void hitToleranceLetsALineHitFromAFewSquaresAway() {
        rules.setLaneGaps(List.of(6, 5, 6));
        rules.setHitTolerance(1);
        Game game = startedGame();

        Move move = play(game, Team.RED, 4, "R2");
        assertTrue(move.hit());
        assertFalse(game.findJet("B2").orElseThrow().isAlive());
        assertEquals(4, game.findJet("R2").orElseThrow().getLineLengthInSquares());
    }

    @Test
    void aLineOnlyHitsTheJetInItsOwnLane() {
        rules.setLaneGaps(List.of(3, 3, 3));
        Game game = startedGame();

        play(game, Team.RED, 3, "R1");
        assertFalse(game.findJet("B1").orElseThrow().isAlive());
        assertTrue(game.findJet("B2").orElseThrow().isAlive());
        assertTrue(game.findJet("B3").orElseThrow().isAlive());
    }

    @Test
    void destroyedJetCannotBeSelectedAndKeepsItsLine() {
        rules.setLaneGaps(List.of(6, 5, 6));
        Game game = startedGame();
        play(game, Team.RED, 5, "R2"); // destroys B2

        assertFalse(engine.advanceableJets(game, Team.BLUE).stream().anyMatch(j -> j.getId().equals("B2")));

        scriptedRolls.addLast(2);
        engine.roll(game, Team.BLUE);
        assertEquals(ErrorCode.JET_DESTROYED, failure(() -> engine.move(game, Team.BLUE, "B2")).getCode());
        assertEquals(0, game.findJet("B2").orElseThrow().getLineLengthInSquares());
        assertEquals(5, game.findJet("R2").orElseThrow().getLineLengthInSquares(), "attacker's line stays on the paper");
    }

    @Test
    void jetWhoseLineReachedTheEndCannotAdvanceAgain() {
        rules.setLaneGaps(List.of(3, 3, 3));
        Game game = startedGame();
        play(game, Team.RED, 3, "R1");
        play(game, Team.BLUE, 1, "B2");

        scriptedRolls.addLast(2);
        engine.roll(game, Team.RED);
        assertEquals(ErrorCode.JET_CANNOT_ADVANCE, failure(() -> engine.move(game, Team.RED, "R1")).getCode());
    }


    // ----------------------------------------------------------- exact roll to hit

    @Test
    void rollBiggerThanTheDistanceDoesNotHit() {
        rules.setLaneGaps(List.of(6, 2, 6)); // centre jets are 2 squares apart
        Game game = startedGame();
        scriptedRolls.addLast(5);
        engine.roll(game, Team.RED);

        assertEquals(ErrorCode.ROLL_TOO_HIGH, failure(() -> engine.move(game, Team.RED, "R2")).getCode());
        assertTrue(game.findJet("B2").orElseThrow().isAlive());
        assertEquals(0, game.findJet("R2").orElseThrow().getLineLengthInSquares());
        assertFalse(engine.selectableJets(game, Team.RED).stream().anyMatch(j -> j.getId().equals("R2")));
        assertTrue(engine.selectableJets(game, Team.RED).stream().anyMatch(j -> j.getId().equals("R1")));
    }

    @Test
    void onlyAnExactRollDestroysTheJet() {
        rules.setLaneGaps(List.of(6, 2, 6));
        Game game = startedGame();

        Move creep = play(game, Team.RED, 1, "R2"); // 1 of 2 squares: closer, no hit
        assertFalse(creep.hit());
        play(game, Team.BLUE, 1, "B1");
        Move kill = play(game, Team.RED, 1, "R2"); // exactly the 1 square still needed
        assertTrue(kill.hit());
        assertFalse(game.findJet("B2").orElseThrow().isAlive());
    }

    @Test
    void exactRollFromTheStartHitsToo() {
        rules.setLaneGaps(List.of(6, 2, 6));
        Game game = startedGame();
        assertTrue(play(game, Team.RED, 2, "R2").hit());
    }

    @Test
    void turnPassesAutomaticallyWhenNoJetCanUseTheRoll() {
        rules.setLaneGaps(List.of(2, 2, 2));
        Game game = startedGame();
        scriptedRolls.addLast(5);

        engine.roll(game, Team.RED);

        Move pass = game.getMoves().get(game.getMoves().size() - 1);
        assertEquals(MoveType.SKIP, pass.type());
        assertEquals(5, pass.dieValue());
        assertEquals(Team.RED, pass.team());
        assertEquals(Team.BLUE, game.getCurrentTurn());
        assertNull(game.getPendingRoll());
        assertEquals(5, game.getLastRollValue());
        assertTrue(game.getJets().stream().allMatch(j -> j.isAlive() && j.getLineLengthInSquares() == 0));
    }

    @Test
    void rollOnlyFitsSomeJetsSoOnlyThoseAreSelectable() {
        rules.setLaneGaps(List.of(6, 2, 6));
        Game game = startedGame();
        scriptedRolls.addLast(4);
        engine.roll(game, Team.RED);

        List<String> ids = engine.selectableJets(game, Team.RED).stream().map(Jet::getId).toList();
        assertEquals(List.of("R1", "R3"), ids);
    }

    // ------------------------------------------------------------------- skipping

    @Test
    void turnIsSkippedWhenNoJetCanAdvance() {
        Game game = startedGame();
        // Red has a single living jet and its line already reaches the end of its lane.
        game.findJet("R2").orElseThrow().setAlive(false);
        game.findJet("R3").orElseThrow().setAlive(false);
        game.findJet("R1").orElseThrow().setLineLengthInSquares(rules.getLaneGaps().get(0));
        game.setCurrentTurn(Team.RED);

        engine.startTurn(game);

        Move skip = game.getMoves().get(game.getMoves().size() - 1);
        assertEquals(MoveType.SKIP, skip.type());
        assertEquals(Team.RED, skip.team());
        assertEquals(Team.BLUE, game.getCurrentTurn());
        assertEquals(GameStatus.IN_PROGRESS, game.getStatus());
    }

    @Test
    void gameEndsWhenNeitherTeamCanAdvanceAndTheTeamWithMoreJetsWins() {
        Game game = stalemateBoard();
        engine.startTurn(game);

        assertEquals(GameStatus.FINISHED, game.getStatus());
        assertEquals(Team.RED, game.getWinner());
        assertEquals(EndReason.STALEMATE_MOST_JETS, game.getEndReason());
    }

    @Test
    void stalemateIsADrawWhenTheTieBreakIsDisabled() {
        rules.setResolveStalemateByJetCount(false);
        Game game = stalemateBoard();
        engine.startTurn(game);

        assertEquals(GameStatus.FINISHED, game.getStatus());
        assertNull(game.getWinner());
        assertEquals(EndReason.STALEMATE_DRAW, game.getEndReason());
    }

    /** Red won lanes 1 and 3, Blue won lane 2; every surviving jet has a full line. */
    private Game stalemateBoard() {
        Game game = startedGame();
        for (String id : List.of("B1", "B3", "R2")) {
            game.findJet(id).orElseThrow().setAlive(false);
        }
        for (String id : List.of("R1", "R3", "B2")) {
            Jet jet = game.findJet(id).orElseThrow();
            jet.setLineLengthInSquares(rules.laneGap(jet.getLane()));
        }
        game.setCurrentTurn(Team.BLUE);
        return game;
    }

    // ------------------------------------------------------------------ winning

    @Test
    void teamWinsWhenAllThreeEnemyJetsAreCrossedOut() {
        rules.setLaneGaps(List.of(3, 3, 3));
        Game game = startedGame();

        play(game, Team.RED, 3, "R1");   // destroys B1
        play(game, Team.BLUE, 1, "B2");
        play(game, Team.RED, 3, "R2");   // destroys B2
        play(game, Team.BLUE, 1, "B3");
        assertEquals(GameStatus.IN_PROGRESS, game.getStatus());
        Move last = play(game, Team.RED, 3, "R3"); // destroys B3

        assertTrue(last.hit());
        assertEquals(GameStatus.FINISHED, game.getStatus());
        assertEquals(Team.RED, game.getWinner());
        assertEquals(EndReason.ENEMY_JETS_DESTROYED, game.getEndReason());

        scriptedRolls.addLast(2);
        assertEquals(ErrorCode.GAME_NOT_ACTIVE, failure(() -> engine.roll(game, Team.BLUE)).getCode());
    }

    @Test
    void winConditionCanBeLoweredToTwoJets() {
        rules.setLaneGaps(List.of(3, 3, 3));
        rules.setJetsToDestroyToWin(2);
        Game game = startedGame();

        play(game, Team.RED, 3, "R1");
        play(game, Team.BLUE, 1, "B2");
        play(game, Team.RED, 3, "R2");

        assertEquals(GameStatus.FINISHED, game.getStatus());
        assertEquals(Team.RED, game.getWinner());
    }

    // ------------------------------------------------------------------ rematch

    @Test
    void rematchResetsTheBoardOnceBothTeamsAgree() {
        rules.setLaneGaps(List.of(3, 3, 3));
        rules.setJetsToDestroyToWin(1);
        Game game = startedGame();
        play(game, Team.RED, 3, "R1");
        assertEquals(GameStatus.FINISHED, game.getStatus());

        assertFalse(engine.requestRematch(game, Team.RED));
        assertEquals(GameStatus.FINISHED, game.getStatus());
        assertTrue(engine.requestRematch(game, Team.BLUE));

        assertEquals(GameStatus.IN_PROGRESS, game.getStatus());
        assertEquals(Team.RED, game.getCurrentTurn());
        assertNull(game.getWinner());
        assertTrue(game.getMoves().isEmpty());
        assertTrue(game.getJets().stream().allMatch(j -> j.isAlive() && j.getLineLengthInSquares() == 0));
    }

    @Test
    void rematchIsRejectedWhileTheGameIsStillRunning() {
        Game game = startedGame();
        assertEquals(ErrorCode.GAME_NOT_FINISHED, failure(() -> engine.requestRematch(game, Team.RED)).getCode());
    }
}
