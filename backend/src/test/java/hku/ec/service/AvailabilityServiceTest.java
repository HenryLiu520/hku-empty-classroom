package hku.ec.service;

import hku.ec.config.AppProps;
import hku.ec.domain.ClassSlot;
import hku.ec.domain.Room;
import hku.ec.domain.RoomUpdate;
import hku.ec.repo.ClassSlotRepository;
import hku.ec.repo.RoomRepository;
import hku.ec.repo.RoomUpdateRepository;
import hku.ec.web.Dtos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 六个边界算例（对应 Plan §3.4 的验证设计）。
 * 每个用例自己造数据（房间 + 课时 + 临时变更），固定用"下一个周一"，断言"期望结果在跑之前就写好"。
 * 用 @Transactional，测试完自动回滚，不污染演示数据。
 */
@SpringBootTest
@Transactional
class AvailabilityServiceTest {

    private static final String BUILDING = "TST";
    private static final String ROOM_CODE = "TST-101";

    /** 固定用下一个周一：星期几可预测，避免"今天是周三所以课表不一样"这类偶然 */
    private final LocalDate date = LocalDate.now().with(TemporalAdjusters.next(java.time.DayOfWeek.MONDAY));

    @Autowired private AvailabilityService availability;
    @Autowired private UpdateService updateService;
    @Autowired private RoomRepository rooms;
    @Autowired private ClassSlotRepository slots;
    @Autowired private RoomUpdateRepository updates;
    @Autowired private AppProps props;

    private Room room;

    @BeforeEach
    void setUp() {
        room = new Room();
        room.setCode(ROOM_CODE);
        room.setBuilding(BUILDING);
        room.setFloor("1F");
        room.setCapacity(40);
        room.setRoomType("Tutorial");
        room = rooms.save(room);
    }

    // ---------------------------------------------------------------- 算例

    @Test
    @DisplayName("1. 正常间隙：两节课之间的大段空档应被识别为可用")
    void normalGapIsUsable() {
        slot("09:00", "10:00");
        slot("14:00", "15:00");

        Dtos.RoomAvailability ra = at("11:00", 60);

        assertTrue(ra.matches(), "11:00 起 60 分钟应落在 10:10–13:50 这段空档里");
        assertTrue(hasWindowCovering(ra, 11 * 60, 12 * 60), "空档应包含 11:00–12:00");
        assertEquals("AVAILABLE", ra.status());
    }

    @Test
    @DisplayName("2. 零间隙：两节课背靠背，中间不应出现可用窗口")
    void backToBackGivesNoWindow() {
        slot("09:00", "10:00");
        slot("10:00", "11:00");

        Dtos.RoomAvailability ra = at("10:00", 30);

        assertFalse(hasWindowCovering(ra, 10 * 60, 10 * 60 + 30), "10:00–10:30 处在两节课之间，不能算可用");
        assertNotEquals("AVAILABLE", ra.status());
    }

    @Test
    @DisplayName("3. 间隙小于换场缓冲：短间隙不能被展示为可用")
    void gapShorterThanBufferIsNotOffered() {
        slot("09:00", "10:00");
        slot("10:05", "11:00");   // 只有 5 分钟空档，缓冲是 10 分钟

        Dtos.RoomAvailability ra = at("10:02", 3);

        assertFalse(hasWindowCovering(ra, 10 * 60 + 2, 10 * 60 + 5), "5 分钟的间隙小于缓冲，不该被算作可用");
        assertFalse(hasAnyWindowStartingAt(ra, "10:00"), "不应出现从 10:00 开始的窗口");
    }

    @Test
    @DisplayName("4. 释放时间：RELEASE 只能撤销管理员自己添加的占用")
    void releaseOnlyCancelsAnAdminAddition() {
        update(RoomUpdate.ChangeType.USE, "14:00", "15:00");
        boolean before = hasWindowCovering(at("14:30", 20), 14 * 60 + 30, 14 * 60 + 50);

        update(RoomUpdate.ChangeType.RELEASE, "14:00", "15:00");
        boolean after = hasWindowCovering(at("14:30", 20), 14 * 60 + 30, 14 * 60 + 50);

        assertFalse(before, "管理员加上的占用生效后，14:30 不该算可用");
        assertTrue(after, "撤销这次添加之后，14:30 应恢复可用");
        assertEquals("AVAILABLE", at("14:30", 20).status(),
                "撤销后状态不能还停在 IN_USE —— 那会和同一响应里的空闲窗口自相矛盾");
    }

    @Test
    @DisplayName("4b. 优先级：学校课表高于管理员 —— RELEASE 挖不掉课表里的课")
    void releaseCannotOverrideTheTimetable() {
        slot("14:00", "15:00");

        update(RoomUpdate.ChangeType.RELEASE, "14:00", "15:00");

        Dtos.RoomAvailability ra = at("14:30", 20);

        assertFalse(hasWindowCovering(ra, 14 * 60 + 30, 14 * 60 + 50),
                "学校课表是权威来源：管理员发布的释放不能把课表里已有的课挖掉");
        assertEquals("IN_USE", ra.status());
    }

    @Test
    @DisplayName("5. 添加使用：USE 会占据原本空闲的时段，并以 IN_USE 显示")
    void useBlocksFreeTime() {
        Dtos.RoomAvailability before = at("11:30", 30);
        assertTrue(before.matches(), "添加使用之前 11:30 是空的（这间房当天没排课）");

        update(RoomUpdate.ChangeType.USE, "11:00", "12:00");

        Dtos.RoomAvailability after = at("11:30", 30);
        assertEquals("IN_USE", after.status());
        assertFalse(after.matches(), "被占用后不应再算可用");
    }

    @Test
    @DisplayName("6. 课进行中：查询时刻正在上课，应显示 IN_USE 而不是可用")
    void duringClassIsInUse() {
        slot("11:00", "12:00");

        Dtos.RoomAvailability ra = at("11:30", 30);

        assertEquals("IN_USE", ra.status());
        assertFalse(ra.matches());
        // 当天没有更晚的占用段，所以"下一次被占用"为空（界面显示 —）
        assertNull(ra.nextBusyStart());
    }

    @Test
    @DisplayName("7. 以小时为单位：非整点的时间应被拒绝")
    void offHourTimesAreRejected() {
        Dtos.UpdateRequest odd = new Dtos.UpdateRequest(room.getId(), "USE", date.toString(),
                "14:30", "16:00", "unit test", null);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> updateService.create(odd, "tester"), "14:30 不是整点，应被拒绝");
        assertTrue(e.getMessage().contains("on the hour"), "错误信息应说明必须整点，实际：" + e.getMessage());

        Dtos.UpdateRequest whole = new Dtos.UpdateRequest(room.getId(), "USE", date.toString(),
                "14:00", "16:00", "unit test", null);
        assertEquals("USE", updateService.create(whole, "tester").getChangeType().name());
    }

    @Test
    @DisplayName("8. 学校课表优先级最高：管理员不能对课表里的课提交任何变更")
    void adminCannotChangeClassTime() {
        slot("14:00", "15:00");

        Dtos.UpdateRequest use = new Dtos.UpdateRequest(room.getId(), "USE", date.toString(),
                "14:00", "15:00", "unit test", null);
        IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class,
                () -> updateService.create(use, "admin1"), "课表里已有课，USE 应被拒绝");
        assertTrue(e1.getMessage().contains("Timetable has priority"), "实际：" + e1.getMessage());

        Dtos.UpdateRequest release = new Dtos.UpdateRequest(room.getId(), "RELEASE", date.toString(),
                "13:00", "16:00", "unit test", null);
        IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class,
                () -> updateService.create(release, "admin1"), "跨越课程时段，RELEASE 也应被拒绝");
        assertTrue(e2.getMessage().contains("Timetable has priority"), "实际：" + e2.getMessage());

        Dtos.UpdateRequest freeTime = new Dtos.UpdateRequest(room.getId(), "USE", date.toString(),
                "16:00", "17:00", "unit test", null);
        assertEquals("USE", updateService.create(freeTime, "admin1").getChangeType().name(),
                "课表没课的时间仍然可以正常添加使用");
    }

    @Test
    @DisplayName("9. 时间轴：连续占用合成一整块 —— 两小时的课 = 跨两格，标签写 13:00–14:50")
    void timelineMergesWholeHourBlocks() {
        slot("13:00", "14:50");            // 两小时 = 2 个整点块

        List<Dtos.Segment> segs = availability.timeline(date, ROOM_CODE);

        Dtos.Segment block = segs.stream()
                .filter(s -> "BUSY".equals(s.type()))
                .findFirst().orElseThrow(() -> new AssertionError("应该有一块被占的方格"));
        assertEquals("13:00", block.hour(), "块从 13:00 开始");
        assertEquals(2, block.span(), "两小时的课占两个整点块");
        assertEquals("13:00", block.from());
        assertEquals("14:50", block.to(), "显示的是真实下课时间");
        assertEquals("13:00\u201314:50", block.label(), "一整块写完整区间，不拆成 13:00–13:50 与 14:00–14:50");
        assertEquals(14, segs.stream().mapToInt(Dtos.Segment::span).sum(), "08:00–22:00 一共 14 个整点块");
    }

    // ---------------------------------------------------------------- helpers

    private void slot(String start, String end) {
        ClassSlot s = new ClassSlot();
        s.setRoomId(room.getId());
        s.setDayOfWeek(date.getDayOfWeek().getValue());
        s.setStartTime(LocalTime.parse(start));
        s.setEndTime(LocalTime.parse(end));
        s.setCourseCode("TEST");
        slots.save(s);
    }

    private void update(RoomUpdate.ChangeType type, String start, String end) {
        RoomUpdate u = new RoomUpdate();
        u.setRoomId(room.getId());
        u.setChangeType(type);
        u.setSlotDate(date);
        u.setStartTime(LocalTime.parse(start));
        u.setEndTime(LocalTime.parse(end));
        u.setReason("unit test");
        u.setCreatedBy("test");
        u.setCreatedAt(LocalDateTime.now());
        u.setActive(true);
        updates.save(u);
    }

    private Dtos.RoomAvailability at(String from, int minutes) {
        Dtos.AvailabilityResponse resp = availability.availability(date, BUILDING, LocalTime.parse(from), minutes);
        return resp.rooms().stream()
                .filter(r -> r.room().code().equals(ROOM_CODE))
                .findFirst()
                .orElseThrow(() -> new AssertionError("test room not returned by availability()"));
    }

    private boolean hasWindowCovering(Dtos.RoomAvailability ra, int from, int to) {
        return ra.windows().stream().anyMatch(w -> toMin(w.start()) <= from && toMin(w.end()) >= to);
    }

    private boolean hasAnyWindowStartingAt(Dtos.RoomAvailability ra, String start) {
        return ra.windows().stream().anyMatch(w -> w.start().equals(start));
    }

    private static int toMin(String hhmm) {
        return Integer.parseInt(hhmm.substring(0, 2)) * 60 + Integer.parseInt(hhmm.substring(3));
    }
}
