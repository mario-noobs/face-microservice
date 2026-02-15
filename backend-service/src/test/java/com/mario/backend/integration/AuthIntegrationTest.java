package com.mario.backend.integration;

import com.mario.backend.auth.dto.*;
import com.mario.backend.auth.repository.AuthRepository;
import com.mario.backend.common.dto.ApiResponse;
import com.mario.backend.rbac.entity.Permission;
import com.mario.backend.rbac.entity.Role;
import com.mario.backend.rbac.repository.PermissionRepository;
import com.mario.backend.rbac.repository.RoleRepository;
import com.mario.backend.testutil.IntegrationTestBase;
import com.mario.backend.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AuthIntegrationTest extends IntegrationTestBase {

//    @Autowired private AuthRepository authRepository;
//    @Autowired private UserRepository userRepository;
//    @Autowired private RoleRepository roleRepository;
//    @Autowired private PermissionRepository permissionRepository;
//
//    @BeforeEach
//    void cleanDb() {
//        authRepository.deleteAll();
//        userRepository.deleteAll();
//    }
//
//    private void seedDefaultRole() {
//        if (roleRepository.findByName("BASIC_USER").isEmpty()) {
//            Permission p1 = permissionRepository.save(Permission.builder().name("user:read_self").service("user").build());
//            Permission p2 = permissionRepository.save(Permission.builder().name("user:update_self").service("user").build());
//            Set<Permission> perms = new HashSet<>();
//            perms.add(p1);
//            perms.add(p2);
//            roleRepository.save(Role.builder().name("BASIC_USER").description("Basic").isDefault(true).permissions(perms).build());
//        }
//    }
//
//    @Test
//    void register_createsUserAndReturnsTokens() {
//        seedDefaultRole();
//        RegisterRequest request = RegisterRequest.builder()
//                .firstName("John").lastName("Doe")
//                .email("john@test.com").password("Password123")
//                .build();
//
//        ResponseEntity<ApiResponse> response = restTemplate.postForEntity(
//                "/api/v1/user/register", request, ApiResponse.class);
//
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
//        assertThat(response.getBody().getData()).isNotNull();
//        assertThat(userRepository.findByEmail("john@test.com")).isPresent();
//    }
//
//    @Test
//    void register_duplicateEmail_returns409() {
//        seedDefaultRole();
//        RegisterRequest request = RegisterRequest.builder()
//                .firstName("John").lastName("Doe")
//                .email("dup@test.com").password("Password123")
//                .build();
//
//        restTemplate.postForEntity("/api/v1/user/register", request, ApiResponse.class);
//        ResponseEntity<ApiResponse> response = restTemplate.postForEntity(
//                "/api/v1/user/register", request, ApiResponse.class);
//
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
//    }
//
//    @Test
//    void login_withValidCredentials_returnsTokens() {
//        seedDefaultRole();
//        RegisterRequest registerReq = RegisterRequest.builder()
//                .firstName("John").lastName("Doe")
//                .email("login@test.com").password("Password123")
//                .build();
//        restTemplate.postForEntity("/api/v1/user/register", registerReq, ApiResponse.class);
//
//        LoginRequest loginReq = LoginRequest.builder()
//                .email("login@test.com").password("Password123")
//                .build();
//        ResponseEntity<ApiResponse> response = restTemplate.postForEntity(
//                "/api/v1/user/authenticate", loginReq, ApiResponse.class);
//
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
//        assertThat(response.getBody().getData()).isNotNull();
//    }
//
//    @Test
//    void login_wrongPassword_returns401() {
//        seedDefaultRole();
//        RegisterRequest registerReq = RegisterRequest.builder()
//                .firstName("John").lastName("Doe")
//                .email("wrong@test.com").password("Password123")
//                .build();
//        restTemplate.postForEntity("/api/v1/user/register", registerReq, ApiResponse.class);
//
//        LoginRequest loginReq = LoginRequest.builder()
//                .email("wrong@test.com").password("WrongPassword1")
//                .build();
//        ResponseEntity<ApiResponse> response = restTemplate.postForEntity(
//                "/api/v1/user/authenticate", loginReq, ApiResponse.class);
//
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
//    }
//
//    @Test
//    void protectedEndpoint_withoutToken_returns401() {
//        ResponseEntity<ApiResponse> response = restTemplate.getForEntity(
//                "/api/v1/face/is-registered", ApiResponse.class);
//
//        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
//    }
}
