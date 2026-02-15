package com.mario.backend.integration;

import com.mario.backend.auth.dto.RegisterRequest;
import com.mario.backend.auth.repository.AuthRepository;
import com.mario.backend.common.dto.ApiResponse;
import com.mario.backend.rbac.entity.Permission;
import com.mario.backend.rbac.entity.Role;
import com.mario.backend.rbac.repository.PermissionRepository;
import com.mario.backend.rbac.repository.RoleRepository;
import com.mario.backend.testutil.IntegrationTestBase;
import com.mario.backend.users.dto.UpdateProfileRequest;
import com.mario.backend.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.mario.backend.testutil.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

class UserIntegrationTest extends IntegrationTestBase {
//
//    @Autowired private AuthRepository authRepository;
//    @Autowired private UserRepository userRepository;
//    @Autowired private RoleRepository roleRepository;
//    @Autowired private PermissionRepository permissionRepository;
//
//    private String accessToken;
//
//    @BeforeEach
//    void setUp() {
//        authRepository.deleteAll();
//        userRepository.deleteAll();
//
//        seedRolesAndPermissions();
//        accessToken = registerAndLogin("profile@test.com");
//    }
//
//    private void seedRolesAndPermissions() {
//        if (roleRepository.findByName("BASIC_USER").isEmpty()) {
//            Permission p1 = permissionRepository.save(Permission.builder().name("user:read_self").service("user").build());
//            Permission p2 = permissionRepository.save(Permission.builder().name("user:update_self").service("user").build());
//            Permission p3 = permissionRepository.save(Permission.builder().name("face:check").service("face").build());
//            Permission p4 = permissionRepository.save(Permission.builder().name("audit:read_self").service("audit").build());
//            Set<Permission> perms = new HashSet<>(Set.of(p1, p2, p3, p4));
//            roleRepository.save(Role.builder().name("BASIC_USER").description("Basic").isDefault(true).permissions(perms).build());
//        }
//    }
//
//    @SuppressWarnings("unchecked")
//    private String registerAndLogin(String email) {
//        RegisterRequest req = RegisterRequest.builder()
//                .firstName("Test").lastName("User")
//                .email(email).password("Password123")
//                .build();
//        ResponseEntity<ApiResponse> resp = restTemplate.postForEntity("/api/v1/user/register", req, ApiResponse.class);
//        var data = (java.util.Map<String, Object>) resp.getBody().getData();
//        var tokenInfo = (java.util.Map<String, Object>) data.get("access_token");
//        return (String) tokenInfo.get("token");
//    }
//
//    @Test
//    void getProfile_returnsUserData() {
//        HttpHeaders headers = authHeaders(accessToken);
//        ResponseEntity<ApiResponse> response = restTemplate.exchange(
//                "/api/v1/profile", HttpMethod.POST, new HttpEntity<>(headers), ApiResponse.class);
//
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
//        assertThat(response.getBody().getData()).isNotNull();
//    }
//
//    @Test
//    void updateProfile_updatesAndReturnsNewData() {
//        HttpHeaders headers = authHeaders(accessToken);
//        headers.setContentType(MediaType.APPLICATION_JSON);
//        UpdateProfileRequest updateReq = UpdateProfileRequest.builder()
//                .firstName("Updated").lastName("Name").phone("1234567890")
//                .build();
//
//        ResponseEntity<ApiResponse> response = restTemplate.exchange(
//                "/api/v1/profile", HttpMethod.PUT, new HttpEntity<>(updateReq, headers), ApiResponse.class);
//
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
//    }
//
//    @Test
//    void getProfile_withoutToken_returns401() {
//        ResponseEntity<ApiResponse> response = restTemplate.exchange(
//                "/api/v1/profile", HttpMethod.POST, new HttpEntity<>(new HttpHeaders()), ApiResponse.class);
//
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
//    }
}
