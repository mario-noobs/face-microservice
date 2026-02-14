package com.mario.backend.unittest.users;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mario.backend.auth.security.AuthenticatedUser;
import com.mario.backend.auth.security.JwtTokenProvider;
import com.mario.backend.auth.service.TokenBlacklistService;
import com.mario.backend.audit.service.AuditService;
import com.mario.backend.users.controller.ProfileController;
import com.mario.backend.users.dto.UpdateProfileRequest;
import com.mario.backend.users.dto.UserResponse;
import com.mario.backend.users.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.ArrayList;
import java.util.List;

import static com.mario.backend.testutil.TestConstants.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ProfileController.class)
class ProfileControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private UserService userService;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private TokenBlacklistService tokenBlacklistService;
    @MockBean private AuditService auditService;

    private RequestPostProcessor basicUser() {
        AuthenticatedUser principal = new AuthenticatedUser(USER_ID, USER_EMAIL, ROLE_BASIC_USER, BASIC_USER_PERMISSIONS);
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + ROLE_BASIC_USER));
        BASIC_USER_PERMISSIONS.forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, authorities);
        return authentication(auth);
    }

    @Test
    void getProfile_returns200() throws Exception {
        UserResponse response = UserResponse.builder()
                .id(USER_ID).email(USER_EMAIL).firstName("John").lastName("Doe")
                .build();
        when(userService.getProfile(USER_ID)).thenReturn(response);

        mockMvc.perform(post("/api/v1/profile")
                        .with(basicUser()).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(USER_EMAIL))
                .andExpect(jsonPath("$.data.first_name").value("John"));
    }

    @Test
    void updateProfile_returns200() throws Exception {
        UpdateProfileRequest request = UpdateProfileRequest.builder().firstName("Jane").build();
        UserResponse response = UserResponse.builder().id(USER_ID).firstName("Jane").build();
        when(userService.updateProfile(eq(USER_ID), any())).thenReturn(response);

        mockMvc.perform(put("/api/v1/profile")
                        .with(basicUser()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.first_name").value("Jane"));
    }

    @Test
    void updateProfile_firstNameTooLong_returns400() throws Exception {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .firstName("A".repeat(31))
                .build();

        mockMvc.perform(put("/api/v1/profile")
                        .with(basicUser()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
