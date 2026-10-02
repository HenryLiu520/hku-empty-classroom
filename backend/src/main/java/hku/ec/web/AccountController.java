/*
 * Empty Classroom Plan — HKU COMP1110 Group 08
 * Author: LIU Haoran (u3686264) · 2026
 * Signature: EC-COMP1110-G08-u3686264-2026
 */
package hku.ec.web;

import hku.ec.service.AccountService;
import hku.ec.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 账户管理（仅超级管理员）。
 * 权限分离：普通用户只浏览；管理员能锁定教室时间；账户的增删改查和提权降权只给超级管理员。
 */
@RestController
@RequestMapping("/api")
public class AccountController {

    private final AccountService accounts;
    private final AuthService auth;

    public AccountController(AccountService accounts, AuthService auth) {
        this.accounts = accounts;
        this.auth = auth;
    }

    /** 账户列表 */
    @GetMapping("/accounts")
    public ResponseEntity<?> list(@RequestHeader(value = "Authorization", required = false) String header) {
        try {
            auth.require(Tokens.from(header), "superadmin");
            return ResponseEntity.ok(accounts.list());
        } catch (IllegalStateException e) {
            return denied(e);
        }
    }

    /** 新建账户（可以指定角色，等于直接给权限） */
    @PostMapping("/accounts")
    public ResponseEntity<?> create(@RequestBody Dtos.AccountRequest req,
                                    @RequestHeader(value = "Authorization", required = false) String header) {
        String actor;
        try {
            actor = auth.require(Tokens.from(header), "superadmin").username();
        } catch (IllegalStateException e) {
            return denied(e);
        }
        try {
            return ResponseEntity.status(201).body(accounts.create(req, actor));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new Dtos.ApiError("BAD_ACCOUNT", e.getMessage()));
        }
    }

    /** 改账户：资料 / 重置口令 / 提权降权（role 字段） */
    @PatchMapping("/accounts/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Dtos.AccountUpdateRequest req,
                                    @RequestHeader(value = "Authorization", required = false) String header) {
        String actor;
        try {
            actor = auth.require(Tokens.from(header), "superadmin").username();
        } catch (IllegalStateException e) {
            return denied(e);
        }
        try {
            return ResponseEntity.ok(accounts.update(id, req, actor));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new Dtos.ApiError("BAD_ACCOUNT", e.getMessage()));
        }
    }

    /** 删除账户 */
    @DeleteMapping("/accounts/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id,
                                    @RequestHeader(value = "Authorization", required = false) String header) {
        String actor;
        try {
            actor = auth.require(Tokens.from(header), "superadmin").username();
        } catch (IllegalStateException e) {
            return denied(e);
        }
        try {
            accounts.delete(id, actor);
            return ResponseEntity.ok(java.util.Map.of("deleted", id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new Dtos.ApiError("BAD_ACCOUNT", e.getMessage()));
        }
    }

    private ResponseEntity<?> denied(IllegalStateException e) {
        return "UNAUTHORIZED".equals(e.getMessage())
                ? ResponseEntity.status(401).body(new Dtos.ApiError("UNAUTHORIZED", "Sign in first"))
                : ResponseEntity.status(403).body(new Dtos.ApiError("FORBIDDEN",
                        "Only a superadmin can manage accounts"));
    }
}
