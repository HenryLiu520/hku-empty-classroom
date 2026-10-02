/*
 * Empty Classroom Plan — HKU COMP1110 Group 08
 * Author: LIU Haoran (u3686264) · 2026
 * Signature: EC-COMP1110-G08-u3686264-2026
 */
package hku.ec.service;

import hku.ec.domain.AppUser;
import hku.ec.repo.AppUserRepository;
import hku.ec.web.Dtos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 角色分层与账户管理的规则。
 * 权限分离：user 只浏览 < admin 能锁定教室时间 < superadmin 还能管理账户（增删改查、提权降权）。
 */
@SpringBootTest
@Transactional
class AccountRulesTest {

    @Autowired private AccountService accounts;
    @Autowired private AppUserRepository users;

    @Test
    @DisplayName("1. 角色是分层的：superadmin ⊇ admin ⊇ user")
    void roleHierarchy() {
        assertTrue(AuthService.atLeast("superadmin", "admin"), "超级管理员能做管理员的事");
        assertTrue(AuthService.atLeast("superadmin", "user"));
        assertTrue(AuthService.atLeast("admin", "user"), "管理员能做普通用户的事");
        assertTrue(AuthService.atLeast("admin", "admin"));
        assertFalse(AuthService.atLeast("user", "admin"), "普通用户不能锁教室时间");
        assertFalse(AuthService.atLeast("admin", "superadmin"), "管理员不能管理账户");
        assertFalse(AuthService.atLeast("user", "superadmin"));
        assertFalse(AuthService.atLeast("user", "student"), "未知角色不产生任何权限");
        assertTrue(AuthService.isKnownRole("superadmin"));
        assertFalse(AuthService.isKnownRole("root"));
    }

    @Test
    @DisplayName("2. 超级管理员可以增、改（含提权降权）、删账户")
    void fullCrud() {
        Dtos.AccountView created = accounts.create(
                new Dtos.AccountRequest("newadmin", "New Admin", "new.admin@connect.hku.hk", "secret123", "admin"),
                "super1");
        assertEquals("admin", created.role());
        assertEquals("new.admin@connect.hku.hk", created.email());

        Dtos.AccountView promoted = accounts.update(created.id(),
                new Dtos.AccountUpdateRequest(null, null, null, "superadmin"), "super1");
        assertEquals("superadmin", promoted.role(), "提权");

        Dtos.AccountView demoted = accounts.update(created.id(),
                new Dtos.AccountUpdateRequest("Renamed Admin", null, null, "user"), "super1");
        assertEquals("user", demoted.role(), "降权");
        assertEquals("Renamed Admin", demoted.displayName());

        accounts.delete(created.id(), "super1");
        assertTrue(users.findById(created.id()).isEmpty(), "账户已删除");
    }

    @Test
    @DisplayName("3. 非法输入被拒绝：角色、重名、短口令、非 HKU 邮箱")
    void rejectsBadInput() {
        assertThrows(IllegalArgumentException.class, () -> accounts.create(
                new Dtos.AccountRequest("x1", null, null, "secret123", "root"), "super1"),
                "未知角色应被拒绝");
        assertThrows(IllegalArgumentException.class, () -> accounts.create(
                new Dtos.AccountRequest("user1", null, null, "secret123", "user"), "super1"),
                "重名应被拒绝");
        assertThrows(IllegalArgumentException.class, () -> accounts.create(
                new Dtos.AccountRequest("shortpw", null, null, "123", "user"), "super1"),
                "口令太短应被拒绝");
        assertThrows(IllegalArgumentException.class, () -> accounts.create(
                new Dtos.AccountRequest("badmail", null, "someone@gmail.com", "secret123", "user"), "super1"),
                "非 HKU 邮箱应被拒绝");
    }

    @Test
    @DisplayName("4. 护栏：不能改自己的角色、不能删自己、不能删掉最后一个超级管理员")
    void guardsHold() {
        AppUser me = users.findByUsername("super1").orElseThrow();

        assertThrows(IllegalArgumentException.class, () -> accounts.update(me.getId(),
                new Dtos.AccountUpdateRequest(null, null, null, "user"), "super1"),
                "不能给自己降权");
        assertThrows(IllegalArgumentException.class, () -> accounts.delete(me.getId(), "super1"),
                "不能删自己");

        long supers = users.findAll().stream().filter(u -> "superadmin".equals(u.getRole())).count();
        if (supers <= 1) {
            assertThrows(IllegalArgumentException.class, () -> accounts.update(me.getId(),
                    new Dtos.AccountUpdateRequest(null, null, null, "admin"), "someone-else"),
                    "最后一个超级管理员不能被降权");
        }
    }
}
