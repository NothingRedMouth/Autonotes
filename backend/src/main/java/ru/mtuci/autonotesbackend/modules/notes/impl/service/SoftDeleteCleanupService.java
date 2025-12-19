package ru.mtuci.autonotesbackend.modules.notes.impl.service;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import ru.mtuci.autonotesbackend.modules.filestorage.api.FileStorageFacade;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.LectureNote;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.NoteImage;
import ru.mtuci.autonotesbackend.modules.notes.impl.repository.LectureNoteRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class SoftDeleteCleanupService {

    private final LectureNoteRepository noteRepository;
    private final FileStorageFacade fileStorageFacade;
    private final TransactionTemplate transactionTemplate;

    @Value("${app.notes.soft-delete-retention-days:30}")
    private int retentionDays;

    @Scheduled(cron = "${app.scheduling.soft-delete-cleanup-cron:0 0 4 * * *}")
    public void permanentlyDeleteOldNotes() {
        log.info("Starting permanent deletion of soft-deleted notes older than {} days...", retentionDays);

        OffsetDateTime threshold = OffsetDateTime.now().minusDays(retentionDays);
        boolean hasMore = true;
        int batchSize = 50;
        int totalDeleted = 0;

        while (hasMore) {
            hasMore = Boolean.TRUE.equals(transactionTemplate.execute(ignored -> {
                List<LectureNote> expiredNotes =
                        noteRepository.findAllSoftDeletedBefore(threshold, PageRequest.of(0, batchSize));

                if (expiredNotes.isEmpty()) {
                    return false;
                }

                boolean progressMade = false;

                for (LectureNote note : expiredNotes) {
                    try {
                        boolean allFilesDeleted = true;

                        if (note.getImages() != null) {
                            for (NoteImage image : note.getImages()) {
                                try {
                                    fileStorageFacade.delete(image.getFileStoragePath());
                                } catch (Exception e) {
                                    log.warn(
                                            "Failed to delete file from S3: {}. Skipping DB deletion for note {}.",
                                            image.getFileStoragePath(),
                                            note.getId(),
                                            e);
                                    allFilesDeleted = false;
                                }
                            }
                        }

                        if (allFilesDeleted) {
                            noteRepository.hardDeleteById(note.getId());
                            log.debug("Permanently deleted note ID: {}", note.getId());
                            progressMade = true;
                        }

                    } catch (Exception e) {
                        log.error("Failed to cleanup note ID: {}", note.getId(), e);
                    }
                }

                return progressMade;
            }));

            if (hasMore) {
                totalDeleted += batchSize;
            }
        }

        if (totalDeleted > 0) {
            log.info("Completed permanent deletion. Total records removed: approx {}", totalDeleted);
        }
    }
}
