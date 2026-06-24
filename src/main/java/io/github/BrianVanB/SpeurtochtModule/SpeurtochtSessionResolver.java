package io.github.BrianVanB.SpeurtochtModule;

import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class SpeurtochtSessionResolver {

    private final JavaPlugin plugin;

    /*
     * Mapping:
     *
     * fysieke wereldnaam -> logische sessie-key
     *
     * Voorbeeld:
     * climatecrafter_diamant -> climatecrafter
     * climatecrafter_koper   -> climatecrafter
     */
    private final Map<String, String> worldNameToSessionKey = new HashMap<>();

    /*
     * Mapping:
     *
     * sessie-key -> nette displaynaam
     *
     * Voorbeeld:
     * climatecrafter -> ClimateCrafter
     */
    private final Map<String, String> sessionKeyToDisplayName = new HashMap<>();

    /*
     * Mapping:
     *
     * sessie-key -> type
     *
     * Voorbeeld:
     * climatecrafter -> MULTI_WORLD
     */
    private final Map<String, SpeurtochtSessionGroupType> sessionKeyToType = new HashMap<>();

    public SpeurtochtSessionResolver(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin may not be null");
        reload();
    }

    public void reload() {
        worldNameToSessionKey.clear();
        sessionKeyToDisplayName.clear();
        sessionKeyToType.clear();

        ConfigurationSection groupsSection =
                plugin.getConfig().getConfigurationSection("speurtocht-session-groups");

        if (groupsSection == null) {
            plugin.getLogger().warning(
                    "Geen 'speurtocht-session-groups' gevonden in config.yml. " +
                            "Elke wereld gebruikt nu zijn eigen wereldnaam als sessie-key."
            );
            return;
        }

        for (String sessionKey : groupsSection.getKeys(false)) {
            ConfigurationSection groupSection = groupsSection.getConfigurationSection(sessionKey);

            if (groupSection == null) {
                continue;
            }

            String displayName = groupSection.getString("display-name", sessionKey);
            String rawType = groupSection.getString("type", "SINGLE_WORLD");
            SpeurtochtSessionGroupType type = SpeurtochtSessionGroupType.fromConfig(rawType);

            List<String> worlds = groupSection.getStringList("worlds");

            sessionKeyToDisplayName.put(sessionKey, displayName);
            sessionKeyToType.put(sessionKey, type);

            for (String worldName : worlds) {
                if (worldName == null || worldName.isBlank()) {
                    continue;
                }

                worldNameToSessionKey.put(worldName, sessionKey);
            }
        }

        plugin.getLogger().info(
                "Loaded " + worldNameToSessionKey.size() + " grouped speurtocht worlds."
        );
    }

    public String resolveSessionKey(World world) {
        if (world == null) {
            throw new IllegalArgumentException("world may not be null");
        }

        return resolveSessionKey(world.getName());
    }

    public String resolveSessionKey(String worldName) {
        if (worldName == null || worldName.isBlank()) {
            throw new IllegalArgumentException("worldName may not be null or blank");
        }

        return worldNameToSessionKey.getOrDefault(worldName, worldName);
    }

    public String getDisplayName(String sessionKey) {
        if (sessionKey == null || sessionKey.isBlank()) {
            return "Onbekende speurtocht";
        }

        return sessionKeyToDisplayName.getOrDefault(sessionKey, sessionKey);
    }

    public SpeurtochtSessionGroupType getType(String sessionKey) {
        if (sessionKey == null || sessionKey.isBlank()) {
            return SpeurtochtSessionGroupType.SINGLE_WORLD;
        }

        return sessionKeyToType.getOrDefault(
                sessionKey,
                SpeurtochtSessionGroupType.SINGLE_WORLD
        );
    }

    public boolean isGroupedWorld(World world) {
        if (world == null) {
            return false;
        }

        return worldNameToSessionKey.containsKey(world.getName());
    }
}