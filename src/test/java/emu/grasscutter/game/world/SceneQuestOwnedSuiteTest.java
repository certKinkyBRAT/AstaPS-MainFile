package emu.grasscutter.game.world;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.game.quest.QuestGroupSuite;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 46209 owns suite 1 of the Cryo tablet puzzle 133002173, whose init suite 2 is empty. */
final class SceneQuestOwnedSuiteTest {
    private static QuestGroupSuite owned(int group, int suite) {
        return QuestGroupSuite.of().scene(3).group(group).suite(suite).build();
    }

    @Test
    void groupLoadingOnInitSuiteGetsTheQuestOwnedSuite() {
        var saved = List.of(owned(133004902, 1), owned(133002173, 1));
        assertEquals(1, Scene.questOwnedSuite(saved, 133002173, 2));
        assertEquals(1, Scene.questOwnedSuite(saved, 133002173, 0), "fresh instance has no suite yet");
    }

    @Test
    void groupAlreadyOnTheOwnedSuiteIsRestoredWithoutARefresh() {
        // Re-refreshing would respawn killed monsters every time the area reloads.
        assertEquals(0, Scene.questOwnedSuite(List.of(owned(133002173, 1)), 133002173, 1));
    }

    @Test
    void groupsNoQuestOwnsKeepTheirOwnSuite() {
        assertEquals(0, Scene.questOwnedSuite(List.of(owned(133004902, 2)), 133002173, 2));
        assertEquals(0, Scene.questOwnedSuite(List.of(), 133002173, 2));
        assertEquals(0, Scene.questOwnedSuite(null, 133002173, 2));
    }
}
