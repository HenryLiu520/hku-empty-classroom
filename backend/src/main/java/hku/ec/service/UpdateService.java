package hku.ec.service;

import hku.ec.domain.AuditEntry;
import hku.ec.domain.Room;
import hku.ec.domain.RoomUpdate;
import hku.ec.repo.AuditRepository;
import hku.ec.repo.RoomRepository;
import hku.ec.repo.RoomUpdateRepository;
import hku.ec.web.Dtos;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** 管理端变更：写入、撤销、到期自动失效，全部留审计 */
@Service
public class UpdateService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MIN = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final RoomUpdateRepository updates;
    private final RoomRepository rooms;
    private final AuditRepository audit;

    public UpdateService(RoomUpdateRepository updates, RoomRepository rooms, AuditRepository audit) {
        this.updates = updates;
        this.rooms = rooms;
        this.audit = audit;
    }

    @Transactional
    public RoomUpdate create(Dtos.UpdateRequest req, String actor) {
        Room room = rooms.findById(req.roomId())
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + req.roomId()));

        LocalDate date = LocalDate.parse(req.date(), DATE);
        LocalTime start = LocalTime.parse(req.start(), MIN);
        LocalTime end = LocalTime.parse(req.end(), MIN);
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("End time must be after start time");
        }
        if (start.getMinute() != 0 || end.getMinute() != 0) {
            throw new IllegalArgumentException("Times must be on the hour, for example 14:00 to 16:00");
        }
        RoomUpdate.ChangeType type;
        try {
            type = RoomUpdate.ChangeType.valueOf(req.changeType());
        } catch (Exception e) {
            throw new IllegalArgumentException("Unknown change type: " + req.changeType());
        }

        RoomUpdate u = new RoomUpdate();
        u.setRoomId(room.getId());
        u.setChangeType(type);
        u.setSlotDate(date);
        u.setStartTime(start);
        u.setEndTime(end);
        u.setReason(req.reason());
        u.setCreatedBy(actor);
        u.setCreatedAt(LocalDateTime.now());
        u.setExpiresAt(req.expiresAt() == null || req.expiresAt().isBlank()
                ? null : LocalDateTime.parse(req.expiresAt(), TS));
        u.setActive(true);

        RoomUpdate saved = updates.save(u);
        audit.save(new AuditEntry("CREATE_UPDATE", "room_updates", saved.getId(), actor,
                "%s %s %s %s-%s %s".formatted(room.getCode(), type, date, req.start(), req.end(),
                        req.reason() == null ? "" : req.reason())));
        return saved;
    }

    @Transactional
    public void cancel(Long id, String actor) {
        RoomUpdate u = updates.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Update not found: " + id));
        String owner = u.getCreatedBy();
        int n = updates.cancelById(id);
        if (n == 0) {
            throw new IllegalArgumentException("Update not found: " + id);
        }
        audit.save(new AuditEntry("CANCEL_UPDATE", "room_updates", id, actor,
                "cancelled by %s (submitted by %s)".formatted(actor, owner)));
    }

    @Transactional(readOnly = true)
    public List<Dtos.UpdateView> mine(String actor) {
        return updates.findByCreatedByOrderByCreatedAtDesc(actor).stream().map(this::view).toList();
    }

    /** 管理人员视图：所有账号提交的变更 */
    @Transactional(readOnly = true)
    public List<Dtos.UpdateView> all() {
        return updates.findAllByOrderByCreatedAtDesc().stream().map(this::view).toList();
    }

    /** 每小时把过期变更置为失效（并留审计） */
    @Scheduled(fixedDelay = 3_600_000L, initialDelay = 60_000L)
    @Transactional
    public void expire() {
        int n = updates.expireOverdue();
        if (n > 0) {
            audit.save(new AuditEntry("EXPIRE_UPDATES", "room_updates", null, "system",
                    n + " update(s) expired"));
        }
    }

    private Dtos.UpdateView view(RoomUpdate u) {
        String roomCode = rooms.findById(u.getRoomId()).map(Room::getCode).orElse("?");
        String state = Boolean.TRUE.equals(u.getActive()) ? "Active" : "Expired";
        return new Dtos.UpdateView(u.getId(), roomCode, u.getChangeType().name(),
                u.getSlotDate().toString(), u.getStartTime().format(MIN), u.getEndTime().format(MIN),
                u.getReason(), u.getCreatedBy(), state);
    }
}
