package io.github.BrianVanB.SpeurtochtModule;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
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

	public SpeurtochtManager(GeoSpeurtocht plugin)
	{
		Master = plugin;

		Startpunt = LoadStartpunt();
		Running = false;
		Timers = null;
		TimerBar = new BossBarTimer(Master);
		ActiveWorld = null;

		cmdExecutor = new SpeurCommands(Master, this);
		Master.getCommand("setstart").setExecutor(cmdExecutor);
		Master.getCommand("startpunt").setExecutor(cmdExecutor);
		Master.getCommand("startall").setExecutor(cmdExecutor);
		Master.getCommand("stopall").setExecutor(cmdExecutor);
		Master.getCommand("stoptimers").setExecutor(cmdExecutor);

		Master.getServer().getPluginManager().registerEvents(TimerBar, Master);
	}

	public Location LoadStartpunt()
	{
		try
		{
			return new Location(
					Bukkit.getWorld(Master.getConfig().getString("Startpunt.World")),
					Master.getConfig().getInt("Startpunt.X"),
					Master.getConfig().getInt("Startpunt.Y"),
					Master.getConfig().getInt("Startpunt.Z"),
					Master.getConfig().getInt("Startpunt.Yaw"),
					Master.getConfig().getInt("Startpunt.Pitch")
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

		Master.getConfig().set("Startpunt.X", Startpunt.getBlockX());
		Master.getConfig().set("Startpunt.Y", Startpunt.getBlockY());
		Master.getConfig().set("Startpunt.Z", Startpunt.getBlockZ());
		Master.getConfig().set("Startpunt.Yaw", Startpunt.getYaw());
		Master.getConfig().set("Startpunt.Pitch", Startpunt.getPitch());
		Master.getConfig().set("Startpunt.World", Startpunt.getWorld().getName());
		Master.saveConfig();
	}
}