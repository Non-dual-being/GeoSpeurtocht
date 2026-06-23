package io.github.BrianVanB.SpeurtochtModule;

import org.bukkit.ChatColor;
import org.bukkit.World;
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
				if (!(sender instanceof Player)) {
					return Master.CommandError(
							sender,
							ChatColor.RED + "Dit commando heeft een spelerlocatie nodig."
					);
				}

				Player setStartPlayer = (Player) sender;
				Manager.SetStartpunt(setStartPlayer.getLocation());

				sender.sendMessage(
						ChatColor.GREEN
								+ "Startpunt geupdate voor wereld "
								+ setStartPlayer.getWorld().getName()
								+ "."
				);

				Master.getLogger().info(
						"Startpunt geupdate voor wereld "
								+ setStartPlayer.getWorld().getName()
				);

				return true;

			case "startpunt":
				if (!(sender instanceof Player)) {
					return Master.CommandError(
							sender,
							ChatColor.RED + "Dit commando heeft een spelerwereld nodig."
					);
				}

				Player startpuntPlayer = (Player) sender;
				World startpuntWorld = startpuntPlayer.getWorld();

				if (Manager.GetStartpunt(startpuntWorld) == null) {
					return Master.CommandError(
							sender,
							"Geen startpunt gevonden voor deze wereld. Plaats een startpunt met /setstart",
							true
					);
				}

				startpuntPlayer.teleport(Manager.GetStartpunt(startpuntWorld));
				return true;

			case "startall":
				if (!(sender instanceof Player)) {
					return Master.CommandError(
							sender,
							ChatColor.RED + "Dit commando moet door een speler worden uitgevoerd, zodat de plugin weet in welke wereld gestart moet worden."
					);
				}

				Player startPlayer = (Player) sender;
				World startWorld = startPlayer.getWorld();

				if (Manager.GetStartpunt(startWorld) == null) {
					return Master.CommandError(
							sender,
							"Geen startpunt gevonden voor deze wereld. Plaats een startpunt met /setstart"
					);
				}

				if (args.length < 1) {
					return Master.CommandError(
							sender,
							"Geen tijd gegeven: "
									+ Master.getCommand("startall").getUsage()
					);
				}

				if (Manager.IsRunning(startWorld)) {
					return Master.CommandError(
							sender,
							"Er is al een speurtocht bezig in deze wereld! Gebruik /stopall om deze eerst te stoppen."
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

				Manager.StartSpeurtocht(startWorld, minutes);
				return true;

			case "stopall":
				if (!(sender instanceof Player)) {
					return Master.CommandError(
							sender,
							ChatColor.RED + "Dit commando moet door een speler worden uitgevoerd, zodat de plugin weet welke wereld gestopt moet worden."
					);
				}

				Player stopPlayer = (Player) sender;
				World stopWorld = stopPlayer.getWorld();

				if (Manager.GetStartpunt(stopWorld) == null) {
					return Master.CommandError(
							sender,
							"Geen startpunt gevonden voor deze wereld. Plaats een startpunt met /setstart"
					);
				}

				if (Manager.GetActiveWorld(stopWorld) == null) {
					return Master.CommandError(
							sender,
							"Er is geen actieve speurtocht in deze wereld."
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
						"Speurtocht wordt gestopt in wereld "
								+ stopWorld.getName()
								+ "..."
								+ (force
								? " (force: hele wereld)"
								: " (alleen actieve spelers)")
				);

				Manager.FinishSpeurtocht(stopWorld, force, keepInventory, true);
				return true;

			case "stoptimers":
				if (!(sender instanceof Player)) {
					return Master.CommandError(
							sender,
							ChatColor.RED + "Dit commando moet door een speler worden uitgevoerd, zodat de plugin weet welke timer gestopt moet worden."
					);
				}

				Player stopTimerPlayer = (Player) sender;
				World stopTimerWorld = stopTimerPlayer.getWorld();

				if (!Manager.IsRunning(stopTimerWorld)) {
					return Master.CommandError(
							sender,
							"Er is geen actieve speurtocht in deze wereld."
					);
				}

				Master.getLogger().info(
						"Speurtocht timer gestopt in wereld "
								+ stopTimerWorld.getName()
								+ "."
				);

				Manager.StopTimersOnly(stopTimerWorld);

				sender.sendMessage(
						"Timer gestopt. Gebruik /stopall om spelers alsnog te resetten."
				);

				return true;

			case "addtime":
				if (!(sender instanceof Player)) {
					return Master.CommandError(
							sender,
							ChatColor.RED + "Dit commando moet door een speler worden uitgevoerd, zodat de plugin weet welke timer aangepast moet worden."
					);
				}

				Player addTimePlayer = (Player) sender;
				World addTimeWorld = addTimePlayer.getWorld();

				if (!Manager.IsRunning(addTimeWorld)) {
					return Master.CommandError(
							sender,
							"Er is geen actieve timer in deze wereld."
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

				Manager.AddTime(addTimeWorld, seconds);

				sender.sendMessage(
						ChatColor.GREEN
								+ "Er zijn "
								+ seconds
								+ " seconden toegevoegd aan de timer van deze wereld."
				);

				Master.broadcastToWorld(
						addTimeWorld,
						ChatColor.GOLD
								+ "Er zijn "
								+ seconds
								+ " seconden toegevoegd aan de timer."
				);

				return true;

			case "addspeler":
				if (!(sender instanceof Player)) {
					return Master.CommandError(
							sender,
							ChatColor.RED + "Dit commando moet door een speler worden uitgevoerd, zodat de plugin weet aan welke speurtocht de speler toegevoegd moet worden."
					);
				}

				Player addCommandPlayer = (Player) sender;
				World addCommandWorld = addCommandPlayer.getWorld();

				if (!Manager.IsRunning(addCommandWorld)) {
					return Master.CommandError(
							sender,
							"Er is geen actieve speurtocht in deze wereld."
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

				if (!Manager.AddPlayerToSpeurtocht(toAdd, addCommandWorld)) {
					return Master.CommandError(
							sender,
							"Kon speler niet toevoegen aan de speurtocht."
					);
				}

				sender.sendMessage(
						ChatColor.GREEN
								+ toAdd.getName()
								+ " is toegevoegd aan de speurtocht in wereld "
								+ addCommandWorld.getName()
								+ "."
				);
				return true;

			case "removespeler":
				if (!(sender instanceof Player)) {
					return Master.CommandError(
							sender,
							ChatColor.RED + "Dit commando moet door een speler worden uitgevoerd, zodat de plugin weet uit welke speurtocht de speler verwijderd moet worden."
					);
				}

				Player removeCommandPlayer = (Player) sender;
				World removeCommandWorld = removeCommandPlayer.getWorld();

				if (Manager.GetActiveWorld(removeCommandWorld) == null) {
					return Master.CommandError(
							sender,
							"Er is geen actieve speurtocht in deze wereld."
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
					if (!Manager.RemovePlayerFreeze(toRemove, removeCommandWorld)) {
						return Master.CommandError(
								sender,
								"Kon speler niet verwijderen. Zit deze speler wel in de speurtocht van deze wereld?"
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
					if (!Manager.RemovePlayerRelease(toRemove, removeCommandWorld)) {
						return Master.CommandError(
								sender,
								"Kon speler niet verwijderen. Zit deze speler wel in de speurtocht van deze wereld?"
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