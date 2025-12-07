package ru.mtuci.autonotesbackend.modules.notes.impl.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import ru.mtuci.autonotesbackend.modules.filestorage.api.FileStorageFacade;
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

    @Mock
    private FileStorageFacade fileStorageFacade;

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private NoteService noteService;

    @BeforeEach
    void setUp() {
        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            if (callback == null) {
                return null;
            }
            return callback.doInTransaction(mock(TransactionStatus.class));
        });
    }

    @Test
    void createNote_shouldUploadFileAndSaveNote() throws Exception {
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

        when(fileStorageFacade.save(file, userId)).thenReturn(filePath);

        when(userRepository.getReferenceById(userId)).thenReturn(user);
        when(noteRepository.save(any(LectureNote.class))).thenReturn(savedNote);
        when(objectMapper.writeValueAsString(any(NoteProcessingEvent.class))).thenReturn("{\"noteId\":100}");

        // Act
        LectureNote result = noteService.createNote(title, file, userId);

        // Assert
        assertThat(result).isEqualTo(savedNote);
        verify(fileStorageFacade).save(file, userId);
        verify(outboxEventRepository)
                .save(argThat(event -> event.getAggregateId().equals(100L)
                        && event.getEventType().equals("NOTE_CREATED")));
    }

    @Test
    void createNote_whenDbFails_shouldRollbackFile() {
        // Arrange
        Long userId = 1L;
        String filePath = "1/rollback.jpg";
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[0]);

        when(fileStorageFacade.save(file, userId)).thenReturn(filePath);

        doThrow(new RuntimeException("DB Error")).when(transactionTemplate).execute(any());

        // Act & Assert
        assertThatThrownBy(() -> noteService.createNote("Title", file, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB Error");

        verify(fileStorageFacade).delete(filePath);
    }

    @Test
    void createNote_whenDbAndRollbackFail_shouldPropagateDbError() {
        // Arrange
        Long userId = 1L;
        String filePath = "1/rollback-fail.jpg";
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[0]);

        when(fileStorageFacade.save(file, userId)).thenReturn(filePath);
        doThrow(new RuntimeException("DB Error")).when(transactionTemplate).execute(any());
        doThrow(new RuntimeException("S3 is down")).when(fileStorageFacade).delete(filePath);

        // Act & Assert
        assertThatThrownBy(() -> noteService.createNote("Title", file, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB Error");

        verify(fileStorageFacade).save(file, userId);
        verify(fileStorageFacade).delete(filePath);
    }

    @Test
    void createNote_whenEventSerializationFails_shouldRollbackFileAndThrowException() throws Exception {
        // Arrange
        Long userId = 1L;
        String filePath = "1/serialization-fail.jpg";
        MockMultipartFile file = new MockMultipartFile("file", "original.jpg", "image/jpeg", new byte[0]);

        when(fileStorageFacade.save(file, userId)).thenReturn(filePath);

        doThrow(new RuntimeException("Failed to serialize event payload", new JsonProcessingException("... ") {}))
                .when(transactionTemplate)
                .execute(any());

        // Act & Assert
        assertThatThrownBy(() -> noteService.createNote("Title", file, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Failed to serialize event payload");

        verify(fileStorageFacade).delete(filePath);
    }
}
