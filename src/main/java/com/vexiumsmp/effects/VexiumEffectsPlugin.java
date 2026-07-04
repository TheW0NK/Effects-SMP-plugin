package com.vexiumsmp.effects;

import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;

public final class VexiumEffectsPlugin extends JavaPlugin implements Listener {
    private static final int PERMANENT_DURATION_TICKS = Integer.MAX_VALUE;

    private EffectsDatabase database;

    @Override
    public void onEnable() {
        database = new EffectsDatabase(this);
        try {
            database.open();
        } catch (SQLException exception) {
            getLogger().log(Level.SEVERE, "Could not open VexiumSMP effects database.", exception);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(this, this);
        for (Player player : Bukkit.getOnlinePlayers()) {
            database.ensurePlayer(player);
            applyStoredEffects(player);
        }
    }

    @Override
    public void onDisable() {
        if (database == null) {
            return;
        }
        try {
            database.close();
        } catch (SQLException exception) {
            getLogger().log(Level.WARNING, "Failed to close VexiumSMP effects database.", exception);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        database.ensurePlayer(player);
        applyStoredEffects(player);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        database.ensurePlayer(player);
        getServer().getScheduler().runTask(this, () -> applyStoredEffects(player));
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || killer.getUniqueId().equals(victim.getUniqueId())) {
            return;
        }

        database.ensurePlayer(killer);
        database.ensurePlayer(victim);

        List<PlayerEffect> victimEffectsBeforeDowngrade = database.getEffects(victim.getUniqueId());
        boolean killerHasCappedEffect = database.hasCappedEffect(killer.getUniqueId());

        for (PlayerEffect victimEffect : victimEffectsBeforeDowngrade) {
            progressKillerEffect(killer, victimEffect, killerHasCappedEffect);
            downgradeVictimEffect(victim, victimEffect);
        }

        applyStoredEffects(killer);
        Bukkit.getScheduler().runTaskLater(this, () -> applyStoredEffects(victim), 1L);
    }

    private void progressKillerEffect(Player killer, PlayerEffect victimEffect, boolean killerHasCappedEffect) {
        int currentLevel = database.getEffectLevel(killer.getUniqueId(), victimEffect.effect());
        if (currentLevel >= EffectsDatabase.MAX_LEVEL) {
            return;
        }

        if (currentLevel >= EffectsDatabase.MIN_LEVEL) {
            database.setEffectLevel(killer.getUniqueId(), victimEffect.effect(), currentLevel + 1);
            return;
        }

        if (killerHasCappedEffect) {
            database.setEffectLevel(killer.getUniqueId(), victimEffect.effect(), EffectsDatabase.STARTING_LEVEL);
        }
    }

    private void downgradeVictimEffect(Player victim, PlayerEffect victimEffect) {
        if (victimEffect.level() <= EffectsDatabase.MIN_LEVEL) {
            return;
        }
        database.setEffectLevel(victim.getUniqueId(), victimEffect.effect(), victimEffect.level() - 1);
    }

    private void applyStoredEffects(Player player) {
        for (PlayerEffect playerEffect : database.getEffects(player.getUniqueId())) {
            player.addPotionEffect(new PotionEffect(
                    playerEffect.effect().potionEffectType(),
                    PERMANENT_DURATION_TICKS,
                    playerEffect.level(),
                    true,
                    false,
                    true));
        }
    }
}
