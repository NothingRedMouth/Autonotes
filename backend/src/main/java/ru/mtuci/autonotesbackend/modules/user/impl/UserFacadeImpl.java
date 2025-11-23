package ru.mtuci.autonotesbackend.modules.user.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.mtuci.autonotesbackend.modules.user.api.UserFacade;
import ru.mtuci.autonotesbackend.modules.user.api.dto.AuthRequestDto;
import ru.mtuci.autonotesbackend.modules.user.api.dto.AuthResponseDto;
import ru.mtuci.autonotesbackend.modules.user.api.dto.RegistrationRequestDto;
import ru.mtuci.autonotesbackend.modules.user.api.dto.UserProfileDto;
import ru.mtuci.autonotesbackend.modules.user.impl.service.AuthService;
import ru.mtuci.autonotesbackend.modules.user.impl.service.ProfileService;

@Component
@RequiredArgsConstructor
public class UserFacadeImpl implements UserFacade {

    private final AuthService authService;
    private final ProfileService profileService;

    @Override
    public AuthResponseDto register(RegistrationRequestDto request) {
        return authService.register(request);
    }

    @Override
    public AuthResponseDto login(AuthRequestDto request) {
        return authService.login(request);
    }

    @Override
    public UserProfileDto getProfile(String username) {
        return profileService.getProfileByUsername(username);
    }
}
