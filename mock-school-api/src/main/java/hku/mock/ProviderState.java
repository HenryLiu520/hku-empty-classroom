/*
 * Empty Classroom Plan — HKU COMP1110 Group 08
 * Author: LIU Haoran (u3686264) · 2026
 * Signature: EC-COMP1110-G08-u3686264-2026
 */
package hku.mock;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * 模拟上游的「状态开关」。真接口不会有这些 —— 它们的唯一用途是让我们能测边界情况：
 *   staleMinutes > 0  数据看起来是 N 分钟前生成的（消费端应该据此提示「数据较旧」）
 *   slowMs       > 0  每个响应先睡这么多毫秒（测超时与降级）
 *   down             直接返回 503（测服务不可用）
 */
@Component
public class ProviderState {

    /** 这份种子数据「生成」的时刻：进程启动时间 */
    private final Instant generatedAt = Instant.now();

    private volatile int staleMinutes = 0;
    private volatile long slowMs = 0;
    private volatile boolean down = false;

    public Instant generatedAt() {
        return generatedAt;
    }

    /** 对外声称的数据时间：生成时间往前推 staleMinutes */
    public Instant lastUpdated() {
        return generatedAt.minus(staleMinutes, ChronoUnit.MINUTES);
    }

    public int staleMinutes() { return staleMinutes; }
    public long slowMs() { return slowMs; }
    public boolean down() { return down; }

    public void stale(int minutes) { this.staleMinutes = Math.max(0, minutes); }
    public void slow(long ms) { this.slowMs = Math.max(0, Math.min(ms, 30_000)); }
    public void down(boolean value) { this.down = value; }

    public void reset() {
        this.staleMinutes = 0;
        this.slowMs = 0;
        this.down = false;
    }
}
