package io.github.BrianVanB.SpeurtochtModule;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;

/**
 * Runtime-state van één actieve speurtocht.
 *
 * Fase 2:
 * - Eén sessie hoort nog bij precies één wereld.
 * - Meerdere sessies kunnen tegelijk bestaan, zolang ze in verschillende werelden draaien.
 *
 * Fase 3:
 * - Eén sessie kan meerdere werelden besturen, bijvoorbeeld ClimateCrafter.
 */
public class SpeurtochtSession {

    private final UUID sessionId;
    private final Location startpunt;
    private final World activeWorld;
    private final Set<UUID> activePlayers;
    private final BossBarTimer timerBar;

    private boolean running;

    public SpeurtochtSession(Location startpunt, World activeWorld, BossBarTimer timerBar) {
        this.sessionId = UUID.randomUUID();
        this.startpunt = startpunt;
        this.activeWorld = activeWorld;
        this.timerBar = timerBar;
        this.activePlayers = new HashSet<>();
        this.running = false;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public boolean isRunning() {
        return running;
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
        activePlayers.clear();
    }
}