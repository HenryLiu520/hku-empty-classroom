/*
 * Empty Classroom Plan — HKU COMP1110 Group 08
 * Author: LIU Haoran (u3686264) · 2026
 * Signature: EC-COMP1110-G08-u3686264-2026
 */
package hku.ec.service;

import hku.ec.domain.AuditEntry;
import hku.ec.domain.Room;
import hku.ec.domain.RoomReview;
import hku.ec.repo.AuditRepository;
import hku.ec.repo.RoomRepository;
import hku.ec.repo.RoomReviewRepository;
import hku.ec.web.Dtos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 房间评价：一个人对一间房一条，可改可删。
 * 评价对象是**房间**，不是人；数据结构里也没有"评价某位老师"的位置。
 */
@Service
public class ReviewService {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int MAX_BODY = 600;

    private final RoomReviewRepository reviews;
    private final RoomRepository rooms;
    private final AuditRepository audit;

    public ReviewService(RoomReviewRepository reviews, RoomRepository rooms, AuditRepository audit) {
        this.reviews = reviews;
        this.rooms = rooms;
        this.audit = audit;
    }

    // ---------------------------------------------------------------- 写

    @Transactional
    public Dtos.ReviewView upsert(String roomCode, Integer rating, String body, String actor) {
        Room room = room(roomCode);
        validate(rating, body);

        RoomReview r = reviews.findByRoomIdAndCreatedBy(room.getId(), actor).orElseGet(RoomReview::new);
        boolean isNew = r.getId() == null;
        r.setRoomId(room.getId());
        r.setRating(rating);
        r.setBody(body.trim());
        r.setCreatedBy(actor);
        LocalDateTime now = LocalDateTime.now();
        if (isNew) r.setCreatedAt(now);
        r.setUpdatedAt(now);
        RoomReview saved = reviews.save(r);

        audit.save(new AuditEntry(isNew ? "CREATE_REVIEW" : "UPDATE_REVIEW", "room_reviews", saved.getId(),
                actor, "%s %d/5 %s".formatted(room.getCode(), rating, isNew ? "(new)" : "(edited)")));
        return view(saved, room.getCode(), actor);
    }

    /**
     * 删评价。
     * 权限分离：任何人都只能删**自己的**；只有超级管理员能删别人的。
     */
    @Transactional
    public void delete(Long id, String actor, boolean canDeleteAny) {
        RoomReview r = reviews.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Review not found: " + id));
        if (!canDeleteAny && !r.getCreatedBy().equals(actor)) {
            throw new IllegalArgumentException("You can only delete your own review");
        }
        String owner = r.getCreatedBy();
        reviews.delete(r);
        audit.save(new AuditEntry("DELETE_REVIEW", "room_reviews", id, actor,
                "deleted review by %s%s".formatted(owner,
                        canDeleteAny && !owner.equals(actor) ? " (as superadmin)" : "")));
    }

    // ---------------------------------------------------------------- 读

    /** sort: time（默认，最新在前）| rating（星级高的在前） */
    @Transactional(readOnly = true)
    public Dtos.ReviewList list(String roomCode, String sort, String viewer) {
        Room room = room(roomCode);
        List<RoomReview> all = reviews.findByRoomIdOrderByCreatedAtDesc(room.getId());

        List<RoomReview> sorted = new ArrayList<>(all);
        if ("rating".equalsIgnoreCase(sort)) {
            sorted.sort(Comparator.comparing(RoomReview::getRating).reversed()
                    .thenComparing(RoomReview::getCreatedAt, Comparator.reverseOrder()));
        }

        double avg = all.stream().mapToInt(RoomReview::getRating).average().orElse(0.0);
        List<Dtos.ReviewView> items = sorted.stream().map(r -> view(r, room.getCode(), viewer)).toList();
        return new Dtos.ReviewList(room.getCode(), Math.round(avg * 10.0) / 10.0, all.size(),
                "rating".equalsIgnoreCase(sort) ? "rating" : "time", items);
    }

    /** 每间房的平均分与条数（房间列表上直接显示星级用） */
    @Transactional(readOnly = true)
    public Map<String, Dtos.ReviewSummary> summary() {
        Map<Long, String> codes = new HashMap<>();
        for (Room r : rooms.findAll()) codes.put(r.getId(), r.getCode());

        Map<String, Dtos.ReviewSummary> out = new HashMap<>();
        for (Object[] row : reviews.summaryByRoom()) {
            Long roomId = (Long) row[0];
            double avg = Math.round(((Number) row[1]).doubleValue() * 10.0) / 10.0;
            int count = ((Number) row[2]).intValue();
            String code = codes.get(roomId);
            if (code != null) out.put(code, new Dtos.ReviewSummary(code, avg, count));
        }
        return out;
    }

    // ---------------------------------------------------------------- internal

    private Room room(String code) {
        return rooms.findByCode(code).orElseThrow(() -> new IllegalArgumentException("unknown room: " + code));
    }

    private void validate(Integer rating, String body) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Please give a rating from 1 to 5 stars");
        }
        String text = body == null ? "" : body.trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("Please write a few words along with the rating");
        }
        if (text.length() > MAX_BODY) {
            throw new IllegalArgumentException("Please keep the review under " + MAX_BODY + " characters");
        }
    }

    private Dtos.ReviewView view(RoomReview r, String roomCode, String viewer) {
        return new Dtos.ReviewView(r.getId(), roomCode, r.getRating(), r.getBody(), r.getCreatedBy(),
                r.getCreatedAt().format(TS), r.getUpdatedAt().format(TS),
                viewer != null && viewer.equals(r.getCreatedBy()));
    }
}
