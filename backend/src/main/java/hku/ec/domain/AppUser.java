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

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getDisplayName() { return displayName; }
    public String getRole() { return role; }
    public String getSalt() { return salt; }
    public String getPasswordHash() { return passwordHash; }
}
