package ru.mtuci.autonotesbackend.modules.user.impl.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.mtuci.autonotesbackend.BaseIntegrationTest;
import ru.mtuci.autonotesbackend.modules.user.impl.domain.User;
import ru.mtuci.autonotesbackend.modules.user.impl.repository.UserRepository;

class UserServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private UserDetailsService userDetailsService;

    @Test
    void changePassword_shouldUpdateDbAndEvictCache() {
        // 1. Arrange
        String username = "cache_test_user";
        String oldPassword = "oldPassword123";
        String newPassword = "newPassword123";

        userRepository.save(User.builder()
                .username(username)
                .email("cache@test.com")
                .password(passwordEncoder.encode(oldPassword))
                .build());

        userDetailsService.loadUserByUsername(username);

        Cache usersCache = cacheManager.getCache("users");
        assertThat(usersCache).isNotNull();
        assertThat(usersCache.get(username)).isNotNull();

        // 2. Act
        userService.changePassword(username, newPassword);

        // 3. Assert
        User updatedUser = userRepository.findByUsername(username).orElseThrow();
        assertThat(passwordEncoder.matches(newPassword, updatedUser.getPassword()))
                .isTrue();
        assertThat(passwordEncoder.matches(oldPassword, updatedUser.getPassword()))
                .isFalse();

        assertThat(usersCache.get(username)).isNull();
    }
}
