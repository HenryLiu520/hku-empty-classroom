package hku.ec.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 注册的邮箱后缀规则：只接受 HKU 的地址。
 * 这是纯函数测试，不起 Spring 容器。
 */
class RegistrationRulesTest {

    @Test
    @DisplayName("接受：hku.hk 本身与它的各个子域")
    void acceptsHkuDomains() {
        assertTrue(AuthService.isHkuEmail("henry.liu.hr@connect.hku.hk"));
        assertTrue(AuthService.isHkuEmail("u3686264@hku.hk"));
        assertTrue(AuthService.isHkuEmail("someone@hku.hk"));
        assertTrue(AuthService.isHkuEmail("someone@stu.hku.hk"));
        assertTrue(AuthService.isHkuEmail("SOMEONE@CONNECT.HKU.HK"), "大小写不敏感");
        assertTrue(AuthService.isHkuEmail("  someone@connect.hku.hk  "), "前后空格应被忽略");
    }

    @Test
    @DisplayName("拒绝：不是 hku.hk 的后缀（含伪装成子域的外部域名）")
    void rejectsEverythingElse() {
        assertFalse(AuthService.isHkuEmail(null));
        assertFalse(AuthService.isHkuEmail(""));
        assertFalse(AuthService.isHkuEmail("someone@gmail.com"));
        assertFalse(AuthService.isHkuEmail("someone@hku.hk.evil.com"), "后缀伪装不能通过");
        assertFalse(AuthService.isHkuEmail("someone@hkuhk.com"));
        assertFalse(AuthService.isHkuEmail("someone@not-hku.hk"));
        assertFalse(AuthService.isHkuEmail("someone@connect.hku.hk.evil.com"));
        assertFalse(AuthService.isHkuEmail("no-at-sign.hku.hk"));
        assertFalse(AuthService.isHkuEmail("two@at@connect.hku.hk"));
        assertFalse(AuthService.isHkuEmail("@connect.hku.hk"), "缺用户名");
    }
}
