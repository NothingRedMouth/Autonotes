package ru.mtuci.autonotesbackend.modules.filestorage.impl.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.mtuci.autonotesbackend.modules.notes.impl.repository.LectureNoteRepository;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageCleanupService {

    private final S3Client s3Client;
    private final LectureNoteRepository noteRepository;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${app.scheduling.cleanup-retention-hours:24}")
    private int retentionHours;

    /**
     * Задача Garbage Collector'а:
     * Удалять файлы из S3, на которые нет ссылок в БД (файлы-сироты).
     * Запускается раз в сутки (ночью).
     */
    @Scheduled(cron = "${app.scheduling.s3-cleanup-cron:0 0 3 * * *}")
    public void cleanupOrphanedFiles() {
        log.info("Starting S3 orphaned files cleanup job...");

        Instant retentionThreshold = Instant.now().minus(retentionHours, ChronoUnit.HOURS);

        String continuationToken = null;
        long deletedCount = 0;
        long scannedCount = 0;

        try {
            do {
                ListObjectsV2Request.Builder requestBuilder =
                        ListObjectsV2Request.builder().bucket(bucketName);

                if (continuationToken != null) {
                    requestBuilder.continuationToken(continuationToken);
                }

                ListObjectsV2Response response = s3Client.listObjectsV2(requestBuilder.build());

                for (S3Object s3Object : response.contents()) {
                    scannedCount++;

                    if (s3Object.lastModified().isAfter(retentionThreshold)) {
                        continue;
                    }

                    String fileKey = s3Object.key();

                    if (fileKey.endsWith("/")) {
                        continue;
                    }

                    if (!noteRepository.existsByFileStoragePath(fileKey)) {
                        deleteFromS3(fileKey);
                        deletedCount++;
                    }
                }

                continuationToken = response.nextContinuationToken();

            } while (continuationToken != null);
        } catch (Exception e) {
            log.error("Error during S3 cleanup", e);
        }

        if (deletedCount > 0) {
            log.info("S3 cleanup finished. Scanned objects: {}, Deleted orphans: {}", scannedCount, deletedCount);
        } else {
            log.info("S3 cleanup finished. No orphans found.");
        }
    }

    private void deleteFromS3(String key) {
        try {
            s3Client.deleteObject(
                    DeleteObjectRequest.builder().bucket(bucketName).key(key).build());
            log.info("GC: Deleted orphaned file: {}", key);
        } catch (Exception e) {
            log.error("GC: Failed to delete file: {}", key, e);
        }
    }
}
