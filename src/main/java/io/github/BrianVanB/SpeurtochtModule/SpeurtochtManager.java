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

	/*
	 * FASE 3:
	 * Deze map is niet meer echt "by world", maar "by session key".
	 *
	 * Voorbeeld:
	 * GeoFort_Heat             -> startpunt in GeoFort_Heat
	 * GeoFort_overstroming     -> startpunt in GeoFort_overstroming
	 * climatecrafter           -> startpunt in bijvoorbeeld climatecrafter_diamant
	 */
	private final Map<String, Location> startpuntenBySessionKey;

	/*
	 * FASE 3:
	 * Actieve sessies worden opgeslagen per logische speurtocht-sessie.
	 *
	 * Voorbeeld:
	 * GeoFort_Heat             -> eigen sessie
	 * GeoFort_overstroming     -> eigen sessie
	 * climatecrafter           -> gezamenlijke sessie voor diamant/goud/ijzer/koper/steenkool
	 */
	private final Map<String, SpeurtochtSession> activeSessionsBySessionKey;

	/*
	 * Snelle lookup:
	 * speler UUID -> actieve speurtocht sessie
	 */
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

		/*
		 * Belangrijk:
		 * De key is de logische sessie.
		 * De Location zelf bewaart nog steeds de echte Minecraft-wereld.
		 *
		 * Dus:
		 * key: climatecrafter
		 * location.world: climatecrafter_diamant
		 */
		startpuntenBySessionKey.put(sessionKey, location);

		SaveStartpunt();
	}

	public void StartSpeurtocht(World world, int minutes) {
		if (world == null) {
			return;
		}

		String sessionKey = GetSessionKey(world);
		String displayName = GetDisplayName(sessionKey);

		if (activeSessionsBySessionKey.containsKey(sessionKey)) {
			return;
		}

		Location startpunt = GetStartpunt(world);

		if (startpunt == null) {
			return;
		}

		BossBarTimer timerBar = new BossBarTimer(Master);

		/*
		 * activeWorld blijft de wereld van waaruit je start.
		 * Bij ClimateCrafter kan dat bijvoorbeeld climatecrafter_diamant zijn.
		 */
		SpeurtochtSession session = new SpeurtochtSession(startpunt, world, timerBar);

		/*
		 * FASE 3:
		 * Niet meer alleen world.getPlayers().
		 *
		 * Want bij ClimateCrafter moeten spelers uit:
		 * - climatecrafter_diamant
		 * - climatecrafter_goud
		 * - climatecrafter_ijzer
		 * - climatecrafter_koper
		 * - climatecrafter_steenkool
		 *
		 * allemaal bij dezelfde sessie kunnen horen.
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
						+ "..."
		);

		/*
		 * Deze blijft staan voor backwards compatibility.
		 * Daarna unfreezen we sowieso de actieve spelers individueel.
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

		BroadcastToSession(
				sessionKey,
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

		String sessionKey = GetSessionKey(finishedWorld);

		/*
		 * We gebruiken de gamemode van de startpuntwereld.
		 * Dat is veiliger dan blind de command-world gebruiken.
		 */
		GameMode targetMode = Master.getWorldConfiguredGameMode(startpunt.getWorld());

		if (force) {
			/*
			 * FASE 3:
			 * Force geldt voor de volledige logische sessie.
			 * Dus bij ClimateCrafter niet alleen de fysieke wereld waarin je staat.
			 */
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

		/*
		 * Veilig verwijderen op basis van objectreferentie.
		 * Dit werkt ook als activeWorld null zou zijn of als de sessie-key later anders wordt bepaald.
		 */
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

				/*
				 * FASE 3:
				 * Oude configs konden een fysieke wereldnaam als key hebben.
				 * Nieuwe configs gebruiken de sessionKey.
				 *
				 * Daarom normaliseren we hier altijd:
				 * location.world -> sessionKey
				 */
				String sessionKey = GetSessionKey(location.getWorld());

				result.put(sessionKey, location);
			}
		}

		if (!result.isEmpty()) {
			return result;
		}

		/*
		 * Legacy fallback:
		 * Oude config had mogelijk maar één Startpunt.
		 */
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
		/*
		 * Oude fysieke wereldkeys verwijderen en opnieuw netjes opslaan per sessionKey.
		 * Dit voorkomt dat je straks Startpunten.climatecrafter én
		 * Startpunten.climatecrafter_diamant naast elkaar krijgt.
		 */
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
}