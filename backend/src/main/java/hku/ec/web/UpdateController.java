/*
 * Empty Classroom Plan — HKU COMP1110 Group 08
 * Author: LIU Haoran (u3686264) · 2026
 * Signature: EC-COMP1110-G08-u3686264-2026
 */
package hku.ec.web;

import hku.ec.domain.AuditEntry;
import hku.ec.repo.AuditRepository;
import hku.ec.service.AuthService;
import hku.ec.service.UpdateService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api")
public class UpdateController {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final UpdateService updates;
    private final AuthService auth;
    private final AuditRepository audit;

    public UpdateController(UpdateService updates, AuthService auth, AuditRepository audit) {
        this.updates = updates;
        this.auth = auth;
        this.audit = audit;
    }

    /** 教师端：提交临时变更 */
    @PostMapping("/updates")
    public ResponseEntity<?> create(@RequestBody Dtos.UpdateRequest req,
                                    @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = Tokens.from(authHeader);
        AuthService.Session s;
        try {
            s = auth.require(token, "admin");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(e.getMessage().equals("FORBIDDEN") ? 403 : 401)
                    .body(new Dtos.ApiError(e.getMessage(), "Only admins can change room use time"));
        }
        try {
            updates.create(req, s.username());
            return ResponseEntity.ok(updates.mine(s.username()));
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage() == null ? "" : e.getMessage();
            String code = msg.startsWith("Timetable has priority") ? "TIMETABLE_PRIORITY" : "BAD_REQUEST";
            return ResponseEntity.badRequest().body(new Dtos.ApiError(code, msg));
        }
    }

    /** 教师端：我提交过的变更 */
    @GetMapping("/updates/mine")
    public ResponseEntity<?> mine(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = Tokens.from(authHeader);
        try {
            AuthService.Session s = auth.require(token, "admin");
            return ResponseEntity.ok(updates.mine(s.username()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).body(new Dtos.ApiError(e.getMessage(), "Admins only"));
        }
    }

    /** 管理人员：所有变更（跨账号可见，便于交接与纠错） */
    @GetMapping("/updates")
    public ResponseEntity<?> all(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = Tokens.from(authHeader);
        try {
            auth.require(token, "admin");
            return ResponseEntity.ok(updates.all());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).body(new Dtos.ApiError(e.getMessage(), "Admins only"));
        }
    }

    @DeleteMapping("/updates/{id}")
    public ResponseEntity<?> cancel(@PathVariable Long id,
                                    @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = Tokens.from(authHeader);
        try {
            AuthService.Session s = auth.require(token, "admin");
            // 管理员只能撤销自己提交的；超级管理员可以撤销任何人的
            updates.cancel(id, s.username(), AuthService.atLeast(s.role(), "superadmin"));
            return ResponseEntity.ok(updates.mine(s.username()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new Dtos.ApiError("BAD_REQUEST", e.getMessage()));
        } catch (IllegalStateException e) {
            boolean notOwner = "FORBIDDEN_OWNER".equals(e.getMessage());
            return ResponseEntity.status(403).body(new Dtos.ApiError(e.getMessage(),
                    notOwner ? "Only a superadmin can cancel another administrator's change"
                             : "Admins only"));
        }
    }

    /** 审计日志（教师端可见） */
    @GetMapping("/audit")
    public ResponseEntity<?> audit(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = Tokens.from(authHeader);
        try {
            auth.require(token, "admin");
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).body(new Dtos.ApiError(e.getMessage(), "Admins only"));
        }
        List<Dtos.AuditView> rows = audit.findTop100ByOrderByCreatedAtDesc().stream()
                .map((AuditEntry a) -> new Dtos.AuditView(a.getId(), a.getAction(), a.getEntity(), a.getEntityId(),
                        a.getActor(), a.getDetail(), a.getCreatedAt().format(TS)))
                .toList();
        return ResponseEntity.ok(rows);
    }
}
