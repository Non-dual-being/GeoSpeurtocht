package io.github.BrianVanB.SpeurtochtModule;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import io.github.BrianVanB.GeoSpeurtocht.GeoSpeurtocht;
import io.github.BrianVanB.Utilities.ScheduledBroadcast;

public class SpeurCommands implements CommandExecutor {

	private GeoSpeurtocht Master;
	private SpeurtochtManager Manager;

	public SpeurCommands(GeoSpeurtocht master, SpeurtochtManager manager)
	{
		Master = master;
		Manager = manager;
	}

	private boolean shouldAffectPlayer(Player p, boolean force)
	{
		if (force)
			return true;

		if (Manager.ActiveWorld == null)
			return false;

		return p.getWorld().equals(Manager.ActiveWorld);
	}

	@Override
	public boolean onCommand(
			CommandSender sender,
			Command command,
			String label,
			String[] args
	) {

		switch (command.getName().toLowerCase())
		{
			case "setstart":
				if (sender instanceof Player)
				{
					Manager.Startpunt = ((Player) sender).getLocation();
					sender.sendMessage(ChatColor.GREEN + "Startpunt geupdate.");
					Master.getLogger().info("Startpunt geupdate");
					return true;
				}
				else
				{
					return Master.CommandError(
							sender,
							ChatColor.RED + "Player-only command"
					);
				}

			case "startpunt":
				if (sender instanceof Player)
				{
					if (Manager.Startpunt == null)
					{
						return Master.CommandError(
								sender,
								"Geen startpunt gevonden. Plaats een startpunt met /setstart",
								true
						);
					}

					((Player) sender).teleport(Manager.Startpunt);
					return true;
				}
				else
				{
					return Master.CommandError(
							sender,
							"That command is player only"
					);
				}

			case "startall":
				if (Manager.Startpunt == null)
					return Master.CommandError(
							sender,
							"Geen startpunt gevonden. Plaats een startpunt met /setstart"
					);

				if (args.length < 1)
					return Master.CommandError(
							sender,
							"Geen tijd gegeven: "
									+ Master.getCommand("startall").getUsage()
					);

				if (Manager.Running)
					return Master.CommandError(
							sender,
							"Er is al een speurtocht bezig! Gebruik /stopall om deze eerst te stoppen."
					);

				float minutes = 0;
				try
				{
					int tijd = Integer.parseInt(args[0]);
					minutes = tijd;
				}
				catch (NumberFormatException e)
				{
					Master.getLogger().warning(e.getMessage());
					return Master.CommandError(sender, "Ongeldige waarde in tijd.");
				}

				Manager.ActiveWorld = Manager.Startpunt.getWorld();

				Master.getLogger().info(
						"Speurtocht wordt gestart in wereld "
								+ Manager.ActiveWorld.getName()
								+ "..."
				);

				Manager.Timers = new BukkitTask[] {
						new ScheduledBroadcast(
								Master,
								ChatColor.GOLD + "Tijd is op.",
								5
						).runTaskTimer(
								Master,
								(long) (minutes * 60 * 20),
								20
						),

						new SpeurtochtEind(
								Master,
								Manager.Startpunt,
								Manager.ActiveWorld
						).runTaskLater(
								Master,
								(long) (minutes * 60 * 20 + 100)
						)
				};

				Master.FreezeManager.Unfreezeall(Manager.ActiveWorld);
				Master.ExtraStuff.TPallInWorld(
						Manager.Startpunt,
						Manager.ActiveWorld.getName()
				);

				Master.broadcastToWorld(
						Manager.ActiveWorld,
						ChatColor.GREEN + "Je mag nu beginnen. Succes!"
				);
				Master.broadcastToWorld(
						Manager.ActiveWorld,
						ChatColor.GOLD + "Je hebt " + (int) minutes + " minuten."
				);

				Manager.TimerBar.Create((int) minutes, Manager.ActiveWorld);
				Manager.Running = true;
				return true;

			case "stopall":
				if (Manager.Startpunt == null)
					return Master.CommandError(
							sender,
							"Geen startpunt gevonden. Plaats een startpunt met /setstart"
					);

				if (Manager.ActiveWorld == null)
					return Master.CommandError(
							sender,
							"Er is geen actieve speurtochtwereld ingesteld."
					);

				boolean force = false;
				boolean keepInventory = false;

				for (String arg : args)
				{
					if (arg.equalsIgnoreCase("--force"))
						force = true;

					if (arg.equalsIgnoreCase("--keepinventory"))
						keepInventory = true;
				}

				Master.getLogger().info(
						"Speurtocht wordt gestopt..."
								+ (force
								? " (force: hele server)"
								: " (alleen actieve wereld)")
				);

				if (Manager.Running && Manager.Timers != null)
				{
					for (BukkitTask t : Manager.Timers)
					{
						if (t != null)
							t.cancel();
					}
				}

				if (Manager.Running)
					Manager.TimerBar.Cancel();

				GameMode targetMode = Master.getWorldConfiguredGameMode(
						Manager.Startpunt.getWorld()
				);

				for (Player p : Master.getServer().getOnlinePlayers())
				{
					if (!shouldAffectPlayer(p, force))
						continue;

					if (p.isOp())
						continue;

					p.teleport(Manager.Startpunt);
					Master.FreezeManager.Freeze(p);
					p.setGameMode(targetMode);

					if (!keepInventory && Master.shouldClearInventory(p))
					{
						p.getInventory().clear();
						p.getInventory().setArmorContents(null);
						p.updateInventory();
					}
				}

				if (force)
				{
					Master.getServer().broadcastMessage(
							ChatColor.YELLOW
									+ "Speurtocht geforceerd gestopt. Alle spelers zijn teruggezet."
					);
				}
				else
				{
					Master.broadcastToWorld(
							Manager.ActiveWorld,
							ChatColor.GOLD + "Tijd is op."
					);
				}

				Manager.Running = false;
				Manager.ActiveWorld = null;

				return true;

			case "stoptimers":
				if (!Manager.Running)
					return Master.CommandError(
							sender,
							"Er is geen actieve speurtocht"
					);

				Master.getLogger().info("Speurtocht timers gestopt.");

				for (BukkitTask t : Manager.Timers)
				{
					if (t != null)
						t.cancel();
				}

				Manager.TimerBar.Cancel();
				Manager.Running = false;
				sender.sendMessage(
						"Timers gestopt. Gebruik /stopall om alle spelers te resetten"
				);

				return true;
		}

		return false;
	}
}