package io.github.BrianVanB.SpeurtochtModule;

import io.github.BrianVanB.GeoSpeurtocht.GeoSpeurtocht;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;


public class SpeurCommands implements CommandExecutor {

	private final GeoSpeurtocht Master;
	private final SpeurtochtManager Manager;

	public SpeurCommands(GeoSpeurtocht master, SpeurtochtManager manager) {
		this.Master = master;
		this.Manager = manager;
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
				return handleSetStart(sender);

			case "startpunt":
				return handleStartpunt(sender);

			case "startall":
				return handleStartAll(sender, args);

			case "stopall":
				return handleStopAll(sender, args);

			case "stoptimers":
				return handleStopTimers(sender);

			case "addtime":
				return handleAddTime(sender, args);

			case "pausetimer":
				return handlePauseTimer(sender);

			case "resumetimer":
				return handleResumeTimer(sender);

			case "addspeler":
				return handleAddSpeler(sender, args);

			case "removespeler":
				return handleRemoveSpeler(sender, args);

			default:
				return false;
		}
	}

	private boolean handleSetStart(CommandSender sender) {
		Player player = getPlayerOrError(
				sender,
				ChatColor.RED + "Dit commando heeft een spelerlocatie nodig."
		);

		if (player == null) {
			return false;
		}

		SessionContext context = getSessionContext(player);

		/*
		 * Let op:
		 * We slaan bewust de echte fysieke Location op.
		 * Dus bij ClimateCrafter bijvoorbeeld:
		 *
		 * sessionKey: climatecrafter
		 * location.world: climatecrafter_diamant
		 *
		 * Dat is correct.
		 */
		Manager.SetStartpunt(player.getLocation());

		sender.sendMessage(
				ChatColor.GREEN
						+ "Startpunt geüpdatet voor speurtocht "
						+ ChatColor.WHITE
						+ context.displayName
						+ ChatColor.GREEN
						+ "."
		);

		Master.getLogger().info(
				"Startpunt geüpdatet voor sessie "
						+ context.sessionKey
						+ " vanuit wereld "
						+ context.world.getName()
		);

		return true;
	}

	private boolean handleStartpunt(CommandSender sender) {
		Player player = getPlayerOrError(
				sender,
				ChatColor.RED + "Dit commando heeft een spelerwereld nodig."
		);

		if (player == null) {
			return false;
		}

		SessionContext context = getSessionContext(player);

		if (Manager.GetStartpunt(context.world) == null) {
			return Master.CommandError(
					sender,
					ChatColor.RED
							+ "Geen startpunt gevonden voor speurtocht "
							+ ChatColor.WHITE
							+ context.displayName
							+ ChatColor.RED
							+ ". Plaats een startpunt met /setstart",
					true
			);
		}

		player.teleport(Manager.GetStartpunt(context.world));

		sender.sendMessage(
				ChatColor.GREEN
						+ "Je bent geteleporteerd naar het startpunt van "
						+ ChatColor.WHITE
						+ context.displayName
						+ ChatColor.GREEN
						+ "."
		);

		return true;
	}

	private boolean handleStartAll(CommandSender sender, String[] args) {
		StartAllOptions options;

		try {
			options = StartAllOptionsParser.parse(args);
		} catch (StartAllOptionsParseException exception) {
			return Master.CommandError(
					sender,
					ChatColor.RED + exception.getMessage()
			);
		}

		Player player = getPlayerOrError(
				sender,
				ChatColor.RED
						+ "Dit commando moet door een speler worden uitgevoerd, "
						+ "zodat de plugin weet welke speurtocht gestart moet worden."
		);

		if (player == null) {
			return false;
		}

		SessionContext context = getSessionContext(player);

		if (Manager.GetStartpunt(context.world) == null) {
			return Master.CommandError(
					sender,
					ChatColor.RED
							+ "Geen startpunt gevonden voor speurtocht "
							+ ChatColor.WHITE
							+ context.displayName
							+ ChatColor.RED
							+ ". Plaats een startpunt met /setstart"
			);
		}

		if (Manager.IsRunning(context.world)) {
			return Master.CommandError(
					sender,
					ChatColor.RED
							+ "Er is al een speurtocht bezig voor "
							+ ChatColor.WHITE
							+ context.displayName
							+ ChatColor.RED
							+ ". Gebruik /stopall om deze eerst te stoppen."
			);
		}

		Master.getLogger().info(
				"Speurtocht wordt gestart voor sessie "
						+ context.sessionKey
						+ " vanuit wereld "
						+ context.world.getName()
						+ " met timerMode="
						+ options.getTimerMode()
						+ ", minutes="
						+ options.getConfiguredMinutes()
						+ ", team="
						+ options.getTeamName()
		);

		Manager.StartSpeurtocht(context.world, options);

		if (options.isCountdown()) {
			sender.sendMessage(
					ChatColor.GREEN
							+ "Speurtocht gestart voor "
							+ ChatColor.WHITE
							+ context.displayName
							+ ChatColor.GREEN
							+ " met aflopende timer van "
							+ ChatColor.WHITE
							+ options.getConfiguredMinutes()
							+ ChatColor.GREEN
							+ " minuten."
			);

			return true;
		}

		String maxTimeText = options.hasConfiguredMinutes()
				? " met maximale tijd van " + options.getConfiguredMinutes() + " minuten"
				: " zonder maximale tijd";

		sender.sendMessage(
				ChatColor.GREEN
						+ "Time-trial gestart voor "
						+ ChatColor.WHITE
						+ context.displayName
						+ ChatColor.GREEN
						+ " voor team "
						+ ChatColor.WHITE
						+ options.getTeamName()
						+ ChatColor.GREEN
						+ maxTimeText
						+ "."
		);

		return true;
	}


	private boolean handleStopAll(CommandSender sender, String[] args) {
		Player player = getPlayerOrError(
				sender,
				ChatColor.RED
						+ "Dit commando moet door een speler worden uitgevoerd, "
						+ "zodat de plugin weet welke speurtocht gestopt moet worden."
		);

		if (player == null) {
			return false;
		}

		SessionContext context = getSessionContext(player);

		if (Manager.GetStartpunt(context.world) == null) {
			return Master.CommandError(
					sender,
					ChatColor.RED
							+ "Geen startpunt gevonden voor speurtocht "
							+ ChatColor.WHITE
							+ context.displayName
							+ ChatColor.RED
							+ ". Plaats een startpunt met /setstart"
			);
		}

		if (Manager.GetActiveWorld(context.world) == null) {
			return Master.CommandError(
					sender,
					ChatColor.RED
							+ "Er is geen actieve speurtocht voor "
							+ ChatColor.WHITE
							+ context.displayName
							+ ChatColor.RED
							+ "."
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
				"Speurtocht wordt gestopt voor sessie "
						+ context.sessionKey
						+ " vanuit wereld "
						+ context.world.getName()
						+ "..."
						+ (force
						? " (force: hele sessie/fysieke wereld volgens Manager-logica)"
						: " (alleen actieve spelers)")
		);

		Manager.FinishSpeurtocht(context.world, force, keepInventory, true);

		sender.sendMessage(
				ChatColor.YELLOW
						+ "Speurtocht gestopt voor "
						+ ChatColor.WHITE
						+ context.displayName
						+ ChatColor.YELLOW
						+ "."
		);

		return true;
	}

	private boolean handleStopTimers(CommandSender sender) {
		Player player = getPlayerOrError(
				sender,
				ChatColor.RED
						+ "Dit commando moet door een speler worden uitgevoerd, "
						+ "zodat de plugin weet welke timer gestopt moet worden."
		);

		if (player == null) {
			return false;
		}

		SessionContext context = getSessionContext(player);

		if (!Manager.IsRunning(context.world)) {
			return Master.CommandError(
					sender,
					ChatColor.RED
							+ "Er is geen actieve speurtocht voor "
							+ ChatColor.WHITE
							+ context.displayName
							+ ChatColor.RED
							+ "."
			);
		}

		Master.getLogger().info(
				"Speurtocht timer gestopt voor sessie "
						+ context.sessionKey
						+ " vanuit wereld "
						+ context.world.getName()
						+ "."
		);

		Manager.StopTimersOnly(context.world);

		sender.sendMessage(
				ChatColor.YELLOW
						+ "Timer gestopt voor "
						+ ChatColor.WHITE
						+ context.displayName
						+ ChatColor.YELLOW
						+ ". Gebruik /stopall om spelers alsnog te resetten."
		);

		return true;
	}

	private boolean handleAddTime(CommandSender sender, String[] args) {
		Player player = getPlayerOrError(
				sender,
				ChatColor.RED
						+ "Dit commando moet door een speler worden uitgevoerd, "
						+ "zodat de plugin weet welke timer aangepast moet worden."
		);

		if (player == null) {
			return false;
		}

		SessionContext context = getSessionContext(player);

		if (!Manager.IsRunning(context.world)) {
			return Master.CommandError(
					sender,
					ChatColor.RED
							+ "Er is geen actieve timer voor "
							+ ChatColor.WHITE
							+ context.displayName
							+ ChatColor.RED
							+ "."
			);
		}

		if (args.length < 1) {
			return Master.CommandError(
					sender,
					ChatColor.RED + "Gebruik: /addtime <seconden>"
			);
		}

		int seconds;

		try {
			seconds = Integer.parseInt(args[0]);
		} catch (NumberFormatException e) {
			return Master.CommandError(
					sender,
					ChatColor.RED + "Ongeldig aantal seconden."
			);
		}

		if (seconds <= 0) {
			return Master.CommandError(
					sender,
					ChatColor.RED + "Aantal seconden moet groter zijn dan 0."
			);
		}

		Manager.AddTime(context.world, seconds);

		sender.sendMessage(
				ChatColor.GREEN
						+ "Er zijn "
						+ seconds
						+ " seconden toegevoegd aan de timer van "
						+ ChatColor.WHITE
						+ context.displayName
						+ ChatColor.GREEN
						+ "."
		);

		Master.broadcastToWorld(
				context.world,
				ChatColor.GOLD
						+ "Er zijn "
						+ seconds
						+ " seconden toegevoegd aan de timer."
		);

		return true;
	}

	private boolean handlePauseTimer(CommandSender sender) {
		Player player = getPlayerOrError(
				sender,
				ChatColor.RED
						+ "Dit commando moet door een speler worden uitgevoerd, "
						+ "zodat de plugin weet welke timer gepauzeerd moet worden."
		);

		if (player == null) {
			return false;
		}

		SessionContext context = getSessionContext(player);

		if (!Manager.PauseTimer(context.world)) {
			return Master.CommandError(
					sender,
					ChatColor.RED
							+ "Kon timer niet pauzeren. Er is geen actieve timer, of de timer is al gepauzeerd."
			);
		}

		sender.sendMessage(
				ChatColor.YELLOW
						+ "Timer gepauzeerd voor "
						+ ChatColor.WHITE
						+ context.displayName
						+ ChatColor.YELLOW
						+ "."
		);

		return true;
	}

	private boolean handleResumeTimer(CommandSender sender) {
		Player player = getPlayerOrError(
				sender,
				ChatColor.RED
						+ "Dit commando moet door een speler worden uitgevoerd, "
						+ "zodat de plugin weet welke timer hervat moet worden."
		);

		if (player == null) {
			return false;
		}

		SessionContext context = getSessionContext(player);

		if (!Manager.ResumeTimer(context.world)) {
			return Master.CommandError(
					sender,
					ChatColor.RED
							+ "Kon timer niet hervatten. Er is geen actieve gepauzeerde timer."
			);
		}

		sender.sendMessage(
				ChatColor.GREEN
						+ "Timer hervat voor "
						+ ChatColor.WHITE
						+ context.displayName
						+ ChatColor.GREEN
						+ "."
		);

		return true;
	}

	private boolean handleAddSpeler(CommandSender sender, String[] args) {
		Player commandPlayer = getPlayerOrError(
				sender,
				ChatColor.RED
						+ "Dit commando moet door een speler worden uitgevoerd, "
						+ "zodat de plugin weet aan welke speurtocht de speler toegevoegd moet worden."
		);

		if (commandPlayer == null) {
			return false;
		}

		SessionContext context = getSessionContext(commandPlayer);

		if (!Manager.IsRunning(context.world)) {
			return Master.CommandError(
					sender,
					ChatColor.RED
							+ "Er is geen actieve speurtocht voor "
							+ ChatColor.WHITE
							+ context.displayName
							+ ChatColor.RED
							+ "."
			);
		}

		if (args.length < 1) {
			return Master.CommandError(
					sender,
					ChatColor.RED + "Gebruik: /addspeler <speler>"
			);
		}

		Player playerToAdd = Master.getServer().getPlayerExact(args[0]);

		if (playerToAdd == null) {
			return Master.CommandError(
					sender,
					ChatColor.RED + "Speler niet gevonden."
			);
		}

		if (!Manager.AddPlayerToSpeurtocht(playerToAdd, context.world)) {
			return Master.CommandError(
					sender,
					ChatColor.RED
							+ "Kon speler niet toevoegen aan de speurtocht "
							+ ChatColor.WHITE
							+ context.displayName
							+ ChatColor.RED
							+ "."
			);
		}

		sender.sendMessage(
				ChatColor.GREEN
						+ playerToAdd.getName()
						+ " is toegevoegd aan "
						+ ChatColor.WHITE
						+ context.displayName
						+ ChatColor.GREEN
						+ "."
		);

		return true;
	}

	private boolean handleRemoveSpeler(CommandSender sender, String[] args) {
		Player commandPlayer = getPlayerOrError(
				sender,
				ChatColor.RED
						+ "Dit commando moet door een speler worden uitgevoerd, "
						+ "zodat de plugin weet uit welke speurtocht de speler verwijderd moet worden."
		);

		if (commandPlayer == null) {
			return false;
		}

		SessionContext context = getSessionContext(commandPlayer);

		if (Manager.GetActiveWorld(context.world) == null) {
			return Master.CommandError(
					sender,
					ChatColor.RED
							+ "Er is geen actieve speurtocht voor "
							+ ChatColor.WHITE
							+ context.displayName
							+ ChatColor.RED
							+ "."
			);
		}

		if (args.length < 2) {
			return Master.CommandError(
					sender,
					ChatColor.RED
							+ "Gebruik: /removespeler <speler> <freeze|release>"
			);
		}

		Player playerToRemove = Master.getServer().getPlayerExact(args[0]);

		if (playerToRemove == null) {
			return Master.CommandError(
					sender,
					ChatColor.RED + "Speler niet gevonden."
			);
		}

		if (args[1].equalsIgnoreCase("freeze")) {
			if (!Manager.RemovePlayerFreeze(playerToRemove, context.world)) {
				return Master.CommandError(
						sender,
						ChatColor.RED
								+ "Kon speler niet verwijderen. Zit deze speler wel in "
								+ ChatColor.WHITE
								+ context.displayName
								+ ChatColor.RED
								+ "?"
				);
			}

			sender.sendMessage(
					ChatColor.YELLOW
							+ playerToRemove.getName()
							+ " is verwijderd uit "
							+ ChatColor.WHITE
							+ context.displayName
							+ ChatColor.YELLOW
							+ ", teruggezet en bevroren."
			);

			return true;
		}

		if (args[1].equalsIgnoreCase("release")) {
			if (!Manager.RemovePlayerRelease(playerToRemove, context.world)) {
				return Master.CommandError(
						sender,
						ChatColor.RED
								+ "Kon speler niet verwijderen. Zit deze speler wel in "
								+ ChatColor.WHITE
								+ context.displayName
								+ ChatColor.RED
								+ "?"
				);
			}

			sender.sendMessage(
					ChatColor.YELLOW
							+ playerToRemove.getName()
							+ " is verwijderd uit "
							+ ChatColor.WHITE
							+ context.displayName
							+ ChatColor.YELLOW
							+ " en vrijgegeven."
			);

			return true;
		}

		return Master.CommandError(
				sender,
				ChatColor.RED + "Onbekende optie. Gebruik freeze of release."
		);
	}

	private Player getPlayerOrError(CommandSender sender, String errorMessage) {
		if (!(sender instanceof Player)) {
			Master.CommandError(sender, errorMessage);
			return null;
		}

		return (Player) sender;
	}

	private SessionContext getSessionContext(Player player) {
		World world = player.getWorld();
		String sessionKey = Master.getSessionResolver().resolveSessionKey(world);
		String displayName = Master.getSessionResolver().getDisplayName(sessionKey);

		return new SessionContext(world, sessionKey, displayName);
	}

	private static final class SessionContext {
		private final World world;
		private final String sessionKey;
		private final String displayName;

		private SessionContext(World world, String sessionKey, String displayName) {
			this.world = world;
			this.sessionKey = sessionKey;
			this.displayName = displayName;
		}
	}
}