package hku.ec.service;

import hku.ec.domain.AppUser;
import hku.ec.domain.AuditEntry;
import hku.ec.repo.AppUserRepository;
import hku.ec.repo.AuditRepository;
import hku.ec.web.Dtos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * 账户管理（仅超级管理员）。
 *
 * 权限分离：普通用户只浏览；管理员能锁定教室时间；只有超级管理员能管理账户，
 * 包括新增、删除、改资料、重置口令，以及提权 / 降权。
 *
 * 两条护栏（防止把自己锁在外面）：
 *   1. 不能改自己的角色、也不能删自己；
 *   2. 不能把最后一个超级管理员降权或删掉。
 */
@Service
public class AccountService {

    private final AppUserRepository users;
    private final AuditRepository audit;

    public AccountService(AppUserRepository users, AuditRepository audit) {
        this.users = users;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<Dtos.AccountView> list() {
        return users.findAll().stream()
                .sorted(Comparator.comparing(AppUser::getId))
                .map(this::view)
                .toList();
    }

    @Transactional
    public Dtos.AccountView create(Dtos.AccountRequest req, String actor) {
        String username = req.username() == null ? "" : req.username().trim();
        if (username.length() < 3) {
            throw new IllegalArgumentException("Username must be at least 3 characters");
        }
        if (users.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("That username is already taken: " + username);
        }
        String role = normaliseRole(req.role());
        String email = normaliseEmail(req.email());
        String password = req.password() == null ? "" : req.password();
        if (password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }

        String salt = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        AppUser u = AppUser.registered(username, email, salt, AuthService.hash(salt, password));
        u.setRole(role);
        u.setDisplayName(req.displayName() == null || req.displayName().isBlank()
                ? username : req.displayName().trim());
        AppUser saved = users.save(u);

        audit.save(new AuditEntry("CREATE_ACCOUNT", "app_user", saved.getId(), actor,
                "%s role=%s".formatted(username, role)));
        return view(saved);
    }

    @Transactional
    public Dtos.AccountView update(Long id, Dtos.AccountUpdateRequest req, String actor) {
        AppUser u = users.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + id));

        StringBuilder what = new StringBuilder();
        if (req.displayName() != null && !req.displayName().isBlank()) {
            u.setDisplayName(req.displayName().trim());
            what.append("displayName ");
        }
        if (req.email() != null) {
            u.setEmail(normaliseEmail(req.email()));
            what.append("email ");
        }
        if (req.password() != null && !req.password().isBlank()) {
            if (req.password().length() < 6) {
                throw new IllegalArgumentException("Password must be at least 6 characters");
            }
            String salt = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            u.setSalt(salt);
            u.setPasswordHash(AuthService.hash(salt, req.password()));
            what.append("password ");
        }
        if (req.role() != null && !req.role().isBlank()) {
            String role = normaliseRole(req.role());
            // 护栏 1：不能改自己的角色
            if (u.getUsername().equals(actor)) {
                throw new IllegalArgumentException("You cannot change your own role");
            }
            // 护栏 2：不能把最后一个超级管理员降权
            if ("superadmin".equals(u.getRole()) && !"superadmin".equals(role) && countSuperadmins() <= 1) {
                throw new IllegalArgumentException("There must be at least one superadmin left");
            }
            if (!role.equals(u.getRole())) {
                what.append("role ").append(u.getRole()).append("->").append(role).append(' ');
            }
            u.setRole(role);
        }

        AppUser saved = users.save(u);
        audit.save(new AuditEntry("UPDATE_ACCOUNT", "app_user", saved.getId(), actor,
                "%s %s".formatted(saved.getUsername(), what.toString().trim())));
        return view(saved);
    }

    @Transactional
    public void delete(Long id, String actor) {
        AppUser u = users.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + id));
        if (u.getUsername().equals(actor)) {
            throw new IllegalArgumentException("You cannot delete your own account");
        }
        if ("superadmin".equals(u.getRole()) && countSuperadmins() <= 1) {
            throw new IllegalArgumentException("There must be at least one superadmin left");
        }
        users.delete(u);
        audit.save(new AuditEntry("DELETE_ACCOUNT", "app_user", id, actor,
                "removed %s (role %s)".formatted(u.getUsername(), u.getRole())));
    }

    private long countSuperadmins() {
        return users.findAll().stream().filter(x -> "superadmin".equals(x.getRole())).count();
    }

    private String normaliseRole(String role) {
        String r = role == null ? "" : role.trim().toLowerCase();
        if (!AuthService.isKnownRole(r)) {
            throw new IllegalArgumentException("Unknown role: " + role + " (use user, admin or superadmin)");
        }
        return r;
    }

    /** 邮箱可选；给了就必须是 HKU 的地址 */
    private String normaliseEmail(String email) {
        if (email == null || email.isBlank()) return null;
        if (!AuthService.isHkuEmail(email)) {
            throw new IllegalArgumentException("Use an HKU email address, or leave it empty");
        }
        return email.trim().toLowerCase();
    }

    private Dtos.AccountView view(AppUser u) {
        return new Dtos.AccountView(u.getId(), u.getUsername(), u.getDisplayName(), u.getRole(), u.getEmail());
    }
}
