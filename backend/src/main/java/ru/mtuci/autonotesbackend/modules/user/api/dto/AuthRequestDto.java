package ru.mtuci.autonotesbackend.modules.user.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Запрос на аутентификацию (вход)")
public class AuthRequestDto {
    @Schema(description = "Имя пользователя", example = "john_doe")
    @NotBlank(message = "Username cannot be blank")
    private String username;

    @Schema(description = "Пароль", example = "Str0ngP@ssw0rd")
    @NotBlank(message = "Password cannot be blank")
    @ToString.Exclude
    private String password;
}
