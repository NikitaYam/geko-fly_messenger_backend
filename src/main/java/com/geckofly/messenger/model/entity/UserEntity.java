package com.geckofly.messenger.model.entity;

import com.geckofly.messenger.model.enums.UserRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;


@Getter
@Setter
@Entity
@Table(
    name = "users",
    indexes = {
        @Index(name = "idx_users_uuid", columnList = "uuid"),
        @Index(name = "idx_users_role", columnList = "role")
    }
)
public class UserEntity extends BaseEntity implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Публичный идентификатор: передаётся на фронт, никогда не отображается пользователю.
    // Числовой id остаётся внутренним PK для FK-связей в БД.
    @UuidGenerator
    @Column(name = "uuid", nullable = false, unique = true, updatable = false)
    private UUID uuid;

    @Column(nullable = false, unique = true)
    private String login;

    @Email
    @Column(name = "user_email", nullable = true, unique = true)
    private String userEmail;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRole role = UserRole.USER;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "hide_push_previews", nullable = false)
    private boolean hidePushPreviews = false;

    /** Мягкое удаление (R10): анонимизированный «тумбстоун», логиниться нельзя. */
    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;

    public boolean isSuperAdmin() {
        return role == UserRole.SUPER_ADMIN;
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN || role == UserRole.SUPER_ADMIN;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        if (isAdmin()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        if (isSuperAdmin()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
        }
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return login;
    }

    /** Анонимизированный аккаунт отключён — Spring Security не пустит его. */
    @Override
    public boolean isEnabled() {
        return !deleted;
    }
}
