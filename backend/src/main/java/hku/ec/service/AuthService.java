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
        if (role != null && !atLeast(s.role(), role)) {
            throw new IllegalStateException("FORBIDDEN");
        }
        return s;
    }

    /**
     * 角色等级：user(1) < admin(2) < superadmin(3)。
     * 判定用"至少"而不是"等于"，所以 superadmin 自动拥有它下面所有角色的权限。
     */
    private static final java.util.Map<String, Integer> RANK = java.util.Map.of(
            "user", 1, "admin", 2, "superadmin", 3);

    public static boolean atLeast(String role, String minRole) {
        // 要求一个不存在的角色时一律拒绝（否则把 "admin" 拼错就等于对所有人放行）
        Integer need = RANK.get(minRole == null ? "" : minRole.toLowerCase());
        if (need == null) return false;
        int have = RANK.getOrDefault(role == null ? "" : role.toLowerCase(), 0);
        return have >= need;
    }

    public static boolean isKnownRole(String role) {
        return role != null && RANK.containsKey(role.toLowerCase());
    }

    public boolean isAdmin(String token) {
        return resolve(token).map(s -> atLeast(s.role(), "admin")).orElse(false);
    }

    /**
     * 只接受 HKU 邮箱：@hku.hk，或任意子域（如 @connect.hku.hk）。
     * 判断方式是"域名部分等于 hku.hk 或以 .hku.hk 结尾"，所以 hku.hk.evil.com 这类过不了。
     */
    public static boolean isHkuEmail(String email) {
        if (email == null) return false;
        String e = email.trim().toLowerCase(java.util.Locale.ROOT);
        int at = e.indexOf('@');
        if (at <= 0 || at != e.lastIndexOf('@')) return false;
        String domain = e.substring(at + 1);
        return domain.equals("hku.hk") || domain.endsWith(".hku.hk");
    }

    public boolean usernameTaken(String username) {
        return username != null && users.findByUsername(username.trim()).isPresent();
    }

    /** 自助注册：邮箱 + 密码即可，不发验证邮件；角色固定 user（学生只能浏览） */
    public Session register(String username, String email, String password) {
        String salt = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        AppUser u = AppUser.registered(
                username.trim(),
                email.trim().toLowerCase(java.util.Locale.ROOT),
                salt,
                hash(salt, password));
        users.save(u);
        return new Session(u.getUsername(), u.getDisplayName(), u.getRole());
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
