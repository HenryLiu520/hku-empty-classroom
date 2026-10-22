/*
 * Empty Classroom Plan — HKU COMP1110 Group 08
 * Author: LIU Haoran (u3686264) · 2026
 * Signature: EC-COMP1110-G08-u3686264-2026
 */
package hku.mock.web;

import hku.mock.service.TimetableService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 写接口（「教务处那一侧」）。真实接口我们只会读，所以这一组是模拟系统自己的便利：
 * 它让课表可以被改动，从而测到「上游数据变了，我们的系统跟不跟得上」。
 */
@RestController
@RequestMapping("/v1/admin")
public class AdminController {

    private final TimetableService timetable;
    private final ApiKeyGuard guard;

    public AdminController(TimetableService timetable, ApiKeyGuard guard) {
        this.timetable = timetable;
        this.guard = guard;
    }

    /** 新增教室 */
    @PostMapping("/rooms")
    public TimetableService.RoomView createRoom(
            @RequestHeader(value = "X-Api-Key", required = false) String key,
            @RequestBody TimetableService.RoomView body) {
        guard.check(key);
        return timetable.createRoom(body);
    }

    /** 修改教室（只改传了的字段） */
    @PatchMapping("/rooms/{code}")
    public TimetableService.RoomView updateRoom(
            @RequestHeader(value = "X-Api-Key", required = false) String key,
            @PathVariable String code,
            @RequestBody TimetableService.RoomView body) {
        guard.check(key);
        return timetable.updateRoom(code, body);
    }

    /** 删除教室（连带它的课时） */
    @DeleteMapping("/rooms/{code}")
    public Map<String, Object> deleteRoom(
            @RequestHeader(value = "X-Api-Key", required = false) String key,
            @PathVariable String code) {
        guard.check(key);
        timetable.deleteRoom(code);
        return Map.of("deleted", code);
    }

    /** 某间房的全部课时（不分日期） */
    @GetMapping("/rooms/{code}/slots")
    public List<TimetableService.SlotView> slotsOfRoom(
            @RequestHeader(value = "X-Api-Key", required = false) String key,
            @PathVariable String code) {
        guard.check(key);
        return timetable.slotsOfRoom(code);
    }

    /** 新增一条课时（同房同日不允许时间重叠） */
    @PostMapping("/slots")
    public TimetableService.SlotView createSlot(
            @RequestHeader(value = "X-Api-Key", required = false) String key,
            @RequestBody SlotRequest body) {
        guard.check(key);
        return timetable.createSlot(body.roomCode(), body.dayOfWeek(), body.start(), body.end(), body.courseCode());
    }

    /** 删除一条课时 */
    @DeleteMapping("/slots/{id}")
    public Map<String, Object> deleteSlot(
            @RequestHeader(value = "X-Api-Key", required = false) String key,
            @PathVariable Long id) {
        guard.check(key);
        timetable.deleteSlot(id);
        return Map.of("deleted", id);
    }

    public record SlotRequest(String roomCode, Integer dayOfWeek, String start, String end, String courseCode) { }
}
