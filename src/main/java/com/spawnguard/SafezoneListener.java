package com.spawnguard;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.projectiles.ProjectileSource;

public class SafezoneListener implements Listener {

    private final SpawnGuardPlugin plugin;

    public SafezoneListener(SpawnGuardPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent event) {
        if (!plugin.blockSpawning()) return;
        if (!plugin.isHostile(event.getEntity())) return;
        if (plugin.allowedReasons().contains(event.getSpawnReason())) return;
        if (plugin.inZone(event.getLocation())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTarget(EntityTargetLivingEntityEvent event) {
        if (!plugin.blockTargeting()) return;
        if (!(event.getTarget() instanceof Player player)) return;
        if (!plugin.isHostile(event.getEntity())) return;
        if (!plugin.inZone(player.getLocation())) return;

        event.setCancelled(true);
        if (event.getEntity() instanceof Mob mob) mob.setTarget(null);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!plugin.blockDamage()) return;
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!plugin.inZone(victim.getLocation())) return;

        Entity damager = event.getDamager();
        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Entity shooterEntity) damager = shooterEntity;
        }
        if (plugin.isHostile(damager)) event.setCancelled(true);
    }
}
