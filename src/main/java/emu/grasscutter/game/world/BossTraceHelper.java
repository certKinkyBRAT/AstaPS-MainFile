package emu.grasscutter.game.world;

import emu.grasscutter.Grasscutter;
import emu.grasscutter.game.entity.EntityBaseGadget;
import emu.grasscutter.game.entity.EntityMonster;
import emu.grasscutter.game.entity.GameEntity;

/**
 * Diagnostic trail for the Hypostasis and Regisvine fights, logged at INFO with a {@code
 * [BossTrace]} prefix.
 *
 * <p>Both bosses build their key pieces (revival prisms, the Regisvine's corolla and core) from
 * ability events the server never logged, so a broken fight left no evidence. This records only
 * those bosses and their own gadgets: pieces created or skipped, modifiers added and removed,
 * global values, and HP changes, so one fight shows where the chain stops.
 */
public final class BossTraceHelper {
    private static final String PREFIX = "[BossTrace] ";

    private BossTraceHelper() {}

    /** Hypostasis (200401xx–200407xx) and Regisvine (260201xx Cryo, 260202xx Pyro) monsters. */
    public static boolean isTracedMonsterId(int monsterId) {
        if (EffigyCombatHelper.isEffigyMonsterId(monsterId)) return true;
        int family = monsterId / 100;
        return family == 260201 || family == 260202;
    }

    /** Hypostasis gadgets (4200xxxx below 42008000) and Regisvine gadgets (42602xxx). */
    public static boolean isTracedGadgetId(int gadgetId) {
        return (gadgetId >= 42001000 && gadgetId < 42008000) || gadgetId / 1000 == 42602;
    }

    public static boolean isTraced(GameEntity entity) {
        if (entity instanceof EntityMonster monster && monster.getMonsterData() != null) {
            return isTracedMonsterId(monster.getMonsterData().getId());
        }
        if (entity instanceof EntityBaseGadget gadget) {
            return isTracedGadgetId(gadget.getGadgetId());
        }
        return false;
    }

    public static void gadgetCreated(String how, int gadgetId, int entityId, int ownerEntityId) {
        if (!isTracedGadgetId(gadgetId)) return;
        log("gadget {} {} entity={} owner={}", gadgetId, how, entityId, ownerEntityId);
    }

    public static void modifier(GameEntity entity, boolean added, String abilityName, String modifierName) {
        if (!isTraced(entity)) return;
        log("{} {} modifier {} / {}", describe(entity), added ? "+" : "-", abilityName, modifierName);
    }

    public static void globalValue(GameEntity entity, String key, float value) {
        if (!isTraced(entity)) return;
        log("{} GV {} = {}", describe(entity), key, value);
    }

    public static void damage(GameEntity entity, float requested, float applied, float hpAfter, float maxHp, String note) {
        if (!isTraced(entity)) return;
        float pct = maxHp > 0f ? 100f * hpAfter / maxHp : -1f;
        log(
                "{} hit {} applied {} hp {}/{} ({}%) lockHP={} {}",
                describe(entity),
                requested,
                applied,
                hpAfter,
                maxHp,
                String.format("%.2f", pct),
                entity.isLockHP(),
                note);
    }

    public static void removed(GameEntity entity, String why) {
        if (!isTraced(entity)) return;
        log("{} removed ({})", describe(entity), why);
    }

    private static String describe(GameEntity entity) {
        if (entity instanceof EntityMonster monster && monster.getMonsterData() != null) {
            return "monster " + monster.getMonsterData().getId() + "#" + entity.getId();
        }
        if (entity instanceof EntityBaseGadget gadget) {
            return "gadget " + gadget.getGadgetId() + "#" + entity.getId();
        }
        return "entity#" + entity.getId();
    }

    private static void log(String format, Object... args) {
        try {
            Grasscutter.getLogger().info(PREFIX + format, args);
        } catch (Throwable ignored) {
        }
    }
}
