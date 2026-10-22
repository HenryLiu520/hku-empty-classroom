/*
 * Empty Classroom Plan — HKU COMP1110 Group 08
 * Author: LIU Haoran (u3686264) · 2026
 * Signature: EC-COMP1110-G08-u3686264-2026
 */
package hku.mock.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 单行元信息：接口由谁提供、哪个学期、数据什么时候生成、版本号。
 * 消费端判断"数据有多新"就靠 generatedAt；revision 变了说明课表变过。
 */
@Entity
@Table(name = "feed_meta")
public class FeedMeta {

    @Id
    private Integer id;

    @Column(nullable = false)
    private String provider;

    @Column(nullable = false)
    private String term;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Column(nullable = false)
    private Integer revision;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getTerm() { return term; }
    public void setTerm(String term) { this.term = term; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
    public Integer getRevision() { return revision; }
    public void setRevision(Integer revision) { this.revision = revision; }
}
