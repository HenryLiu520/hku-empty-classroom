/*
 * Empty Classroom Plan — HKU COMP1110 Group 08
 * Author: LIU Haoran (u3686264) · 2026
 * Signature: EC-COMP1110-G08-u3686264-2026
 */
package hku.ec.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/** 区间运算的纯函数测试（不启动 Spring，毫秒级）——空闲区间计算的底层 */
class AvailabilityMathTest {

    @Test
    @DisplayName("merge：重叠与相邻的区间会合并")
    void mergeOverlapping() {
        List<int[]> in = List.of(new int[]{540, 600}, new int[]{570, 660}, new int[]{700, 720});
        List<int[]> out = AvailabilityService.merge(in);
        assertEquals(2, out.size());
        assertArrayEquals(new int[]{540, 660}, out.get(0));
        assertArrayEquals(new int[]{700, 720}, out.get(1));
    }

    @Test
    @DisplayName("subtract：整段被挖掉")
    void subtractWhole() {
        List<int[]> out = AvailabilityService.subtract(List.of(new int[]{600, 660}), List.of(new int[]{590, 670}));
        assertTrue(out.isEmpty());
    }

    @Test
    @DisplayName("subtract：从中间挖掉会留下两截碎片")
    void subtractMiddle() {
        List<int[]> out = AvailabilityService.subtract(List.of(new int[]{600, 720}), List.of(new int[]{630, 660}));
        assertEquals(2, out.size());
        assertArrayEquals(new int[]{600, 630}, out.get(0));
        assertArrayEquals(new int[]{660, 720}, out.get(1));
    }

    @Test
    @DisplayName("subtract：不相交时不改变原区间")
    void subtractDisjoint() {
        List<int[]> out = AvailabilityService.subtract(List.of(new int[]{600, 660}), List.of(new int[]{700, 730}));
        assertEquals(1, out.size());
        assertArrayEquals(new int[]{600, 660}, out.get(0));
    }

    @Test
    @DisplayName("gaps：在开放时段内求出补集")
    void gapsWithinOpeningHours() {
        List<int[]> out = AvailabilityService.gaps(List.of(new int[]{600, 660}), 480, 1320);
        assertEquals(2, out.size());
        assertArrayEquals(new int[]{480, 600}, out.get(0));
        assertArrayEquals(new int[]{660, 1320}, out.get(1));
    }

    @Test
    @DisplayName("gaps：没有任何占用时，整天都空")
    void gapsWithNoBusy() {
        List<int[]> out = AvailabilityService.gaps(List.of(), 480, 1320);
        assertEquals(1, out.size());
        assertArrayEquals(new int[]{480, 1320}, out.get(0));
    }

    // ------------------------------------------------- 整小时口径（接口契约）

    @Test
    @DisplayName("整小时口径：非整点的起点被拒绝，整点通过")
    void offHourStartIsRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> AvailabilityService.requireWholeHours(LocalTime.parse("14:30"), 120));
        assertTrue(e.getMessage().contains("on the hour"), "错误信息应说明必须整点，实际：" + e.getMessage());

        assertDoesNotThrow(() -> AvailabilityService.requireWholeHours(LocalTime.parse("14:00"), 120));
        assertDoesNotThrow(() -> AvailabilityService.requireWholeHours(null, 60), "不指定起点（查整天）应放行");
    }

    @Test
    @DisplayName("整小时口径：时长必须是整小时")
    void partialHourDurationIsRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> AvailabilityService.requireWholeHours(LocalTime.parse("14:00"), 90));
        assertTrue(e.getMessage().contains("whole number of hours"), "实际：" + e.getMessage());

        assertDoesNotThrow(() -> AvailabilityService.requireWholeHours(LocalTime.parse("14:00"), 180));
    }

    @Test
    @DisplayName("查询口径：起点整点，终点必须是 :50（留 10 分钟离开）")
    void searchWindowContract() {
        assertDoesNotThrow(() -> AvailabilityService.requireSearchWindow(
                LocalTime.parse("14:00"), LocalTime.parse("15:50")));

        assertThrows(IllegalArgumentException.class, () -> AvailabilityService.requireSearchWindow(
                LocalTime.parse("14:30"), LocalTime.parse("15:50")), "起点非整点应被拒");

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> AvailabilityService.requireSearchWindow(LocalTime.parse("14:00"), LocalTime.parse("16:00")),
                "终点不是 :50 应被拒");
        assertTrue(e.getMessage().contains(":50"), "错误信息应说明必须是 :50，实际：" + e.getMessage());

        assertThrows(IllegalArgumentException.class, () -> AvailabilityService.requireSearchWindow(
                LocalTime.parse("14:00"), LocalTime.parse("13:50")), "终点早于起点应被拒");
    }
}
