package io.github.BrianVanB.GeoSpeurtocht;

import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.mvplugins.multiverse.core.MultiverseCoreApi;
import org.mvplugins.multiverse.core.world.MultiverseWorld;
import org.mvplugins.multiverse.core.world.WorldManager;

import io.github.BrianVanB.FreezeModule.FreezeManager;
import io.github.BrianVanB.SpeurtochtModule.SpeurtochtManager;
import io.github.BrianVanB.Utilities.ExtraCommands;

public class GeoSpeurtocht extends JavaPlugin {

	//main class that is loadin in plugin.yml
	public SpeurtochtManager SpeurManager;
	public FreezeManager FreezeManager;
	public ExtraCommands ExtraStuff;

	@Override
	public void onEnable()
	{
		FreezeManager = new FreezeManager(this);
		SpeurManager = new SpeurtochtManager(this);
		ExtraStuff = new ExtraCommands(this);

		/*
		 * instantiating with this enables subclasses to have access to the main plugin
		 * */
		getLogger().info("Finished loading");
	}

	@Override
	public void onDisable()
	{
		getLogger().info("Storing data...");
		SpeurManager.SaveStartpunt();
	}

	public boolean CommandError(CommandSender sender, String msg)
	{
		sender.sendMessage(msg);
		return false;
	}

	public boolean CommandError(CommandSender sender, String msg, boolean returnval)
	{
		sender.sendMessage(msg);
		return returnval;
	}

	public void broadcastToWorld(World world, String message)
	{
		if (world == null)
			return;

		for (Player p : world.getPlayers())
		{
			p.sendMessage(message);
		}
	}

	public boolean isBegeleider(Player player)
	{
		return player.isOp() || player.hasPermission("begeleider");
	}

	public boolean shouldClearInventory(Player player)
	{
		return !player.isOp() && !player.hasPermission("begeleider");
	}

	public boolean isSpeurtochtSpeler(Player player, World activeWorld)
	{
		if (player == null) return false;

		if (!player.isOnline()) return false;

		if (isBegeleider(player)) return false;

        return activeWorld == null || player.getWorld().equals(activeWorld);

    }
	public GameMode getWorldConfiguredGameMode(World world)
	{
		try
		{
			MultiverseCoreApi api = MultiverseCoreApi.get();
			WorldManager worldManager = api.getWorldManager();

			if (worldManager != null)
			{
				var mvWorldOption = worldManager.getWorld(world.getName());
				if (mvWorldOption.isDefined())
				{
					MultiverseWorld mvWorld = mvWorldOption.get();

					if (mvWorld.getGameMode() != null)
						return mvWorld.getGameMode();
				}
			}
		}
		catch (Exception e)
		{
			getLogger().warning(
					"Kon Multiverse gamemode niet ophalen voor wereld "
							+ world.getName()
							+ ": "
							+ e.getMessage()
			);
		}

		return getServer().getDefaultGameMode();
	}
}