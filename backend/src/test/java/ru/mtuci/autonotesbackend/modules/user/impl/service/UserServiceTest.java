package ru.mtuci.autonotesbackend.modules.user.impl.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.mtuci.autonotesbackend.modules.user.api.dto.RegistrationRequestDto;
import ru.mtuci.autonotesbackend.modules.user.api.exception.UserAlreadyExistsException;
import ru.mtuci.autonotesbackend.modules.user.impl.domain.User;
import ru.mtuci.autonotesbackend.modules.user.impl.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private final RegistrationRequestDto requestDto = RegistrationRequestDto.builder()
        .username("newuser")
        .email("new@mail.com")
        .password("password123")
        .build();

    @Test
    void createUser_whenEmailAlreadyExists_shouldThrowException() {
        // Arrange
        when(userRepository.findByUsername(requestDto.getUsername())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(requestDto.getEmail())).thenReturn(Optional.of(new User()));

        // Act & Assert
        assertThatThrownBy(() -> userService.createUser(requestDto))
            .isInstanceOf(UserAlreadyExistsException.class)
            .hasMessage("Email is already taken");

        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void createUser_whenRaceConditionOccurs_shouldThrowException() {
        // Arrange
        when(userRepository.findByUsername(requestDto.getUsername())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(requestDto.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");
        when(userRepository.saveAndFlush(any(User.class)))
            .thenThrow(new DataIntegrityViolationException("Unique constraint violation"));

        // Act & Assert
        assertThatThrownBy(() -> userService.createUser(requestDto))
            .isInstanceOf(UserAlreadyExistsException.class)
            .hasMessage("User with this username or email already exists");
    }
}
