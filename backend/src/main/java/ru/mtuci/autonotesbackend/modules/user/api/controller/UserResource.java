package ru.mtuci.autonotesbackend.modules.user.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import ru.mtuci.autonotesbackend.exception.dto.ErrorResponseDto;
import ru.mtuci.autonotesbackend.modules.user.api.dto.UserProfileDto;

@Tag(name = "02. Пользователи", description = "API для управления профилями пользователей")
@SecurityRequirement(name = "bearerAuth")
public interface UserResource {

    @Operation(
            summary = "Получить профиль пользователя по имени",
            description = "Возвращает публичную информацию о профиле пользователя."
                    + " **Важно:** Пользователь может запрашивать только свой собственный профиль.",
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Профиль пользователя успешно получен.",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = UserProfileDto.class))),
                @ApiResponse(
                        responseCode = "401",
                        description = "Пользователь не аутентифицирован (отсутствует или невалидный JWT токен).",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = ErrorResponseDto.class))),
                @ApiResponse(
                        responseCode = "403",
                        description = "Доступ запрещен (попытка просмотра чужого профиля).",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = ErrorResponseDto.class))),
                @ApiResponse(
                        responseCode = "404",
                        description = "Пользователь с указанным именем не найден.",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = ErrorResponseDto.class)))
            })
    ResponseEntity<UserProfileDto> getProfile(
            @Parameter(description = "Имя пользователя (username)", required = true, example = "john_doe") @PathVariable
                    String username);
}
