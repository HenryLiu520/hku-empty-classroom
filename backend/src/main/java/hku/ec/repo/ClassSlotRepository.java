/*
 * Empty Classroom Plan — HKU COMP1110 Group 08
 * Author: LIU Haoran (u3686264) · 2026
 * Signature: EC-COMP1110-G08-u3686264-2026
 */
package hku.ec.repo;

import hku.ec.domain.ClassSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassSlotRepository extends JpaRepository<ClassSlot, Long> {
    List<ClassSlot> findByRoomIdAndDayOfWeekOrderByStartTimeAsc(Long roomId, Integer dayOfWeek);
    List<ClassSlot> findByDayOfWeek(Integer dayOfWeek);
}
