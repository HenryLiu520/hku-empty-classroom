package hku.ec.service;

import hku.ec.domain.AuditEntry;
import hku.ec.domain.Room;
import hku.ec.repo.AuditRepository;
import hku.ec.repo.RoomRepository;
import hku.ec.web.Dtos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** 房间本身的维护：目前只有设施（第一批三条）。改完盖"最后核实时间"并留审计。 */
@Service
public class RoomService {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final RoomRepository rooms;
    private final AuditRepository audit;

    public RoomService(RoomRepository rooms, AuditRepository audit) {
        this.rooms = rooms;
        this.audit = audit;
    }

    /** 房间出参（列表、可用性接口共用） */
    public static Dtos.RoomView view(Room r) {
        return new Dtos.RoomView(r.getId(), r.getCode(), r.getBuilding(), r.getFloor(), r.getCapacity(),
                r.getRoomType(), r.getSockets(), r.getSeatType(),
                r.getFacilitiesVerifiedAt() == null ? null : r.getFacilitiesVerifiedAt().format(TS));
    }

    @Transactional
    public Dtos.RoomView updateFacilities(String code, Dtos.FacilitiesRequest req, String actor) {
        Room room = rooms.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("unknown room: " + code));

        if (req.capacity() != null && (req.capacity() < 1 || req.capacity() > 500)) {
            throw new IllegalArgumentException("Seats total must be between 1 and 500");
        }
        String seatType = req.seatType() == null ? null : req.seatType().trim();
        if (seatType != null && seatType.length() > 60) {
            throw new IllegalArgumentException("Seat type must be under 60 characters");
        }

        StringBuilder change = new StringBuilder();
        if (req.capacity() != null) {
            change.append("seats ").append(room.getCapacity()).append("->").append(req.capacity()).append(' ');
            room.setCapacity(req.capacity());
        }
        if (req.sockets() != null) {
            change.append("sockets ").append(room.getSockets()).append("->").append(req.sockets()).append(' ');
            room.setSockets(req.sockets());
        }
        if (seatType != null && !seatType.isEmpty()) {
            change.append("seatType '").append(room.getSeatType()).append("'->'").append(seatType).append("' ");
            room.setSeatType(seatType);
        }
        if (change.isEmpty()) {
            throw new IllegalArgumentException("Nothing to update");
        }

        room.setFacilitiesVerifiedAt(LocalDateTime.now());
        Room saved = rooms.save(room);
        audit.save(new AuditEntry("UPDATE_FACILITIES", "rooms", saved.getId(), actor,
                saved.getCode() + " " + change.toString().trim()));
        return view(saved);
    }
}
