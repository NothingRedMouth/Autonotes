package ru.mtuci.autonotesbackend.modules.notes.impl.service;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.LectureNote;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.NoteStatus;
import ru.mtuci.autonotesbackend.modules.notes.impl.repository.LectureNoteRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class NoteCleanupService {

    private final LectureNoteRepository noteRepository;
    private final TransactionTemplate transactionTemplate;

    @Value("${app.notes.processing-timeout-minutes:10}")
    private int processingTimeoutMinutes;

    @Scheduled(cron = "${app.scheduling.cleanup-cron:0 */2 * * * *}")
    public void markStuckNotesAsFailed() {
        OffsetDateTime threshold = OffsetDateTime.now().minusMinutes(processingTimeoutMinutes);
        int batchSize = 100;
        boolean hasMoreNotes = true;
        int totalProcessed = 0;

        log.info("Starting cleanup of stuck notes (older than {} min)...", processingTimeoutMinutes);

        while (hasMoreNotes) {
            hasMoreNotes =
                    Boolean.TRUE.equals(transactionTemplate.execute(ignored -> processBatch(threshold, batchSize)));

            if (hasMoreNotes) {
                totalProcessed += batchSize;
            }
        }

        if (totalProcessed > 0) {
            log.info("Cleanup finished. Total notes marked as FAILED: approx {}", totalProcessed);
        }
    }

    private boolean processBatch(OffsetDateTime threshold, int batchSize) {
        Pageable pageable = PageRequest.of(0, batchSize);

        List<LectureNote> stuckNotes =
                noteRepository.findAllByStatusAndUpdatedAtBefore(NoteStatus.PROCESSING, threshold, pageable);

        if (stuckNotes.isEmpty()) {
            return false;
        }

        for (LectureNote note : stuckNotes) {
            note.setStatus(NoteStatus.FAILED);
            note.setSummaryText("Processing timed out. The server took too long to respond.");

            log.debug("Note ID {} marked as FAILED.", note.getId());
        }

        return stuckNotes.size() == batchSize;
    }
}
