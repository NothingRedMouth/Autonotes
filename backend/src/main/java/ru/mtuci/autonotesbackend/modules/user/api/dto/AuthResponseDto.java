package ru.mtuci.autonotesbackend.modules.user.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Ответ с токеном доступа")
public class AuthResponseDto {
    @Schema(
            description = "JWT токен доступа",
            example =
                    "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2huX2RvZSIsImlhdCI6MTYxNjQ0NjQwMCwiZXhwIjoxNjE2NTMyODAwfQ.xyz...")
    private String token;
}
