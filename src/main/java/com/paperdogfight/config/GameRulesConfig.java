package com.paperdogfight.config;

import com.paperdogfight.model.Lane;
import com.paperdogfight.model.Team;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * The single place where every tunable rule of the game lives.
 *
 * <p>Change a default here, or override it in {@code application.yml} under {@code game.rules}.
 * The pure {@code GameEngine} only reads its rules from this class, and the frontend receives the
 * board geometry (columns, rows, lane gaps) with every game state, so nothing else needs editing.
 *
 * <p>The two rules the video does not show are the ones most likely to be tweaked:
 * <ul>
 *   <li><b>Hit reach</b> - {@link #hitTolerance}: how many squares short of the enemy jet a line may
 *       end and still count as a hit (0 = the line must reach the enemy jet).</li>
 *   <li><b>Win condition</b> - {@link #jetsToDestroyToWin} (3 = all enemy jets, 2 = first to cross out
 *       two) and {@link #resolveStalemateByJetCount} (see below).</li>
 * </ul>
 */
@Component
@ConfigurationProperties(prefix = "game.rules")
public class GameRulesConfig {

    /** Number of grid columns on the sheet of paper. */
    private int cols = 12;

    /** Number of grid rows on the sheet of paper. */
    private int rows = 18;

    /** The team that takes the very first turn of every game. */
    private Team firstTeam = Team.RED;

    /**
     * Squares a line must travel, from a jet's nose to the nose of the enemy jet in the same lane,
     * in lane order LEFT, CENTER, RIGHT. The centre jets sit closer together than the wing jets.
     * Keep {@code rows} large enough to hold the resulting layout (default 18 fits 13/11/13).
     */
    private List<Integer> laneGaps = new ArrayList<>(List.of(13, 11, 13));

    /** Squares short of the enemy jet that still count as reaching it (0 = must reach it). */
    private int hitTolerance = 0;

    /**
     * When true, a jet can only use a roll that is not bigger than the squares it still needs, so
     * an enemy jet 2 squares away is destroyed only by rolling exactly 2 (a roll of 1 creeps closer,
     * a roll of 5 cannot be used on it). If no jet can use the roll, the turn passes.
     * When false, a bigger roll still destroys the jet and the line is simply capped at its row.
     */
    private boolean exactRollToHit = true;

    /** Enemy jets a team must cross out to win. */
    private int jetsToDestroyToWin = 3;

    /**
     * Every lane is a race, so once all three lanes are decided nobody can advance any more. With
     * this switch on, the team that kept more jets wins that deadlock; with it off the game is a draw.
     */
    private boolean resolveStalemateByJetCount = true;

    /** Number of faces on the die. */
    private int dieSides = 6;

    /** Length (in squares) a line must reach to hit the enemy jet in the given lane. */
    public int laneGap(Lane lane) {
        return laneGaps.get(lane.ordinal());
    }

    public int getCols() { return cols; }
    public void setCols(int cols) { this.cols = cols; }
    public int getRows() { return rows; }
    public void setRows(int rows) { this.rows = rows; }
    public Team getFirstTeam() { return firstTeam; }
    public void setFirstTeam(Team firstTeam) { this.firstTeam = firstTeam; }
    public List<Integer> getLaneGaps() { return laneGaps; }
    public void setLaneGaps(List<Integer> laneGaps) { this.laneGaps = new ArrayList<>(laneGaps); }
    public boolean isExactRollToHit() { return exactRollToHit; }
    public void setExactRollToHit(boolean exactRollToHit) { this.exactRollToHit = exactRollToHit; }
    public int getHitTolerance() { return hitTolerance; }
    public void setHitTolerance(int hitTolerance) { this.hitTolerance = hitTolerance; }
    public int getJetsToDestroyToWin() { return jetsToDestroyToWin; }
    public void setJetsToDestroyToWin(int jetsToDestroyToWin) { this.jetsToDestroyToWin = jetsToDestroyToWin; }
    public boolean isResolveStalemateByJetCount() { return resolveStalemateByJetCount; }
    public void setResolveStalemateByJetCount(boolean value) { this.resolveStalemateByJetCount = value; }
    public int getDieSides() { return dieSides; }
    public void setDieSides(int dieSides) { this.dieSides = dieSides; }
}
