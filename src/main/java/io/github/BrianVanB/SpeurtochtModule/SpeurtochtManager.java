package io.github.BrianVanB.SpeurtochtModule;

import java.util.HashSet;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import io.github.BrianVanB.GeoSpeurtocht.GeoSpeurtocht;

public class SpeurtochtManager {

	private final GeoSpeurtocht Master;
	private final SpeurCommands cmdExecutor;

	private final SpeurtochtSession activeSession;

	public SpeurtochtManager(GeoSpeurtocht plugin) {
		Master = plugin;

		BossBarTimer timerBar = new BossBarTimer(Master);
		activeSession = new SpeurtochtSession(LoadStartpunt(), timerBar);

		cmdExecutor = new SpeurCommands(Master, this);

		Master.getCommand("setstart").setExecutor(cmdExecutor);
		Master.getCommand("startpunt").setExecutor(cmdExecutor);
		Master.getCommand("startall").setExecutor(cmdExecutor);
		Master.getCommand("stopall").setExecutor(cmdExecutor);
		Master.getCommand("stoptimers").setExecutor(cmdExecutor);

		Master.getCommand("addtime").setExecutor(cmdExecutor);
		Master.getCommand("addspeler").setExecutor(cmdExecutor);
		Master.getCommand("removespeler").setExecutor(cmdExecutor);

		Master.getServer().getPluginManager().registerEvents(timerBar, Master);
	}

	public boolean IsRunning() {
		return activeSession.isRunning();
	}

	public Location GetStartpunt() {
		return activeSession.getStartpunt();
	}

	public World GetActiveWorld() {
		return activeSession.getActiveWorld();
	}

	public BossBarTimer GetTimerBar() {
		return activeSession.getTimerBar();
	}

	public void SetStartpunt(Location location) {
		activeSession.setStartpunt(location);
	}

	public void StartSpeurtocht(int minutes) {
		Location startpunt = activeSession.getStartpunt();

		if (startpunt == null) {
			return;
		}

		World activeWorld = startpunt.getWorld();

		if (activeWorld == null) {
			return;
		}

		activeSession.setActiveWorld(activeWorld);
		activeSession.clearActivePlayers();

		for (Player p : Master.getServer().getOnlinePlayers()) {
			if (!Master.isSpeurtochtSpeler(p, activeWorld)) {
				continue;
			}

			activeSession.addActivePlayer(p.getUniqueId());
		}

		Master.getLogger().info(
				"Speurtocht wordt gestart in wereld "
						+ activeWorld.getName()
						+ "..."
		);

		Master.FreezeManager.Unfreezeall(activeWorld);

		for (UUID uuid : activeSession.copyActivePlayers()) {
			Player p = Master.getServer().getPlayer(uuid);

			if (p == null) {
				continue;
			}

			if (!p.isOnline()) {
				continue;
			}

			p.teleport(startpunt);
			p.setGameMode(Master.getWorldConfiguredGameMode(activeWorld));
			Master.FreezeManager.UnFreeze(p);
		}

		Master.broadcastToWorld(
				activeWorld,
				ChatColor.GREEN + "Je mag nu beginnen. Succes!"
		);

		Master.broadcastToWorld(
				activeWorld,
				ChatColor.GOLD + "Je hebt " + minutes + " minuten."
		);

		activeSession.setRunning(true);

		activeSession.getTimerBar().Create(
				minutes,
				activeWorld,
				activeSession.copyActivePlayers(),
				() -> FinishSpeurtocht(false, true, false)
		);
	}

	public void FinishSpeurtocht(
			boolean force,
			boolean keepInventory,
			boolean clearInventory
	) {
		World finishedWorld = activeSession.getActiveWorld();
		Location startpunt = activeSession.getStartpunt();

		activeSession.getTimerBar().Cancel();

		if (startpunt == null) {
			return;
		}

		GameMode targetMode = Master.getWorldConfiguredGameMode(startpunt.getWorld());

		if (force) {
			for (Player p : Master.getServer().getOnlinePlayers()) {
				ResetPlayer(p, targetMode, keepInventory, clearInventory);
			}

			Master.getServer().broadcastMessage(
					ChatColor.YELLOW
							+ "Speurtocht geforceerd gestopt. Alle spelers zijn teruggezet."
			);
		} else {
			for (UUID uuid : activeSession.copyActivePlayers()) {
				Player p = Master.getServer().getPlayer(uuid);

				if (p == null) {
					continue;
				}

				if (!p.isOnline()) {
					continue;
				}

				ResetPlayer(p, targetMode, keepInventory, clearInventory);
			}

			if (finishedWorld != null) {
				Master.broadcastToWorld(
						finishedWorld,
						ChatColor.GOLD + "Tijd is op."
				);
			}
		}

		activeSession.clearRuntimeState();
	}

	public void StopTimersOnly() {
		activeSession.getTimerBar().Cancel();
		activeSession.setRunning(false);
	}

	private void ResetPlayer(
			Player p,
			GameMode targetMode,
			boolean keepInventory,
			boolean clearInventory
	) {
		if (Master.isBegeleider(p)) {
			return;
		}

		Location startpunt = activeSession.getStartpunt();

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

	public boolean AddTime(int seconds) {
		if (!activeSession.isRunning()) {
			return false;
		}

		activeSession.getTimerBar().AddTime(seconds);
		return true;
	}

	public boolean AddPlayerToSpeurtocht(Player p) {
		if (!activeSession.isRunning()) {
			return false;
		}

		World activeWorld = activeSession.getActiveWorld();
		Location startpunt = activeSession.getStartpunt();

		if (activeWorld == null) {
			return false;
		}

		if (startpunt == null) {
			return false;
		}

		if (Master.isBegeleider(p)) {
			return false;
		}

		activeSession.addActivePlayer(p.getUniqueId());

		p.teleport(startpunt);
		p.setGameMode(Master.getWorldConfiguredGameMode(activeWorld));
		Master.FreezeManager.UnFreeze(p);
		activeSession.getTimerBar().AddPlayer(p);

		p.sendMessage(ChatColor.GREEN + "Je bent toegevoegd aan de speurtocht.");
		return true;
	}

	public boolean RemovePlayerFreeze(Player p) {
		World activeWorld = activeSession.getActiveWorld();
		Location startpunt = activeSession.getStartpunt();

		if (activeWorld == null) {
			return false;
		}

		if (startpunt == null) {
			return false;
		}

		if (Master.isBegeleider(p)) {
			return false;
		}

		activeSession.removeActivePlayer(p.getUniqueId());
		activeSession.getTimerBar().RemovePlayer(p);

		p.teleport(startpunt);
		p.setGameMode(Master.getWorldConfiguredGameMode(activeWorld));
		Master.FreezeManager.Freeze(p);

		p.sendMessage(
				ChatColor.YELLOW
						+ "Je bent uit de speurtocht gehaald en teruggezet naar de start."
		);

		return true;
	}

	public boolean RemovePlayerRelease(Player p) {
		World activeWorld = activeSession.getActiveWorld();

		if (activeWorld == null) {
			return false;
		}

		if (Master.isBegeleider(p)) {
			return false;
		}

		activeSession.removeActivePlayer(p.getUniqueId());
		activeSession.getTimerBar().RemovePlayer(p);
		Master.FreezeManager.UnFreeze(p);

		p.sendMessage(
				ChatColor.YELLOW
						+ "Je bent uit de speurtocht gehaald en vrijgegeven."
		);

		return true;
	}

	public Location LoadStartpunt() {
		try {
			String worldName = Master.getConfig().getString("Startpunt.World");

			if (worldName == null) {
				return null;
			}

			World world = Bukkit.getWorld(worldName);

			if (world == null) {
				return null;
			}

			return new Location(
					world,
					Master.getConfig().getDouble("Startpunt.X"),
					Master.getConfig().getDouble("Startpunt.Y"),
					Master.getConfig().getDouble("Startpunt.Z"),
					(float) Master.getConfig().getDouble("Startpunt.Yaw"),
					(float) Master.getConfig().getDouble("Startpunt.Pitch")
			);
		} catch (Exception e) {
			Master.getLogger().warning(
					"Fout bij laden van startpunt: " + e.getMessage()
			);
		}

		return null;
	}

	public void SaveStartpunt() {
		Location startpunt = activeSession.getStartpunt();

		if (startpunt == null) {
			Master.getLogger().warning(
					"Kan geen startpunt opslaan want er is geen startpunt"
			);
			return;
		}

		Master.getConfig().set("Startpunt.X", startpunt.getX());
		Master.getConfig().set("Startpunt.Y", startpunt.getY());
		Master.getConfig().set("Startpunt.Z", startpunt.getZ());
		Master.getConfig().set("Startpunt.Yaw", startpunt.getYaw());
		Master.getConfig().set("Startpunt.Pitch", startpunt.getPitch());
		Master.getConfig().set("Startpunt.World", startpunt.getWorld().getName());
		Master.saveConfig();
	}
}