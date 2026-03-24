package io.github.BrianVanB.Utilities;

import java.util.Set;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import io.github.BrianVanB.GeoSpeurtocht.GeoSpeurtocht;

public class ExtraCommands implements CommandExecutor, Listener {

	private GeoSpeurtocht Master;
	private ItemStack kinderKiller;

	public ExtraCommands(GeoSpeurtocht plugin)
	{
		Master = plugin;
		Master.getCommand("tpall").setExecutor(this);
		Master.getCommand("broadcast").setExecutor(this);
		Master.getCommand("kinderkiller").setExecutor(this);

		Master.getServer().getPluginManager().registerEvents(this, Master);

		kinderKiller = new ItemStack(Material.STICK);
		ItemMeta meta = kinderKiller.getItemMeta();
		meta.setDisplayName("Kinder Killer");
		meta.addEnchant(Enchantment.INFINITY, 1, true);
		kinderKiller.setItemMeta(meta);
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
			case "tpall":
				if (sender instanceof Player)
				{
					TPall(((Player) sender).getLocation());
					return true;
				}
				else
				{
					return Master.CommandError(
							sender,
							ChatColor.RED + "Player-only command"
					);
				}

			case "broadcast":
				if (args.length < 1)
					return Master.CommandError(
							sender,
							ChatColor.RED + "No message given"
					);

				String msg = "";
				for (String s : args)
					msg += s + " ";

				Master.getServer().broadcastMessage(ChatColor.GOLD + msg);
				return true;

			case "kinderkiller":
				if (sender instanceof Player)
				{
					if (((Player) sender).getInventory().contains(kinderKiller))
						return Master.CommandError(sender, "Je hebt er al een!", true);

					((Player) sender).getInventory().addItem(kinderKiller);
					return true;
				}
				else
				{
					return Master.CommandError(
							sender,
							ChatColor.RED + "Player-only command"
					);
				}
		}

		return false;
	}

	@EventHandler
	private void onPlayerClick(PlayerInteractEvent e)
	{
		Player p = e.getPlayer();
		if (p.getInventory().getItemInMainHand().equals(kinderKiller))
		{
			if (e.getAction().equals(Action.LEFT_CLICK_AIR))
			{
				p.getWorld().strikeLightning(
						p.getTargetBlock((Set<Material>) null, 200).getLocation()
				);
			}
		}
	}

	public void TPall(Location target)
	{
		for (Player p : Master.getServer().getOnlinePlayers())
		{
			if (p.isOp())
				continue;

			p.teleport(target);
		}
	}

	public void TPallInWorld(Location target, String worldName)
	{
		for (Player p : Master.getServer().getOnlinePlayers())
		{
			if (p.isOp())
				continue;

			if (!p.getWorld().getName().equals(worldName))
				continue;

			p.teleport(target);
		}
	}
}