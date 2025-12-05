package ru.mtuci.autonotesbackend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import io.jsonwebtoken.JwtException;
import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private JwtService jwtService;

    @Mock
    private Clock clock;

    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(clock);

        String secret = "dGVzdC1zZWNyZXQtZm9yLWp3dC10ZXN0aW5nLWxvbmctZW5vdWdo";
        ReflectionTestUtils.setField(jwtService, "jwtSecret", secret);
        long expirationMs = 3600000;
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", expirationMs);

        when(clock.millis()).thenReturn(Instant.parse("2024-01-01T12:00:00Z").toEpochMilli());
        when(clock.instant()).thenReturn(Instant.parse("2024-01-01T12:00:00Z"));

        userDetails = new User("testUser", "pass", Collections.emptyList());
    }

    @Test
    void generateToken_shouldCreateValidToken() {
        String token = jwtService.generateToken(userDetails);

        assertThat(token).isNotNull();
        assertThat(jwtService.extractUsername(token)).isEqualTo("testUser");
    }

    @Test
    void isTokenValid_whenTokenIsNotExpired_shouldReturnTrue() {
        String token = jwtService.generateToken(userDetails);

        boolean isValid = jwtService.isTokenValid(token, userDetails);

        assertThat(isValid).isTrue();
    }

    @Test
    void isTokenValid_whenTokenIsExpired_shouldThrowExceptionOrReturnFalse() {
        String token = jwtService.generateToken(userDetails);

        Instant future = Instant.parse("2024-01-01T13:00:01Z");
        when(clock.instant()).thenReturn(future);

        assertThatThrownBy(() -> jwtService.isTokenValid(token, userDetails)).isInstanceOf(JwtException.class);
    }

    @Test
    void isTokenValid_whenUsernameDoesNotMatch_shouldReturnFalse() {
        String token = jwtService.generateToken(userDetails);

        UserDetails otherUser = new User("hacker", "pass", Collections.emptyList());

        boolean isValid = jwtService.isTokenValid(token, otherUser);

        assertThat(isValid).isFalse();
    }
}
