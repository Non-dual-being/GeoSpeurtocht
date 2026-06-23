package io.github.BrianVanB.SpeurtochtModule;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import io.github.BrianVanB.GeoSpeurtocht.GeoSpeurtocht;

public class SpeurtochtManager {

	private final GeoSpeurtocht Master;
	private final SpeurCommands cmdExecutor;

	/**
	 * Startpunten per wereld.
	 *
	 * Key: worldName
	 * Value: startlocatie in die wereld
	 */
	private final Map<String, Location> startpuntenByWorld;

	/**
	 * Actieve speurtochten per wereld.
	 *
	 * Fase 2:
	 * - Eén sessie per wereld.
	 */
	private final Map<String, SpeurtochtSession> activeSessionsByWorld;

	/**
	 * Voorkomt dat één speler tegelijk in twee speurtochten zit.
	 *
	 * Key: player UUID
	 * Value: sessie waarin de speler actief is
	 */
	private final Map<UUID, SpeurtochtSession> activeSessionsByPlayer;

	public SpeurtochtManager(GeoSpeurtocht plugin) {
		Master = plugin;

		startpuntenByWorld = LoadStartpunten();
		activeSessionsByWorld = new HashMap<>();
		activeSessionsByPlayer = new HashMap<>();

		cmdExecutor = new SpeurCommands(Master, this);

		Master.getCommand("setstart").setExecutor(cmdExecutor);
		Master.getCommand("startpunt").setExecutor(cmdExecutor);
		Master.getCommand("startall").setExecutor(cmdExecutor);
		Master.getCommand("stopall").setExecutor(cmdExecutor);
		Master.getCommand("stoptimers").setExecutor(cmdExecutor);

		Master.getCommand("addtime").setExecutor(cmdExecutor);
		Master.getCommand("addspeler").setExecutor(cmdExecutor);
		Master.getCommand("removespeler").setExecutor(cmdExecutor);
	}

	public boolean IsRunning(World world) {
		SpeurtochtSession session = GetSession(world);

		return session != null && session.isRunning();
	}

	public Location GetStartpunt(World world) {
		if (world == null) {
			return null;
		}

		return startpuntenByWorld.get(world.getName());
	}

	/**
	 * Alleen nog aanwezig voor backwards compatibility.
	 * Gebruik in nieuwe code liever GetStartpunt(World world).
	 */
	public Location GetStartpunt() {
		if (startpuntenByWorld.size() != 1) {
			return null;
		}

		return startpuntenByWorld.values().iterator().next();
	}

	public World GetActiveWorld(World world) {
		SpeurtochtSession session = GetSession(world);

		if (session == null) {
			return null;
		}

		return session.getActiveWorld();
	}

	public void SetStartpunt(Location location) {
		if (location == null || location.getWorld() == null) {
			return;
		}

		startpuntenByWorld.put(location.getWorld().getName(), location);
		SaveStartpunt();
	}

	public void StartSpeurtocht(World world, int minutes) {
		if (world == null) {
			return;
		}

		String worldName = world.getName();

		if (activeSessionsByWorld.containsKey(worldName)) {
			return;
		}

		Location startpunt = GetStartpunt(world);

		if (startpunt == null) {
			return;
		}

		BossBarTimer timerBar = new BossBarTimer(Master);
		SpeurtochtSession session = new SpeurtochtSession(startpunt, world, timerBar);

		for (Player p : world.getPlayers()) {
			if (!Master.isSpeurtochtSpeler(p, world)) {
				continue;
			}

			session.addActivePlayer(p.getUniqueId());
			activeSessionsByPlayer.put(p.getUniqueId(), session);
		}

		activeSessionsByWorld.put(worldName, session);

		Master.getLogger().info(
				"Speurtocht wordt gestart in wereld "
						+ worldName
						+ "..."
		);

		Master.FreezeManager.Unfreezeall(world);

		for (UUID uuid : session.copyActivePlayers()) {
			Player p = Master.getServer().getPlayer(uuid);

			if (p == null) {
				continue;
			}

			if (!p.isOnline()) {
				continue;
			}

			p.teleport(startpunt);
			p.setGameMode(Master.getWorldConfiguredGameMode(world));
			Master.FreezeManager.UnFreeze(p);
		}

		Master.broadcastToWorld(
				world,
				ChatColor.GREEN + "Je mag nu beginnen. Succes!"
		);

		Master.broadcastToWorld(
				world,
				ChatColor.GOLD + "Je hebt " + minutes + " minuten."
		);

		session.setRunning(true);

		timerBar.Create(
				minutes,
				world,
				session.copyActivePlayers(),
				() -> FinishSpeurtocht(world, false, true, false)
		);
	}

	public void FinishSpeurtocht(
			World world,
			boolean force,
			boolean keepInventory,
			boolean clearInventory
	) {
		SpeurtochtSession session = GetSession(world);

		if (session == null) {
			return;
		}

		World finishedWorld = session.getActiveWorld();
		Location startpunt = session.getStartpunt();

		session.getTimerBar().Cancel();

		if (startpunt == null || finishedWorld == null) {
			UnregisterSession(session);
			return;
		}

		GameMode targetMode = Master.getWorldConfiguredGameMode(finishedWorld);

		if (force) {
			for (Player p : finishedWorld.getPlayers()) {
				ResetPlayer(p, session, targetMode, keepInventory, clearInventory);
			}

			Master.broadcastToWorld(
					finishedWorld,
					ChatColor.YELLOW
							+ "Speurtocht geforceerd gestopt. Alle spelers in deze wereld zijn teruggezet."
			);
		} else {
			for (UUID uuid : session.copyActivePlayers()) {
				Player p = Master.getServer().getPlayer(uuid);

				if (p == null) {
					continue;
				}

				if (!p.isOnline()) {
					continue;
				}

				ResetPlayer(p, session, targetMode, keepInventory, clearInventory);
			}

			Master.broadcastToWorld(
					finishedWorld,
					ChatColor.GOLD + "Tijd is op."
			);
		}

		UnregisterSession(session);
	}

	public void StopTimersOnly(World world) {
		SpeurtochtSession session = GetSession(world);

		if (session == null) {
			return;
		}

		session.getTimerBar().Cancel();
		session.setRunning(false);
	}

	private void ResetPlayer(
			Player p,
			SpeurtochtSession session,
			GameMode targetMode,
			boolean keepInventory,
			boolean clearInventory
	) {
		if (Master.isBegeleider(p)) {
			return;
		}

		Location startpunt = session.getStartpunt();

		if (startpunt == null) {
			return;
		}

		p.teleport(startpunt);
		Master.FreezeManager.Freeze(p);
		p.setGameMode(targetMode);

		if (clearInventory && !keepInventory && Master.shouldClearInventory(p)) {
			p.getInventory().clear();
			p.getInventory().setArmorContents(null);
			p.updateInventory();
		}
	}

	public boolean AddTime(World world, int seconds) {
		SpeurtochtSession session = GetSession(world);

		if (session == null) {
			return false;
		}

		if (!session.isRunning()) {
			return false;
		}

		session.getTimerBar().AddTime(seconds);
		return true;
	}

	public boolean AddPlayerToSpeurtocht(Player p, World commandWorld) {
		SpeurtochtSession session = GetSession(commandWorld);

		if (session == null) {
			return false;
		}

		if (!session.isRunning()) {
			return false;
		}

		if (Master.isBegeleider(p)) {
			return false;
		}

		SpeurtochtSession existingSession = activeSessionsByPlayer.get(p.getUniqueId());

		if (existingSession != null && existingSession != session) {
			p.sendMessage(
					ChatColor.RED
							+ "Je zit al in een andere actieve speurtocht."
			);
			return false;
		}

		World activeWorld = session.getActiveWorld();
		Location startpunt = session.getStartpunt();

		if (activeWorld == null || startpunt == null) {
			return false;
		}

		session.addActivePlayer(p.getUniqueId());
		activeSessionsByPlayer.put(p.getUniqueId(), session);

		p.teleport(startpunt);
		p.setGameMode(Master.getWorldConfiguredGameMode(activeWorld));
		Master.FreezeManager.UnFreeze(p);
		session.getTimerBar().AddPlayer(p);

		p.sendMessage(ChatColor.GREEN + "Je bent toegevoegd aan de speurtocht.");
		return true;
	}

	public boolean RemovePlayerFreeze(Player p, World commandWorld) {
		SpeurtochtSession session = GetSession(commandWorld);

		if (session == null) {
			return false;
		}

		if (Master.isBegeleider(p)) {
			return false;
		}

		if (!session.hasActivePlayer(p.getUniqueId())) {
			return false;
		}

		World activeWorld = session.getActiveWorld();
		Location startpunt = session.getStartpunt();

		if (activeWorld == null || startpunt == null) {
			return false;
		}

		session.removeActivePlayer(p.getUniqueId());
		RemovePlayerSessionLink(p.getUniqueId(), session);
		session.getTimerBar().RemovePlayer(p);

		p.teleport(startpunt);
		p.setGameMode(Master.getWorldConfiguredGameMode(activeWorld));
		Master.FreezeManager.Freeze(p);

		p.sendMessage(
				ChatColor.YELLOW
						+ "Je bent uit de speurtocht gehaald en teruggezet naar de start."
		);

		return true;
	}

	public boolean RemovePlayerRelease(Player p, World commandWorld) {
		SpeurtochtSession session = GetSession(commandWorld);

		if (session == null) {
			return false;
		}

		if (Master.isBegeleider(p)) {
			return false;
		}

		if (!session.hasActivePlayer(p.getUniqueId())) {
			return false;
		}

		session.removeActivePlayer(p.getUniqueId());
		RemovePlayerSessionLink(p.getUniqueId(), session);
		session.getTimerBar().RemovePlayer(p);
		Master.FreezeManager.UnFreeze(p);

		p.sendMessage(
				ChatColor.YELLOW
						+ "Je bent uit de speurtocht gehaald en vrijgegeven."
		);

		return true;
	}

	private SpeurtochtSession GetSession(World world) {
		if (world == null) {
			return null;
		}

		return activeSessionsByWorld.get(world.getName());
	}

	private void UnregisterSession(SpeurtochtSession session) {
		if (session == null) {
			return;
		}

		World world = session.getActiveWorld();

		if (world != null) {
			activeSessionsByWorld.remove(world.getName());
		}

		for (UUID uuid : session.copyActivePlayers()) {
			RemovePlayerSessionLink(uuid, session);
		}

		session.clearRuntimeState();
	}

	private void RemovePlayerSessionLink(UUID uuid, SpeurtochtSession session) {
		SpeurtochtSession currentSession = activeSessionsByPlayer.get(uuid);

		if (currentSession == session) {
			activeSessionsByPlayer.remove(uuid);
		}
	}

	private Map<String, Location> LoadStartpunten() {
		Map<String, Location> result = new HashMap<>();

		ConfigurationSection section = Master.getConfig().getConfigurationSection("Startpunten");

		if (section != null) {
			for (String worldName : section.getKeys(false)) {
				Location location = LoadStartpuntFromPath("Startpunten." + worldName);

				if (location == null) {
					continue;
				}

				result.put(location.getWorld().getName(), location);
			}
		}

		if (!result.isEmpty()) {
			return result;
		}

		Location legacyStartpunt = LoadStartpuntFromPath("Startpunt");

		if (legacyStartpunt != null && legacyStartpunt.getWorld() != null) {
			result.put(legacyStartpunt.getWorld().getName(), legacyStartpunt);

			Master.getLogger().info(
					"Oud Startpunt gevonden en geladen als wereldspecifiek startpunt voor "
							+ legacyStartpunt.getWorld().getName()
			);
		}

		return result;
	}

	private Location LoadStartpuntFromPath(String path) {
		try {
			String worldName = Master.getConfig().getString(path + ".World");

			if (worldName == null) {
				return null;
			}

			World world = Bukkit.getWorld(worldName);

			if (world == null) {
				Master.getLogger().warning(
						"Kan startpunt niet laden. Wereld niet gevonden: " + worldName
				);
				return null;
			}

			return new Location(
					world,
					Master.getConfig().getDouble(path + ".X"),
					Master.getConfig().getDouble(path + ".Y"),
					Master.getConfig().getDouble(path + ".Z"),
					(float) Master.getConfig().getDouble(path + ".Yaw"),
					(float) Master.getConfig().getDouble(path + ".Pitch")
			);
		} catch (Exception e) {
			Master.getLogger().warning(
					"Fout bij laden van startpunt op pad "
							+ path
							+ ": "
							+ e.getMessage()
			);
		}

		return null;
	}

	public void SaveStartpunt() {
		for (Map.Entry<String, Location> entry : startpuntenByWorld.entrySet()) {
			String worldName = entry.getKey();
			Location location = entry.getValue();

			if (location == null || location.getWorld() == null) {
				continue;
			}

			String path = "Startpunten." + worldName;

			Master.getConfig().set(path + ".X", location.getX());
			Master.getConfig().set(path + ".Y", location.getY());
			Master.getConfig().set(path + ".Z", location.getZ());
			Master.getConfig().set(path + ".Yaw", location.getYaw());
			Master.getConfig().set(path + ".Pitch", location.getPitch());
			Master.getConfig().set(path + ".World", location.getWorld().getName());
		}

		Master.saveConfig();
	}
}