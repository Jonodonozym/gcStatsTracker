
package jdz.statsTracker.achievement;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import jdz.statsTracker.stats.StatsDatabase;
import jdz.bukkitUtils.fileIO.FileExporter;
import jdz.bukkitUtils.misc.RomanNumber;
import jdz.statsTracker.GCStatsTracker;
import jdz.statsTracker.GCStatsTrackerConfig;
import jdz.statsTracker.stats.StatType;

public class AchievementData {
	public static Map<String, Map<String, List<Achievement>>> achievementsByType = new HashMap<String, Map<String, List<Achievement>>>();
	public static Map<String, List<Achievement>> achievements = new HashMap<String, List<Achievement>>();
	public static Map<Achievement, Integer> numTiers = new HashMap<Achievement, Integer>();
	
	public static boolean awardPoints = true;
	public static boolean isGlobal = false;

	public static void reloadData() {
		String location = GCStatsTracker.instance.getDataFolder().getPath() + File.separator + "Achievements.yml";
		File file = new File(location);
		if (!file.exists())
			new FileExporter(GCStatsTracker.instance).ExportResource("Achievements.yml", location);

		HashMap<StatType, List<Achievement>> localAchievements = new HashMap<StatType, List<Achievement>>();
		FileConfiguration achConfig = YamlConfiguration.loadConfiguration(file);
		
		awardPoints = achConfig.getBoolean("points.enabled", true);
		isGlobal = achConfig.getBoolean("points.isGlobal", false);
		
		for (StatType type : GCStatsTrackerConfig.enabledStats)
			localAchievements.put(type, new ArrayList<Achievement>());
		for (String achievement : achConfig.getConfigurationSection("achievements").getKeys(false)) {
			try {
				StatType type = StatType.valueOf(achConfig.getString("achievements." + achievement + ".type"));
				if (!GCStatsTrackerConfig.enabledStats.contains(type)){
					GCStatsTracker.instance.getLogger().info("achievement " + achievement + " stat type is disabled in config.yml, skipping...");
					continue;
				}
				String description = achConfig.getString("achievements." + achievement + ".description");
				Material m = Material.GRASS;
				try {
					m = Material.valueOf(achConfig.getString("achievements." + achievement + ".icon"));
				} catch (Exception e) {
				}
				short iconDamage = (short)achConfig.getInt("achievements." + achievement + ".iconDamage");

				List<Double> required = achConfig.getDoubleList("achievements." + achievement + ".required");
				List<Integer> points = achConfig.getIntegerList("achievements." + achievement + ".points");
				if (required == null) {
					required = new ArrayList<Double>();
					points = new ArrayList<Integer>();
					required.add(achConfig.getDouble("achievements." + achievement + ".required"));
					points.add(achConfig.getInt("achievements." + achievement + ".points"));
				}

				for (int i = 0; i < required.size(); i++) {
					String name = achievement + (required.size() == 1 ? "" : " " + RomanNumber.of(i+1));
					Achievement ach = new Achievement(name, type.toString(), required.get(i), points.get(i), m, iconDamage,
							description.replaceAll("\\{required\\}", type.valueToString(required.get(i)) + ""), GCStatsTrackerConfig.serverName);
					localAchievements.get(type).add(ach);
					numTiers.put(ach, required.size());
				}

			} catch (Exception e) {
				GCStatsTracker.instance.getLogger().info("achievement " + achievement + " has invalid configuration, skipping...");
			}
		}

		// updating the sql db to match
		AchievementDatabase.getInstance().ensureCorrectAchMetaTable(localAchievements);
		AchievementDatabase.getInstance().ensureCorrectAchTable(localAchievements);
		
		achievements.clear();

		// fetching the entire achievements db from the sql db
		for (String s : GCStatsTrackerConfig.servers) {
			achievementsByType.put(s, new HashMap<String, List<Achievement>>());
			achievements.put(s, new ArrayList<Achievement>());
			for (String stat : StatsDatabase.getInstance().getEnabledStats(s))
				achievementsByType.get(s).put(stat, new ArrayList<Achievement>());
		}

		for (Achievement a : AchievementDatabase.getInstance().getAllAchievements()) {
			achievementsByType.get(a.server).get(a.statType).add(a);
			achievements.get(a.server).add(a);
		}
		
		for (String s: GCStatsTrackerConfig.servers) {
			Collections.sort(achievements.get(s),  (a,b)->{
				return a.name.compareTo(b.name);
			});
		}
	}

	public static void checkAchievements(Player p, StatType s) {
		new BukkitRunnable() {
			@Override
			public void run() {
				try{
					double value = StatsDatabase.getInstance().getStat(p, s);
					for (Achievement a : achievementsByType.get(GCStatsTrackerConfig.serverName).get(s.toString()))
						if (a.isAchieved(value))
							AchievementDatabase.getInstance().setAchieved(p, a);
				}
				catch(NullPointerException e){} // so it shuts up on auto-reconnect
			}
		}.runTaskAsynchronously(GCStatsTracker.instance);
	}
}
