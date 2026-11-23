package emu.grasscutter.server.packet.recv;

import emu.grasscutter.Grasscutter;
import emu.grasscutter.data.GameData;
import emu.grasscutter.data.excels.giving.GivingData;
import emu.grasscutter.game.inventory.GameItem;
import emu.grasscutter.game.quest.enums.QuestCond;
import emu.grasscutter.game.quest.enums.QuestContent;
import emu.grasscutter.net.packet.*;
import emu.grasscutter.net.proto.ItemGivingReqOuterClass.ItemGivingReq;
import emu.grasscutter.net.proto.ItemParamOuterClass.ItemParam;
import emu.grasscutter.server.game.GameSession;
import emu.grasscutter.server.packet.send.PacketItemGivingRsp;
import emu.grasscutter.server.packet.send.PacketItemGivingRsp.Mode;
import java.util.*;
import java.util.function.LongFunction;

/**
 * Quest item hand-ins (the "give items" window). The 7.2 request has no giving type, so the
 * handler used to be commented out and every FINISH_ITEM_GIVING step stalled. A giving is now
 * accepted only when it is a quest giving that the player's quests have activated.
 */
@Opcodes(PacketOpcodes.ItemGivingReq)
public final class HandlerItemGivingReq extends PacketHandler {
    @Override
    public void handle(GameSession session, byte[] header, byte[] payload) throws Exception {
        var req = ItemGivingReq.parseFrom(payload);

        var player = session.getPlayer();
        var inventory = player.getInventory();
        var questManager = player.getQuestManager();

        var giveId = req.getGivingId();
        var data = GameData.getGivingDataMap().get(giveId);
        var activeGivings = player.getPlayerProgress().getItemGivings();
        if (data == null
                || data.getGiveType() == GivingData.GiveType.GADGET
                || !activeGivings.containsKey(giveId)) {
            Grasscutter.getLogger()
                    .debug("Rejected item giving {} (known={}, active={})",
                            giveId, data != null, activeGivings.containsKey(giveId));
            player.sendPacket(new PacketItemGivingRsp());
            return;
        }

        var items = offeredItems(
                req.getItemParamListList(), req.getItemGuidCountMapMap(), inventory::getItemByGuid);
        if (items.isEmpty()) {
            player.sendPacket(new PacketItemGivingRsp());
            return;
        }

        switch (data.getGivingMethod()) {
            case GIVING_METHOD_EXACT -> {
                if (!inventory.hasAllItems(items)) {
                    player.sendPacket(new PacketItemGivingRsp());
                    return;
                }
                if (data.isRemoveItem()) inventory.removeItems(items);

                player.sendPacket(new PacketItemGivingRsp(giveId, Mode.EXACT_SUCCESS));
                questManager.removeGivingItemAction(giveId);
                questManager.queueEvent(QuestContent.QUEST_CONTENT_FINISH_ITEM_GIVING, giveId, 0);
                questManager.queueEvent(QuestCond.QUEST_COND_ITEM_GIVING_FINISHED, giveId, 0);
            }
            case GIVING_METHOD_VAGUE_GROUP, GIVING_METHOD_GROUP -> {
                var groupData = GameData.getGivingGroupDataMap();
                Integer matchedGroup = null;
                for (var groupId : data.getGivingGroupIds()) {
                    var group = groupData.get((int) groupId);
                    if (group == null || group.getItemIds() == null) continue;
                    boolean offeredFromGroup = items.stream()
                            .anyMatch(item -> group.getItemIds().contains(item.getItemId())
                                    && inventory.getFirstItem(item.getItemId()) != null);
                    if (offeredFromGroup) {
                        matchedGroup = group.getId();
                        break;
                    }
                }
                if (matchedGroup == null || !inventory.hasAllItems(items)) {
                    player.sendPacket(new PacketItemGivingRsp());
                    return;
                }
                if (data.isRemoveItem()) inventory.removeItems(items);

                player.sendPacket(new PacketItemGivingRsp(matchedGroup, Mode.GROUP_SUCCESS));
                questManager.markCompleted(giveId);
                questManager.queueEvent(
                        QuestContent.QUEST_CONTENT_FINISH_ITEM_GIVING, giveId, matchedGroup);
                questManager.queueEvent(QuestCond.QUEST_COND_ITEM_GIVING_FINISHED, giveId, matchedGroup);
            }
            default -> {
                Grasscutter.getLogger()
                        .debug("Unsupported giving method {} for giving {}", data.getGivingMethod(), giveId);
                player.sendPacket(new PacketItemGivingRsp());
            }
        }
    }

    /**
     * The items the client offered. 7.2 may send them as item params or as inventory guids with
     * counts; guids are resolved to item ids, and unknown guids are ignored.
     */
    static List<ItemParam> offeredItems(
            List<ItemParam> params, Map<Long, Integer> guidCounts, LongFunction<GameItem> byGuid) {
        var offered = new ArrayList<ItemParam>();
        if (params != null) {
            for (var param : params) {
                if (param.getItemId() > 0 && param.getCount() > 0) offered.add(param);
            }
        }
        if (offered.isEmpty() && guidCounts != null) {
            for (var entry : guidCounts.entrySet()) {
                var item = byGuid.apply(entry.getKey());
                if (item == null || entry.getValue() == null || entry.getValue() <= 0) continue;
                offered.add(ItemParam.newBuilder()
                        .setItemId(item.getItemId())
                        .setCount(entry.getValue())
                        .build());
            }
        }
        return offered;
    }
}
