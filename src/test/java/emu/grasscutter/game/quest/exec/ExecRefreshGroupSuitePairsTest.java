package emu.grasscutter.game.quest.exec;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

final class ExecRefreshGroupSuitePairsTest {
    @Test
    void parsesSingleAndMultiplePairs() {
        var one = ExecRefreshGroupSuite.parseGroupSuitePairs("133002173,1");
        assertEquals(1, one.size());
        assertArrayEquals(new int[] {133002173, 1}, one.get(0));

        var two = ExecRefreshGroupSuite.parseGroupSuitePairs("133004902,2; 133002173,1");
        assertEquals(2, two.size());
        assertArrayEquals(new int[] {133002173, 1}, two.get(1));
    }

    @Test
    void zeroSuiteIsKeptSoTheOverrideCanBeCleared() {
        assertArrayEquals(new int[] {133004902, 0},
                ExecRefreshGroupSuite.parseGroupSuitePairs("133004902,0").get(0));
    }

    @Test
    void malformedEntriesAreSkipped() {
        assertTrue(ExecRefreshGroupSuite.parseGroupSuitePairs(null).isEmpty());
        assertTrue(ExecRefreshGroupSuite.parseGroupSuitePairs("abc").isEmpty());
        assertTrue(ExecRefreshGroupSuite.parseGroupSuitePairs("1,2,3;x,1;0,1;5,-1").isEmpty());
    }
}
