package hku.ec.service;

import hku.ec.domain.AppUser;
import hku.ec.repo.AppUserRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 原型级鉴权：登录后发一个 token（内存保存，24 小时有效）。
 * 口令校验用 SHA-256(salt:password)。注意：这不是生产级方案，报告里会写清楚。
 */
@Service
public class AuthService {

    public record Session(String username, String displayName, String role) { }

    private record Token(String username, String displayName, String role, Instant issuedAt) { }

    private final AppUserRepository users;
    private final Map<String, Token> tokens = new ConcurrentHashMap<>();

    public AuthService(AppUserRepository users) {
        this.users = users;
    }

    public Optional<Session> login(String username, String password) {
        return users.findByUsername(username)
                .filter(u -> hash(u.getSalt(), password).equals(u.getPasswordHash()))
                .map(u -> {
                    String token = UUID.randomUUID().toString();
                    tokens.put(token, new Token(u.getUsername(), u.getDisplayName(), u.getRole(), Instant.now()));
                    return new Session(u.getUsername(), u.getDisplayName(), u.getRole());
                });
    }

    public String issueTokenFor(String username, String displayName, String role) {
        String token = UUID.randomUUID().toString();
        tokens.put(token, new Token(username, displayName, role, Instant.now()));
        return token;
    }

    public Optional<Session> resolve(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        Token t = tokens.get(token);
        if (t == null) return Optional.empty();
        if (t.issuedAt().plusSeconds(24 * 3600).isBefore(Instant.now())) {
            tokens.remove(token);
            return Optional.empty();
        }
        return Optional.of(new Session(t.username(), t.displayName(), t.role()));
    }

    /** 需要某个角色，否则抛 403 */
    public Session require(String token, String role) {
        Session s = resolve(token)
                .orElseThrow(() -> new IllegalStateException("UNAUTHORIZED"));
        if (role != null && !role.equals(s.role())) {
            throw new IllegalStateException("FORBIDDEN");
        }
        return s;
    }

    public boolean isAdmin(String token) {
        return resolve(token).map(s -> "admin".equals(s.role())).orElse(false);
    }

    static String hash(String salt, String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest((salt + ":" + password).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public AppUser requireUser(String token) {
        Session s = require(token, null);
        return users.findByUsername(s.username()).orElseThrow();
    }
}
