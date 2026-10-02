/*
 * Empty Classroom Plan — HKU COMP1110 Group 08
 * Author: LIU Haoran (u3686264) · 2026
 * Signature: EC-COMP1110-G08-u3686264-2026
 */
package hku.ec.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    /** STUDENT | TEACHER */
    @Column(name = "role", nullable = false)
    private String role;

    @Column(name = "salt", nullable = false)
    private String salt;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /** 自助注册时填的 HKU 邮箱；早期演示账号这一列是空的 */
    @Column(name = "email", length = 128)
    private String email;

    /** 自助注册：角色固定 user，学生只有浏览权限 */
    public static AppUser registered(String username, String email, String salt, String passwordHash) {
        AppUser u = new AppUser();
        u.username = username;
        u.displayName = username;
        u.role = "user";
        u.email = email;
        u.salt = salt;
        u.passwordHash = passwordHash;
        return u;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getDisplayName() { return displayName; }
    public String getRole() { return role; }
    public String getSalt() { return salt; }
    public String getPasswordHash() { return passwordHash; }
    public String getEmail() { return email; }

    // 管理员改账户 / 提权降权时用
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setRole(String role) { this.role = role; }
    public void setSalt(String salt) { this.salt = salt; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setEmail(String email) { this.email = email; }
}
