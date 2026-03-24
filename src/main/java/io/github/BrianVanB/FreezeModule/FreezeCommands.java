package io.github.BrianVanB.FreezeModule;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import io.github.BrianVanB.GeoSpeurtocht.GeoSpeurtocht;

public class FreezeCommands implements CommandExecutor {

	//Toegang tot andere functies
	private GeoSpeurtocht Master;
	private FreezeManager Manager;
	
	/**  
	 * <h1>FreezeCommands</h1>
	 * FreezeCommands luistert naar commando's die met bevriezen
	 * te maken hebben en voert deze uit.
	 * <p>
	 * @param master De hoofdplugin
	 * @param manager De class die het freezen beheert
	 */
	public FreezeCommands(GeoSpeurtocht master, FreezeManager manager)
	{
		Master = master;
		Manager = manager;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) 
	{		
		switch(command.getName().toLowerCase())
		{
			case "freeze":
				
				//Kijk of er een spelersnaam is gegeven
				if(args.length < 1)
					return Master.CommandError(sender, "Geef een speler om te bevriezen");
				
				//Kijk of het een geldige spelersnaam is
				Player toFreeze = Master.getServer().getPlayerExact(args[0]);
				if(toFreeze == null)
					return Master.CommandError(sender, "Speler niet gevonden");
				
				//bevries de speler
				Manager.Freeze(toFreeze);
			
				return true;
				
	//////////////////////////////////////////////////////////////////////////
				
			case "unfreeze":
				
				//check if argument given
				if(args.length < 1)
					return Master.CommandError(sender, "Geef een speler om te bevrijden");
				
				//get the targeted player
				Player toUnFreeze =  Master.getServer().getPlayerExact(args[0]);
				if(toUnFreeze == null)
					return Master.CommandError(sender, "Speler niet gevonden");
				
				//unfreeze
				Manager.UnFreeze(toUnFreeze);
				
				return true;
				
	//////////////////////////////////////////////////////////////////////////
				
			//Bevriest alle spelers
			case "freezeall":
				Manager.Freezeall();
				return true;
				
	//////////////////////////////////////////////////////////////////////////
				
			//bevrijd alle spelers
			case "unfreezeall":
				Manager.Unfreezeall();
				return true;
		}
		
		return false;
	}	
}
