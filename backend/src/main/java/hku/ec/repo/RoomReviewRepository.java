package hku.ec.repo;

import hku.ec.domain.RoomReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RoomReviewRepository extends JpaRepository<RoomReview, Long> {

    List<RoomReview> findByRoomIdOrderByCreatedAtDesc(Long roomId);

    Optional<RoomReview> findByRoomIdAndCreatedBy(Long roomId, String createdBy);

    /** 每间房的平均分与条数，用于房间列表上直接显示星级 */
    @Query("select r.roomId, avg(r.rating), count(r) from RoomReview r group by r.roomId")
    List<Object[]> summaryByRoom();
}
