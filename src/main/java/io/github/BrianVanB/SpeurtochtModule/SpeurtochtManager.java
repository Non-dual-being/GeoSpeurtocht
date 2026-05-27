package io.github.BrianVanB.SpeurtochtModule;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import io.github.BrianVanB.GeoSpeurtocht.GeoSpeurtocht;

public class SpeurtochtManager {

	private GeoSpeurtocht Master;
	private SpeurCommands cmdExecutor;

	public boolean Running;
	public Location Startpunt;
	public BukkitTask[] Timers;
	public BossBarTimer TimerBar;
	public World ActiveWorld;

	public Set<UUID> ActivePlayers;

	public SpeurtochtManager(GeoSpeurtocht plugin)
	{
		Master = plugin;

		Startpunt = LoadStartpunt();
		Running = false;
		Timers = null;
		TimerBar = new BossBarTimer(Master);
		ActiveWorld = null;
		ActivePlayers = new HashSet<>();

		cmdExecutor = new SpeurCommands(Master, this);

		Master.getCommand("setstart").setExecutor(cmdExecutor);
		Master.getCommand("startpunt").setExecutor(cmdExecutor);
		Master.getCommand("startall").setExecutor(cmdExecutor);
		Master.getCommand("stopall").setExecutor(cmdExecutor);
		Master.getCommand("stoptimers").setExecutor(cmdExecutor);

		Master.getCommand("addtime").setExecutor(cmdExecutor);
		Master.getCommand("addspeler").setExecutor(cmdExecutor);
		Master.getCommand("removespeler").setExecutor(cmdExecutor);

		Master.getServer().getPluginManager().registerEvents(TimerBar, Master);
	}

	public void StartSpeurtocht(int minutes)
	{
		ActiveWorld = Startpunt.getWorld();
		ActivePlayers.clear();

		for (Player p : Master.getServer().getOnlinePlayers())
		{
			if (p.isOp())
				continue;

			if (!p.getWorld().equals(ActiveWorld))
				continue;

			ActivePlayers.add(p.getUniqueId());
		}

		Master.getLogger().info(
				"Speurtocht wordt gestart in wereld "
						+ ActiveWorld.getName()
						+ "..."
		);

		Master.FreezeManager.Unfreezeall(ActiveWorld);
		Master.ExtraStuff.TPallInWorld(Startpunt, ActiveWorld.getName());

		Master.broadcastToWorld(
				ActiveWorld,
				ChatColor.GREEN + "Je mag nu beginnen. Succes!"
		);

		Master.broadcastToWorld(
				ActiveWorld,
				ChatColor.GOLD + "Je hebt " + minutes + " minuten."
		);

		Running = true;

		TimerBar.Create(
				minutes,
				ActiveWorld,
				ActivePlayers,
				() -> FinishSpeurtocht(false, true, false)
		);
	}

	public void FinishSpeurtocht(
			boolean force,
			boolean keepInventory,
			boolean clearInventory
	) {
		World finishedWorld = ActiveWorld;

		TimerBar.Cancel();

		if (Startpunt == null)
			return;

		GameMode targetMode = Master.getWorldConfiguredGameMode(Startpunt.getWorld());

		if (force)
		{
			for (Player p : Master.getServer().getOnlinePlayers())
			{
				ResetPlayer(p, targetMode, keepInventory, clearInventory);
			}

			Master.getServer().broadcastMessage(
					ChatColor.YELLOW
							+ "Speurtocht geforceerd gestopt. Alle spelers zijn teruggezet."
			);
		}
		else
		{
			for (UUID uuid : new HashSet<>(ActivePlayers))
			{
				Player p = Master.getServer().getPlayer(uuid);

				if (p == null)
					continue;

				if (!p.isOnline())
					continue;

				ResetPlayer(p, targetMode, keepInventory, clearInventory);
			}

			if (finishedWorld != null)
			{
				Master.broadcastToWorld(
						finishedWorld,
						ChatColor.GOLD + "Tijd is op."
				);
			}
		}

		Running = false;
		ActiveWorld = null;
		ActivePlayers.clear();
	}

	private void ResetPlayer(
			Player p,
			GameMode targetMode,
			boolean keepInventory,
			boolean clearInventory
	) {
		if (p.isOp())
			return;

		p.teleport(Startpunt);
		Master.FreezeManager.Freeze(p);
		p.setGameMode(targetMode);

		if (clearInventory && !keepInventory && Master.shouldClearInventory(p))
		{
			p.getInventory().clear();
			p.getInventory().setArmorContents(null);
			p.updateInventory();
		}
	}

	public boolean AddTime(int seconds)
	{
		if (!Running)
			return false;

		TimerBar.AddTime(seconds);
		return true;
	}

	public boolean AddPlayerToSpeurtocht(Player p)
	{
		if (!Running)
			return false;

		if (ActiveWorld == null)
			return false;

		if (Startpunt == null)
			return false;

		if (p.isOp())
			return false;

		ActivePlayers.add(p.getUniqueId());

		p.teleport(Startpunt);
		p.setGameMode(Master.getWorldConfiguredGameMode(ActiveWorld));
		Master.FreezeManager.UnFreeze(p);
		TimerBar.AddPlayer(p);

		p.sendMessage(ChatColor.GREEN + "Je bent toegevoegd aan de speurtocht.");
		return true;
	}

	public boolean RemovePlayerFreeze(Player p)
	{
		if (ActiveWorld == null)
			return false;

		if (Startpunt == null)
			return false;

		ActivePlayers.remove(p.getUniqueId());
		TimerBar.RemovePlayer(p);

		p.teleport(Startpunt);
		p.setGameMode(Master.getWorldConfiguredGameMode(ActiveWorld));
		Master.FreezeManager.Freeze(p);

		p.sendMessage(
				ChatColor.YELLOW
						+ "Je bent uit de speurtocht gehaald en teruggezet naar de start."
		);

		return true;
	}

	public boolean RemovePlayerRelease(Player p)
	{
		if (ActiveWorld == null)
			return false;

		ActivePlayers.remove(p.getUniqueId());
		TimerBar.RemovePlayer(p);
		Master.FreezeManager.UnFreeze(p);

		p.sendMessage(
				ChatColor.YELLOW
						+ "Je bent uit de speurtocht gehaald en vrijgegeven."
		);

		return true;
	}

	public Location LoadStartpunt()
	{
		try
		{
			String worldName = Master.getConfig().getString("Startpunt.World");

			if (worldName == null)
				return null;

			World world = Bukkit.getWorld(worldName);

			if (world == null)
				return null;

			return new Location(
					world,
					Master.getConfig().getDouble("Startpunt.X"),
					Master.getConfig().getDouble("Startpunt.Y"),
					Master.getConfig().getDouble("Startpunt.Z"),
					(float) Master.getConfig().getDouble("Startpunt.Yaw"),
					(float) Master.getConfig().getDouble("Startpunt.Pitch")
			);
		}
		catch (Exception e)
		{
			Master.getLogger().warning(
					"Fout bij laden van startpunt: " + e.getMessage()
			);
		}

		return null;
	}

	public void SaveStartpunt()
	{
		if (Startpunt == null)
		{
			Master.getLogger().warning(
					"Kan geen startpunt opslaan want er is geen startpunt"
			);
			return;
		}

		Master.getConfig().set("Startpunt.X", Startpunt.getX());
		Master.getConfig().set("Startpunt.Y", Startpunt.getY());
		Master.getConfig().set("Startpunt.Z", Startpunt.getZ());
		Master.getConfig().set("Startpunt.Yaw", Startpunt.getYaw());
		Master.getConfig().set("Startpunt.Pitch", Startpunt.getPitch());
		Master.getConfig().set("Startpunt.World", Startpunt.getWorld().getName());
		Master.saveConfig();
	}
}