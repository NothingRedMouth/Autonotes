package ru.mtuci.autonotesbackend.modules.user.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import lombok.Data;

@Data
@Schema(description = "Профиль пользователя")
public class UserProfileDto {

    @Schema(description = "Уникальный идентификатор", example = "1")
    private Long id;

    @Schema(description = "Имя пользователя", example = "john_doe")
    private String username;

    @Schema(description = "Электронная почта", example = "john.doe@example.com")
    private String email;

    @Schema(description = "Дата регистрации")
    private OffsetDateTime createdAt;
}
