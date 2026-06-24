package io.github.BrianVanB.SpeurtochtModule;

import io.github.BrianVanB.GeoSpeurtocht.GeoSpeurtocht;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SpeurtochtManager {

	private final GeoSpeurtocht Master;
	private final SpeurCommands cmdExecutor;

	private final Map<String, Location> startpuntenBySessionKey;
	private final Map<String, SpeurtochtSession> activeSessionsBySessionKey;
	private final Map<UUID, SpeurtochtSession> activeSessionsByPlayer;

	public SpeurtochtManager(GeoSpeurtocht plugin) {
		Master = plugin;

		startpuntenBySessionKey = LoadStartpunten();
		activeSessionsBySessionKey = new HashMap<>();
		activeSessionsByPlayer = new HashMap<>();

		cmdExecutor = new SpeurCommands(Master, this);

		Master.getCommand("setstart").setExecutor(cmdExecutor);
		Master.getCommand("startpunt").setExecutor(cmdExecutor);
		Master.getCommand("startall").setExecutor(cmdExecutor);
		Master.getCommand("stopall").setExecutor(cmdExecutor);
		Master.getCommand("stoptimers").setExecutor(cmdExecutor);

		/*
		 * Fase 4:
		 * Deze twee stonden nog niet in jouw constructor.
		 * Als ze niet in plugin.yml staan, geeft getCommand(...) null.
		 */
		Master.getCommand("pausetimer").setExecutor(cmdExecutor);
		Master.getCommand("resumetimer").setExecutor(cmdExecutor);

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

		String sessionKey = GetSessionKey(world);

		return startpuntenBySessionKey.get(sessionKey);
	}

	public Location GetStartpunt() {
		if (startpuntenBySessionKey.size() != 1) {
			return null;
		}

		return startpuntenBySessionKey.values().iterator().next();
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

		String sessionKey = GetSessionKey(location.getWorld());

		startpuntenBySessionKey.put(sessionKey, location);

		SaveStartpunt();
	}

	/*
	 * Oude methode blijft bestaan.
	 * Alles wat nog StartSpeurtocht(world, minutes) gebruikt,
	 * blijft dus gewoon werken als COUNTDOWN.
	 */
	public void StartSpeurtocht(World world, int minutes) {
		StartAllOptions options = new StartAllOptions(
				TimerMode.COUNTDOWN,
				minutes,
				null
		);

		StartSpeurtocht(world, options);
	}

	/*
	 * Nieuwe fase-4-startmethode.
	 * Deze ondersteunt:
	 *
	 * /startall 30
	 * /startall 30 --countdown
	 * /startall --countup --team "Team Blauw"
	 * /startall 45 --countup --team "Team Blauw"
	 */
	public void StartSpeurtocht(World world, StartAllOptions options) {
		if (world == null || options == null) {
			return;
		}

		String sessionKey = GetSessionKey(world);
		String displayName = GetDisplayName(sessionKey);

		if (activeSessionsBySessionKey.containsKey(sessionKey)) {
			Master.getLogger().warning(
					"Kan speurtocht niet starten: sessie is al actief: " + sessionKey
			);
			return;
		}

		Location startpunt = GetStartpunt(world);

		if (startpunt == null) {
			Master.getLogger().warning(
					"Kan speurtocht niet starten: geen startpunt voor sessie " + sessionKey
			);
			return;
		}

		BossBarTimer timerBar = new BossBarTimer(Master);

		SpeurtochtSession session = new SpeurtochtSession(
				sessionKey,
				startpunt,
				world,
				timerBar,
				options
		);

		/*
		 * Fase 3/4:
		 * Niet meer alleen spelers uit de fysieke command-world.
		 * Master.isSpeurtochtSpeler(...) kijkt via de sessionResolver
		 * of de speler in dezelfde logische speurtocht zit.
		 */
		for (Player player : Master.getServer().getOnlinePlayers()) {
			if (!Master.isSpeurtochtSpeler(player, world)) {
				continue;
			}

			session.addActivePlayer(player.getUniqueId());
			activeSessionsByPlayer.put(player.getUniqueId(), session);
		}

		activeSessionsBySessionKey.put(sessionKey, session);

		Master.getLogger().info(
				"Speurtocht wordt gestart voor sessie "
						+ sessionKey
						+ " ("
						+ displayName
						+ ") vanuit wereld "
						+ world.getName()
						+ " met timerMode="
						+ options.getTimerMode()
						+ ", minutes="
						+ options.getConfiguredMinutes()
						+ ", team="
						+ options.getTeamName()
		);

		/*
		 * Deze mag blijven staan voor je oude flow.
		 * Bij MULTI_WORLD unfreezet dit alleen de fysieke wereld,
		 * maar hieronder unfreezen we de actieve spelers individueel.
		 */
		Master.FreezeManager.Unfreezeall(world);

		for (UUID uuid : session.copyActivePlayers()) {
			Player player = Master.getServer().getPlayer(uuid);

			if (player == null) {
				continue;
			}

			if (!player.isOnline()) {
				continue;
			}

			player.teleport(startpunt);
			player.setGameMode(Master.getWorldConfiguredGameMode(startpunt.getWorld()));
			Master.FreezeManager.UnFreeze(player);
		}

		BroadcastToSession(
				sessionKey,
				ChatColor.GREEN + "Je mag nu beginnen. Succes!"
		);

		if (options.isCountdown()) {
			BroadcastToSession(
					sessionKey,
					ChatColor.GOLD
							+ "Je hebt "
							+ options.getConfiguredMinutes()
							+ " minuten."
			);
		} else {
			String maxTimeText = options.hasConfiguredMinutes()
					? "Maximale tijd: " + options.getConfiguredMinutes() + " minuten."
					: "Er is geen maximale tijd ingesteld.";

			BroadcastToSession(
					sessionKey,
					ChatColor.GOLD
							+ "Time-trial gestart voor team "
							+ ChatColor.WHITE
							+ options.getTeamName()
							+ ChatColor.GOLD
							+ ". "
							+ maxTimeText
			);
		}

		session.markStarted();

		timerBar.Create(
				options,
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

		String sessionKey = GetSessionKey(finishedWorld);

		GameMode targetMode = Master.getWorldConfiguredGameMode(startpunt.getWorld());

		if (force) {
			for (Player player : Master.getServer().getOnlinePlayers()) {
				if (!IsPlayerInSession(player, sessionKey)) {
					continue;
				}

				ResetPlayer(player, session, targetMode, keepInventory, clearInventory);
			}

			BroadcastToSession(
					sessionKey,
					ChatColor.YELLOW
							+ "Speurtocht geforceerd gestopt. Alle spelers in deze speurtocht zijn teruggezet."
			);
		} else {
			for (UUID uuid : session.copyActivePlayers()) {
				Player player = Master.getServer().getPlayer(uuid);

				if (player == null) {
					continue;
				}

				if (!player.isOnline()) {
					continue;
				}

				ResetPlayer(player, session, targetMode, keepInventory, clearInventory);
			}

			BroadcastToSession(
					sessionKey,
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
			Player player,
			SpeurtochtSession session,
			GameMode targetMode,
			boolean keepInventory,
			boolean clearInventory
	) {
		if (Master.isBegeleider(player)) {
			return;
		}

		Location startpunt = session.getStartpunt();

		if (startpunt == null) {
			return;
		}

		player.teleport(startpunt);
		Master.FreezeManager.Freeze(player);
		player.setGameMode(targetMode);

		if (clearInventory && !keepInventory && Master.shouldClearInventory(player)) {
			player.getInventory().clear();
			player.getInventory().setArmorContents(null);
			player.updateInventory();
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

	public boolean AddPlayerToSpeurtocht(Player player, World commandWorld) {
		SpeurtochtSession session = GetSession(commandWorld);

		if (session == null) {
			return false;
		}

		if (!session.isRunning()) {
			return false;
		}

		if (Master.isBegeleider(player)) {
			return false;
		}

		SpeurtochtSession existingSession = activeSessionsByPlayer.get(player.getUniqueId());

		if (existingSession != null && existingSession != session) {
			player.sendMessage(
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

		session.addActivePlayer(player.getUniqueId());
		activeSessionsByPlayer.put(player.getUniqueId(), session);

		player.teleport(startpunt);
		player.setGameMode(Master.getWorldConfiguredGameMode(startpunt.getWorld()));
		Master.FreezeManager.UnFreeze(player);
		session.getTimerBar().AddPlayer(player);

		player.sendMessage(ChatColor.GREEN + "Je bent toegevoegd aan de speurtocht.");
		return true;
	}

	public boolean RemovePlayerFreeze(Player player, World commandWorld) {
		SpeurtochtSession session = GetSession(commandWorld);

		if (session == null) {
			return false;
		}

		if (Master.isBegeleider(player)) {
			return false;
		}

		if (!session.hasActivePlayer(player.getUniqueId())) {
			return false;
		}

		Location startpunt = session.getStartpunt();

		if (startpunt == null || startpunt.getWorld() == null) {
			return false;
		}

		GameMode targetMode = Master.getWorldConfiguredGameMode(startpunt.getWorld());

		session.removeActivePlayer(player.getUniqueId());
		RemovePlayerSessionLink(player.getUniqueId(), session);
		session.getTimerBar().RemovePlayer(player);

		player.teleport(startpunt);
		player.setGameMode(targetMode);
		Master.FreezeManager.Freeze(player);

		player.sendMessage(
				ChatColor.YELLOW
						+ "Je bent uit de speurtocht gehaald en teruggezet naar de start."
		);

		return true;
	}

	public boolean RemovePlayerRelease(Player player, World commandWorld) {
		SpeurtochtSession session = GetSession(commandWorld);

		if (session == null) {
			return false;
		}

		if (Master.isBegeleider(player)) {
			return false;
		}

		if (!session.hasActivePlayer(player.getUniqueId())) {
			return false;
		}

		session.removeActivePlayer(player.getUniqueId());
		RemovePlayerSessionLink(player.getUniqueId(), session);
		session.getTimerBar().RemovePlayer(player);
		Master.FreezeManager.UnFreeze(player);

		player.sendMessage(
				ChatColor.YELLOW
						+ "Je bent uit de speurtocht gehaald en vrijgegeven."
		);

		return true;
	}

	private SpeurtochtSession GetSession(World world) {
		if (world == null) {
			return null;
		}

		String sessionKey = GetSessionKey(world);

		return activeSessionsBySessionKey.get(sessionKey);
	}

	private void UnregisterSession(SpeurtochtSession session) {
		if (session == null) {
			return;
		}

		activeSessionsBySessionKey.entrySet().removeIf(entry -> entry.getValue() == session);

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
			for (String configKey : section.getKeys(false)) {
				Location location = LoadStartpuntFromPath("Startpunten." + configKey);

				if (location == null || location.getWorld() == null) {
					continue;
				}

				String sessionKey = GetSessionKey(location.getWorld());

				result.put(sessionKey, location);
			}
		}

		if (!result.isEmpty()) {
			return result;
		}

		Location legacyStartpunt = LoadStartpuntFromPath("Startpunt");

		if (legacyStartpunt != null && legacyStartpunt.getWorld() != null) {
			String sessionKey = GetSessionKey(legacyStartpunt.getWorld());

			result.put(sessionKey, legacyStartpunt);

			Master.getLogger().info(
					"Oud Startpunt gevonden en geladen als startpunt voor sessie "
							+ sessionKey
							+ " vanuit wereld "
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
		Master.getConfig().set("Startpunten", null);

		for (Map.Entry<String, Location> entry : startpuntenBySessionKey.entrySet()) {
			String sessionKey = entry.getKey();
			Location location = entry.getValue();

			if (location == null || location.getWorld() == null) {
				continue;
			}

			String path = "Startpunten." + sessionKey;

			Master.getConfig().set(path + ".X", location.getX());
			Master.getConfig().set(path + ".Y", location.getY());
			Master.getConfig().set(path + ".Z", location.getZ());
			Master.getConfig().set(path + ".Yaw", location.getYaw());
			Master.getConfig().set(path + ".Pitch", location.getPitch());
			Master.getConfig().set(path + ".World", location.getWorld().getName());
		}

		Master.saveConfig();
	}

	private String GetSessionKey(World world) {
		return Master.getSessionResolver().resolveSessionKey(world);
	}

	private String GetDisplayName(String sessionKey) {
		return Master.getSessionResolver().getDisplayName(sessionKey);
	}

	private boolean IsPlayerInSession(Player player, String sessionKey) {
		if (player == null || sessionKey == null) {
			return false;
		}

		if (!player.isOnline()) {
			return false;
		}

		String playerSessionKey = GetSessionKey(player.getWorld());

		return sessionKey.equals(playerSessionKey);
	}

	private void BroadcastToSession(String sessionKey, String message) {
		if (sessionKey == null || message == null) {
			return;
		}

		for (Player player : Master.getServer().getOnlinePlayers()) {
			if (!IsPlayerInSession(player, sessionKey)) {
				continue;
			}

			player.sendMessage(message);
		}
	}

	public SpeurtochtSession GetActiveSession(World world) {
		if (world == null) {
			return null;
		}

		String sessionKey = GetSessionKey(world);

		return activeSessionsBySessionKey.get(sessionKey);
	}

	public boolean PauseTimer(World world) {
		SpeurtochtSession session = GetActiveSession(world);

		if (session == null || !session.isRunning()) {
			return false;
		}

		return session.pause();
	}

	public boolean ResumeTimer(World world) {
		SpeurtochtSession session = GetActiveSession(world);

		if (session == null || !session.isRunning()) {
			return false;
		}

		return session.resume();
	}
}