package ru.mtuci.autonotesbackend.modules.notes.impl.service;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.LectureNote;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.NoteStatus;
import ru.mtuci.autonotesbackend.modules.notes.impl.repository.LectureNoteRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class NoteCleanupService {

    private final LectureNoteRepository noteRepository;

    @Value("${app.notes.processing-timeout-minutes:10}")
    private int processingTimeoutMinutes;

    @Scheduled(cron = "${app.scheduling.cleanup-cron:0 */2 * * * *}")
    @Transactional
    public void markStuckNotesAsFailed() {
        OffsetDateTime threshold = OffsetDateTime.now().minusMinutes(processingTimeoutMinutes);

        List<LectureNote> stuckNotes = noteRepository.findAllByStatusAndUpdatedAtBefore(
            NoteStatus.PROCESSING, threshold
        );

        if (stuckNotes.isEmpty()) {
            return;
        }

        log.info("Found {} stuck notes (older than {} min). Marking as FAILED.",
            stuckNotes.size(), processingTimeoutMinutes);

        for (LectureNote note : stuckNotes) {
            note.setStatus(NoteStatus.FAILED);
            note.setSummaryText("Processing timed out. The server took too long to respond.");

            log.warn("Note ID {} marked as FAILED. Created: {}, Last Updated: {}",
                note.getId(), note.getCreatedAt(), note.getUpdatedAt());
        }
    }
}
