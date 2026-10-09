package entity;

import java.sql.Timestamp;

/**
 * Entity đại diện cho một bản ghi trận đấu trong bảng 'matches'.
 */
public class Match {
    private int matchId;
    private int playerRedId;
    private int playerBlueId;
    private int redScore;
    private int blueScore;
    private Timestamp finishedAt;

    public Match() {
    }

    public Match(int playerRedId, int playerBlueId, int redScore, int blueScore) {
        this.playerRedId = playerRedId;
        this.playerBlueId = playerBlueId;
        this.redScore = redScore;
        this.blueScore = blueScore;
    }

    public int getMatchId() {
        return matchId;
    }

    public void setMatchId(int matchId) {
        this.matchId = matchId;
    }

    public int getPlayerRedId() {
        return playerRedId;
    }

    public void setPlayerRedId(int playerRedId) {
        this.playerRedId = playerRedId;
    }

    public int getPlayerBlueId() {
        return playerBlueId;
    }

    public void setPlayerBlueId(int playerBlueId) {
        this.playerBlueId = playerBlueId;
    }

    public int getRedScore() {
        return redScore;
    }

    public void setRedScore(int redScore) {
        this.redScore = redScore;
    }

    public int getBlueScore() {
        return blueScore;
    }

    public void setBlueScore(int blueScore) {
        this.blueScore = blueScore;
    }

    public Timestamp getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Timestamp finishedAt) {
        this.finishedAt = finishedAt;
    }
}
