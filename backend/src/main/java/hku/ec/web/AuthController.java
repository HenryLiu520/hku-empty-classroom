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
