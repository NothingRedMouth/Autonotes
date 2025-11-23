package ru.mtuci.autonotesbackend.modules.user.impl.mapper;

import org.mapstruct.Mapper;
import ru.mtuci.autonotesbackend.modules.user.api.dto.UserProfileDto;
import ru.mtuci.autonotesbackend.modules.user.impl.domain.User;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserProfileDto toProfileDto(User user);
}
