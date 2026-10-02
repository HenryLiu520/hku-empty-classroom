package hku.ec.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true)
    private String code;

    @Column(name = "building", nullable = false)
    private String building;

    @Column(name = "floor", nullable = false)
    private String floor;

    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @Column(name = "room_type", nullable = false)
    private String roomType;

    // —— 设施（第一批三条）：座位总数用 capacity；下面两条是新增的 ——
    @Column(name = "sockets")
    private Boolean sockets;

    @Column(name = "seat_type")
    private String seatType;

    /** 为空表示这些设施值是占位样例、还没人核实过 */
    @Column(name = "facilities_verified_at")
    private java.time.LocalDateTime facilitiesVerifiedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getBuilding() { return building; }
    public void setBuilding(String building) { this.building = building; }

    public String getFloor() { return floor; }
    public void setFloor(String floor) { this.floor = floor; }

    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }

    public String getRoomType() { return roomType; }
    public void setRoomType(String roomType) { this.roomType = roomType; }

    public Boolean getSockets() { return sockets; }
    public void setSockets(Boolean sockets) { this.sockets = sockets; }

    public String getSeatType() { return seatType; }
    public void setSeatType(String seatType) { this.seatType = seatType; }

    public java.time.LocalDateTime getFacilitiesVerifiedAt() { return facilitiesVerifiedAt; }
    public void setFacilitiesVerifiedAt(java.time.LocalDateTime v) { this.facilitiesVerifiedAt = v; }
}
