package io.github.BrianVanB.Utilities;

import org.bukkit.scheduler.BukkitRunnable;

import io.github.BrianVanB.GeoSpeurtocht.GeoSpeurtocht;

public class ScheduledBroadcast extends BukkitRunnable
{
	private GeoSpeurtocht Master;
	private int Counter;
	private String msg;
	
	/**
	 * <h1>ScheduledBroadcast</h1>
	 * Een bericht wat na een bepaalde tijd door de server naar alle spelers wordt gestuurd.
	 * <p>
	 * @param master De hoofdplugin
	 * @param message Het bericht om te sturen
	 * @param repeats Aantal keer dat het bericht herhaald wordt.
	 */
	public ScheduledBroadcast(GeoSpeurtocht master, String message, int repeats)
	{
		Master = master;
		
		msg = message;

		Counter = repeats;
		if(Counter < 1)
			Counter = 1;		
	}
	
	@Override
	public void run() 
	{
		if(Counter > 0)
		{
			Master.getServer().broadcastMessage(msg);
			Counter--;
		}
		else
		{
			this.cancel();
		}
	}
}
