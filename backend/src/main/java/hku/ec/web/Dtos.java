package hku.ec.web;

import java.util.List;

/** 接口出参/入参（原型阶段用 record，字段名与前端对齐） */
public final class Dtos {

    public record LoginRequest(String username, String password) { }

    public record LoginResponse(String token, String username, String displayName, String role) { }

    public record RoomView(Long id, String code, String building, String floor, int capacity, String roomType,
                           Boolean sockets, String seatType, String facilitiesVerifiedAt) { }

    /** 一个空闲窗口，时间是 "HH:mm" */
    public record Window(String start, String end, int minutes) { }

    /**
     * 时间轴的一格 —— 一格就是一个整点（"11:00" 代表 11:00–12:00）。
     * from/to 是这一格里真实被占（或换场）的时间：一格跨着一节课的尾巴时，
     * 写的是真实时间（例如 "11:00" 格里是 11:00–11:50），而不是硬凑成整点。
     * label 直接给界面显示，FREE 格为空。
     */
    public record Segment(String hour, String type, String from, String to, String label) { }

    public record RoomAvailability(
            RoomView room,
            String status,               // AVAILABLE / AVAILABLE_LATER / IN_USE / NONE
            boolean matches,             // 是否满足查询（从 from 起连续 minutes 分钟都空）
            List<Window> windows,
            String nextBusyStart,        // 下一次被占用开始的时刻，null 表示当天余下都空
            String note
    ) { }

    public record AvailabilityResponse(
            String date,
            String weekday,
            String building,
            String from,
            int minutes,
            int bufferMinutes,
            int matchedCount,
            List<RoomAvailability> rooms
    ) { }

    public record UpdateRequest(
            Long roomId,
            String changeType,           // USE = 添加使用 / RELEASE = 释放时间
            String date,                 // yyyy-MM-dd
            String start,                // HH:mm，必须整点
            String end,                  // HH:mm，必须整点
            String reason,
            String expiresAt             // yyyy-MM-dd HH:mm, 可为空
    ) { }

    public record UpdateView(
            Long id,
            String roomCode,
            String changeType,
            String date,
            String start,
            String end,
            String reason,
            String createdBy,
            String state                 // Active / Expired / Cancelled
    ) { }

    public record AuditView(
            Long id, String action, String entity, Long entityId,
            String actor, String detail, String createdAt
    ) { }

    /** 一条房间评价。mine = 是不是当前登录用户写的（决定 UI 上给不给删） */
    public record ReviewView(
            Long id, String roomCode, int rating, String body, String author,
            String createdAt, String updatedAt, boolean mine
    ) { }

    /** 某间房的评价列表：平均分 + 条数 + 排序后的条目 */
    public record ReviewList(
            String roomCode, double average, int count, String sort, List<ReviewView> items
    ) { }

    public record ReviewSummary(String roomCode, double average, int count) { }

    public record ReviewRequest(Integer rating, String body) { }

    /** 管理员编辑设施（第一批三条）：座位总数 / 有没有插座 / 座位类型 */
    public record FacilitiesRequest(Integer capacity, Boolean sockets, String seatType) { }

    /** 自助注册：邮箱 + 密码；邮箱后缀必须在接口层卡成 HKU 的 */
    public record RegisterRequest(String username, String email, String password) { }

    public record ApiError(String error, String message) { }
}
