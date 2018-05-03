
package jdz.statsTracker.stats.abstractTypes;

import org.bukkit.OfflinePlayer;

import jdz.statsTracker.stats.StatType;
import jdz.statsTracker.stats.database.StatsDatabase;

public abstract class AbstractStatType implements StatType {

	@Override
	public String getNameUnderscores() {
		return getName().replaceAll(" ", "_");
	}

	@Override
	public String getNameNoSpaces() {
		return getName().replaceAll(" ", "");
	}

	@Override
	public boolean isVisible() {
		return true;
	}

	@Override
	public double get(OfflinePlayer player) {
		if (player.isOnline())
			return get(player.getPlayer());
		else
			return StatsDatabase.getInstance().getStat(player, this);
	}

	@Override
	public double getDefault() {
		return 0;
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof StatType))
			return false;
		return ((StatType) other).getName().equalsIgnoreCase(getName());
	}

	@Override
	public int hashCode() {
		return getName().toLowerCase().hashCode();
	}
}