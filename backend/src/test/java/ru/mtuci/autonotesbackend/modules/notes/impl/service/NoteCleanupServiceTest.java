package ru.mtuci.autonotesbackend.modules.notes.impl.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.LectureNote;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.NoteStatus;
import ru.mtuci.autonotesbackend.modules.notes.impl.repository.LectureNoteRepository;

@ExtendWith(MockitoExtension.class)
class NoteCleanupServiceTest {

    @Mock
    private LectureNoteRepository noteRepository;

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private NoteCleanupService noteCleanupService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(noteCleanupService, "processingTimeoutMinutes", 10);

        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });
    }

    @Test
    void markStuckNotesAsFailed_shouldUpdateStuckNotes() {
        // Arrange
        LectureNote stuckNote = new LectureNote();
        stuckNote.setId(1L);
        stuckNote.setStatus(NoteStatus.PROCESSING);

        when(noteRepository.findAllByStatusAndUpdatedAtBefore(
            eq(NoteStatus.PROCESSING),
            any(OffsetDateTime.class),
            any(Pageable.class)))
            .thenReturn(List.of(stuckNote));

        // Act
        noteCleanupService.markStuckNotesAsFailed();

        // Assert
        assert stuckNote.getStatus() == NoteStatus.FAILED;
        assert stuckNote.getSummaryText().contains("timed out");

        verify(transactionTemplate, atLeastOnce()).execute(any());

        verify(noteRepository, times(1))
            .findAllByStatusAndUpdatedAtBefore(
                eq(NoteStatus.PROCESSING),
                any(OffsetDateTime.class),
                any(Pageable.class));
    }

    @Test
    void markStuckNotesAsFailed_whenNoStuckNotes_shouldDoNothing() {
        // Arrange
        when(noteRepository.findAllByStatusAndUpdatedAtBefore(any(), any(), any(Pageable.class)))
            .thenReturn(List.of());

        // Act
        noteCleanupService.markStuckNotesAsFailed();

        // Assert
        verify(noteRepository, times(1))
            .findAllByStatusAndUpdatedAtBefore(any(), any(), any(Pageable.class));
    }
}
