package ru.mtuci.autonotesbackend.modules.user.impl.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mtuci.autonotesbackend.exception.ResourceNotFoundException;
import ru.mtuci.autonotesbackend.modules.user.api.dto.UserProfileDto;
import ru.mtuci.autonotesbackend.modules.user.impl.mapper.UserMapper;
import ru.mtuci.autonotesbackend.modules.user.impl.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserProfileDto getProfileByUsername(String username) {
        return userRepository
                .findByUsername(username)
                .map(userMapper::toProfileDto)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    }
}
