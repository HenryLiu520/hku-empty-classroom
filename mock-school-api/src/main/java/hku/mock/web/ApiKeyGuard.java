/*
 * Empty Classroom Plan — HKU COMP1110 Group 08
 * Author: LIU Haoran (u3686264) · 2026
 * Signature: EC-COMP1110-G08-u3686264-2026
 */
package hku.mock.web;

import hku.mock.ProviderState;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * 所有对外端点共用的入口检查：
 *   1) 模拟延迟（慢响应）
 *   2) 模拟宕机（503）
 *   3) 校验 API key
 * 控制端点（/_control）不走这里 —— 否则开关一开就关不掉了。
 */
@Component
public class ApiKeyGuard {

    private final ProviderState state;
    private final String apiKey;

    public ApiKeyGuard(ProviderState state, @Value("${app.api-key}") String apiKey) {
        this.state = state;
        this.apiKey = apiKey;
    }

    public void check(String providedKey) {
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
        if (providedKey == null || !providedKey.equals(apiKey)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "missing or wrong X-Api-Key");
        }
    }
}
