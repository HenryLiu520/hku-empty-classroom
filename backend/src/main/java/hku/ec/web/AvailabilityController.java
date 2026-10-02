/*
 * Empty Classroom Plan — HKU COMP1110 Group 08
 * Author: LIU Haoran (u3686264) · 2026
 * Signature: EC-COMP1110-G08-u3686264-2026
 */
package hku.ec.web;

import hku.ec.service.AvailabilityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api")
public class AvailabilityController {

    private final AvailabilityService availability;

    public AvailabilityController(AvailabilityService availability) {
        this.availability = availability;
    }

    /**
     * 学生端主查询：某天、某楼栋、几点到几点。
     * 例：/api/availability?date=2026-10-05&building=Knowles%20Building&from=14:00&to=15:50
     * 起点必须是整点，终点必须是 :50（最后 10 分钟留给收拾离开）。
     * 兼容旧写法：不给 to 时可用 from + minutes（如 from=14:00&minutes=120）。
     */
    @GetMapping("/availability")
    public Dtos.AvailabilityResponse availability(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String building,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "120") int minutes) {

        LocalDate d = (date == null || date.isBlank()) ? LocalDate.now() : LocalDate.parse(date);
        LocalTime f = (from == null || from.isBlank()) ? null : LocalTime.parse(from);

        if (to != null && !to.isBlank()) {
            if (f == null) {
                throw new IllegalArgumentException(
                        "Give the start time together with the end time, for example from=14:00&to=15:50");
            }
            LocalTime t = LocalTime.parse(to);
            AvailabilityService.requireSearchWindow(f, t);
            minutes = (int) java.time.Duration.between(f, t).toMinutes();
        } else {
            AvailabilityService.requireWholeHours(f, minutes);
        }
        return availability.availability(d, building, f, minutes);
    }

    /** 时间轴：给一间教室画一天的忙/闲条 */
    @GetMapping("/rooms/{code}/timeline")
    public ResponseEntity<?> timeline(@PathVariable String code,
                                      @RequestParam(required = false) String date) {
        LocalDate d = (date == null || date.isBlank()) ? LocalDate.now() : LocalDate.parse(date);
        try {
            List<Dtos.Segment> segments = availability.timeline(d, code);
            return ResponseEntity.ok(java.util.Map.of(
                    "room", code, "date", d.toString(), "dayOfWeek", d.getDayOfWeek().getValue(),
                    "segments", segments));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(new Dtos.ApiError("ROOM_NOT_FOUND", e.getMessage()));
        }
    }
}
