package ru.mtuci.autonotesbackend.modules.notes.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import ru.mtuci.autonotesbackend.modules.notes.api.dto.NoteDto;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.LectureNote;
import ru.mtuci.autonotesbackend.modules.notes.impl.mapper.NoteMapper;
import ru.mtuci.autonotesbackend.modules.notes.impl.service.NoteService;

@ExtendWith(MockitoExtension.class)
class NoteFacadeImplTest {

    @Mock
    private NoteService noteService;

    @Mock
    private NoteMapper noteMapper;

    @InjectMocks
    private NoteFacadeImpl noteFacade;

    @Test
    void createNote_shouldDelegateToService() {
        // Arrange
        String title = "Test";
        MultipartFile file = mock(MultipartFile.class);
        Long userId = 1L;
        LectureNote note = new LectureNote();
        NoteDto noteDto = new NoteDto();

        when(noteService.createNote(title, file, userId)).thenReturn(note);
        when(noteMapper.toDto(note)).thenReturn(noteDto);

        // Act
        NoteDto result = noteFacade.createNote(title, file, userId);

        // Assert
        assertThat(result).isEqualTo(noteDto);
        verify(noteService).createNote(title, file, userId);
    }
}
