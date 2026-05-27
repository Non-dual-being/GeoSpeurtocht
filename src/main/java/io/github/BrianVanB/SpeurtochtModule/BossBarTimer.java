package io.github.BrianVanB.SpeurtochtModule;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

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

	private int totalSeconds;
	private int remainingSeconds;

	private Set<UUID> activePlayers;
	private Runnable onFinish;

	public BossBarTimer(GeoSpeurtocht master)
	{
		Master = master;
		activePlayers = new HashSet<>();
	}

	public void Create(
			int minutes,
			World activeWorld,
			Set<UUID> players,
			Runnable finishAction
	) {
		Cancel();

		totalSeconds = minutes * 60;
		remainingSeconds = totalSeconds;

		activePlayers = new HashSet<>(players);
		onFinish = finishAction;

		Bar = Bukkit.createBossBar(
				formatTime(remainingSeconds),
				BarColor.GREEN,
				BarStyle.SOLID
		);
		Bar.setProgress(1.0);

		for (UUID uuid : activePlayers)
		{
			Player p = Master.getServer().getPlayer(uuid);

			if (p == null)
				continue;

			if (!p.isOnline())
				continue;

			if (!p.getWorld().equals(activeWorld))
				continue;

			Bar.addPlayer(p);
		}

		updater = new BukkitRunnable()
		{
			@Override
			public void run()
			{
				Tick();
			}
		}.runTaskTimer(Master, 20, 20);
	}

	private void Tick()
	{
		remainingSeconds--;

		if (remainingSeconds <= 0)
		{
			Cancel();

			if (onFinish != null)
				onFinish.run();

			return;
		}

		UpdateBar();
	}

	private void UpdateBar()
	{
		if (Bar == null)
			return;

		Bar.setTitle(formatTime(remainingSeconds));

		if (remainingSeconds <= 30)
			Bar.setColor(BarColor.RED);
		else if (remainingSeconds <= totalSeconds / 2)
			Bar.setColor(BarColor.YELLOW);
		else
			Bar.setColor(BarColor.GREEN);

		double progress = (double) remainingSeconds / (double) totalSeconds;
		progress = Math.max(0.001, Math.min(1.0, progress));

		Bar.setProgress(progress);
	}

	private String formatTime(int seconds)
	{
		int minutes = seconds / 60;
		int sec = seconds % 60;

		if (sec < 10)
			return minutes + ":0" + sec;

		return minutes + ":" + sec;
	}

	public void AddTime(int seconds)
	{
		if (seconds <= 0)
			return;

		totalSeconds += seconds;
		remainingSeconds += seconds;

		UpdateBar();
	}

	public int GetRemainingSeconds()
	{
		return remainingSeconds;
	}

	public void AddPlayer(Player p)
	{
		if (Bar == null || p == null)
			return;

		activePlayers.add(p.getUniqueId());
		Bar.addPlayer(p);
	}

	public void RemovePlayer(Player p)
	{
		if (Bar == null || p == null)
			return;

		activePlayers.remove(p.getUniqueId());
		Bar.removePlayer(p);
	}

	public void Cancel()
	{
		if (updater != null)
		{
			updater.cancel();
			updater = null;
		}

		if (Bar != null)
		{
			Bar.removeAll();
			Bar.setVisible(false);
			Bar = null;
		}
	}
}