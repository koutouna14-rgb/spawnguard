package com.spawnguard;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;

/** Only loaded when WorldGuard is present, so the plugin still runs without it. */
public class WorldGuardZone implements Zone {

    private final String regionId;

    public WorldGuardZone(String regionId) {
        this.regionId = regionId;
    }

    @Override
    public boolean contains(Location loc) {
        if (loc.getWorld() == null) return false;
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();
        return query.getApplicableRegions(BukkitAdapter.adapt(loc))
                .getRegions()
                .stream()
                .anyMatch(r -> r.getId().equalsIgnoreCase(regionId));
    }
}
