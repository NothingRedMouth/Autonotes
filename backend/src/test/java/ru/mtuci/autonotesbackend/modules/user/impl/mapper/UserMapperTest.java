package ru.mtuci.autonotesbackend.modules.user.impl.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.mtuci.autonotesbackend.modules.user.api.dto.UserProfileDto;
import ru.mtuci.autonotesbackend.modules.user.impl.domain.User;

class UserMapperTest {

    private final UserMapper mapper = Mappers.getMapper(UserMapper.class);

    @Test
    void toProfileDto_shouldMapPublicFieldsOnly() {
        // Arrange
        User user = User.builder()
                .id(10L)
                .username("john")
                .email("john@mail.com")
                .password("secret_hash")
                .createdAt(OffsetDateTime.now())
                .build();

        // Act
        UserProfileDto dto = mapper.toProfileDto(user);

        // Assert
        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getUsername()).isEqualTo("john");
        assertThat(dto.getEmail()).isEqualTo("john@mail.com");
        assertThat(dto.getCreatedAt()).isEqualTo(user.getCreatedAt());
    }
}
