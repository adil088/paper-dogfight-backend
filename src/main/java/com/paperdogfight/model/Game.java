package com.paperdogfight.model;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Mutable state of one game. All rules live in {@code GameEngine}; this class only holds data. */
public class Game {
    private final String id;
    private GameStatus status = GameStatus.WAITING;
    private Team currentTurn = Team.RED;
    private Team winner;
    private EndReason endReason;
    private int turnNumber = 1;
    /** Die value rolled this turn and not yet used by a move; null when the player must roll. */
    private Integer pendingRoll;
    /** Total rolls ever made in this game object; lets clients animate each roll exactly once. */
    private int rollCount;
    private Integer lastRollValue;
    private Team lastRollTeam;
    private List<Jet> jets = freshJets();
    private final List<Move> moves = new ArrayList<>();
    private final Set<Team> rematchVotes = EnumSet.noneOf(Team.class);
    /** Increases on every change so clients can ignore stale, out-of-order updates. */
    private long version;

    // Secrets that identify the two players. Never copied into a DTO.
    private String redToken;
    private String blueToken;

    public Game(String id) {
        this.id = id;
    }

    /** Six jets: Red left/centre/right, then Blue left/centre/right. */
    private static List<Jet> freshJets() {
        List<Jet> list = new ArrayList<>();
        for (Team team : Team.values()) {
            for (Lane lane : Lane.values()) {
                list.add(new Jet(team, lane));
            }
        }
        return list;
    }

    /** Puts every jet, line and counter back to the start (used for a rematch). */
    public void resetBoard() {
        jets = freshJets();
        moves.clear();
        pendingRoll = null;
        lastRollValue = null;
        lastRollTeam = null;
        winner = null;
        endReason = null;
        rematchVotes.clear();
    }

    public Optional<Jet> findJet(String jetId) {
        return jets.stream().filter(j -> j.getId().equals(jetId)).findFirst();
    }

    public Jet jet(Team team, Lane lane) {
        return jets.stream()
                .filter(j -> j.getTeam() == team && j.getLane() == lane)
                .findFirst()
                .orElseThrow();
    }

    public void touch() { version++; }

    public String getId() { return id; }
    public GameStatus getStatus() { return status; }
    public void setStatus(GameStatus status) { this.status = status; }
    public Team getCurrentTurn() { return currentTurn; }
    public void setCurrentTurn(Team currentTurn) { this.currentTurn = currentTurn; }
    public Team getWinner() { return winner; }
    public void setWinner(Team winner) { this.winner = winner; }
    public EndReason getEndReason() { return endReason; }
    public void setEndReason(EndReason endReason) { this.endReason = endReason; }
    public int getTurnNumber() { return turnNumber; }
    public void setTurnNumber(int turnNumber) { this.turnNumber = turnNumber; }
    public Integer getPendingRoll() { return pendingRoll; }
    public void setPendingRoll(Integer pendingRoll) { this.pendingRoll = pendingRoll; }
    public int getRollCount() { return rollCount; }
    public void setRollCount(int rollCount) { this.rollCount = rollCount; }
    public Integer getLastRollValue() { return lastRollValue; }
    public void setLastRollValue(Integer lastRollValue) { this.lastRollValue = lastRollValue; }
    public Team getLastRollTeam() { return lastRollTeam; }
    public void setLastRollTeam(Team lastRollTeam) { this.lastRollTeam = lastRollTeam; }
    public List<Jet> getJets() { return jets; }
    public List<Move> getMoves() { return moves; }
    public Set<Team> getRematchVotes() { return rematchVotes; }
    public long getVersion() { return version; }
    public String getRedToken() { return redToken; }
    public void setRedToken(String redToken) { this.redToken = redToken; }
    public String getBlueToken() { return blueToken; }
    public void setBlueToken(String blueToken) { this.blueToken = blueToken; }
}
