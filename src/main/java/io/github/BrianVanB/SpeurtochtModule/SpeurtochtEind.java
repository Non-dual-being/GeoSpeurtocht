package io.github.BrianVanB.SpeurtochtModule;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import io.github.BrianVanB.GeoSpeurtocht.GeoSpeurtocht;

public class SpeurtochtEind extends BukkitRunnable
{
	private GeoSpeurtocht Master;
	private Location dest;
	private World world;

	public SpeurtochtEind(GeoSpeurtocht master, Location destination, World activeWorld)
	{
		Master = master;
		dest = destination;
		world = activeWorld;
	}

	@Override
	public void run()
	{
		GameMode targetMode = Master.getWorldConfiguredGameMode(world);

		for (Player p : Master.getServer().getOnlinePlayers())
		{
			if (p.isOp())
				continue;

			if (!p.getWorld().equals(world))
				continue;

			p.teleport(dest);
			p.setGameMode(targetMode);
			Master.FreezeManager.Freeze(p);
		}

		this.cancel();
	}
}