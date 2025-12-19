package ru.mtuci.autonotesbackend.modules.notes.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Информация об изображении конспекта")
public class NoteImageDto {
    @Schema(description = "ID изображения", example = "101")
    private Long id;

    @Schema(description = "Оригинальное имя файла", example = "IMG_2024.jpg")
    private String originalFileName;

    @Schema(description = "Порядковый номер при просмотре (сортировка)", example = "0")
    private int orderIndex;
}
