package com.spawnguard;

import org.bukkit.Location;
import org.bukkit.World;

public class RadiusZone implements Zone {

    private final boolean useWorldSpawn;
    private final double x;
    private final double z;
    private final double sizeSquared;

    public RadiusZone(boolean useWorldSpawn, double x, double z, double size) {
        this.useWorldSpawn = useWorldSpawn;
        this.x = x;
        this.z = z;
        this.sizeSquared = size * size;
    }

    @Override
    public boolean contains(Location loc) {
        World world = loc.getWorld();
        if (world == null) return false;
        double cx = x;
        double cz = z;
        if (useWorldSpawn) {
            Location spawn = world.getSpawnLocation();
            cx = spawn.getX();
            cz = spawn.getZ();
        }
        double dx = loc.getX() - cx;
        double dz = loc.getZ() - cz;
        return dx * dx + dz * dz <= sizeSquared;
    }
}
