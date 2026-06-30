# Vexium Effects SMP Plugin

Paper plugin for the VexiumSMP effect progression premise.

## Gameplay rules

- First-time players are added to a SQLite database and randomly receive one positive effect at stored level `1`.
- Stored levels use Minecraft command-style amplifiers, so level `0` is still functional.
- Effects downgrade by one level when the holder is killed by another player.
- Non-player deaths such as fall damage, fire, lava, or mobs do not downgrade effects.
- Levels never go below `0` and never go above `3`.
- Killing a player levels up the killer's matching effect if they already have it and it is below level `3`.
- If the killer does not have the victim's effect, they can gain it at level `1` only when they already have at least one capped level `3` effect.
- If the killer's matching effect is already capped at level `3`, nothing is gained for that effect.

## Effects

- Regeneration
- Jump Boost
- Slow Falling
- Strength
- Speed
- Absorption
- Fire Resistance

## Building

```bash
mvn package
```

The shaded plugin jar is produced in `target/`.
