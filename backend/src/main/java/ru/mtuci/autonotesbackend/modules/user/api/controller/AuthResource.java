package ru.mtuci.autonotesbackend.modules.user.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import ru.mtuci.autonotesbackend.exception.dto.ErrorResponseDto;
import ru.mtuci.autonotesbackend.modules.user.api.dto.AuthRequestDto;
import ru.mtuci.autonotesbackend.modules.user.api.dto.AuthResponseDto;
import ru.mtuci.autonotesbackend.modules.user.api.dto.RegistrationRequestDto;

@Tag(name = "01. Аутентификация", description = "API для регистрации и входа пользователей")
public interface AuthResource {

    @Operation(
            summary = "Регистрация нового пользователя",
            description = "Создает нового пользователя в системе и возвращает JWT токен для дальнейшей работы.",
            responses = {
                @ApiResponse(
                        responseCode = "201",
                        description = "Пользователь успешно создан",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = AuthResponseDto.class))),
                @ApiResponse(
                        responseCode = "400",
                        description = "Ошибка валидации данных (например, короткий пароль или невалидный email)",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = ErrorResponseDto.class))),
                @ApiResponse(
                        responseCode = "409",
                        description = "Пользователь с таким username или email уже существует",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = ErrorResponseDto.class)))
            })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Данные для создания нового пользователя.",
            required = true,
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = RegistrationRequestDto.class),
                            examples =
                                    @ExampleObject(
                                            name = "Пример регистрации",
                                            summary = "Регистрация пользователя 'newuser'",
                                            value =
                                                    """
                                    {
                                      "username": "newuser",
                                      "email": "new.user@example.com",
                                      "password": "Password123!"
                                    }
                                    """)))
    ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegistrationRequestDto request);

    @Operation(
            summary = "Вход в систему",
            description = "Аутентифицирует пользователя по имени и паролю, возвращая JWT токен.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Успешный вход в систему",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = AuthResponseDto.class))),
                @ApiResponse(
                        responseCode = "401",
                        description = "Неверные учетные данные (имя пользователя или пароль)",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = ErrorResponseDto.class)))
            })
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Учетные данные пользователя для входа.",
            required = true,
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AuthRequestDto.class),
                            examples =
                                    @ExampleObject(
                                            name = "Пример входа",
                                            summary = "Вход пользователя 'newuser'",
                                            value =
                                                    """
                                    {
                                      "username": "newuser",
                                      "password": "Password123!"
                                    }
                                    """)))
    ResponseEntity<AuthResponseDto> login(@Valid @RequestBody AuthRequestDto request);
}
