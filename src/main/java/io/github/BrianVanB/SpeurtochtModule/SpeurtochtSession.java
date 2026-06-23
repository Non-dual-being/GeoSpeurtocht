package io.github.BrianVanB.SpeurtochtModule;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.World;

/**
 * Houdt de runtime-state bij van één actieve speurtocht.
 *
 * Fase 1:
 * - Er bestaat nog maar één sessie tegelijk.
 * - Maar de state staat niet meer los op SpeurtochtManager.
 *
 * Later:
 * - Meerdere SpeurtochtSession-objecten naast elkaar.
 * - Eén sessie per wereld of één sessie voor meerdere werelden.
 */
public class SpeurtochtSession {

    private boolean running;
    private Location startpunt;
    private World activeWorld;
    private final Set<UUID> activePlayers;
    private final BossBarTimer timerBar;

    public SpeurtochtSession(Location startpunt, BossBarTimer timerBar) {
        this.startpunt = startpunt;
        this.timerBar = timerBar;
        this.running = false;
        this.activeWorld = null;
        this.activePlayers = new HashSet<>();
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

    public void setStartpunt(Location startpunt) {
        this.startpunt = startpunt;
    }

    public World getActiveWorld() {
        return activeWorld;
    }

    public void setActiveWorld(World activeWorld) {
        this.activeWorld = activeWorld;
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
        activeWorld = null;
        activePlayers.clear();
    }
}