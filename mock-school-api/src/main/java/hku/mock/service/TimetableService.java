/*
 * Empty Classroom Plan — HKU COMP1110 Group 08
 * Author: LIU Haoran (u3686264) · 2026
 * Signature: EC-COMP1110-G08-u3686264-2026
 */
package hku.mock.service;

import hku.mock.ProviderState;
import hku.mock.domain.ClassSlot;
import hku.mock.domain.FeedMeta;
import hku.mock.domain.Room;
import hku.mock.repo.ClassSlotRepository;
import hku.mock.repo.FeedMetaRepository;
import hku.mock.repo.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 模拟上游的业务层：读课表、维护课表（管理员侧）。
 *
 * 关键语义（消费端要能依赖的）：
 *   - 课表是**周课表**：date → dayOfWeek（周一 = 1）→ 查该天的课时；
 *   - feed_meta.generated_at 表示「数据什么时候生成的」，任何写操作都会把它推到 now 并把 revision + 1；
 *   - 对外报的 lastUpdated = generated_at 减去测试开关的 staleMinutes。
 */
@Service
public class TimetableService {

    public static final ZoneId HK = ZoneId.of("Asia/Hong_Kong");
    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

    /** capacity 用 Integer：PATCH 时可以只传要改的字段，null 表示不改 */
    public record RoomView(String code, String building, String floor, Integer capacity,
                           Boolean sockets, String seatType) { }

    public record SlotView(Long id, String roomCode, String building, int dayOfWeek,
                           String start, String end, String courseCode) { }

    private final RoomRepository rooms;
    private final ClassSlotRepository slots;
    private final FeedMetaRepository meta;
    private final ProviderState state;

    public TimetableService(RoomRepository rooms, ClassSlotRepository slots,
                            FeedMetaRepository meta, ProviderState state) {
        this.rooms = rooms;
        this.slots = slots;
        this.meta = meta;
        this.state = state;
    }

    // ---------------------------------------------------------------- 元信息

    @Transactional(readOnly = true)
    public FeedMeta feedMeta() {
        return meta.findById(1).orElseThrow(() -> new IllegalStateException("feed_meta row is missing"));
    }

    /** 对外报的数据时间（受 stale 开关影响） */
    public Instant lastUpdated() {
        LocalDateTime generated = feedMeta().getGeneratedAt();
        return generated.atZone(HK).toInstant().minusSeconds(60L * state.staleMinutes());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> metaView() {
        FeedMeta m = feedMeta();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("provider", m.getProvider());
        out.put("term", m.getTerm());
        out.put("version", "v1");
        out.put("note", "Test double: a simulated University timetable feed, not an official service.");
        out.put("generatedAt", m.getGeneratedAt().atZone(HK).toInstant().toString());
        out.put("lastUpdated", lastUpdated().toString());
        out.put("revision", m.getRevision());
        out.put("now", Instant.now().toString());
        out.put("staleMinutes", state.staleMinutes());
        out.put("rooms", rooms.count());
        out.put("classSlots", slots.count());
        return out;
    }

    // ---------------------------------------------------------------- 读

    @Transactional(readOnly = true)
    public List<RoomView> listRooms(String building) {
        List<Room> found = (building == null || building.isBlank())
                ? rooms.findAllByOrderByCodeAsc()
                : rooms.findByBuildingIgnoreCaseOrderByCodeAsc(building);
        return found.stream().map(TimetableService::roomView).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> day(String date, String building) {
        LocalDate d = LocalDate.parse(date);
        int dow = d.getDayOfWeek().getValue();
        List<ClassSlot> found = slots.findByDayOfWeekOrderByStartTimeAsc(dow);
        List<SlotView> out = new ArrayList<>();
        for (ClassSlot s : found) {
            Room r = rooms.findById(s.getRoomId()).orElse(null);
            if (r == null) continue;
            if (building != null && !building.isBlank() && !r.getBuilding().equalsIgnoreCase(building)) continue;
            out.add(slotView(s, r));
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("date", d.toString());
        res.put("dayOfWeek", dow);
        res.put("lastUpdated", lastUpdated().toString());
        res.put("count", out.size());
        res.put("slots", out);
        return res;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> roomDay(String roomCode, String date) {
        Room r = rooms.findByCode(roomCode)
                .orElseThrow(() -> new IllegalArgumentException("unknown room: " + roomCode));
        LocalDate d = LocalDate.parse(date);
        int dow = d.getDayOfWeek().getValue();
        List<SlotView> out = slots.findByRoomIdAndDayOfWeekOrderByStartTimeAsc(r.getId(), dow)
                .stream().map(s -> slotView(s, r)).toList();
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("roomCode", r.getCode());
        res.put("date", d.toString());
        res.put("dayOfWeek", dow);
        res.put("lastUpdated", lastUpdated().toString());
        res.put("count", out.size());
        res.put("slots", out);
        return res;
    }

    // ---------------------------------------------------------------- 写（管理员侧）

    @Transactional
    public RoomView createRoom(RoomView in) {
        validateRoom(in, true);
        if (rooms.findByCode(in.code()).isPresent()) {
            throw new IllegalArgumentException("room already exists: " + in.code());
        }
        Room r = new Room();
        apply(r, in);
        touch();
        return roomView(rooms.save(r));
    }

    @Transactional
    public RoomView updateRoom(String code, RoomView in) {
        Room r = rooms.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("unknown room: " + code));
        validateRoom(in, false);
        if (in.building() != null) r.setBuilding(in.building());
        if (in.floor() != null) r.setFloor(in.floor());
        if (in.capacity() != null) r.setCapacity(in.capacity());
        r.setSockets(in.sockets());
        if (in.seatType() != null) r.setSeatType(in.seatType());
        touch();
        return roomView(rooms.save(r));
    }

    @Transactional
    public void deleteRoom(String code) {
        Room r = rooms.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("unknown room: " + code));
        rooms.delete(r);
        touch();
    }

    @Transactional
    public SlotView createSlot(String roomCode, Integer dayOfWeek, String start, String end, String courseCode) {
        Room r = rooms.findByCode(roomCode)
                .orElseThrow(() -> new IllegalArgumentException("unknown room: " + roomCode));
        if (dayOfWeek == null || dayOfWeek < 1 || dayOfWeek > 7) {
            throw new IllegalArgumentException("dayOfWeek must be 1 (Monday) to 7 (Sunday)");
        }
        LocalTime s = parseTime(start, "start");
        LocalTime e = parseTime(end, "end");
        if (!e.isAfter(s)) throw new IllegalArgumentException("end must be after start");
        if (courseCode == null || courseCode.isBlank()) throw new IllegalArgumentException("courseCode is required");

        // 同一间房同一天不允许时间重叠 —— 这是「课表」这个数据本身的性质
        for (ClassSlot existing : slots.findByRoomIdAndDayOfWeekOrderByStartTimeAsc(r.getId(), dayOfWeek)) {
            boolean overlaps = s.isBefore(existing.getEndTime()) && existing.getStartTime().isBefore(e);
            if (overlaps) {
                throw new IllegalArgumentException("overlaps an existing class: %s %s-%s".formatted(
                        existing.getCourseCode(),
                        existing.getStartTime().format(HM), existing.getEndTime().format(HM)));
            }
        }

        ClassSlot slot = new ClassSlot();
        slot.setRoomId(r.getId());
        slot.setDayOfWeek(dayOfWeek);
        slot.setStartTime(s);
        slot.setEndTime(e);
        slot.setCourseCode(courseCode.trim());
        touch();
        return slotView(slots.save(slot), r);
    }

    @Transactional
    public void deleteSlot(Long id) {
        ClassSlot slot = slots.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("unknown slot: " + id));
        slots.delete(slot);
        touch();
    }

    @Transactional(readOnly = true)
    public List<SlotView> slotsOfRoom(String roomCode) {
        Room r = rooms.findByCode(roomCode)
                .orElseThrow(() -> new IllegalArgumentException("unknown room: " + roomCode));
        return slots.findByRoomIdOrderByDayOfWeekAscStartTimeAsc(r.getId())
                .stream().map(s -> slotView(s, r)).toList();
    }

    // ---------------------------------------------------------------- internal

    /** 任何写操作都要动这里：数据变了，generated_at 与 revision 一起变 */
    private void touch() {
        meta.findById(1).ifPresent(m -> {
            m.setGeneratedAt(LocalDateTime.now(HK));
            m.setRevision(m.getRevision() + 1);
            meta.save(m);
        });
    }

    private void validateRoom(RoomView in, boolean requireCode) {
        if (requireCode && (in.code() == null || in.code().isBlank())) {
            throw new IllegalArgumentException("code is required");
        }
        if (in.building() == null || in.building().isBlank()) throw new IllegalArgumentException("building is required");
        if (in.floor() == null || in.floor().isBlank()) throw new IllegalArgumentException("floor is required");
        if (in.capacity() == null || in.capacity() <= 0) throw new IllegalArgumentException("capacity must be positive");
    }

    private void apply(Room r, RoomView in) {
        r.setCode(in.code().trim());
        r.setBuilding(in.building().trim());
        r.setFloor(in.floor().trim());
        r.setCapacity(in.capacity());
        r.setSockets(in.sockets());
        r.setSeatType(in.seatType());
    }

    private static LocalTime parseTime(String raw, String field) {
        try {
            return LocalTime.parse(raw);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(field + " must look like HH:mm");
        }
    }

    private static RoomView roomView(Room r) {
        return new RoomView(r.getCode(), r.getBuilding(), r.getFloor(), r.getCapacity(),
                r.getSockets(), r.getSeatType());
    }

    private static SlotView slotView(ClassSlot s, Room r) {
        return new SlotView(s.getId(), r.getCode(), r.getBuilding(), s.getDayOfWeek(),
                s.getStartTime().format(HM), s.getEndTime().format(HM), s.getCourseCode());
    }
}
