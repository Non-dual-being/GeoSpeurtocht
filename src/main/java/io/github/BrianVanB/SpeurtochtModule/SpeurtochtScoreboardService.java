package io.github.BrianVanB.SpeurtochtModule;

import io.github.BrianVanB.GeoSpeurtocht.GeoSpeurtocht;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SpeurtochtScoreboardService {

    private final GeoSpeurtocht Master;
    private final SpeurtochtManager Manager;

    private final Map<UUID, Scoreboard> previousScoreboards;
    private final Map<UUID, Boolean> enabledByPlayer;

    private BukkitTask updater;

    public SpeurtochtScoreboardService(
            GeoSpeurtocht master,
            SpeurtochtManager manager
    ) {
        this.Master = master;
        this.Manager = manager;

        this.previousScoreboards = new HashMap<>();
        this.enabledByPlayer = new HashMap<>();

        startUpdater();
    }

    public boolean toggle(Player player) {
        if (player == null) {
            return false;
        }

        UUID uuid = player.getUniqueId();

        if (isEnabled(player)) {
            disable(player);
            return false;
        }

        previousScoreboards.put(uuid, player.getScoreboard());
        enabledByPlayer.put(uuid, true);

        updatePlayerScoreboard(player);

        return true;
    }

    public boolean isEnabled(Player player) {
        if (player == null) {
            return false;
        }

        return enabledByPlayer.getOrDefault(player.getUniqueId(), false);
    }

    public void disable(Player player) {
        if (player == null) {
            return;
        }

        UUID uuid = player.getUniqueId();

        enabledByPlayer.remove(uuid);

        Scoreboard previous = previousScoreboards.remove(uuid);

        if (previous != null) {
            player.setScoreboard(previous);
            return;
        }

        if (Bukkit.getScoreboardManager() != null) {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    public void disableAll() {
        for (UUID uuid : List.copyOf(enabledByPlayer.keySet())) {
            Player player = Bukkit.getPlayer(uuid);

            if (player == null || !player.isOnline()) {
                enabledByPlayer.remove(uuid);
                previousScoreboards.remove(uuid);
                continue;
            }

            disable(player);
        }
    }

    private void startUpdater() {
        if (updater != null) {
            return;
        }

        updater = new BukkitRunnable() {
            @Override
            public void run() {
                updateAll();
            }
        }.runTaskTimer(Master, 20L, 20L);
    }

    private void updateAll() {
        for (UUID uuid : List.copyOf(enabledByPlayer.keySet())) {
            Player player = Bukkit.getPlayer(uuid);

            if (player == null || !player.isOnline()) {
                enabledByPlayer.remove(uuid);
                previousScoreboards.remove(uuid);
                continue;
            }

            updatePlayerScoreboard(player);
        }
    }

    private void updatePlayerScoreboard(Player player) {
        if (Bukkit.getScoreboardManager() == null) {
            return;
        }

        World world = player.getWorld();
        String sessionKey = Master.getSessionResolver().resolveSessionKey(world);
        String displayName = Master.getSessionResolver().getDisplayName(sessionKey);

        SpeurtochtSession session = Manager.GetActiveSession(world);
        List<SpeurtochtScoreEntry> topScores = Manager.GetTopScores(world, 1);

        String bestTime = "-";

        if (!topScores.isEmpty()) {
            bestTime = SpeurtochtScoreService.formatTime(
                    topScores.get(0).getElapsedSeconds()
            );
        }

        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();

        Objective objective = board.registerNewObjective(
                "geospeur",
                "dummy",
                ChatColor.GOLD + "GeoSpeurtocht"
        );

        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        int line = 12;

        addLine(objective, ChatColor.YELLOW + displayName, line--);
        addLine(objective, ChatColor.GRAY + "────────────", line--);

        if (session == null || !session.isRunning()) {
            addLine(objective, ChatColor.RED + "Status: gestopt", line--);
            addLine(objective, ChatColor.WHITE + "Record: " + ChatColor.GREEN + bestTime, line--);
            addLine(objective, ChatColor.GRAY + "Geen actieve run", line--);

            player.setScoreboard(board);
            return;
        }

        String status = session.isPaused()
                ? ChatColor.YELLOW + "gepauzeerd"
                : ChatColor.GREEN + "actief";

        String timerMode = session.getTimerMode() == TimerMode.COUNTUP
                ? "countup"
                : "countdown";

        String teamName = session.hasTeamName()
                ? session.getTeamName()
                : "-";

        String currentTime = getCurrentTimeText(session);

        addLine(objective, ChatColor.WHITE + "Status: " + status, line--);
        addLine(objective, ChatColor.WHITE + "Mode: " + ChatColor.AQUA + timerMode, line--);
        addLine(objective, ChatColor.WHITE + "Team: " + ChatColor.AQUA + trim(teamName, 16), line--);
        addLine(objective, ChatColor.WHITE + "Tijd: " + ChatColor.GREEN + currentTime, line--);
        addLine(objective, ChatColor.WHITE + "Record: " + ChatColor.GREEN + bestTime, line--);
        addLine(objective, ChatColor.WHITE + "Spelers: " + ChatColor.AQUA + session.getActivePlayers().size(), line--);

        player.setScoreboard(board);
    }

    private String getCurrentTimeText(SpeurtochtSession session) {
        if (session == null) {
            return "-";
        }

        if (session.getTimerMode() == TimerMode.COUNTUP) {
            return SpeurtochtScoreService.formatTime(session.getElapsedSeconds());
        }

        if (session.getTimerBar() == null) {
            return "-";
        }

        return SpeurtochtScoreService.formatTime(
                Math.max(0, session.getTimerBar().GetRemainingSeconds())
        );
    }

    private void addLine(Objective objective, String text, int score) {
        /*
         * Scoreboard entries moeten uniek zijn.
         * Daarom plakken we er per regel een andere ChatColor-resetcode achter.
         */
        String uniqueText = trim(text, 36) + ChatColor.values()[Math.max(0, Math.min(ChatColor.values().length - 1, score))];

        objective.getScore(uniqueText).setScore(score);
    }

    private String trim(String text, int maxLength) {
        if (text == null) {
            return "";
        }

        if (text.length() <= maxLength) {
            return text;
        }

        return text.substring(0, maxLength - 1) + "…";
    }
}