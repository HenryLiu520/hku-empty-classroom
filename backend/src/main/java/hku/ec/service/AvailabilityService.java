/*
 * Empty Classroom Plan — HKU COMP1110 Group 08
 * Author: LIU Haoran (u3686264) · 2026
 * Signature: EC-COMP1110-G08-u3686264-2026
 */
package hku.ec.service;

import hku.ec.config.AppProps;
import hku.ec.domain.ClassSlot;
import hku.ec.domain.Room;
import hku.ec.domain.RoomUpdate;
import hku.ec.repo.ClassSlotRepository;
import hku.ec.repo.RoomRepository;
import hku.ec.repo.RoomUpdateRepository;
import hku.ec.web.Dtos;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 技术核心：把两类输入折成每间教室的空闲区间。
 *   输入 A：学校课表（class_slots，按星期几循环）—— 权威来源，由学校自己的系统更新；
 *   输入 B：管理员发布的临时变更（room_updates）—— 只有管理员能写。
 * 学生只查询，不产生任何占用。
 *
 * 规则：
 *   1. 学校课表优先：管理员发布的变更既不能取消、也不能覆盖课表里已有的课（学校 > 管理员）；
 *   2. USE（添加使用）加进占用，实际只会落在课表没排课的时间上；
 *   3. RELEASE（释放时间）只撤销管理员自己用 USE 加上的那段占用；
 *   4. 每个占用段两侧各留 buffer 分钟（换场时间），不足 buffer 的间隙不算可用；
 *   5. 只在开放时段内（默认 08:00-22:00）计算空闲。
 */
@Service
public class AvailabilityService {

    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

    private final RoomRepository roomRepo;
    private final ClassSlotRepository slotRepo;
    private final RoomUpdateRepository updateRepo;
    private final AppProps props;

    public AvailabilityService(RoomRepository roomRepo, ClassSlotRepository slotRepo,
                               RoomUpdateRepository updateRepo, AppProps props) {
        this.roomRepo = roomRepo;
        this.slotRepo = slotRepo;
        this.updateRepo = updateRepo;
        this.props = props;
    }

    // ---------------------------------------------------------------- public

    public Dtos.AvailabilityResponse availability(LocalDate date, String building,
                                                  LocalTime from, int minutes) {
        int openStart = toMinutes(props.getOpeningStart());
        int openEnd = toMinutes(props.getOpeningEnd());
        int buffer = props.getBufferMinutes();

        List<Room> rooms = (building == null || building.isBlank())
                ? roomRepo.findAllByOrderByCodeAsc()
                : roomRepo.findByBuildingOrderByCodeAsc(building);

        // 一次性取回当天全部课表和变更，按房间分组：3 次查询服务整页，而不是每间房各问一遍
        Map<Long, List<ClassSlot>> slotsByRoom = slotRepo.findByDayOfWeek(date.getDayOfWeek().getValue())
                .stream().collect(Collectors.groupingBy(ClassSlot::getRoomId));
        Map<Long, List<RoomUpdate>> updatesByRoom = updateRepo.findBySlotDateAndActiveTrue(date)
                .stream().collect(Collectors.groupingBy(RoomUpdate::getRoomId));

        List<Dtos.RoomAvailability> views = new ArrayList<>();
        for (Room room : rooms) {
            views.add(evaluate(room, from, minutes, openStart, openEnd, buffer,
                    slotsByRoom.getOrDefault(room.getId(), List.of()),
                    updatesByRoom.getOrDefault(room.getId(), List.of())));
        }
        // 满足查询的排前面，其次按最长可用窗口排
        views.sort(Comparator.comparing(Dtos.RoomAvailability::matches).reversed()
                .thenComparing(ra -> -longest(ra.windows())));

        int matched = (int) views.stream().filter(Dtos.RoomAvailability::matches).count();

        return new Dtos.AvailabilityResponse(
                date.toString(),
                date.getDayOfWeek().name().substring(0, 3),
                building == null ? "ALL" : building,
                from == null ? null : from.format(HM),
                minutes,
                buffer,
                matched,
                views);
    }

    /** 接口口径：时间以整小时为单位（查询的起点与时长都必须是整点/整小时） */
    public static void requireWholeHours(LocalTime from, int minutes) {
        if (from != null && from.getMinute() != 0) {
            throw new IllegalArgumentException("Times must be on the hour, for example from=14:00");
        }
        if (minutes % 60 != 0) {
            throw new IllegalArgumentException("The duration must be a whole number of hours, for example minutes=120");
        }
    }

    /**
     * 查询口径："几点到几点"。起点整点，终点必须落在 :50 ——
     * 因为下课前 10 分钟是收拾离开的时间，不能被算作可用。
     */
    public static void requireSearchWindow(LocalTime from, LocalTime to) {
        if (from == null || to == null) return;
        if (from.getMinute() != 0) {
            throw new IllegalArgumentException("The start time must be on the hour, for example from=14:00");
        }
        if (to.getMinute() != 50) {
            throw new IllegalArgumentException(
                    "The end time must end at :50, for example to=15:50, so that the last 10 minutes are left for leaving");
        }
        if (!to.isAfter(from)) {
            throw new IllegalArgumentException("The end time must be after the start time");
        }
    }

    /**
     * 时间轴：一格一个整点，连续的占用合成「一整块」。
     *
     * 占用以整块为单位（Plan §3.1）：所有课整点开始、:50 结束，所以
     *   50 分钟 = 1 块、110 分钟 = 2 块（13:00–14:50）、170 分钟 = 3 块。
     * 两小时的课在图上就是跨两格的一整块，标签写真实的 "13:00–14:50"，
     * 不会拆成 "13:00–13:50" 和 "14:00–14:50" 两小块。
     */
    public List<Dtos.Segment> timeline(LocalDate date, String roomCode) {
        Room room = roomRepo.findByCode(roomCode)
                .orElseThrow(() -> new IllegalArgumentException("unknown room: " + roomCode));
        int openStart = toMinutes(props.getOpeningStart());
        int openEnd = toMinutes(props.getOpeningEnd());

        List<int[]> busy = busyIntervals(room.getId(), date);

        int cells = Math.max(1, (openEnd - openStart) / 60);
        boolean[] occupied = new boolean[cells];
        for (int[] b : busy) {
            int first = Math.max(0, (b[0] - openStart) / 60);
            int last = Math.min(cells - 1, (b[1] - 1 - openStart) / 60);   // 贴着边界结束时不占下一格
            for (int i = first; i <= last; i++) {
                occupied[i] = true;
            }
        }

        List<Dtos.Segment> out = new ArrayList<>();
        int i = 0;
        while (i < cells) {
            int hour = openStart + i * 60;
            if (!occupied[i]) {
                out.add(new Dtos.Segment(mm(hour), "FREE", 1, mm(hour), mm(hour + 60), ""));
                i++;
                continue;
            }
            int j = i;
            while (j + 1 < cells && occupied[j + 1]) {
                j++;
            }
            int blockStart = openStart + i * 60;
            int blockEnd = openStart + (j + 1) * 60;      // 这一块的右边界（整点）
            int classEnd = blockEnd - 10;                 // 按课表口径显示到 :50
            out.add(new Dtos.Segment(mm(blockStart), "BUSY", j - i + 1,
                    mm(blockStart), mm(classEnd),
                    mm(blockStart) + "\u2013" + mm(classEnd)));
            i = j + 1;
        }
        return out;
    }

    // --------------------------------------------------------------- internal

    private Dtos.RoomAvailability evaluate(Room room, LocalTime from, int minutes,
                                          int openStart, int openEnd, int buffer,
                                          List<ClassSlot> slots, List<RoomUpdate> updates) {
        List<int[]> busy = busyFrom(slots, updates);
        List<int[]> blocked = new ArrayList<>();
        for (int[] b : busy) {
            blocked.add(new int[]{Math.max(b[0] - buffer, openStart), Math.min(b[1] + buffer, openEnd)});
        }
        List<int[]> free = gaps(merge(blocked), openStart, openEnd);

        List<Dtos.Window> windows = new ArrayList<>();
        for (int[] f : free) {
            if (f[1] - f[0] >= Math.max(5, buffer)) {
                windows.add(new Dtos.Window(mm(f[0]), mm(f[1]), f[1] - f[0]));
            }
        }

        Integer fromMin = from == null ? null : toMinutes(from);
        boolean matches = false;
        if (fromMin != null) {
            for (int[] f : free) {
                if (f[0] <= fromMin && f[1] >= fromMin + minutes) { matches = true; break; }
            }
        }

        String status = statusAt(fromMin, busy, matches, free);
        String note = switch (status) {
            case "IN_USE" -> "In use right now";
            case "AVAILABLE_LATER" -> "Free later today";
            case "AVAILABLE" -> "Free now";
            default -> "No free window long enough today";
        };

        return new Dtos.RoomAvailability(
                RoomService.view(room),
                status, matches, windows, nextBusyStart(busy, fromMin), note);
    }

    /** 取当天真实占用：课表 + 添加使用 − 释放 */
    private List<int[]> busyIntervals(Long roomId, LocalDate date) {
        return busyFrom(
                slotRepo.findByRoomIdAndDayOfWeekOrderByStartTimeAsc(roomId, date.getDayOfWeek().getValue()),
                updateRepo.findByRoomIdAndSlotDateAndActiveTrue(roomId, date));
    }

    /**
     * 纯计算，不碰数据库：把学校课表和临时变更折成占用的区间。
     *
     * 优先级：学校课表 > 管理员。两类输入不会同时产生占用，所以这里不需要"冲突消解"：
     *   - 课表里的课是权威的，管理员的变更挖不掉它；
     *   - 释放只能撤销管理员自己加的占用，碰不到课表；
     *   - 学生没有写路径，产生不了占用。
     */
    static List<int[]> busyFrom(List<ClassSlot> slots, List<RoomUpdate> updates) {
        List<int[]> timetable = new ArrayList<>();
        for (ClassSlot s : slots) {
            timetable.add(new int[]{toMinutes(s.getStartTime()), toMinutes(s.getEndTime())});
        }
        List<int[]> added = new ArrayList<>();
        List<int[]> released = new ArrayList<>();
        for (RoomUpdate u : updates) {
            if (!isEffective(u)) continue;
            int[] iv = new int[]{toMinutes(u.getStartTime()), toMinutes(u.getEndTime())};
            if (u.getChangeType() == RoomUpdate.ChangeType.RELEASE) {
                released.add(iv);
            } else {
                added.add(iv);
            }
        }
        List<int[]> busy = new ArrayList<>(timetable);
        busy.addAll(subtract(added, released));   // 释放只作用于管理员自己加的占用
        return merge(busy);
    }

    /** 撤销过的、或已经过了有效期的变更，一律不再生效 */
    static boolean isEffective(RoomUpdate u) {
        if (Boolean.FALSE.equals(u.getActive())) return false;
        return u.getExpiresAt() == null || !u.getExpiresAt().isBefore(java.time.LocalDateTime.now());
    }

    /**
     * 状态只看"已经折算过"的占用区间（busy）。
     * 以前这里还额外扫一遍原始变更列表，结果被 RELEASE 撤销掉的变更依然报 IN_USE，
     * 与同一响应里给出的空闲窗口自相矛盾。busy 已经等于 课表 ∪ (添加 − 撤销)，所以够用。
     */
    private String statusAt(Integer fromMin, List<int[]> busy, boolean matches, List<int[]> free) {
        if (fromMin != null) {
            for (int[] b : busy) {
                if (fromMin >= b[0] && fromMin < b[1]) return "IN_USE";
            }
            if (matches) return "AVAILABLE";
            for (int[] f : free) {
                if (f[0] > fromMin) return "AVAILABLE_LATER";
            }
            return "NONE";
        }
        return matches ? "AVAILABLE" : "NONE";
    }

    private String nextBusyStart(List<int[]> busy, Integer fromMin) {
        int ref = fromMin == null ? -1 : fromMin;
        for (int[] b : busy) {
            if (b[0] > ref) return mm(b[0]);
        }
        return null;
    }

    // ---------------------------------------------------------------- helpers

    static List<int[]> merge(List<int[]> in) {
        List<int[]> sorted = new ArrayList<>(in);
        sorted.sort(Comparator.comparingInt(a -> a[0]));
        List<int[]> out = new ArrayList<>();
        for (int[] iv : sorted) {
            if (iv[0] >= iv[1]) continue;
            if (!out.isEmpty() && iv[0] <= out.get(out.size() - 1)[1]) {
                out.get(out.size() - 1)[1] = Math.max(out.get(out.size() - 1)[1], iv[1]);
            } else {
                out.add(new int[]{iv[0], iv[1]});
            }
        }
        return out;
    }

    /** base 里挖掉 cut 的部分，返回碎片 */
    static List<int[]> subtract(List<int[]> base, List<int[]> cut) {
        List<int[]> result = new ArrayList<>();
        List<int[]> cuts = merge(cut);
        for (int[] b : merge(base)) {
            List<int[]> pieces = new ArrayList<>();
            pieces.add(new int[]{b[0], b[1]});
            for (int[] c : cuts) {
                List<int[]> next = new ArrayList<>();
                for (int[] p : pieces) {
                    if (c[1] <= p[0] || c[0] >= p[1]) {          // 不相交
                        next.add(p);
                    } else {
                        if (p[0] < c[0]) next.add(new int[]{p[0], c[0]});
                        if (c[1] < p[1]) next.add(new int[]{c[1], p[1]});
                    }
                }
                pieces = next;
            }
            result.addAll(pieces);
        }
        return merge(result);
    }

    static List<int[]> gaps(List<int[]> busy, int lo, int hi) {
        List<int[]> out = new ArrayList<>();
        int cursor = lo;
        for (int[] b : merge(busy)) {
            if (b[0] > cursor) out.add(new int[]{cursor, Math.min(b[0], hi)});
            cursor = Math.max(cursor, b[1]);
        }
        if (cursor < hi) out.add(new int[]{cursor, hi});
        return out;
    }

    private static int longest(List<Dtos.Window> ws) {
        return ws.stream().mapToInt(Dtos.Window::minutes).max().orElse(0);
    }

    static int toMinutes(LocalTime t) { return t.getHour() * 60 + t.getMinute(); }

    static String mm(int minutes) {
        return String.format("%02d:%02d", minutes / 60, minutes % 60);
    }
}
