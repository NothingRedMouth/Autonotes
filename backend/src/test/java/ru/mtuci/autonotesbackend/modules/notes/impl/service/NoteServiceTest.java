package ru.mtuci.autonotesbackend.modules.notes.impl.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
import ru.mtuci.autonotesbackend.exception.ResourceNotFoundException;
import ru.mtuci.autonotesbackend.modules.filestorage.api.FileStorageFacade;
import ru.mtuci.autonotesbackend.modules.filestorage.api.exception.InvalidFileFormatException;
import ru.mtuci.autonotesbackend.modules.notes.api.dto.NoteDto;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.LectureNote;
import ru.mtuci.autonotesbackend.modules.notes.impl.event.NoteProcessingEvent;
import ru.mtuci.autonotesbackend.modules.notes.impl.mapper.NoteMapper;
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

    @Mock
    private NoteMapper noteMapper;

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
    void createNote_whenFileListIsEmpty_shouldThrowException() {
        List<MultipartFile> emptyList = Collections.emptyList();
        assertThatThrownBy(() -> noteService.createNote("Title", emptyList, 1L))
                .isInstanceOf(InvalidFileFormatException.class)
                .hasMessage("At least one image file is required.");
    }

    @Test
    void createNote_whenFileInsideListIsEmpty_shouldThrowException() {
        MockMultipartFile emptyFile = new MockMultipartFile("files", "", "image/jpeg", new byte[0]);
        List<MultipartFile> files = List.of(emptyFile);

        assertThatThrownBy(() -> noteService.createNote("Title", files, 1L))
                .isInstanceOf(InvalidFileFormatException.class)
                .hasMessageContaining("is empty");
    }

    @Test
    void createNote_shouldUploadMultipleFilesAndSaveNote() throws Exception {
        Long userId = 1L;
        MockMultipartFile file1 = new MockMultipartFile("files", "img1.jpg", "image/jpeg", new byte[1]);
        List<MultipartFile> files = List.of(file1);
        String path1 = "1/uuid-img1.jpg";
        User user = new User();
        user.setId(userId);

        when(fileStorageFacade.save(file1, userId)).thenReturn(path1);
        when(userRepository.getReferenceById(userId)).thenReturn(user);
        when(noteRepository.save(any(LectureNote.class))).thenAnswer(i -> {
            LectureNote n = i.getArgument(0);
            n.setId(100L);
            return n;
        });
        when(objectMapper.writeValueAsString(any(NoteProcessingEvent.class))).thenReturn("{}");

        LectureNote result = noteService.createNote("Title", files, userId);

        assertThat(result.getImages()).hasSize(1);
        verify(outboxEventRepository).save(any());
    }

    @Test
    void createNote_whenDbFails_shouldRollbackAllFiles() {
        Long userId = 1L;
        MockMultipartFile file1 = new MockMultipartFile("files", "img1.jpg", "image/jpeg", new byte[1]);
        List<MultipartFile> files = List.of(file1);
        String path1 = "1/path1.jpg";

        when(fileStorageFacade.save(file1, userId)).thenReturn(path1);
        doThrow(new RuntimeException("DB Error")).when(transactionTemplate).execute(any());

        assertThatThrownBy(() -> noteService.createNote("Title", files, userId)).isInstanceOf(RuntimeException.class);

        verify(fileStorageFacade).delete(path1);
    }

    @Test
    void createNote_whenRollbackFails_shouldLogAndNotThrowExtraException() {
        Long userId = 1L;
        MockMultipartFile file1 = new MockMultipartFile("files", "img1.jpg", "image/jpeg", new byte[1]);
        List<MultipartFile> files = List.of(file1);
        String path1 = "1/path1.jpg";

        when(fileStorageFacade.save(file1, userId)).thenReturn(path1);
        doThrow(new RuntimeException("DB Error")).when(transactionTemplate).execute(any());

        doThrow(new RuntimeException("S3 Down")).when(fileStorageFacade).delete(path1);

        assertThatThrownBy(() -> noteService.createNote("Title", files, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("DB Error");

        verify(fileStorageFacade).delete(path1);
    }

    @Test
    void findAllDtosByUserId_shouldCallRepoAndMapper() {
        Long userId = 1L;
        List<LectureNote> notes = List.of(new LectureNote());
        when(noteRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(notes);
        when(noteMapper.toDtoList(notes)).thenReturn(List.of(new NoteDto()));

        List<NoteDto> result = noteService.findAllDtosByUserId(userId);

        assertThat(result).hasSize(1);
        verify(noteRepository).findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Test
    void findByIdAndUserId_whenExists_shouldReturnNote() {
        Long noteId = 1L;
        Long userId = 1L;
        LectureNote note = new LectureNote();
        when(noteRepository.findByIdAndUserId(noteId, userId)).thenReturn(Optional.of(note));

        LectureNote result = noteService.findByIdAndUserId(noteId, userId);

        assertThat(result).isSameAs(note);
    }

    @Test
    void findByIdAndUserId_whenNotExists_shouldThrowException() {
        Long noteId = 1L;
        Long userId = 1L;
        when(noteRepository.findByIdAndUserId(noteId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.findByIdAndUserId(noteId, userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteByIdAndUserId_shouldFindAndDelete() {
        Long noteId = 1L;
        Long userId = 1L;
        LectureNote note = new LectureNote();
        when(noteRepository.findByIdAndUserId(noteId, userId)).thenReturn(Optional.of(note));

        noteService.deleteByIdAndUserId(noteId, userId);

        verify(noteRepository).delete(note);
    }
}
