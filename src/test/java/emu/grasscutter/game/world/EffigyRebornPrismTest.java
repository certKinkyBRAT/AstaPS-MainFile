package emu.grasscutter.game.world;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Electro Hypostasis revival: at 5% HP the client asks the server to create the prisms through a
 * CreateGadget ability action. Treating them as client-placed meant the server dropped the
 * request, so the boss flew to the arena centre and never revived or died.
 */
final class EffigyRebornPrismTest {
    private static final int[] PRISMS = {42004010, 42004016, 42004017, 42004018};

    @Test
    void prismsAreCreatedByTheServer() {
        for (int id : PRISMS) {
            assertTrue(EffigyCombatHelper.isEffigyRebornPrismGadget(id), "prism " + id);
            assertFalse(EffigyCombatHelper.isEffigyClientOwnedGadget(id), "prism " + id + " must not be skipped");
        }
    }

    @Test
    void clientPlacedHelpersAreStillSkipped() {
        for (int id : new int[] {42004003, 42004006, 42004008, 42004012, 42004019, 42004020}) {
            assertTrue(EffigyCombatHelper.isEffigyClientOwnedGadget(id), "helper " + id);
            assertFalse(EffigyCombatHelper.isEffigyRebornPrismGadget(id), "helper " + id);
        }
    }

    @Test
    void bossTraceCoversHypostasisAndRegisvineOnly() {
        assertTrue(BossTraceHelper.isTracedMonsterId(20040101)); // Electro Hypostasis
        assertTrue(BossTraceHelper.isTracedMonsterId(26020101)); // Cryo Regisvine
        assertTrue(BossTraceHelper.isTracedMonsterId(26020201)); // Pyro Regisvine
        assertFalse(BossTraceHelper.isTracedMonsterId(20010101)); // a slime
        assertTrue(BossTraceHelper.isTracedGadgetId(42004010)); // prism
        assertTrue(BossTraceHelper.isTracedGadgetId(42602002)); // Regisvine corolla
        assertFalse(BossTraceHelper.isTracedGadgetId(70210106)); // boss reward flower
    }
}
