package emu.grasscutter.scripts;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.ServerResourceFixture;
import emu.grasscutter.scripts.constants.EventType;
import emu.grasscutter.scripts.data.SceneGroup;
import emu.grasscutter.scripts.data.SceneTrigger;
import emu.grasscutter.scripts.data.ScriptArgs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** Scene 3 has region 70 in groups 133002076, 133003086, 133004901 and 133103061. */
@ExtendWith(ServerResourceFixture.class)
final class RegionTriggerGroupScopeTest {
    private static SceneTrigger trigger(int groupId) {
        var trigger = new SceneTrigger();
        trigger.setName("ENTER_REGION_70");
        trigger.setEvent(EventType.EVENT_ENTER_REGION);
        trigger.currentGroup = SceneGroup.of(groupId);
        return trigger;
    }

    @Test
    void regionEventOnlyReachesTheOwningGroup() {
        var kaeyaRegion = new ScriptArgs(133004901, EventType.EVENT_ENTER_REGION, 70);

        assertTrue(SceneScriptManager.isRegionTriggerInEventGroup(trigger(133004901), kaeyaRegion));
        assertFalse(SceneScriptManager.isRegionTriggerInEventGroup(trigger(133002076), kaeyaRegion));
        assertFalse(SceneScriptManager.isRegionTriggerInEventGroup(trigger(133003086), kaeyaRegion));
    }

    @Test
    void eventWithoutGroupKeepsOldMatching() {
        var ungrouped = new ScriptArgs(-1, EventType.EVENT_ENTER_REGION, 70);

        assertTrue(SceneScriptManager.isRegionTriggerInEventGroup(trigger(133002076), ungrouped));
    }
}
