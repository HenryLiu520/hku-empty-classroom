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

/** 只读契约：真接口会提供的那部分。需要 X-Api-Key。 */
@RestController
@RequestMapping("/v1")
public class PublicController {

    private final TimetableService timetable;
    private final ApiKeyGuard guard;

    public PublicController(TimetableService timetable, ApiKeyGuard guard) {
        this.timetable = timetable;
        this.guard = guard;
    }

    /** 元信息：provider / term / generatedAt / lastUpdated / revision / 计数 */
    @GetMapping("/meta")
    public Map<String, Object> meta(@RequestHeader(value = "X-Api-Key", required = false) String key) {
        guard.check(key);
        return timetable.metaView();
    }

    /** 教室清单，可按楼栋过滤 */
    @GetMapping("/rooms")
    public List<TimetableService.RoomView> rooms(
            @RequestHeader(value = "X-Api-Key", required = false) String key,
            @RequestParam(required = false) String building) {
        guard.check(key);
        return timetable.listRooms(building);
    }

    /** 某一天的课时（date=YYYY-MM-DD，按星期几映射周课表） */
    @GetMapping("/timetable")
    public Map<String, Object> timetable(
            @RequestHeader(value = "X-Api-Key", required = false) String key,
            @RequestParam String date,
            @RequestParam(required = false) String building) {
        guard.check(key);
        return timetable.day(date, building);
    }

    /** 某间房某一天的课时 */
    @GetMapping("/timetable/{roomCode}")
    public Map<String, Object> roomTimetable(
            @RequestHeader(value = "X-Api-Key", required = false) String key,
            @PathVariable String roomCode,
            @RequestParam String date) {
        guard.check(key);
        return timetable.roomDay(roomCode, date);
    }
}
