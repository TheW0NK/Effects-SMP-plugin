package com.vexiumsmp.effects;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.potion.PotionEffectType;

/** Positive effects available in the VexiumSMP progression pool. */
public enum SmpEffect {
    REGENERATION("regeneration", PotionEffectType.REGENERATION),
    JUMP_BOOST("jump_boost", PotionEffectType.JUMP_BOOST),
    SLOW_FALLING("slow_falling", PotionEffectType.SLOW_FALLING),
    STRENGTH("strength", PotionEffectType.STRENGTH),
    SPEED("speed", PotionEffectType.SPEED),
    ABSORPTION("absorption", PotionEffectType.ABSORPTION),
    FIRE_RESISTANCE("fire_resistance", PotionEffectType.FIRE_RESISTANCE);

    private static final List<SmpEffect> VALUES = List.of(values());
    private static final Map<String, SmpEffect> BY_ID = Map.ofEntries(
            Map.entry(REGENERATION.id, REGENERATION),
            Map.entry(JUMP_BOOST.id, JUMP_BOOST),
            Map.entry(SLOW_FALLING.id, SLOW_FALLING),
            Map.entry(STRENGTH.id, STRENGTH),
            Map.entry(SPEED.id, SPEED),
            Map.entry(ABSORPTION.id, ABSORPTION),
            Map.entry(FIRE_RESISTANCE.id, FIRE_RESISTANCE));

    private final String id;
    private final PotionEffectType potionEffectType;

    SmpEffect(String id, PotionEffectType potionEffectType) {
        this.id = id;
        this.potionEffectType = potionEffectType;
    }

    public String id() {
        return id;
    }

    public PotionEffectType potionEffectType() {
        return potionEffectType;
    }

    public static SmpEffect random() {
        return VALUES.get(ThreadLocalRandom.current().nextInt(VALUES.size()));
    }

    public static Optional<SmpEffect> fromId(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_ID.get(id.toLowerCase(Locale.ROOT)));
    }
}
