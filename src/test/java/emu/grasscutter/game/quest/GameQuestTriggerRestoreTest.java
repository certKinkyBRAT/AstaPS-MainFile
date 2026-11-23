package emu.grasscutter.game.quest;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import emu.grasscutter.ServerResourceFixture;
import emu.grasscutter.data.GameData;
import emu.grasscutter.data.excels.TriggerExcelConfigData;
import emu.grasscutter.data.excels.quest.QuestData;
import emu.grasscutter.game.quest.content.ContentTriggerFire;
import emu.grasscutter.game.quest.enums.QuestState;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Kaeya's 46102 waits on trigger 1077, whose lookup is not saved with the quest. */
@ExtendWith(ServerResourceFixture.class)
final class GameQuestTriggerRestoreTest {
    private static final Gson GSON = new Gson();
    private static final int TRIGGER_ID = 1077;
    private static final String TRIGGER_NAME = "ENTER_REGION_70";

    private TriggerExcelConfigData previous;

    @BeforeEach
    void installTrigger() {
        var trigger = GSON.fromJson(
                "{\"id\":1077,\"sceneId\":3,\"groupId\":133004901,\"triggerName\":\"ENTER_REGION_70\"}",
                TriggerExcelConfigData.class);
        previous = GameData.getTriggerExcelConfigDataMap().put(TRIGGER_ID, trigger);
    }

    @AfterEach
    void removeTrigger() {
        if (previous == null) GameData.getTriggerExcelConfigDataMap().remove(TRIGGER_ID);
        else GameData.getTriggerExcelConfigDataMap().put(TRIGGER_ID, previous);
    }

    private static QuestData quest46102() {
        return GSON.fromJson(
                "{\"subId\":46102,\"mainId\":461,\"finishCond\":[{\"type\":\"QUEST_CONTENT_TRIGGER_FIRE\","
                        + "\"param\":[1077,0]}],\"failCond\":[]}",
                QuestData.class);
    }

    @SuppressWarnings("deprecation")
    private static GameQuest loadedQuest(QuestState state, Map<String, Boolean> savedTriggers) {
        var quest = new GameQuest();
        try {
            var idField = GameQuest.class.getDeclaredField("subQuestId");
            idField.setAccessible(true);
            idField.setInt(quest, 46102);
            var triggersField = GameQuest.class.getDeclaredField("triggers");
            triggersField.setAccessible(true);
            triggersField.set(quest, savedTriggers);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        quest.setState(state);
        return quest;
    }

    @Test
    void loadedUnfinishedQuestRebuildsTriggerLookup() {
        var quest = loadedQuest(QuestState.QUEST_STATE_UNFINISHED, null);
        quest.setConfig(quest46102());

        assertNotNull(quest.getTriggerData());
        assertEquals(133004901, quest.getTriggerData().get(TRIGGER_NAME).getGroupId());
        assertEquals(Boolean.FALSE, quest.getTriggers().get(TRIGGER_NAME));
    }

    @Test
    void savedFiredFlagIsKept() {
        var saved = new HashMap<String, Boolean>();
        saved.put(TRIGGER_NAME, true);
        var quest = loadedQuest(QuestState.QUEST_STATE_UNFINISHED, saved);
        quest.setConfig(quest46102());

        assertEquals(Boolean.TRUE, quest.getTriggers().get(TRIGGER_NAME));
        var condition = quest46102().getFinishCond().get(0);
        assertTrue(new ContentTriggerFire().execute(quest, condition, "", TRIGGER_ID, 0));
    }

    @Test
    void finishedQuestIsLeftAlone() {
        var quest = loadedQuest(QuestState.QUEST_STATE_FINISHED, null);
        quest.setConfig(quest46102());

        assertNull(quest.getTriggerData());
    }
}
