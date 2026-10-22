/*
 * Empty Classroom Plan — HKU COMP1110 Group 08
 * Author: LIU Haoran (u3686264) · 2026
 * Signature: EC-COMP1110-G08-u3686264-2026
 */
package hku.mock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 模拟的「学校课表接口」（simulated upstream feed）。
 *
 * 为什么存在：真正的大学课表服务我们还没有读取权限（Plan §5.1 的 Plan B），
 * 而「读外部服务」这条边界恰恰是最容易出错的地方 —— 数据旧了、响应慢了、服务挂了，
 * 我们的系统该怎么表现？没有上游就没法测。所以这里做一个**独立进程**，
 * 实现我们预期的只读契约，并额外提供三个控制开关（变旧 / 变慢 / 挂掉），
 * 让消费端可以在真实接口到手之前就被测试。
 *
 * 这是一个 **test double**，不是学校的真实接口，报告里必须这么写。
 */
@SpringBootApplication
public class MockSchoolApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(MockSchoolApiApplication.class, args);
    }
}
