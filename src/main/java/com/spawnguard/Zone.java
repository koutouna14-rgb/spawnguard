package com.spawnguard;

import org.bukkit.Location;

public interface Zone {
    boolean contains(Location loc);
}
