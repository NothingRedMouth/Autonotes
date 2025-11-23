package ru.mtuci.autonotesbackend.exception.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
@Schema(description = "Стандартизированный ответ об ошибке")
public class ErrorResponseDto {
    @Schema(description = "HTTP статус код", example = "409")
    private int status;

    @Schema(description = "Сообщение об ошибке для пользователя", example = "Username is already taken")
    private String message;

    @Schema(description = "Временная метка ошибки")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private final OffsetDateTime timestamp = OffsetDateTime.now();

    public ErrorResponseDto(HttpStatus status, String message) {
        this.status = status.value();
        this.message = message;
    }
}
