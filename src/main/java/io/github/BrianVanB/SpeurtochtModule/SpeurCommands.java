package io.github.BrianVanB.SpeurtochtModule;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import io.github.BrianVanB.GeoSpeurtocht.GeoSpeurtocht;

public class SpeurCommands implements CommandExecutor {

	private final GeoSpeurtocht Master;
	private final SpeurtochtManager Manager;

	public SpeurCommands(GeoSpeurtocht master, SpeurtochtManager manager) {
		Master = master;
		Manager = manager;
	}

	@Override
	public boolean onCommand(
			CommandSender sender,
			Command command,
			String label,
			String[] args
	) {
		switch (command.getName().toLowerCase()) {
			case "setstart":
				if (sender instanceof Player) {
					Manager.SetStartpunt(((Player) sender).getLocation());
					sender.sendMessage(ChatColor.GREEN + "Startpunt geupdate.");
					Master.getLogger().info("Startpunt geupdate");
					return true;
				}

				return Master.CommandError(
						sender,
						ChatColor.RED + "Player-only command"
				);

			case "startpunt":
				if (!(sender instanceof Player)) {
					return Master.CommandError(sender, "That command is player only");
				}

				if (Manager.GetStartpunt() == null) {
					return Master.CommandError(
							sender,
							"Geen startpunt gevonden. Plaats een startpunt met /setstart",
							true
					);
				}

				((Player) sender).teleport(Manager.GetStartpunt());
				return true;

			case "startall":
				if (Manager.GetStartpunt() == null) {
					return Master.CommandError(
							sender,
							"Geen startpunt gevonden. Plaats een startpunt met /setstart"
					);
				}

				if (args.length < 1) {
					return Master.CommandError(
							sender,
							"Geen tijd gegeven: "
									+ Master.getCommand("startall").getUsage()
					);
				}

				if (Manager.IsRunning()) {
					return Master.CommandError(
							sender,
							"Er is al een speurtocht bezig! Gebruik /stopall om deze eerst te stoppen."
					);
				}

				int minutes;

				try {
					minutes = Integer.parseInt(args[0]);
				} catch (NumberFormatException e) {
					return Master.CommandError(sender, "Ongeldige waarde in tijd.");
				}

				if (minutes <= 0) {
					return Master.CommandError(sender, "Tijd moet groter zijn dan 0.");
				}

				Manager.StartSpeurtocht(minutes);
				return true;

			case "stopall":
				if (Manager.GetStartpunt() == null) {
					return Master.CommandError(
							sender,
							"Geen startpunt gevonden. Plaats een startpunt met /setstart"
					);
				}

				if (Manager.GetActiveWorld() == null) {
					return Master.CommandError(
							sender,
							"Er is geen actieve speurtochtwereld ingesteld."
					);
				}

				boolean force = false;
				boolean keepInventory = false;

				for (String arg : args) {
					if (arg.equalsIgnoreCase("--force")) {
						force = true;
					}

					if (arg.equalsIgnoreCase("--keepinventory")) {
						keepInventory = true;
					}
				}

				Master.getLogger().info(
						"Speurtocht wordt gestopt..."
								+ (force
								? " (force: hele server)"
								: " (alleen actieve spelers)")
				);

				Manager.FinishSpeurtocht(force, keepInventory, true);
				return true;

			case "stoptimers":
				if (!Manager.IsRunning()) {
					return Master.CommandError(
							sender,
							"Er is geen actieve speurtocht"
					);
				}

				Master.getLogger().info("Speurtocht timers gestopt.");

				Manager.StopTimersOnly();

				sender.sendMessage(
						"Timers gestopt. Gebruik /stopall om alle spelers te resetten"
				);

				return true;

			case "addtime":
				if (!Manager.IsRunning()) {
					return Master.CommandError(
							sender,
							"Er is geen actieve timer."
					);
				}

				if (args.length < 1) {
					return Master.CommandError(
							sender,
							"Gebruik: /addtime <seconden>"
					);
				}

				int seconds;

				try {
					seconds = Integer.parseInt(args[0]);
				} catch (NumberFormatException e) {
					return Master.CommandError(sender, "Ongeldig aantal seconden.");
				}

				if (seconds <= 0) {
					return Master.CommandError(
							sender,
							"Aantal seconden moet groter zijn dan 0."
					);
				}

				Manager.AddTime(seconds);

				sender.sendMessage(
						ChatColor.GREEN
								+ "Er zijn "
								+ seconds
								+ " seconden toegevoegd aan de timer."
				);

				if (Manager.GetActiveWorld() != null) {
					Master.broadcastToWorld(
							Manager.GetActiveWorld(),
							ChatColor.GOLD
									+ "Er zijn "
									+ seconds
									+ " seconden toegevoegd aan de timer."
					);
				}

				return true;

			case "addspeler":
				if (!Manager.IsRunning()) {
					return Master.CommandError(
							sender,
							"Er is geen actieve speurtocht."
					);
				}

				if (args.length < 1) {
					return Master.CommandError(
							sender,
							"Gebruik: /addspeler <speler>"
					);
				}

				Player toAdd = Master.getServer().getPlayerExact(args[0]);

				if (toAdd == null) {
					return Master.CommandError(sender, "Speler niet gevonden.");
				}

				if (!Manager.AddPlayerToSpeurtocht(toAdd)) {
					return Master.CommandError(
							sender,
							"Kon speler niet toevoegen aan de speurtocht."
					);
				}

				sender.sendMessage(
						ChatColor.GREEN
								+ toAdd.getName()
								+ " is toegevoegd aan de speurtocht."
				);
				return true;

			case "removespeler":
				if (Manager.GetActiveWorld() == null) {
					return Master.CommandError(
							sender,
							"Er is geen actieve speurtochtwereld."
					);
				}

				if (args.length < 2) {
					return Master.CommandError(
							sender,
							"Gebruik: /removespeler <speler> <freeze|release>"
					);
				}

				Player toRemove = Master.getServer().getPlayerExact(args[0]);

				if (toRemove == null) {
					return Master.CommandError(sender, "Speler niet gevonden.");
				}

				if (args[1].equalsIgnoreCase("freeze")) {
					if (!Manager.RemovePlayerFreeze(toRemove)) {
						return Master.CommandError(
								sender,
								"Kon speler niet verwijderen."
						);
					}

					sender.sendMessage(
							ChatColor.YELLOW
									+ toRemove.getName()
									+ " is verwijderd, teruggezet en bevroren."
					);
					return true;
				}

				if (args[1].equalsIgnoreCase("release")) {
					if (!Manager.RemovePlayerRelease(toRemove)) {
						return Master.CommandError(
								sender,
								"Kon speler niet verwijderen."
						);
					}

					sender.sendMessage(
							ChatColor.YELLOW
									+ toRemove.getName()
									+ " is verwijderd en vrijgegeven."
					);
					return true;
				}

				return Master.CommandError(
						sender,
						"Onbekende optie. Gebruik freeze of release."
				);
		}

		return false;
	}
}