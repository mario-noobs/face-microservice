package com.mario.backend.testutil;

import com.mario.backend.auth.entity.Auth;
import com.mario.backend.audit.entity.AuditLog;
import com.mario.backend.face.entity.FaceFeature;
import com.mario.backend.face.entity.FaceImage;
import com.mario.backend.rbac.entity.Permission;
import com.mario.backend.rbac.entity.Role;
import com.mario.backend.users.entity.User;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static com.mario.backend.testutil.TestConstants.*;

public final class TestDataFactory {

    private TestDataFactory() {}

    public static User createUser() {
        return createUser(USER_ID, USER_EMAIL);
    }

    public static User createUser(Long id, String email) {
        return User.builder()
                .id(id)
                .firstName(USER_FIRST_NAME)
                .lastName(USER_LAST_NAME)
                .email(email)
                .status(User.UserStatus.activated)
                .role(createBasicUserRole())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public static User createUserWithRole(Long id, String email, Role role) {
        User user = createUser(id, email);
        user.setRole(role);
        return user;
    }

    public static Auth createAuth() {
        return createAuth(USER_ID, USER_EMAIL);
    }

    public static Auth createAuth(Long userId, String email) {
        return Auth.builder()
                .id(1L)
                .userId(userId)
                .email(email)
                .salt("$2a$10$abcdefghijklmnopqrstuv")
                .password("$2a$10$hashedpasswordhere")
                .authType(Auth.AuthType.email_password)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public static Role createBasicUserRole() {
        Set<Permission> permissions = new HashSet<>();
        permissions.add(createPermission(1L, "user:read_self", "user"));
        permissions.add(createPermission(2L, "user:update_self", "user"));
        permissions.add(createPermission(3L, "face:check", "face"));
        permissions.add(createPermission(4L, "audit:read_self", "audit"));

        return Role.builder()
                .id(3L)
                .name(ROLE_BASIC_USER)
                .description("Basic user role")
                .isDefault(true)
                .permissions(permissions)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public static Role createPremiumUserRole() {
        Set<Permission> permissions = new HashSet<>();
        permissions.add(createPermission(1L, "user:read_self", "user"));
        permissions.add(createPermission(2L, "user:update_self", "user"));
        permissions.add(createPermission(5L, "face:register", "face"));
        permissions.add(createPermission(6L, "face:recognize", "face"));
        permissions.add(createPermission(7L, "face:delete", "face"));
        permissions.add(createPermission(3L, "face:check", "face"));
        permissions.add(createPermission(4L, "audit:read_self", "audit"));

        return Role.builder()
                .id(2L)
                .name(ROLE_PREMIUM_USER)
                .description("Premium user role")
                .isDefault(false)
                .permissions(permissions)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public static Role createSuperAdminRole() {
        Set<Permission> permissions = new HashSet<>();
        long id = 1L;
        for (String permName : SUPERADMIN_PERMISSIONS) {
            String service = permName.split(":")[0];
            permissions.add(createPermission(id++, permName, service));
        }

        return Role.builder()
                .id(1L)
                .name(ROLE_SUPERADMIN)
                .description("Super administrator")
                .isDefault(false)
                .permissions(permissions)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public static Permission createPermission(Long id, String name, String service) {
        return Permission.builder()
                .id(id)
                .name(name)
                .description("Permission: " + name)
                .service(service)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static FaceFeature createFaceFeature(Long userId) {
        return FaceFeature.builder()
                .id(1L)
                .userId(userId)
                .featureVector(SAMPLE_FACE_ENCODING)
                .status(FaceFeature.FaceStatus.active)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public static FaceImage createFaceImage(Long userId) {
        return FaceImage.builder()
                .id(1L)
                .userId(userId)
                .imagePath("1/test-image.jpg")
                .bucketName("face-images")
                .objectName("1/test-image.jpg")
                .imageHash("abc123hash")
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static AuditLog createAuditLog(Long userId) {
        return AuditLog.builder()
                .id(1L)
                .requestId("req-123")
                .userId(userId)
                .method("GET")
                .path("/api/v1/face/is-registered")
                .statusCode(200)
                .clientIp("127.0.0.1")
                .userAgent("Mozilla/5.0")
                .durationMs(50L)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
