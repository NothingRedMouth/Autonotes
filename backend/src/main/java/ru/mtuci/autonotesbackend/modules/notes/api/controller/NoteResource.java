package ru.mtuci.autonotesbackend.modules.notes.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.multipart.MultipartFile;
import ru.mtuci.autonotesbackend.exception.dto.ErrorResponseDto;
import ru.mtuci.autonotesbackend.modules.notes.api.dto.NoteDetailDto;
import ru.mtuci.autonotesbackend.modules.notes.api.dto.NoteDto;
import ru.mtuci.autonotesbackend.security.SecurityUser;

@Tag(name = "03. Конспекты", description = "API для управления конспектами")
@SecurityRequirement(name = "bearerAuth")
public interface NoteResource {

    @Operation(
            summary = "Загрузить новый конспект",
            description = "Загружает файлы (изображения) и заголовок. Поддерживает множественную загрузку."
                    + " Запрос должен быть типа `multipart/form-data`.",
            responses = {
                @ApiResponse(
                        responseCode = "201",
                        description = "Конспект успешно создан",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = NoteDto.class))),
                @ApiResponse(
                        responseCode = "400",
                        description = "Некорректный запрос",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = ErrorResponseDto.class))),
                @ApiResponse(
                        responseCode = "503",
                        description = "Сервис хранения файлов недоступен",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = ErrorResponseDto.class)))
            })
    ResponseEntity<NoteDto> uploadNote(
            @Parameter(description = "Заголовок конспекта", required = true) String title,
            @Parameter(description = "Список файлов изображений", required = true) List<MultipartFile> files,
            @Parameter(hidden = true) SecurityUser securityUser);

    @Operation(summary = "Получить все конспекты пользователя")
    ResponseEntity<List<NoteDto>> getAllNotes(@Parameter(hidden = true) SecurityUser securityUser);

    @Operation(summary = "Получить детальную информацию о конспекте")
    ResponseEntity<NoteDetailDto> getNoteById(
            @Parameter(description = "ID конспекта") @PathVariable Long id,
            @Parameter(hidden = true) SecurityUser securityUser);

    @Operation(summary = "Удалить конспект")
    ResponseEntity<Void> deleteNote(
            @Parameter(description = "ID конспекта") @PathVariable Long id,
            @Parameter(hidden = true) SecurityUser securityUser);
}
