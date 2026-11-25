package emu.grasscutter.game.ability;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Cryo Regisvine corolla: the client can report CreateFruit_Delayed's removal before the
 * CreateGadget its onRemoved ran, so the server must still resolve that modifier for a moment.
 */
final class RecentlyRemovedModifierTest {
    @Test
    void removedModifierStillResolvesBriefly() {
        var manager = new AbilityManager(new emu.grasscutter.game.player.Player());
        var controller = new AbilityModifierController(null, null, null);
        manager.rememberRemovedModifier(268436805, 42, controller, 1_000L);

        assertSame(controller, manager.recentlyRemovedModifier(268436805, 42, 1_000L + 4_000L));
        assertNull(manager.recentlyRemovedModifier(268436805, 42, 1_000L + 6_000L), "expired");
        assertNull(manager.recentlyRemovedModifier(268436805, 43, 1_000L), "other modifier");
        assertNull(manager.recentlyRemovedModifier(268436806, 42, 1_000L), "other entity");
    }
}
