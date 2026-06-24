package io.github.BrianVanB.SpeurtochtModule;

import io.github.BrianVanB.GeoSpeurtocht.GeoSpeurtocht;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class SpeurtochtScoreService {

    private final GeoSpeurtocht Master;
    private final File scoreFile;
    private YamlConfiguration scoreConfig;

    public SpeurtochtScoreService(GeoSpeurtocht master) {
        this.Master = master;
        this.scoreFile = new File(master.getDataFolder(), "scores.yml");
        this.scoreConfig = YamlConfiguration.loadConfiguration(scoreFile);
    }

    public void saveScore(
            String sessionKey,
            String teamName,
            int elapsedSeconds,
            int playerCount
    ) {
        if (sessionKey == null || sessionKey.isBlank()) {
            return;
        }

        if (teamName == null || teamName.isBlank()) {
            return;
        }

        String id = System.currentTimeMillis()
                + "-"
                + UUID.randomUUID().toString().substring(0, 8);

        String path = "scores." + sessionKey + ".entries." + id;

        scoreConfig.set(path + ".team-name", teamName);
        scoreConfig.set(path + ".elapsed-seconds", elapsedSeconds);
        scoreConfig.set(path + ".player-count", playerCount);
        scoreConfig.set(path + ".finished-at-millis", System.currentTimeMillis());

        save();
    }

    public List<SpeurtochtScoreEntry> getTopScores(String sessionKey, int limit) {
        List<SpeurtochtScoreEntry> scores = new ArrayList<>();

        if (sessionKey == null || sessionKey.isBlank()) {
            return scores;
        }

        ConfigurationSection entriesSection =
                scoreConfig.getConfigurationSection("scores." + sessionKey + ".entries");

        if (entriesSection == null) {
            return scores;
        }

        for (String id : entriesSection.getKeys(false)) {
            String path = "scores." + sessionKey + ".entries." + id;

            String teamName = scoreConfig.getString(path + ".team-name", "Onbekend team");
            int elapsedSeconds = scoreConfig.getInt(path + ".elapsed-seconds", 0);
            int playerCount = scoreConfig.getInt(path + ".player-count", 0);
            long finishedAtMillis = scoreConfig.getLong(path + ".finished-at-millis", 0L);

            scores.add(
                    new SpeurtochtScoreEntry(
                            id,
                            sessionKey,
                            teamName,
                            elapsedSeconds,
                            playerCount,
                            finishedAtMillis
                    )
            );
        }

        scores.sort(Comparator.comparingInt(SpeurtochtScoreEntry::getElapsedSeconds));

        if (limit <= 0 || scores.size() <= limit) {
            return scores;
        }

        return new ArrayList<>(scores.subList(0, limit));
    }

    public void resetScores(String sessionKey) {
        if (sessionKey == null || sessionKey.isBlank()) {
            return;
        }

        scoreConfig.set("scores." + sessionKey, null);
        save();
    }

    private void save() {
        try {
            if (!Master.getDataFolder().exists()) {
                Master.getDataFolder().mkdirs();
            }

            scoreConfig.save(scoreFile);
        } catch (IOException exception) {
            Master.getLogger().severe(
                    "Kon scores.yml niet opslaan: " + exception.getMessage()
            );
        }
    }

    public static String formatTime(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;

        if (seconds < 10) {
            return minutes + ":0" + seconds;
        }

        return minutes + ":" + seconds;
    }
}