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

        List<SpeurtochtScoreEntry> topThree = Manager.GetTopScores(world, 3);
        List<SpeurtochtScoreEntry> allScores = Manager.GetTopScores(world, 0);

        String bestTime = "-";
        if (!topThree.isEmpty()) {
            bestTime = SpeurtochtScoreService.formatTime(
                    topThree.get(0).getElapsedSeconds()
            );
        }

        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();

        Objective objective = board.registerNewObjective(
                "geospeur",
                "dummy",
                ChatColor.GOLD + "GeoSpeurtocht"
        );

        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        /*
         * Dit haalt de rode scorecijfertjes rechts weg.
         * Werkt op Paper API.
         */


        int line = 15;

        addLine(objective, ChatColor.YELLOW + trim(displayName, 24), line--);
        addLine(objective, ChatColor.GRAY + "────────────", line--);

        if (session == null || !session.isRunning()) {
            addLine(objective, ChatColor.WHITE + "Status: " + ChatColor.RED + "gestopt", line--);
            addLine(objective, ChatColor.WHITE + "Record: " + ChatColor.GREEN + bestTime, line--);
            addLine(objective, ChatColor.AQUA + "Top 3:", line--);

            addTopThreeLines(objective, topThree, line);
            player.setScoreboard(board);
            return;
        }

        String status = session.isPaused()
                ? ChatColor.YELLOW + "gepauzeerd"
                : ChatColor.GREEN + "actief";

        String teamName = session.hasTeamName()
                ? session.getTeamName()
                : "-";

        String currentTime = getCurrentTimeText(session);

        addLine(objective, ChatColor.WHITE + "Status: " + status, line--);
        addLine(objective, ChatColor.WHITE + "Team: " + ChatColor.AQUA + trim(teamName, 18), line--);
        addLine(objective, ChatColor.WHITE + "Tijd: " + ChatColor.GREEN + currentTime, line--);
        addLine(objective, ChatColor.WHITE + "Record: " + ChatColor.GREEN + bestTime, line--);
        addLine(objective, ChatColor.WHITE + "Spelers: " + ChatColor.AQUA + session.getActivePlayers().size(), line--);
        addLine(objective, ChatColor.AQUA + "Top 3:", line--);

        line = addTopThreeLines(objective, topThree, line);

        String currentPlaceText = getCurrentPlaceText(session, allScores);
        addLine(objective, ChatColor.WHITE + "Plaats: " + ChatColor.GOLD + currentPlaceText, line);

        player.setScoreboard(board);
    }

    private int addTopThreeLines(
            Objective objective,
            List<SpeurtochtScoreEntry> topThree,
            int startLine
    ) {
        int line = startLine;

        if (topThree.isEmpty()) {
            addLine(objective, ChatColor.GRAY + "Nog geen scores", line--);
            addLine(objective, ChatColor.GRAY + "-", line--);
            addLine(objective, ChatColor.GRAY + "-", line--);
            return line;
        }

        for (int i = 0; i < 3; i++) {
            if (i < topThree.size()) {
                SpeurtochtScoreEntry score = topThree.get(i);

                String text =
                        ChatColor.YELLOW + String.valueOf(i + 1) + ". "
                                + ChatColor.WHITE + trim(score.getTeamName(), 12)
                                + ChatColor.GRAY + " "
                                + ChatColor.GREEN + SpeurtochtScoreService.formatTime(score.getElapsedSeconds());

                addLine(objective, text, line--);
            } else {
                addLine(objective, ChatColor.GRAY + "-", line--);
            }
        }

        return line;
    }

    private String getCurrentPlaceText(
            SpeurtochtSession session,
            List<SpeurtochtScoreEntry> allScores
    ) {
        if (session == null || !session.isRunning()) {
            return "-";
        }

        if (session.getTimerMode() != TimerMode.COUNTUP) {
            return "-";
        }

        if (!session.hasTeamName()) {
            return "-";
        }

        int currentElapsed = session.getElapsedSeconds();

        int betterScores = 0;

        for (SpeurtochtScoreEntry score : allScores) {
            if (score.getElapsedSeconds() < currentElapsed) {
                betterScores++;
            }
        }

        return String.valueOf(betterScores + 1);
    }

    private String getCurrentTimeText(SpeurtochtSession session) {
        if (session == null) {
            return "-";
        }

        if (session.getTimerMode() == TimerMode.COUNTUP) {
            if (session.hasConfiguredMinutes()) {
                int maxSeconds = session.getConfiguredMinutes() * 60;
                return SpeurtochtScoreService.formatTime(session.getElapsedSeconds())
                        + "/"
                        + SpeurtochtScoreService.formatTime(maxSeconds);
            }

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
        String uniqueText = trim(text, 38)
                + ChatColor.values()[Math.max(0, Math.min(ChatColor.values().length - 1, score))];

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