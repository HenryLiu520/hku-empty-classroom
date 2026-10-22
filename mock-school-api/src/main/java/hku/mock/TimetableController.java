/*
 * Empty Classroom Plan — HKU COMP1110 Group 08
 * Author: LIU Haoran (u3686264) · 2026
 * Signature: EC-COMP1110-G08-u3686264-2026
 */
package hku.mock;

import tools.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 只读的课表契约（我们自己预期的形状）+ 三个控制开关。
 *
 * 鉴权：真接口不会裸奔，所以这里也要求 X-Api-Key。控制开关不要求（它们只有本机才有意义）。
 * 绑定 127.0.0.1：这是测试替身，不对外。
 */
@RestController
@RequestMapping("/v1")
public class TimetableController {

    public record Room(String code, String building, String floor, int capacity, boolean sockets, String seatType) { }

    public record Slot(String roomCode, String building, int dayOfWeek, String start, String end, String courseCode) { }

    public record Seed(String provider, String term, String note, List<Room> rooms, List<Slot> classSlots) { }

    private final ProviderState state;
    private final String apiKey;
    private Seed seed;

    public TimetableController(ProviderState state, @Value("${app.api-key}") String apiKey) {
        this.state = state;
        this.apiKey = apiKey;
    }

    @PostConstruct
    void load() throws Exception {
        try (InputStream in = new ClassPathResource("seed.json").getInputStream()) {
            // Jackson 3（Spring Boot 4 自带）的包名是 tools.jackson.*，不是 com.fasterxml.*
            this.seed = new ObjectMapper().readValue(in, Seed.class);
        }
        if (seed == null || seed.rooms() == null || seed.classSlots() == null) {
            throw new IllegalStateException("seed.json is missing rooms or classSlots");
        }
    }

    // ---------------------------------------------------------------- 只读契约

    /** 上游的元信息：版本、数据时间、当前开关状态。消费端用 lastUpdated 判断数据有多新。 */
    @GetMapping("/meta")
    public Map<String, Object> meta(@RequestHeader(value = "X-Api-Key", required = false) String key) {
        guard(key);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("provider", seed.provider());
        out.put("term", seed.term());
        out.put("note", seed.note());
        out.put("version", "v1");
        out.put("lastUpdated", state.lastUpdated().toString());
        out.put("generatedAt", state.generatedAt().toString());
        out.put("now", Instant.now().toString());
        out.put("staleMinutes", state.staleMinutes());
        out.put("rooms", seed.rooms().size());
        out.put("classSlots", seed.classSlots().size());
        return out;
    }

    /** 全部教室（可按楼栋过滤） */
    @GetMapping("/rooms")
    public List<Room> rooms(@RequestHeader(value = "X-Api-Key", required = false) String key,
                            @RequestParam(required = false) String building) {
        guard(key);
        return seed.rooms().stream()
                .filter(r -> building == null || building.isBlank() || r.building().equalsIgnoreCase(building))
                .sorted((a, b) -> a.code().compareTo(b.code()))
                .toList();
    }

    /** 某一天的全部课时（date=YYYY-MM-DD，按星期几映射到周课表） */
    @GetMapping("/timetable")
    public Map<String, Object> timetable(
            @RequestHeader(value = "X-Api-Key", required = false) String key,
            @RequestParam String date,
            @RequestParam(required = false) String building) {

        guard(key);
        LocalDate day = parseDate(date);
        int dow = day.getDayOfWeek().getValue();          // 周一 = 1
        List<Slot> slots = seed.classSlots().stream()
                .filter(s -> s.dayOfWeek() == dow)
                .filter(s -> building == null || building.isBlank() || s.building().equalsIgnoreCase(building))
                .sorted((a, b) -> a.start().compareTo(b.start()))
                .toList();

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("date", day.toString());
        out.put("dayOfWeek", dow);
        out.put("lastUpdated", state.lastUpdated().toString());
        out.put("count", slots.size());
        out.put("slots", slots);
        return out;
    }

    /** 一间房某一天的课时 */
    @GetMapping("/timetable/{roomCode}")
    public Map<String, Object> roomTimetable(
            @RequestHeader(value = "X-Api-Key", required = false) String key,
            @PathVariable String roomCode,
            @RequestParam String date) {

        guard(key);
        boolean known = seed.rooms().stream().anyMatch(r -> r.code().equalsIgnoreCase(roomCode));
        if (!known) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "unknown room: " + roomCode);

        LocalDate day = parseDate(date);
        int dow = day.getDayOfWeek().getValue();
        List<Slot> slots = seed.classSlots().stream()
                .filter(s -> s.roomCode().equalsIgnoreCase(roomCode) && s.dayOfWeek() == dow)
                .sorted((a, b) -> a.start().compareTo(b.start()))
                .collect(Collectors.toCollection(ArrayList::new));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("roomCode", roomCode);
        out.put("date", day.toString());
        out.put("dayOfWeek", dow);
        out.put("lastUpdated", state.lastUpdated().toString());
        out.put("count", slots.size());
        out.put("slots", slots);
        return out;
    }

    // ---------------------------------------------------------------- 控制开关（只有模拟服务有）

    @GetMapping("/_control/state")
    public Map<String, Object> controlState() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("staleMinutes", state.staleMinutes());
        out.put("slowMs", state.slowMs());
        out.put("down", state.down());
        out.put("lastUpdated", state.lastUpdated().toString());
        return out;
    }

    /** 让数据看起来是 N 分钟前生成的 */
    @PostMapping("/_control/stale")
    public Map<String, Object> setStale(@RequestParam int minutes) {
        state.stale(minutes);
        return controlState();
    }

    /** 让每个响应慢 N 毫秒 */
    @PostMapping("/_control/slow")
    public Map<String, Object> setSlow(@RequestParam long ms) {
        state.slow(ms);
        return controlState();
    }

    /** 让上游直接 503 */
    @PostMapping("/_control/down")
    public Map<String, Object> setDown(@RequestParam(defaultValue = "true") boolean value) {
        state.down(value);
        return controlState();
    }

    /** 全部恢复 */
    @PostMapping("/_control/reset")
    public Map<String, Object> reset() {
        state.reset();
        return controlState();
    }

    // ---------------------------------------------------------------- internal

    /** 鉴权 + 模拟延迟 + 模拟宕机。控制端点不走这里。 */
    private void guard(String key) {
        if (state.slowMs() > 0) {
            try {
                Thread.sleep(state.slowMs());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (state.down()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "simulated outage");
        }
        if (key == null || !key.equals(apiKey)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "missing or wrong X-Api-Key");
        }
    }

    private static LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(date);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "date must be YYYY-MM-DD");
        }
    }
}
