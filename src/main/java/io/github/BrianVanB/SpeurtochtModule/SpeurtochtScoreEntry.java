package io.github.BrianVanB.SpeurtochtModule;

public class SpeurtochtScoreEntry {

    private final String id;
    private final String sessionKey;
    private final String teamName;
    private final int elapsedSeconds;
    private final int playerCount;
    private final long finishedAtMillis;

    public SpeurtochtScoreEntry(
            String id,
            String sessionKey,
            String teamName,
            int elapsedSeconds,
            int playerCount,
            long finishedAtMillis
    ) {
        this.id = id;
        this.sessionKey = sessionKey;
        this.teamName = teamName;
        this.elapsedSeconds = elapsedSeconds;
        this.playerCount = playerCount;
        this.finishedAtMillis = finishedAtMillis;
    }

    public String getId() {
        return id;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public String getTeamName() {
        return teamName;
    }

    public int getElapsedSeconds() {
        return elapsedSeconds;
    }

    public int getPlayerCount() {
        return playerCount;
    }

    public long getFinishedAtMillis() {
        return finishedAtMillis;
    }
}