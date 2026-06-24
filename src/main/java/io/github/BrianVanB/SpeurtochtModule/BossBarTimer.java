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

	private final GeoSpeurtocht Master;

	private BossBar Bar;
	private BukkitTask updater;

	private TimerMode timerMode;

	private int totalSeconds;
	private int remainingSeconds;
	private int elapsedSeconds;

	private boolean paused;

	private Set<UUID> activePlayers;
	private Runnable onFinish;

	public BossBarTimer(GeoSpeurtocht master) {
		Master = master;
		activePlayers = new HashSet<>();
		timerMode = TimerMode.COUNTDOWN;
		paused = false;
	}

	public void Create(
			StartAllOptions options,
			World activeWorld,
			Set<UUID> players,
			Runnable finishAction
	) {
		Cancel();

		timerMode = options.getTimerMode();

		totalSeconds = options.hasConfiguredMinutes()
				? options.getConfiguredMinutes() * 60
				: 0;

		remainingSeconds = totalSeconds;
		elapsedSeconds = 0;
		paused = false;

		activePlayers = new HashSet<>(players);
		onFinish = finishAction;

		Bar = Bukkit.createBossBar(
				getTitle(),
				getInitialColor(),
				BarStyle.SOLID
		);

		Bar.setProgress(getProgress());

		for (UUID uuid : activePlayers) {
			Player p = Master.getServer().getPlayer(uuid);

			if (p == null) {
				continue;
			}

			if (!p.isOnline()) {
				continue;
			}

			/*
			 * Belangrijk:
			 * Bij MULTI_WORLD-sessies zitten spelers mogelijk in andere fysieke werelden
			 * binnen dezelfde logische sessie.
			 *
			 * Daarom checken we hier NIET meer:
			 * p.getWorld().equals(activeWorld)
			 *
			 * De Manager heeft al bepaald welke spelers actief zijn.
			 */
			Bar.addPlayer(p);
		}

		updater = new BukkitRunnable() {
			@Override
			public void run() {
				Tick();
			}
		}.runTaskTimer(Master, 20, 20);
	}

	private void Tick() {
		if (paused) {
			UpdateBar();
			return;
		}

		if (timerMode == TimerMode.COUNTDOWN) {
			tickCountdown();
			return;
		}

		tickCountup();
	}

	private void tickCountdown() {
		remainingSeconds--;

		if (remainingSeconds <= 0) {
			Cancel();

			if (onFinish != null) {
				onFinish.run();
			}

			return;
		}

		UpdateBar();
	}

	private void tickCountup() {
		elapsedSeconds++;

		if (totalSeconds > 0 && elapsedSeconds >= totalSeconds) {
			Cancel();

			if (onFinish != null) {
				onFinish.run();
			}

			return;
		}

		UpdateBar();
	}

	private void UpdateBar() {
		if (Bar == null) {
			return;
		}

		Bar.setTitle(getTitle());
		Bar.setColor(getCurrentColor());
		Bar.setProgress(getProgress());
	}

	private String getTitle() {
		if (paused) {
			return "⏸ " + getVisibleTime();
		}

		if (timerMode == TimerMode.COUNTUP) {
			return "⏱ " + getVisibleTime();
		}

		return getVisibleTime();
	}

	private String getVisibleTime() {
		if (timerMode == TimerMode.COUNTUP) {
			if (totalSeconds > 0) {
				return formatTime(elapsedSeconds) + " / " + formatTime(totalSeconds);
			}

			return formatTime(elapsedSeconds);
		}

		return formatTime(remainingSeconds);
	}

	private BarColor getInitialColor() {
		if (timerMode == TimerMode.COUNTUP) {
			return BarColor.BLUE;
		}

		return BarColor.GREEN;
	}

	private BarColor getCurrentColor() {
		if (paused) {
			return BarColor.WHITE;
		}

		if (timerMode == TimerMode.COUNTUP) {
			if (totalSeconds <= 0) {
				return BarColor.BLUE;
			}

			if (elapsedSeconds >= totalSeconds) {
				return BarColor.RED;
			}

			if (elapsedSeconds >= totalSeconds / 2) {
				return BarColor.YELLOW;
			}

			return BarColor.BLUE;
		}

		if (remainingSeconds <= 30) {
			return BarColor.RED;
		}

		if (remainingSeconds <= totalSeconds / 2) {
			return BarColor.YELLOW;
		}

		return BarColor.GREEN;
	}

	private double getProgress() {
		if (timerMode == TimerMode.COUNTUP) {
			if (totalSeconds <= 0) {
				return 1.0;
			}

			double progress = (double) elapsedSeconds / (double) totalSeconds;
			return clampProgress(progress);
		}

		if (totalSeconds <= 0) {
			return 1.0;
		}

		double progress = (double) remainingSeconds / (double) totalSeconds;
		return clampProgress(progress);
	}

	private double clampProgress(double progress) {
		return Math.max(0.001, Math.min(1.0, progress));
	}

	private String formatTime(int seconds) {
		int minutes = seconds / 60;
		int sec = seconds % 60;

		if (sec < 10) {
			return minutes + ":0" + sec;
		}

		return minutes + ":" + sec;
	}

	public void AddTime(int seconds) {
		if (seconds <= 0) {
			return;
		}

		if (timerMode == TimerMode.COUNTDOWN) {
			totalSeconds += seconds;
			remainingSeconds += seconds;
			UpdateBar();
			return;
		}

		/*
		 * Bij countup betekent addtime:
		 * - alleen zinvol als er een maximale tijd is.
		 * - zonder max-tijd is er niets om te verlengen.
		 */
		if (totalSeconds > 0) {
			totalSeconds += seconds;
			UpdateBar();
		}
	}

	public void Pause() {
		paused = true;
		UpdateBar();
	}

	public void Resume() {
		paused = false;
		UpdateBar();
	}

	public boolean IsPaused() {
		return paused;
	}

	public int GetRemainingSeconds() {
		return remainingSeconds;
	}

	public int GetElapsedSeconds() {
		return elapsedSeconds;
	}

	public void AddPlayer(Player p) {
		if (Bar == null || p == null) {
			return;
		}

		activePlayers.add(p.getUniqueId());
		Bar.addPlayer(p);
	}

	public void RemovePlayer(Player p) {
		if (Bar == null || p == null) {
			return;
		}

		activePlayers.remove(p.getUniqueId());
		Bar.removePlayer(p);
	}

	public void Cancel() {
		if (updater != null) {
			updater.cancel();
			updater = null;
		}

		if (Bar != null) {
			Bar.removeAll();
			Bar.setVisible(false);
			Bar = null;
		}

		paused = false;
	}
}