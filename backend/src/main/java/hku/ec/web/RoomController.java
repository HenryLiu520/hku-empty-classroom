/*
 * Empty Classroom Plan — HKU COMP1110 Group 08
 * Author: LIU Haoran (u3686264) · 2026
 * Signature: EC-COMP1110-G08-u3686264-2026
 */
package hku.ec.web;

import hku.ec.repo.RoomRepository;
import hku.ec.service.AuthService;
import hku.ec.service.RoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class RoomController {

    private final RoomRepository rooms;
    private final RoomService roomService;
    private final AuthService auth;

    public RoomController(RoomRepository rooms, RoomService roomService, AuthService auth) {
        this.rooms = rooms;
        this.roomService = roomService;
        this.auth = auth;
    }

    @GetMapping("/rooms")
    public List<Dtos.RoomView> list(@RequestParam(required = false) String building) {
        var list = (building == null || building.isBlank())
                ? rooms.findAllByOrderByCodeAsc()
                : rooms.findByBuildingOrderByCodeAsc(building);
        return list.stream().map(RoomService::view).toList();
    }

    @GetMapping("/buildings")
    public ResponseEntity<?> buildings() {
        return ResponseEntity.ok(rooms.findAllByOrderByCodeAsc().stream()
                .map(r -> r.getBuilding()).distinct().sorted().toList());
    }

    /** 只有管理人员能改设施；改完盖上"最后核实时间"，并写审计 */
    @PatchMapping("/rooms/{code}")
    public ResponseEntity<?> updateFacilities(@PathVariable String code,
                                              @RequestBody Dtos.FacilitiesRequest req,
                                              @RequestHeader(value = "Authorization", required = false) String authHeader) {
        AuthService.Session s;
        try {
            s = auth.require(Tokens.from(authHeader), "admin");
        } catch (IllegalStateException e) {
            return ResponseEntity.status("FORBIDDEN".equals(e.getMessage()) ? 403 : 401)
                    .body(new Dtos.ApiError(e.getMessage(), "Only admins can edit room facilities"));
        }
        try {
            return ResponseEntity.ok(roomService.updateFacilities(code, req, s.username()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new Dtos.ApiError("BAD_FACILITIES", e.getMessage()));
        }
    }
}
