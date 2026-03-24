package io.github.BrianVanB.FreezeModule;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import io.github.BrianVanB.GeoSpeurtocht.GeoSpeurtocht;

public class FreezeManager {

	private GeoSpeurtocht Master;
	private FreezeCommands cmdExecutor;
	private FreezeListener listener;

	public boolean GlobalFreeze = false;

	public FreezeManager(GeoSpeurtocht plugin)
	{
		Master = plugin;

		cmdExecutor = new FreezeCommands(Master, this);
		Master.getCommand("freeze").setExecutor(cmdExecutor);
		Master.getCommand("unfreeze").setExecutor(cmdExecutor);
		Master.getCommand("freezeall").setExecutor(cmdExecutor);
		Master.getCommand("unfreezeall").setExecutor(cmdExecutor);

		listener = new FreezeListener(this);
		Master.getServer().getPluginManager().registerEvents(listener, Master);
	}

	public void Freeze(Player p)
	{
		if (p.isOp())
			return;

		p.setMetadata("Frozen", new FixedMetadataValue(Master, true));
	}

	public void UnFreeze(Player p)
	{
		if (p.hasMetadata("Frozen"))
			p.removeMetadata("Frozen", Master);
	}

	public void Freezeall()
	{
		GlobalFreeze = true;
		for (Player p : Master.getServer().getOnlinePlayers())
		{
			Freeze(p);
		}
	}

	public void Unfreezeall()
	{
		GlobalFreeze = false;
		for (Player p : Master.getServer().getOnlinePlayers())
		{
			UnFreeze(p);
		}
	}

	public void Freezeall(World world)
	{
		for (Player p : Master.getServer().getOnlinePlayers())
		{
			if (!p.getWorld().equals(world))
				continue;

			Freeze(p);
		}
	}

	public void Unfreezeall(World world)
	{
		for (Player p : Master.getServer().getOnlinePlayers())
		{
			if (!p.getWorld().equals(world))
				continue;

			UnFreeze(p);
		}
	}
}