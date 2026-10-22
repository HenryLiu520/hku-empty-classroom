/*
 * Empty Classroom Plan — HKU COMP1110 Group 08
 * Author: LIU Haoran (u3686264) · 2026
 * Signature: EC-COMP1110-G08-u3686264-2026
 */
package hku.mock.web;

import hku.mock.ProviderState;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 测试开关。真接口没有这些，也不要求 API key（否则开关一开就关不掉了）。
 * 只绑 127.0.0.1，不对外。
 */
@RestController
@RequestMapping("/v1/_control")
public class ControlController {

    private final ProviderState state;

    public ControlController(ProviderState state) {
        this.state = state;
    }

    @GetMapping("/state")
    public Map<String, Object> state() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("staleMinutes", state.staleMinutes());
        out.put("slowMs", state.slowMs());
        out.put("down", state.down());
        return out;
    }

    /** 让数据看起来是 N 分钟前生成的 */
    @PostMapping("/stale")
    public Map<String, Object> stale(@RequestParam int minutes) {
        state.stale(minutes);
        return state();
    }

    /** 每个只读/写响应先睡 N 毫秒 */
    @PostMapping("/slow")
    public Map<String, Object> slow(@RequestParam long ms) {
        state.slow(ms);
        return state();
    }

    /** 直接 503 */
    @PostMapping("/down")
    public Map<String, Object> down(@RequestParam(defaultValue = "true") boolean value) {
        state.down(value);
        return state();
    }

    /** 全部复位 */
    @PostMapping("/reset")
    public Map<String, Object> reset() {
        state.reset();
        return state();
    }
}
