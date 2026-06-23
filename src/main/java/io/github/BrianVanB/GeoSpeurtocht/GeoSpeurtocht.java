package io.github.BrianVanB.GeoSpeurtocht;

import io.github.BrianVanB.FreezeModule.FreezeManager;
import io.github.BrianVanB.SpeurtochtModule.SpeurtochtManager;
import io.github.BrianVanB.SpeurtochtModule.SpeurtochtSessionResolver;
import io.github.BrianVanB.Utilities.ExtraCommands;

import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import org.mvplugins.multiverse.core.MultiverseCoreApi;
import org.mvplugins.multiverse.core.world.MultiverseWorld;
import org.mvplugins.multiverse.core.world.WorldManager;

public class GeoSpeurtocht extends JavaPlugin {

	// Main class die in plugin.yml geladen wordt.
	public SpeurtochtManager SpeurManager;
	public FreezeManager FreezeManager;
	public ExtraCommands ExtraStuff;

	private SpeurtochtSessionResolver sessionResolver;

	@Override
	public void onEnable() {
		/*
		 * BELANGRIJK:
		 * Deze regel kopieert src/main/resources/config.yml naar:
		 *
		 * plugins/GeoSpeurtocht/config.yml
		 *
		 * Alleen als die config daar nog niet bestaat.
		 */
		saveDefaultConfig();

		/*
		 * Eerst de resolver maken, zodat de rest van de plugin weet:
		 *
		 * fysieke wereldnaam -> logische speurtocht-sessie
		 *
		 * Bijvoorbeeld:
		 * climatecrafter_diamant -> climatecrafter
		 * climatecrafter_koper   -> climatecrafter
		 * GeoFort_Heat           -> GeoFort_Heat
		 */
		this.sessionResolver = new SpeurtochtSessionResolver(this);

		/*
		 * Instantiating with 'this' geeft submodules toegang tot de main plugin.
		 */
		FreezeManager = new FreezeManager(this);
		SpeurManager = new SpeurtochtManager(this);
		ExtraStuff = new ExtraCommands(this);

		getLogger().info("Finished loading GeoSpeurtocht");
	}

	@Override
	public void onDisable() {
		getLogger().info("Storing data...");

		if (SpeurManager != null) {
			SpeurManager.SaveStartpunt();
		}
	}

	public boolean CommandError(CommandSender sender, String msg) {
		sender.sendMessage(msg);
		return false;
	}

	public boolean CommandError(CommandSender sender, String msg, boolean returnval) {
		sender.sendMessage(msg);
		return returnval;
	}

	public void broadcastToWorld(World world, String message) {
		if (world == null) {
			return;
		}

		for (Player player : world.getPlayers()) {
			player.sendMessage(message);
		}
	}

	public boolean isBegeleider(Player player) {
		return player.isOp() || player.hasPermission("begeleider");
	}

	public boolean shouldClearInventory(Player player) {
		return !player.isOp() && !player.hasPermission("begeleider");
	}

	/*
	 * Deze methode is aangepast voor fase 3.
	 *
	 * Oude gedrag:
	 * speler moest in exact dezelfde fysieke wereld zitten.
	 *
	 * Nieuwe gedrag:
	 * speler moet in dezelfde logische speurtocht-sessie zitten.
	 *
	 * Dus:
	 * climatecrafter_diamant en climatecrafter_koper tellen nu als dezelfde sessie.
	 */
	public boolean isSpeurtochtSpeler(Player player, World activeWorld) {
		if (player == null) {
			return false;
		}

		if (!player.isOnline()) {
			return false;
		}

		if (isBegeleider(player)) {
			return false;
		}

		if (activeWorld == null) {
			return true;
		}

		String playerSessionKey = sessionResolver.resolveSessionKey(player.getWorld());
		String activeSessionKey = sessionResolver.resolveSessionKey(activeWorld);

		return playerSessionKey.equals(activeSessionKey);
	}

	public GameMode getWorldConfiguredGameMode(World world) {
		try {
			MultiverseCoreApi api = MultiverseCoreApi.get();
			WorldManager worldManager = api.getWorldManager();

			if (worldManager != null) {
				var mvWorldOption = worldManager.getWorld(world.getName());

				if (mvWorldOption.isDefined()) {
					MultiverseWorld mvWorld = mvWorldOption.get();

					if (mvWorld.getGameMode() != null) {
						return mvWorld.getGameMode();
					}
				}
			}
		} catch (Exception e) {
			getLogger().warning(
					"Kon Multiverse gamemode niet ophalen voor wereld "
							+ world.getName()
							+ ": "
							+ e.getMessage()
			);
		}

		return getServer().getDefaultGameMode();
	}

	public SpeurtochtSessionResolver getSessionResolver() {
		return sessionResolver;
	}
}