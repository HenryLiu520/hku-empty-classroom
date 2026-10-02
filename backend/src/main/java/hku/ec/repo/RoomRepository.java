package hku.ec.repo;

import hku.ec.domain.Room;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findAllByOrderByCodeAsc();
    List<Room> findByBuildingOrderByCodeAsc(String building);
    Optional<Room> findByCode(String code);
}
