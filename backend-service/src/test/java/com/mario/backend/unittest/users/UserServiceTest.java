package com.mario.backend.unittest.users;

import com.mario.backend.common.exception.ApiException;
import com.mario.backend.testutil.TestDataFactory;
import com.mario.backend.users.dto.UpdateProfileRequest;
import com.mario.backend.users.dto.UserResponse;
import com.mario.backend.users.entity.User;
import com.mario.backend.users.repository.UserRepository;
import com.mario.backend.users.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.mario.backend.testutil.TestConstants.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @InjectMocks private UserService userService;

    @Test
    void getProfile_existingUser_returnsResponse() {
        User user = TestDataFactory.createUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        UserResponse response = userService.getProfile(USER_ID);

        assertThat(response.getId()).isEqualTo(USER_ID);
        assertThat(response.getEmail()).isEqualTo(USER_EMAIL);
        assertThat(response.getRole()).isNotNull();
        assertThat(response.getRole().getName()).isEqualTo(ROLE_BASIC_USER);
    }

    @Test
    void getProfile_nonExistentUser_throws() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile(999L))
                .isInstanceOf(ApiException.class)
                .extracting("code").isEqualTo("USER_NOT_FOUND");
    }

    @Test
    void updateProfile_updatesFieldsSelectively() {
        User user = TestDataFactory.createUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .firstName("Jane")
                .build();

        UserResponse response = userService.updateProfile(USER_ID, request);
        assertThat(response.getFirstName()).isEqualTo("Jane");
        assertThat(response.getLastName()).isEqualTo(USER_LAST_NAME);
    }

    @Test
    void updateProfile_nonExistentUser_throws() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateProfile(999L, UpdateProfileRequest.builder().build()))
                .isInstanceOf(ApiException.class);
    }
}
