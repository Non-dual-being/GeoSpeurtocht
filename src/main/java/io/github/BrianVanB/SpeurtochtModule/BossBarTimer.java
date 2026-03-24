package io.github.BrianVanB.SpeurtochtModule;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import io.github.BrianVanB.GeoSpeurtocht.GeoSpeurtocht;

public class BossBarTimer implements Listener {

	private GeoSpeurtocht Master;
	private BossBar Bar;
	private BukkitTask updater;

	public BossBarTimer(GeoSpeurtocht master)
	{
		Master = master;
	}

	public void Create(int min, World world)
	{
		Bar = Bukkit.createBossBar(
				Integer.toString(min) + ":00",
				BarColor.GREEN,
				BarStyle.SOLID
		);
		Bar.setProgress(1.0);

		for (Player p : Master.getServer().getOnlinePlayers())
		{
			if (p.getWorld().equals(world))
				Bar.addPlayer(p);
		}

		updater = new BarUpdater(Bar, min, 0).runTaskTimer(Master, 20, 20);
	}

	public void Cancel()
	{
		if (Bar != null)
		{
			Bar.removeAll();
			Bar.setVisible(false);
		}

		if (updater != null)
			updater.cancel();
	}
}

class BarUpdater extends BukkitRunnable
{
	private BossBar Bar;
	private int Min, Sec;
	private double StartTotal;
	private double CurrentTotal;

	public BarUpdater(BossBar bar, int m, int s)
	{
		Bar = bar;
		Min = m;
		Sec = s;

		StartTotal = Min * 60.0;
		CurrentTotal = StartTotal;
	}

	@Override
	public void run()
	{
		Sec--;
		CurrentTotal--;

		if (Sec == -1)
		{
			Sec = 59;
			Min--;
		}

		if (Min == -1)
		{
			Bar.removeAll();
			Bar.setVisible(false);
			this.cancel();
			return;
		}

		if (Sec < 10)
			Bar.setTitle(Min + ":0" + Sec);
		else
			Bar.setTitle(Min + ":" + Sec);

		if (CurrentTotal == (int) (StartTotal / 2))
			Bar.setColor(BarColor.YELLOW);

		if (CurrentTotal == 30)
			Bar.setColor(BarColor.RED);

		Bar.setProgress(Math.max(CurrentTotal / StartTotal, 0.001));
	}
}