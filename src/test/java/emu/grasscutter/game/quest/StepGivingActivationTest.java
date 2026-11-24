package emu.grasscutter.game.quest;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

/** A step finishing on FINISH_ITEM_GIVING (e.g. Xiangling's 1100513, giving 8) opens its giving. */
final class StepGivingActivationTest {
    private static ItemGiveRecord record(boolean finished) {
        return ItemGiveRecord.builder().givingId(8).finished(finished).givenItems(Map.of()).build();
    }

    @Test
    void missingGivingIsActivated() {
        assertTrue(QuestManager.needsActivation(null));
    }

    @Test
    void completedGivingIsReopenedWhenItsStepRestarts() {
        assertTrue(QuestManager.needsActivation(record(true)));
    }

    @Test
    void openGivingIsLeftAlone() {
        assertFalse(QuestManager.needsActivation(record(false)));
    }
}
