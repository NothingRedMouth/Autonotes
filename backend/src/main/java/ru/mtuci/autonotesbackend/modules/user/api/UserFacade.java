package ru.mtuci.autonotesbackend.modules.user.api;

import ru.mtuci.autonotesbackend.modules.user.api.dto.AuthRequestDto;
import ru.mtuci.autonotesbackend.modules.user.api.dto.AuthResponseDto;
import ru.mtuci.autonotesbackend.modules.user.api.dto.RegistrationRequestDto;
import ru.mtuci.autonotesbackend.modules.user.api.dto.UserProfileDto;

public interface UserFacade {

    AuthResponseDto register(RegistrationRequestDto request);

    AuthResponseDto login(AuthRequestDto request);

    UserProfileDto getProfile(String username);
}
