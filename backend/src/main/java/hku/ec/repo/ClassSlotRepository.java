package hku.ec.repo;

import hku.ec.domain.ClassSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassSlotRepository extends JpaRepository<ClassSlot, Long> {
    List<ClassSlot> findByRoomIdAndDayOfWeekOrderByStartTimeAsc(Long roomId, Integer dayOfWeek);
    List<ClassSlot> findByDayOfWeek(Integer dayOfWeek);
}
