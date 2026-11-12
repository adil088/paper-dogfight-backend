package com.paperdogfight.model;

/** A single jet and the missile line it has drawn so far. */
public class Jet {
    private final String id;
    private final Team team;
    private final Lane lane;
    private boolean alive = true;
    private int lineLengthInSquares = 0;
    private Team destroyedBy;

    public Jet(Team team, Lane lane) {
        this.team = team;
        this.lane = lane;
        // e.g. RED + LEFT -> "R1", BLUE + RIGHT -> "B3"
        this.id = team.name().charAt(0) + String.valueOf(lane.ordinal() + 1);
    }

    public String getId() { return id; }
    public Team getTeam() { return team; }
    public Lane getLane() { return lane; }
    /** 1 (left), 2 (centre) or 3 (right); used in the move log. */
    public int getNumber() { return lane.ordinal() + 1; }
    public boolean isAlive() { return alive; }
    public void setAlive(boolean alive) { this.alive = alive; }
    public int getLineLengthInSquares() { return lineLengthInSquares; }
    public void setLineLengthInSquares(int length) { this.lineLengthInSquares = length; }
    public Team getDestroyedBy() { return destroyedBy; }
    public void setDestroyedBy(Team destroyedBy) { this.destroyedBy = destroyedBy; }
}
