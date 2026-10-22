/*
 * Empty Classroom Plan — HKU COMP1110 Group 08
 * Author: LIU Haoran (u3686264) · 2026
 * Signature: EC-COMP1110-G08-u3686264-2026
 */
package hku.ec.service;

import hku.ec.domain.AuditEntry;
import hku.ec.domain.ReviewPhoto;
import hku.ec.domain.RoomReview;
import hku.ec.repo.AuditRepository;
import hku.ec.repo.ReviewPhotoRepository;
import hku.ec.repo.RoomReviewRepository;
import hku.ec.web.Dtos;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 评价照片。
 *
 * 三条设计决定（报告里可以写）：
 * 1. **文件在磁盘、指针在库里** —— 不把图片塞进 bytea，列表查询不会被大字段拖慢，备份也不用搬文件。
 * 2. **文件名由服务器生成**（UUID + 由内容嗅探出的扩展名），用户原始文件名一律丢弃 ——
 *    这样既不会有人用 "../../etc/passwd" 之类做路径穿越，也不会互相覆盖。
 * 3. **类型以字节为准，不信浏览器声明的 Content-Type** —— 声明的类型可以随便伪造，
 *    我们只按文件头判断是 JPEG / PNG / WebP，判断不出来就拒收。
 */
@Service
public class ReviewPhotoService {

    /** 一条评价最多几张照片 */
    public static final int MAX_PER_REVIEW = 3;
    /** 单张上限 3 MB */
    public static final int MAX_BYTES = 3 * 1024 * 1024;

    private final ReviewPhotoRepository photos;
    private final RoomReviewRepository reviews;
    private final AuditRepository audit;
    private final Path dir;

    public ReviewPhotoService(ReviewPhotoRepository photos,
                              RoomReviewRepository reviews,
                              AuditRepository audit,
                              @Value("${app.upload-dir:./uploads/review-photos}") String uploadDir) {
        this.photos = photos;
        this.reviews = reviews;
        this.audit = audit;
        this.dir = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    /** 一张可以对外返回的照片：id + 取图 URL + 字节数 */
    public record PhotoFile(Path path, String contentType) { }

    // ---------------------------------------------------------------- 写

    @Transactional
    public Dtos.PhotoView add(Long reviewId, String actor, MultipartFile file, boolean canAny) {
        RoomReview review = reviews.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found: " + reviewId));
        if (!canAny && !review.getCreatedBy().equals(actor)) {
            throw new IllegalArgumentException("You can only add photos to your own review");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Choose an image first");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new IllegalArgumentException("Each photo must be 3 MB or smaller");
        }
        if (photos.countByReviewId(reviewId) >= MAX_PER_REVIEW) {
            throw new IllegalArgumentException("A review can carry at most " + MAX_PER_REVIEW + " photos");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        String type = sniff(bytes);          // 抛异常 = 不是这三种图片，拒收
        String fileName = UUID.randomUUID() + extensionOf(type);

        try {
            Files.createDirectories(dir);
            Files.write(dir.resolve(fileName), bytes, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        ReviewPhoto p = new ReviewPhoto();
        p.setReviewId(reviewId);
        p.setFileName(fileName);
        p.setContentType(type);
        p.setByteSize(bytes.length);
        p.setCreatedAt(LocalDateTime.now());
        ReviewPhoto saved = photos.save(p);

        audit.save(new AuditEntry("ADD_REVIEW_PHOTO", "review_photos", saved.getId(), actor,
                "review %d, %d bytes, %s".formatted(reviewId, bytes.length, type)));

        return view(saved);
    }

    /** 删一张照片（作者本人，或超级管理员） */
    @Transactional
    public void delete(Long photoId, String actor, boolean canAny) {
        ReviewPhoto p = photos.findById(photoId)
                .orElseThrow(() -> new IllegalArgumentException("Photo not found: " + photoId));
        RoomReview review = reviews.findById(p.getReviewId())
                .orElseThrow(() -> new IllegalArgumentException("Review not found: " + p.getReviewId()));
        if (!canAny && !review.getCreatedBy().equals(actor)) {
            throw new IllegalArgumentException("You can only delete photos on your own review");
        }
        photos.delete(p);
        deleteFile(p.getFileName());
        audit.save(new AuditEntry("DELETE_REVIEW_PHOTO", "review_photos", photoId, actor,
                "review %d".formatted(p.getReviewId())));
    }

    /** 评价被删时，把它名下的照片行与文件一起清掉 */
    @Transactional
    public void deleteAllForReview(Long reviewId) {
        List<ReviewPhoto> all = photos.findByReviewIdOrderByIdAsc(reviewId);
        photos.deleteByReviewId(reviewId);
        for (ReviewPhoto p : all) deleteFile(p.getFileName());
    }

    // ---------------------------------------------------------------- 读

    @Transactional(readOnly = true)
    public Map<Long, List<Dtos.PhotoView>> byReview(List<Long> reviewIds) {
        Map<Long, List<Dtos.PhotoView>> out = new HashMap<>();
        if (reviewIds == null || reviewIds.isEmpty()) return out;
        for (ReviewPhoto p : photos.findByReviewIdInOrderByIdAsc(reviewIds)) {
            out.computeIfAbsent(p.getReviewId(), k -> new ArrayList<>()).add(view(p));
        }
        return out;
    }

    /** 取图：返回文件与类型；文件不在磁盘上就当 404 处理 */
    @Transactional(readOnly = true)
    public PhotoFile load(Long photoId) {
        ReviewPhoto p = photos.findById(photoId)
                .orElseThrow(() -> new IllegalArgumentException("Photo not found: " + photoId));
        Path path = dir.resolve(p.getFileName()).normalize();
        if (!path.startsWith(dir) || !Files.isReadable(path)) {
            throw new IllegalArgumentException("Photo file is missing: " + photoId);
        }
        return new PhotoFile(path, p.getContentType());
    }

    // ---------------------------------------------------------------- internal

    private Dtos.PhotoView view(ReviewPhoto p) {
        return new Dtos.PhotoView(p.getId(), "/api/review-photos/" + p.getId(), p.getByteSize());
    }

    private void deleteFile(String fileName) {
        try {
            Files.deleteIfExists(dir.resolve(fileName).normalize());
        } catch (IOException e) {
            // 文件删不掉不该让整个删除失败：行已经删了，最多留一个孤儿文件
            System.err.println("Could not delete review photo file: " + fileName);
        }
    }

    /**
     * 按文件头判断类型。只接受 JPEG / PNG / WebP —— 这三种浏览器都能直接显示，
     * 而且都不需要额外解析器。
     */
    static String sniff(byte[] b) {
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && (b[4] & 0xFF) == 0x0D && (b[5] & 0xFF) == 0x0A && (b[6] & 0xFF) == 0x1A && (b[7] & 0xFF) == 0x0A) {
            return "image/png";
        }
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "image/webp";
        }
        throw new IllegalArgumentException("Only JPEG, PNG or WebP images are accepted");
    }

    private static String extensionOf(String contentType) {
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }
}
