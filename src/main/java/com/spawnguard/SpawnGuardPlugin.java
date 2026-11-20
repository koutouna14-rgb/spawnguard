package com.spawnguard;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Boss;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SpawnGuardPlugin extends JavaPlugin {

    private Zone zone;
    private Set<String> worlds = new HashSet<>();
    private Set<SpawnReason> allowedReasons = EnumSet.noneOf(SpawnReason.class);
    private boolean blockSpawning;
    private boolean blockTargeting;
    private boolean blockDamage;
    private boolean removeMobs;
    private boolean ignoreNamed;
    private BukkitTask task;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(new SafezoneListener(this), this);
        load();
    }

    @Override
    public void onDisable() {
        if (task != null) task.cancel();
    }

    private void load() {
        reloadConfig();
        if (task != null) {
            task.cancel();
            task = null;
        }

        worlds = new HashSet<>(getConfig().getStringList("worlds"));
        blockSpawning = getConfig().getBoolean("block-spawning", true);
        blockTargeting = getConfig().getBoolean("block-targeting", true);
        blockDamage = getConfig().getBoolean("block-damage", true);
        removeMobs = getConfig().getBoolean("remove-mobs", true);
        ignoreNamed = getConfig().getBoolean("ignore-named-mobs", true);

        allowedReasons = EnumSet.noneOf(SpawnReason.class);
        for (String name : getConfig().getStringList("allowed-spawn-reasons")) {
            try {
                allowedReasons.add(SpawnReason.valueOf(name.toUpperCase()));
            } catch (IllegalArgumentException ex) {
                getLogger().warning("Unknown spawn reason in config: " + name);
            }
        }

        String region = getConfig().getString("region", "");
        if (!region.isBlank() && Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) {
            zone = new WorldGuardZone(region);
            getLogger().info("Using WorldGuard region '" + region + "'.");
            Bukkit.getScheduler().runTask(this, () -> warnIfRegionMissing(region));
        } else {
            zone = new RadiusZone(
                    getConfig().getBoolean("radius.use-world-spawn", true),
                    getConfig().getDouble("radius.x", 0),
                    getConfig().getDouble("radius.z", 0),
                    getConfig().getDouble("radius.size", 64));
            getLogger().info("Using radius zone (WorldGuard region not configured or WorldGuard missing).");
        }

        if (removeMobs) {
            long interval = Math.max(5L, getConfig().getLong("check-interval-ticks", 20L));
            task = Bukkit.getScheduler().runTaskTimer(this, this::sweep, interval, interval);
        }
    }

    private void warnIfRegionMissing(String region) {
        try {
            var container = com.sk89q.worldguard.WorldGuard.getInstance().getPlatform().getRegionContainer();
            for (String worldName : worlds) {
                World w = Bukkit.getWorld(worldName);
                if (w == null) continue;
                var manager = container.get(com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(w));
                if (manager == null || manager.getRegion(region) == null) {
                    getLogger().warning("WorldGuard region '" + region + "' was not found in world '" + worldName + "'.");
                }
            }
        } catch (Throwable t) {
            getLogger().warning("Could not verify WorldGuard region: " + t.getMessage());
        }
    }

    /** Removes hostile mobs that are inside the zone. */
    private void sweep() {
        for (String worldName : worlds) {
            World w = Bukkit.getWorld(worldName);
            if (w == null) continue;
            for (LivingEntity e : w.getLivingEntities()) {
                if (!isHostile(e)) continue;
                if (ignoreNamed && e.getCustomName() != null) continue;
                if (zone.contains(e.getLocation())) e.remove();
            }
        }
    }

    boolean isHostile(Entity e) {
        return e instanceof Enemy && !(e instanceof Boss);
    }

    boolean inZone(Location loc) {
        return loc.getWorld() != null && worlds.contains(loc.getWorld().getName()) && zone.contains(loc);
    }

    boolean blockSpawning() { return blockSpawning; }
    boolean blockTargeting() { return blockTargeting; }
    boolean blockDamage() { return blockDamage; }
    Set<SpawnReason> allowedReasons() { return allowedReasons; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            load();
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    getConfig().getString("messages.reloaded", "&aSpawnGuard reloaded.")));
            return true;
        }
        sender.sendMessage(ChatColor.YELLOW + "Usage: /" + label + " reload");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length == 1 ? List.of("reload") : List.of();
    }
}
