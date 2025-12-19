package ru.mtuci.autonotesbackend.modules.notes.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Data;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.NoteStatus;

@Data
@Schema(description = "Полная информация о конспекте, включая результаты обработки")
public class NoteDetailDto {

    @Schema(description = "ID конспекта", example = "42")
    private Long id;

    @Schema(description = "ID пользователя-владельца", example = "1")
    private Long userId;

    @Schema(description = "Заголовок конспекта", example = "Лекция по теории вероятностей")
    private String title;

    @Schema(description = "Список изображений")
    private List<NoteImageDto> images;

    @Schema(description = "Статус обработки конспекта", example = "COMPLETED")
    private NoteStatus status;

    @Schema(description = "Распознанный текст с изображения")
    private String recognizedText;

    @Schema(description = "Краткое содержание (саммари) распознанного текста")
    private String summaryText;

    @Schema(description = "Дата создания")
    private OffsetDateTime createdAt;

    @Schema(description = "Дата последнего обновления")
    private OffsetDateTime updatedAt;
}
