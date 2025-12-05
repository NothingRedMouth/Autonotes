package ru.mtuci.autonotesbackend.modules.notes.impl.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.mtuci.autonotesbackend.modules.notes.api.dto.NoteDetailDto;
import ru.mtuci.autonotesbackend.modules.notes.api.dto.NoteDto;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.LectureNote;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.NoteStatus;
import ru.mtuci.autonotesbackend.modules.user.impl.domain.User;

class NoteMapperTest {

    private final NoteMapper mapper = Mappers.getMapper(NoteMapper.class);

    @Test
    void toDto_shouldMapFieldsCorrectly() {
        // Arrange
        User user = new User();
        user.setId(99L);

        LectureNote note = LectureNote.builder()
                .id(1L)
                .user(user)
                .title("Math")
                .originalFileName("math.jpg")
                .status(NoteStatus.COMPLETED)
                .createdAt(OffsetDateTime.now())
                .build();

        // Act
        NoteDto dto = mapper.toDto(note);

        // Assert
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getUserId()).isEqualTo(99L);
        assertThat(dto.getTitle()).isEqualTo("Math");
        assertThat(dto.getStatus()).isEqualTo(NoteStatus.COMPLETED);
    }

    @Test
    void toDetailDto_shouldIncludeDetails() {
        // Arrange
        LectureNote note = LectureNote.builder()
                .id(2L)
                .user(new User())
                .recognizedText("Hello World")
                .summaryText("Summary")
                .build();

        // Act
        NoteDetailDto detailDto = mapper.toDetailDto(note);

        // Assert
        assertThat(detailDto.getRecognizedText()).isEqualTo("Hello World");
        assertThat(detailDto.getSummaryText()).isEqualTo("Summary");
    }
}
