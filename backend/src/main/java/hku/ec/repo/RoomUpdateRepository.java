/*
 * Empty Classroom Plan — HKU COMP1110 Group 08
 * Author: LIU Haoran (u3686264) · 2026
 * Signature: EC-COMP1110-G08-u3686264-2026
 */
package hku.ec.repo;

import hku.ec.domain.RoomUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface RoomUpdateRepository extends JpaRepository<RoomUpdate, Long> {

    List<RoomUpdate> findByRoomIdAndSlotDateAndActiveTrue(Long roomId, LocalDate slotDate);

    /** 当天全部生效中的变更（批量取，避免逐房间查询） */
    List<RoomUpdate> findBySlotDateAndActiveTrue(LocalDate slotDate);

    List<RoomUpdate> findByCreatedByOrderByCreatedAtDesc(String createdBy);

    List<RoomUpdate> findAllByOrderByCreatedAtDesc();

    /** 管理人员可撤销任何一条变更，不限于自己提交的（撤销动作全部留审计） */
    @Modifying
    @Query("update RoomUpdate u set u.active = false where u.id = :id")
    int cancelById(@Param("id") Long id);

    @Modifying
    @Query("update RoomUpdate u set u.active = false where u.expiresAt is not null and u.expiresAt < CURRENT_TIMESTAMP and u.active = true")
    int expireOverdue();
}
