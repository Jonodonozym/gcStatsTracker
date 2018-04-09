
package jdz.statsTracker.objective;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.event.EventHandler;

import jdz.bukkitUtils.events.Listener;
import jdz.statsTracker.GCStats;
import jdz.statsTracker.event.StatChangeEvent;
import jdz.statsTracker.stats.NoSaveStatType;

class StatObjectiveListener implements Listener {
	private static final Map<NoSaveStatType, Set<StatObjective>> typeToObjectives = new HashMap<NoSaveStatType, Set<StatObjective>>();

	public static void add(StatObjective objective) {
		if (!typeToObjectives.containsKey(objective.getStatType()))
			typeToObjectives.put(objective.getStatType(), new HashSet<StatObjective>());
		typeToObjectives.get(objective.getStatType()).add(objective);
	}

	public static void remove(StatObjective objective) {
		typeToObjectives.get(objective.getStatType()).remove(objective);
	}

	private StatObjectiveListener() {}

	static {
		new StatObjectiveListener().registerEvents(GCStats.getInstance());
	}

	@EventHandler
	public void onStatChange(StatChangeEvent event) {
		if (typeToObjectives.containsKey(event.getType()))
			for (StatObjective objective : typeToObjectives.get(event.getType()))
				if (objective.getPlayers().contains(event.getPlayer().getUniqueId()))
					if (!objective.isUnlocked(event.getPlayer())
							&& objective.getRequired() <= event.getType().get(event.getPlayer()))
						objective.setUnlocked(event.getPlayer());
	}
}