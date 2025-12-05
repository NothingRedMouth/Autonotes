package ru.mtuci.autonotesbackend.modules.user.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.mtuci.autonotesbackend.BaseIntegrationTest;
import ru.mtuci.autonotesbackend.modules.user.api.dto.AuthRequestDto;
import ru.mtuci.autonotesbackend.modules.user.api.dto.RegistrationRequestDto;
import ru.mtuci.autonotesbackend.modules.user.impl.domain.User;
import ru.mtuci.autonotesbackend.modules.user.impl.repository.UserRepository;

class AuthControllerTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void register_whenValidRequest_shouldReturnTokenAndCreateUser() throws Exception {
        // Arrange
        RegistrationRequestDto requestDto = RegistrationRequestDto.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password123")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty());

        // Verify user in DB
        User savedUser = userRepository.findByUsername("testuser").orElseThrow();
        assertThat(savedUser.getEmail()).isEqualTo("test@example.com");
        assertThat(passwordEncoder.matches("password123", savedUser.getPassword()))
                .isTrue();
    }

    @Test
    void register_whenUsernameAlreadyExists_shouldReturnConflict() throws Exception {
        // Arrange
        userRepository.save(User.builder()
                .username("existinguser")
                .email("exists@example.com")
                .password("hashed")
                .build());
        RegistrationRequestDto requestDto = RegistrationRequestDto.builder()
                .username("existinguser")
                .email("new@example.com")
                .password("password123")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Username is already taken"));
    }

    @Test
    void register_whenInvalidData_shouldReturnBadRequest() throws Exception {
        // Arrange
        RegistrationRequestDto requestDto = RegistrationRequestDto.builder()
                .username("u")
                .email("not-an-email")
                .password("123")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_whenValidCredentials_shouldReturnToken() throws Exception {
        // Arrange
        userRepository.save(User.builder()
                .username("loginuser")
                .email("login@example.com")
                .password(passwordEncoder.encode("password123"))
                .build());

        AuthRequestDto requestDto = AuthRequestDto.builder()
                .username("loginuser")
                .password("password123")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void login_whenInvalidPassword_shouldReturnUnauthorized() throws Exception {
        // Arrange
        userRepository.save(User.builder()
                .username("loginuser2")
                .email("login2@example.com")
                .password(passwordEncoder.encode("password123"))
                .build());

        AuthRequestDto requestDto = AuthRequestDto.builder()
                .username("loginuser2")
                .password("wrongpassword")
                .build();

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void requestWithInvalidToken_shouldReturnUnauthorized() throws Exception {
        String invalidToken = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0In0.invalid-signature";

        mockMvc.perform(get("/api/v1/some-protected-endpoint").header("Authorization", invalidToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid or expired token"));
    }

    @Test
    void login_whenUserNotFound_shouldReturnUnauthorized() throws Exception {
        AuthRequestDto requestDto = AuthRequestDto.builder()
                .username("nonexistentuser")
                .password("anypassword")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isUnauthorized());
    }

    @TestConfiguration
    static class TestClockConfiguration {
        @Bean
        @Primary
        public Clock testClock() {
            return Clock.fixed(Instant.parse("2024-01-01T10:00:00Z"), ZoneId.of("UTC"));
        }
    }
}
