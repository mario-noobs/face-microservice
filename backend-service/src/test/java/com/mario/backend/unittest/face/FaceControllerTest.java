package com.mario.backend.unittest.face;

import com.mario.backend.auth.security.AuthenticatedUser;
import com.mario.backend.auth.security.JwtTokenProvider;
import com.mario.backend.auth.service.TokenBlacklistService;
import com.mario.backend.audit.service.AuditService;
import com.mario.backend.face.controller.FaceController;
import com.mario.backend.face.dto.FaceResponse;
import com.mario.backend.face.service.FaceService;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = FaceController.class)
class FaceControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private FaceService faceService;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private TokenBlacklistService tokenBlacklistService;
    @MockBean private AuditService auditService;

    private RequestPostProcessor premiumUser() {
        AuthenticatedUser principal = new AuthenticatedUser(USER_ID, USER_EMAIL, ROLE_PREMIUM_USER, PREMIUM_USER_PERMISSIONS);
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + ROLE_PREMIUM_USER));
        PREMIUM_USER_PERMISSIONS.forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, authorities);
        return authentication(auth);
    }

    @Test
    void registerFace_returns200() throws Exception {
        FaceResponse response = FaceResponse.builder()
                .success(true).code("0000").userId(USER_ID).build();
        when(faceService.registerFace(eq(USER_ID), anyString())).thenReturn(response);

        mockMvc.perform(post("/api/v1/face/register-identity")
                        .with(premiumUser()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"image_data\":\"base64data\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.code").value("0000"))
                .andExpect(jsonPath("$.data.user_id").value(USER_ID.intValue()));
    }

    @Test
    void recognizeFace_returns200() throws Exception {
        FaceResponse response = FaceResponse.builder()
                .success(true).userId(USER_ID).build();
        when(faceService.recognizeFace(eq(USER_ID), anyString())).thenReturn(response);

        mockMvc.perform(post("/api/v1/face/recognize-identity")
                        .with(premiumUser()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"image_data\":\"base64data\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(true));
    }

    @Test
    void deleteFace_returns200() throws Exception {
        FaceResponse response = FaceResponse.builder()
                .success(true).userId(USER_ID).code("0000").build();
        when(faceService.deleteFace(USER_ID)).thenReturn(response);

        mockMvc.perform(post("/api/v1/face/delete-identity")
                        .with(premiumUser()).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.success").value(true));
    }

    @Test
    void isRegistered_returnsTrue() throws Exception {
        FaceResponse response = FaceResponse.builder()
                .success(true).isRegistered(true).userId(USER_ID).build();
        when(faceService.isRegistered(USER_ID)).thenReturn(response);

        mockMvc.perform(get("/api/v1/face/is-registered")
                        .with(premiumUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.is_registered").value(true));
    }
}