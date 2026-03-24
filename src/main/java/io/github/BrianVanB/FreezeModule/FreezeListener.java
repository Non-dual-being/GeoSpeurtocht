package io.github.BrianVanB.FreezeModule;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class FreezeListener implements Listener
{
	private FreezeManager Manager;

	public FreezeListener(FreezeManager fm)
	{
		Manager = fm;
	}

	@EventHandler
	public void onMove(PlayerMoveEvent e)
	{
		if (e.getPlayer().hasMetadata("Frozen"))
		{
			e.getPlayer().sendMessage(
					ChatColor.RED + "Wacht tot de begeleider de speurtocht start!"
			);
			e.getPlayer().teleport(e.getFrom());
		}
	}
}