
package jdz.statsTracker;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventException;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.java.JavaPlugin;

import jdz.bukkitUtils.fileIO.FileLogger;
import jdz.bukkitUtils.misc.Config;
import jdz.statsTracker.achievement.AchievementInventories;
import jdz.statsTracker.achievement.AchievementManager;
import jdz.statsTracker.achievement.AchievementShop;
import jdz.statsTracker.commandHandlers.*;
import jdz.statsTracker.hooks.PlaceholderHook;
import jdz.statsTracker.objective.ObjectivesCommand;
import jdz.statsTracker.stats.StatsManager;
import jdz.statsTracker.stats.StatsDatabase;
import lombok.Getter;

public class GCStats extends JavaPlugin {
	@Getter private static GCStats instance;
	@Getter private static FileLogger fileLogger;

	@Override
	public void onEnable() {
		instance = this;
		fileLogger = new FileLogger(GCStats.getInstance());

		GCStatsConfig.reloadConfig();

		StatsManager.getInstance().loadDefaultStats();

		PluginManager pm = Bukkit.getPluginManager();

		if (pm.isPluginEnabled("PlaceholderAPI")) {
			new PlaceholderHook().hook();
		}

		pm.registerEvents(StatsManager.getInstance(), this);
		pm.registerEvents(AchievementManager.getInstance(), this);

		pm.registerEvents(AchievementInventories.getInstance(), this);
		pm.registerEvents(new AchievementShop(), this);

		AchievementManager.getInstance().addFromConfig(GCStats.getInstance(),
				Config.getConfig(GCStats.getInstance(), "Achievements.yml"));
		AchievementShop.reload();

		new StatsCommandExecutor(this).register();
		new AchievementCommandExecutor(this).register();
		ObjectivesCommand.getInstance().register(this);

		for (RegisteredListener l : HandlerList.getRegisteredListeners(this))
			try {
				for (Player p : Bukkit.getOnlinePlayers())
					l.callEvent(new PlayerJoinEvent(p, ""));
			}
			catch (EventException e) {
				new FileLogger(this).createErrorLog(e);
			}
	}

	@Override
	public void onDisable() {
		StatsDatabase.getInstance().onShutDown();
		if (Bukkit.getOnlinePlayers().isEmpty())
			return;

		ExecutorService es = Executors.newFixedThreadPool(Bukkit.getOnlinePlayers().size());
		for (Player p : Bukkit.getOnlinePlayers())
			es.execute(() -> {
				StatsManager.getInstance().onPlayerQuit(new PlayerQuitEvent(p, ""));
			});
		es.shutdown();
		try {
			es.awaitTermination(30, TimeUnit.SECONDS);
		}
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
