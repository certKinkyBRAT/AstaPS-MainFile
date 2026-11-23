package emu.grasscutter.game.quest.exec;

import emu.grasscutter.Grasscutter;
import emu.grasscutter.data.excels.quest.QuestData;
import emu.grasscutter.game.quest.GameQuest;
import emu.grasscutter.game.quest.QuestValueExec;
import emu.grasscutter.game.quest.enums.QuestExec;
import emu.grasscutter.game.quest.enums.QuestState;
import emu.grasscutter.game.quest.handlers.QuestExecHandler;
import emu.grasscutter.scripts.SceneScriptManager;
import emu.grasscutter.scripts.data.SceneGroup;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;

/** Apply native group-suite actions after scene scripts initialize, without timed polling. */
@QuestValueExec(QuestExec.QUEST_EXEC_REFRESH_GROUP_SUITE)
public class ExecRefreshGroupSuite extends QuestExecHandler {
    @Override
    public boolean execute(GameQuest quest, QuestData.QuestExecParam condition, String... params) {
        if (params == null || params.length < 2) {
            Grasscutter.getLogger().warn(
                    "Quest {} refresh-group-suite exec has invalid params {}",
                    quest.getSubQuestId(), Arrays.toString(params));
            return false;
        }
        final int sceneId;
        try {
            sceneId = Integer.parseInt(params[0]);
        } catch (NumberFormatException e) {
            return false;
        }
        if (sceneId <= 0 || quest.getOwner().getWorld() == null) return false;
        var world = quest.getOwner().getWorld();
        var scene = world.getSceneById(sceneId);
        if (scene == null) return false;
        var scripts = scene.getScriptManager();
        var dispatchedState = quest.getState();
        // Save the quest's intent before applying it. Applying can be deferred or dropped while
        // the scene is still loading at login; the saved suite is replayed when the group loads.
        rememberRequestedSuites(quest, sceneId, params[1]);

        if (!scripts.isInit()) {
            scripts.whenInitialized(ready -> {
                if (!ready) {
                    Grasscutter.getLogger().warn(
                            "Quest {} could not refresh group suite: scene {} scripts unavailable",
                            quest.getSubQuestId(), sceneId);
                    return;
                }
                // Execute mutations on the scene scheduler, never its loader thread.
                scene.getScheduler().scheduleDelayedTask(
                        () -> {
                            if (quest.getState() == dispatchedState
                                    && quest.getOwner().getWorld() == world
                                    && world.getScenes().get(sceneId) == scene
                                    && !scripts.isDestroyed()) {
                                apply(quest, params, sceneId, scene);
                            }
                        }, 1);
            });
            return true;
        }
        return apply(quest, params, sceneId, scene);
    }

    /** Parse "group,suite;group,suite" into pairs, skipping malformed or invalid entries. */
    static List<int[]> parseGroupSuitePairs(String spec) {
        var pairs = new ArrayList<int[]>();
        if (spec == null) return pairs;
        for (String entry : spec.split(";")) {
            String[] pair = entry.split(",");
            if (pair.length != 2) continue;
            try {
                int groupId = Integer.parseInt(pair[0].trim());
                int suiteId = Integer.parseInt(pair[1].trim());
                if (groupId > 0 && suiteId >= 0) pairs.add(new int[] {groupId, suiteId});
            } catch (NumberFormatException ignored) {
                // Reported by apply().
            }
        }
        return pairs;
    }

    private static void rememberRequestedSuites(GameQuest quest, int sceneId, String spec) {
        var main = quest.getMainQuest();
        if (main == null || main.getQuestGroupSuites() == null) return;
        boolean changed = false;
        for (int[] pair : parseGroupSuitePairs(spec)) {
            changed |= SceneScriptManager.rememberQuestGroupSuite(
                    main.getQuestGroupSuites(), sceneId, pair[0], pair[1]);
        }
        if (changed) quest.save();
    }

    /** Keep a group visible only while a successful quest suite override owns it. */
    static boolean pinAfterRefresh(boolean oldPin, boolean success, int suite,
            boolean anotherOverride) {
        return !success ? oldPin : suite > 0 || anotherOverride;
    }

    static boolean refreshWithVisibilityPin(
            SceneGroup group, int suite, BooleanSupplier refresh,
            BooleanSupplier anotherOverrideCheck) {
        boolean wasPinned = group.dontUnload;
        group.dontUnload = true;
        boolean success = false;
        try {
            success = refresh.getAsBoolean();
            return success;
        } finally {
            // Restore even when an attempted refresh or ownership check throws.
            group.dontUnload = wasPinned;
            if (success) {
                boolean another = suite == 0 && anotherOverrideCheck.getAsBoolean();
                group.dontUnload = pinAfterRefresh(wasPinned, true, suite, another);
            }
        }
    }

    private boolean apply(
            GameQuest quest, String[] params, int sceneId,
            emu.grasscutter.game.world.Scene scene) {
        var scripts = scene.getScriptManager();
        if (!scripts.isInit() || scripts.isDestroyed()) return false;
        boolean complete = true;
        for (String entry : params[1].split(";")) {
            String[] pair = entry.split(",");
            if (pair.length != 2) {
                Grasscutter.getLogger().warn(
                        "Quest {} invalid group-suite pair {}", quest.getSubQuestId(), entry);
                complete = false;
                continue;
            }
            final int groupId, suiteId;
            try {
                groupId = Integer.parseInt(pair[0].trim());
                suiteId = Integer.parseInt(pair[1].trim());
            } catch (NumberFormatException e) {
                Grasscutter.getLogger().warn(
                        "Quest {} malformed group-suite pair {}", quest.getSubQuestId(), entry);
                complete = false;
                continue;
            }
            if (groupId <= 0 || suiteId < 0) {
                complete = false;
                continue;
            }
            var group = scripts.getGroupById(groupId);
            if (group == null) {
                Grasscutter.getLogger().warn(
                        "Quest {} group {} is missing in scene {}",
                        quest.getSubQuestId(), groupId, sceneId);
                complete = false;
                continue;
            }
            boolean applied = refreshWithVisibilityPin(
                    group, suiteId,
                    () -> scripts.refreshGroupSuite(groupId, suiteId, quest),
                    () -> quest.getOwner().getQuestManager().getSceneGroupSuite(sceneId).stream()
                            .anyMatch(saved -> saved.getGroup() == groupId));
            if (!applied) {
                Grasscutter.getLogger().warn(
                        "Quest {} failed to refresh group {} suite {} in scene {}",
                        quest.getSubQuestId(), groupId, suiteId, sceneId);
                complete = false;
            }
        }
        return complete;
    }
}
