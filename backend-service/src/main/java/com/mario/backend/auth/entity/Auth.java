package com.mario.backend.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "auths")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Auth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_type", length = 20)
    @Builder.Default
    private AuthType authType = AuthType.email_password;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(length = 40)
    private String salt;

    @Column(length = 100)
    private String password;

    @Column(name = "facebook_id", length = 35)
    private String facebookId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum AuthType {
        email_password, gmail, facebook
    }
}
