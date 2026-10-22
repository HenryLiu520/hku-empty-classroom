/*
 * Empty Classroom Plan — HKU COMP1110 Group 08
 * Author: LIU Haoran (u3686264) · 2026
 * Signature: EC-COMP1110-G08-u3686264-2026
 */
package hku.ec.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 一条评价上的一张照片。
 * 只存**指针**（磁盘文件名 + 类型 + 大小），图片本身在 uploads/review-photos/ 下。
 * file_name 由服务器生成（UUID + 扩展名），用户上传时的原始名字一律丢弃。
 */
@Entity
@Table(name = "review_photos")
public class ReviewPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "review_id", nullable = false)
    private Long reviewId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "byte_size", nullable = false)
    private Integer byteSize;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getReviewId() { return reviewId; }
    public void setReviewId(Long reviewId) { this.reviewId = reviewId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Integer getByteSize() { return byteSize; }
    public void setByteSize(Integer byteSize) { this.byteSize = byteSize; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
