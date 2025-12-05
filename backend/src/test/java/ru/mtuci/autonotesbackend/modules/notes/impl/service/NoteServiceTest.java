package ru.mtuci.autonotesbackend.modules.notes.impl.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.LectureNote;
import ru.mtuci.autonotesbackend.modules.notes.impl.event.NoteProcessingEvent;
import ru.mtuci.autonotesbackend.modules.notes.impl.repository.LectureNoteRepository;
import ru.mtuci.autonotesbackend.modules.notes.impl.repository.OutboxEventRepository;
import ru.mtuci.autonotesbackend.modules.user.impl.domain.User;
import ru.mtuci.autonotesbackend.modules.user.impl.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    @Mock
    private LectureNoteRepository noteRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private NoteService noteService;

    @Test
    void createNote_shouldSaveNoteAndOutboxEvent() throws Exception {
        // Arrange
        Long userId = 1L;
        String title = "Test Note";
        String filePath = "1/test.jpg";
        MockMultipartFile file = new MockMultipartFile("file", "original.jpg", "image/jpeg", new byte[0]);

        User user = new User();
        user.setId(userId);

        LectureNote savedNote = LectureNote.builder()
            .id(100L)
            .user(user)
            .title(title)
            .fileStoragePath(filePath)
            .build();

        when(userRepository.getReferenceById(userId)).thenReturn(user);
        when(noteRepository.save(any(LectureNote.class))).thenReturn(savedNote);
        when(objectMapper.writeValueAsString(any(NoteProcessingEvent.class))).thenReturn("{\"noteId\":100}");

        // Act
        LectureNote result = noteService.createNote(title, file, filePath, userId);

        // Assert
        assertThat(result).isEqualTo(savedNote);

        verify(outboxEventRepository).save(argThat(event ->
            event.getAggregateId().equals(100L) &&
                event.getEventType().equals("NOTE_CREATED") &&
                event.getPayload().equals("{\"noteId\":100}")
        ));
    }

    @Test
    void createNote_whenJsonFails_shouldThrowRuntimeException() throws Exception {
        // Arrange
        when(userRepository.getReferenceById(1L)).thenReturn(new User());
        when(noteRepository.save(any())).thenReturn(LectureNote.builder().id(1L).fileStoragePath("path").build());

        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("Error") {});

        // Act & Assert
        assertThatThrownBy(() ->
            noteService.createNote("Title", new MockMultipartFile("f", "c".getBytes()), "path", 1L)
        ).isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Failed to serialize event");
    }
}
