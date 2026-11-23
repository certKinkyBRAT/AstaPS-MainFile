package emu.grasscutter.server.packet.recv;

import static org.junit.jupiter.api.Assertions.*;

import emu.grasscutter.game.inventory.GameItem;
import emu.grasscutter.net.proto.ItemParamOuterClass.ItemParam;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Xiangling's 1100513 hand-in (giving 8) arrives as item params or inventory guids in 7.2. */
final class HandlerItemGivingReqTest {
    private static ItemParam param(int id, int count) {
        return ItemParam.newBuilder().setItemId(id).setCount(count).build();
    }

    @SuppressWarnings("deprecation")
    private static GameItem item(int itemId) {
        var item = new GameItem();
        try {
            var field = GameItem.class.getDeclaredField("itemId");
            field.setAccessible(true);
            field.setInt(item, itemId);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        return item;
    }

    @Test
    void itemParamsAreUsedAsSent() {
        var offered = HandlerItemGivingReq.offeredItems(
                List.of(param(100014, 1), param(0, 1), param(100026, 0)), Map.of(), guid -> null);
        assertEquals(1, offered.size());
        assertEquals(100014, offered.get(0).getItemId());
    }

    @Test
    void guidCountsResolveToItemIdsWhenNoParamsAreSent() {
        var offered = HandlerItemGivingReq.offeredItems(
                List.of(), Map.of(42L, 2, 43L, 1),
                guid -> guid == 42L ? item(100014) : null);
        assertEquals(1, offered.size(), "unknown guids are ignored");
        assertEquals(100014, offered.get(0).getItemId());
        assertEquals(2, offered.get(0).getCount());
    }

    @Test
    void nothingOfferedMeansNothingToGive() {
        assertTrue(HandlerItemGivingReq.offeredItems(null, null, guid -> null).isEmpty());
        assertTrue(HandlerItemGivingReq.offeredItems(List.of(), Map.of(1L, 0), guid -> item(1)).isEmpty());
    }
}
