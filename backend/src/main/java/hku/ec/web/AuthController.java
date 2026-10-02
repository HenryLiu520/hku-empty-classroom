/*
 * Empty Classroom Plan — HKU COMP1110 Group 08
 * Author: LIU Haoran (u3686264) · 2026
 * Signature: EC-COMP1110-G08-u3686264-2026
 */
package hku.ec.web;

import hku.ec.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody Dtos.LoginRequest req) {
        Optional<AuthService.Session> s = auth.login(req.username(), req.password());
        if (s.isEmpty()) {
            return ResponseEntity.status(401).body(new Dtos.ApiError("BAD_CREDENTIALS", "Wrong username or password"));
        }
        AuthService.Session session = s.get();
        String token = auth.issueTokenFor(session.username(), session.displayName(), session.role());
        return ResponseEntity.ok(new Dtos.LoginResponse(token, session.username(), session.displayName(), session.role()));
    }

    /**
     * 自助注册：邮箱 + 密码。原型不发验证邮件，但邮箱后缀必须是 HKU 的
     * （@hku.hk 或任意子域，如 @connect.hku.hk）；新账号一律是 user 角色。
     */
    @PostMapping("/auth/register")
    public ResponseEntity<?> register(@RequestBody Dtos.RegisterRequest req) {
        if (!AuthService.isHkuEmail(req.email())) {
            return ResponseEntity.badRequest().body(new Dtos.ApiError("BAD_EMAIL",
                    "Use your HKU email address: it has to end in hku.hk, for example @connect.hku.hk"));
        }
        String username = req.username() == null ? "" : req.username().trim();
        if (username.length() < 3) {
            return ResponseEntity.badRequest().body(new Dtos.ApiError("BAD_USERNAME",
                    "Username must be at least 3 characters"));
        }
        if (req.password() == null || req.password().length() < 6) {
            return ResponseEntity.badRequest().body(new Dtos.ApiError("BAD_PASSWORD",
                    "Password must be at least 6 characters"));
        }
        if (auth.usernameTaken(username)) {
            return ResponseEntity.status(409).body(new Dtos.ApiError("USERNAME_TAKEN",
                    "That username is already taken"));
        }
        AuthService.Session s = auth.register(username, req.email(), req.password());
        String token = auth.issueTokenFor(s.username(), s.displayName(), s.role());
        return ResponseEntity.status(201)
                .body(new Dtos.LoginResponse(token, s.username(), s.displayName(), s.role()));
    }

    @GetMapping("/auth/me")
    public ResponseEntity<?> me(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = Tokens.from(authHeader);
        return auth.resolve(token)
                .<ResponseEntity<?>>map(s -> ResponseEntity.ok(s))
                .orElseGet(() -> ResponseEntity.status(401).body(new Dtos.ApiError("UNAUTHORIZED", "Sign in first")));
    }

    /** 演示用小工具：两类账号的说明 */
    @GetMapping("/auth/demo-accounts")
    public ResponseEntity<?> demo() {
        return ResponseEntity.ok(java.util.Map.of(
                "user", "user1 / user123",
                "admin", "admin1 / admin123"));
    }
}
