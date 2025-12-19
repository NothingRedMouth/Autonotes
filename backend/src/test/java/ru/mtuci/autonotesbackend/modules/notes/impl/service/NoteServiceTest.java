package ru.mtuci.autonotesbackend.modules.notes.impl.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
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
import org.springframework.web.multipart.MultipartFile;
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
            if (callback == null) return null;
            return callback.doInTransaction(mock(TransactionStatus.class));
        });
    }

    @Test
    void createNote_shouldUploadMultipleFilesAndSaveNote() throws Exception {
        // Arrange
        Long userId = 1L;
        String title = "Test Note";

        MockMultipartFile file1 = new MockMultipartFile("files", "img1.jpg", "image/jpeg", new byte[1]);
        MockMultipartFile file2 = new MockMultipartFile("files", "img2.jpg", "image/jpeg", new byte[1]);
        List<MultipartFile> files = List.of(file1, file2);

        String path1 = "1/uuid-img1.jpg";
        String path2 = "1/uuid-img2.jpg";

        User user = new User();
        user.setId(userId);

        when(fileStorageFacade.save(file1, userId)).thenReturn(path1);
        when(fileStorageFacade.save(file2, userId)).thenReturn(path2);

        when(userRepository.getReferenceById(userId)).thenReturn(user);

        when(noteRepository.save(any(LectureNote.class))).thenAnswer(i -> {
            LectureNote n = i.getArgument(0);
            n.setId(100L);
            return n;
        });

        when(objectMapper.writeValueAsString(any(NoteProcessingEvent.class))).thenReturn("{\"noteId\":100}");

        // Act
        LectureNote result = noteService.createNote(title, files, userId);

        // Assert
        assertThat(result.getTitle()).isEqualTo(title);
        assertThat(result.getImages()).hasSize(2);
        assertThat(result.getImages().get(0).getFileStoragePath()).isEqualTo(path1);
        assertThat(result.getImages().get(1).getFileStoragePath()).isEqualTo(path2);

        verify(fileStorageFacade).save(file1, userId);
        verify(fileStorageFacade).save(file2, userId);

        verify(outboxEventRepository)
                .save(argThat(event -> event.getAggregateId().equals(100L)
                        && event.getEventType().equals("NOTE_CREATED")));
    }

    @Test
    void createNote_whenDbFails_shouldRollbackAllFiles() {
        // Arrange
        Long userId = 1L;
        MockMultipartFile file1 = new MockMultipartFile("files", "img1.jpg", "image/jpeg", new byte[1]);
        MockMultipartFile file2 = new MockMultipartFile("files", "img2.jpg", "image/jpeg", new byte[1]);
        List<MultipartFile> files = List.of(file1, file2);

        String path1 = "1/path1.jpg";
        String path2 = "1/path2.jpg";

        when(fileStorageFacade.save(file1, userId)).thenReturn(path1);
        when(fileStorageFacade.save(file2, userId)).thenReturn(path2);

        doThrow(new RuntimeException("DB Error")).when(transactionTemplate).execute(any());

        // Act & Assert
        assertThatThrownBy(() -> noteService.createNote("Title", files, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB Error");

        verify(fileStorageFacade).delete(path1);
        verify(fileStorageFacade).delete(path2);
    }

    @Test
    void createNote_whenSecondFileUploadFails_shouldRollbackFirstFile() {
        // Arrange
        Long userId = 1L;
        MockMultipartFile file1 = new MockMultipartFile("files", "img1.jpg", "image/jpeg", new byte[1]);
        MockMultipartFile file2 = new MockMultipartFile("files", "fail.jpg", "image/jpeg", new byte[1]);
        List<MultipartFile> files = List.of(file1, file2);

        String path1 = "1/path1.jpg";

        when(fileStorageFacade.save(file1, userId)).thenReturn(path1);
        when(fileStorageFacade.save(file2, userId)).thenThrow(new RuntimeException("S3 Error"));

        // Act & Assert
        assertThatThrownBy(() -> noteService.createNote("Title", files, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("S3 Error");

        verify(fileStorageFacade).delete(path1);
        verify(fileStorageFacade, never()).delete("null");
        verify(transactionTemplate, never()).execute(any());
    }
}
