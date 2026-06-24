package io.github.BrianVanB.SpeurtochtModule;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;

public class SpeurtochtSession {

    private final UUID sessionId;
    private final String sessionKey;
    private final Location startpunt;
    private final World activeWorld;
    private final Set<UUID> activePlayers;
    private final BossBarTimer timerBar;

    private final TimerMode timerMode;
    private final Integer configuredMinutes;
    private final String teamName;

    private boolean running;
    private boolean paused;

    private long startedAtMillis;
    private long pausedAtMillis;
    private long totalPausedMillis;

    public SpeurtochtSession(
            String sessionKey,
            Location startpunt,
            World activeWorld,
            BossBarTimer timerBar,
            StartAllOptions options
    ) {
        this.sessionId = UUID.randomUUID();
        this.sessionKey = sessionKey;
        this.startpunt = startpunt;
        this.activeWorld = activeWorld;
        this.timerBar = timerBar;
        this.activePlayers = new HashSet<>();

        this.timerMode = options.getTimerMode();
        this.configuredMinutes = options.getConfiguredMinutes();
        this.teamName = options.getTeamName();

        this.running = false;
        this.paused = false;

        this.startedAtMillis = 0L;
        this.pausedAtMillis = 0L;
        this.totalPausedMillis = 0L;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public Location getStartpunt() {
        return startpunt;
    }

    public World getActiveWorld() {
        return activeWorld;
    }

    public String getActiveWorldName() {
        if (activeWorld == null) {
            return null;
        }

        return activeWorld.getName();
    }

    public BossBarTimer getTimerBar() {
        return timerBar;
    }

    public TimerMode getTimerMode() {
        return timerMode;
    }

    public Integer getConfiguredMinutes() {
        return configuredMinutes;
    }

    public boolean hasConfiguredMinutes() {
        return configuredMinutes != null;
    }

    public String getTeamName() {
        return teamName;
    }

    public boolean hasTeamName() {
        return teamName != null && !teamName.isBlank();
    }

    public void markStarted() {
        this.startedAtMillis = System.currentTimeMillis();
        this.pausedAtMillis = 0L;
        this.totalPausedMillis = 0L;
        this.paused = false;
        this.running = true;
    }

    public boolean pause() {
        if (!running || paused) {
            return false;
        }

        paused = true;
        pausedAtMillis = System.currentTimeMillis();

        if (timerBar != null) {
            timerBar.Pause();
        }

        return true;
    }

    public boolean resume() {
        if (!running || !paused) {
            return false;
        }

        long now = System.currentTimeMillis();
        totalPausedMillis += now - pausedAtMillis;

        pausedAtMillis = 0L;
        paused = false;

        if (timerBar != null) {
            timerBar.Resume();
        }

        return true;
    }

    public int getElapsedSeconds() {
        if (startedAtMillis <= 0L) {
            return 0;
        }

        long now = paused ? pausedAtMillis : System.currentTimeMillis();
        long elapsedMillis = now - startedAtMillis - totalPausedMillis;

        return (int) Math.max(0, elapsedMillis / 1000L);
    }

    public int finishAndGetElapsedSeconds() {
        if (paused) {
            resume();
        }

        int elapsedSeconds = getElapsedSeconds();

        running = false;
        paused = false;

        return elapsedSeconds;
    }

    public void addActivePlayer(UUID uuid) {
        activePlayers.add(uuid);
    }

    public void removeActivePlayer(UUID uuid) {
        activePlayers.remove(uuid);
    }

    public boolean hasActivePlayer(UUID uuid) {
        return activePlayers.contains(uuid);
    }

    public void clearActivePlayers() {
        activePlayers.clear();
    }

    public Set<UUID> getActivePlayers() {
        return Collections.unmodifiableSet(activePlayers);
    }

    public Set<UUID> copyActivePlayers() {
        return new HashSet<>(activePlayers);
    }

    public void clearRuntimeState() {
        running = false;
        paused = false;
        activePlayers.clear();
    }
}