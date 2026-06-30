package com.vexiumsmp.effects;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class EffectsDatabase implements AutoCloseable {
    static final int STARTING_LEVEL = 1;
    static final int MIN_LEVEL = 0;
    static final int MAX_LEVEL = 3;

    private final JavaPlugin plugin;
    private Connection connection;

    public EffectsDatabase(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void open() throws SQLException {
        plugin.getDataFolder().mkdirs();
        connection = DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder().toPath().resolve("vexium-effects.db"));
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS players (
                        uuid TEXT PRIMARY KEY,
                        username TEXT NOT NULL,
                        first_joined INTEGER NOT NULL,
                        last_seen INTEGER NOT NULL
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS player_effects (
                        uuid TEXT NOT NULL,
                        effect TEXT NOT NULL,
                        level INTEGER NOT NULL CHECK(level BETWEEN 0 AND 3),
                        PRIMARY KEY (uuid, effect),
                        FOREIGN KEY (uuid) REFERENCES players(uuid) ON DELETE CASCADE
                    )
                    """);
        }
    }

    public void ensurePlayer(Player player) {
        long now = System.currentTimeMillis();
        String uuid = player.getUniqueId().toString();
        try (PreparedStatement upsert = connection.prepareStatement("""
                INSERT INTO players(uuid, username, first_joined, last_seen) VALUES(?, ?, ?, ?)
                ON CONFLICT(uuid) DO UPDATE SET username = excluded.username, last_seen = excluded.last_seen
                """)) {
            upsert.setString(1, uuid);
            upsert.setString(2, player.getName());
            upsert.setLong(3, now);
            upsert.setLong(4, now);
            upsert.executeUpdate();
        } catch (SQLException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to upsert player " + player.getName(), exception);
            return;
        }

        if (getEffects(player.getUniqueId()).isEmpty()) {
            setEffectLevel(player.getUniqueId(), SmpEffect.random(), STARTING_LEVEL);
        }
    }

    public List<PlayerEffect> getEffects(UUID uuid) {
        List<PlayerEffect> effects = new ArrayList<>();
        try (PreparedStatement query = connection.prepareStatement("SELECT effect, level FROM player_effects WHERE uuid = ?")) {
            query.setString(1, uuid.toString());
            try (ResultSet results = query.executeQuery()) {
                while (results.next()) {
                    SmpEffect.fromId(results.getString("effect"))
                            .ifPresent(effect -> effects.add(new PlayerEffect(effect, results.getInt("level"))));
                }
            }
        } catch (SQLException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load effects for " + uuid, exception);
        }
        return effects;
    }

    public int getEffectLevel(UUID uuid, SmpEffect effect) {
        try (PreparedStatement query = connection.prepareStatement("SELECT level FROM player_effects WHERE uuid = ? AND effect = ?")) {
            query.setString(1, uuid.toString());
            query.setString(2, effect.id());
            try (ResultSet results = query.executeQuery()) {
                if (results.next()) {
                    return results.getInt("level");
                }
            }
        } catch (SQLException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load " + effect.id() + " for " + uuid, exception);
        }
        return -1;
    }

    public boolean hasCappedEffect(UUID uuid) {
        try (PreparedStatement query = connection.prepareStatement("SELECT 1 FROM player_effects WHERE uuid = ? AND level >= ? LIMIT 1")) {
            query.setString(1, uuid.toString());
            query.setInt(2, MAX_LEVEL);
            try (ResultSet results = query.executeQuery()) {
                return results.next();
            }
        } catch (SQLException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to check capped effects for " + uuid, exception);
            return false;
        }
    }

    public void setEffectLevel(UUID uuid, SmpEffect effect, int level) {
        int boundedLevel = Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, level));
        try (PreparedStatement upsert = connection.prepareStatement("""
                INSERT INTO player_effects(uuid, effect, level) VALUES(?, ?, ?)
                ON CONFLICT(uuid, effect) DO UPDATE SET level = excluded.level
                """)) {
            upsert.setString(1, uuid.toString());
            upsert.setString(2, effect.id());
            upsert.setInt(3, boundedLevel);
            upsert.executeUpdate();
        } catch (SQLException exception) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save " + effect.id() + " for " + uuid, exception);
        }
    }

    @Override
    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}
