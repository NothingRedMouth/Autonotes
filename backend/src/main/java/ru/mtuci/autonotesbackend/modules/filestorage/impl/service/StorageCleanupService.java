package ru.mtuci.autonotesbackend.modules.filestorage.impl.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    @Scheduled(cron = "${app.scheduling.s3-cleanup-cron:0 0 3 * * *}")
    @Transactional(readOnly = true)
    public void cleanupOrphanedFiles() {
        log.info("Starting S3 orphaned files cleanup job (Batch Mode)...");

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
                List<S3Object> s3Objects = response.contents();

                if (s3Objects.isEmpty()) {
                    break;
                }

                scannedCount += s3Objects.size();

                List<String> candidatesToCheck = s3Objects.stream()
                        .filter(obj -> !obj.key().endsWith("/"))
                        .filter(obj -> obj.lastModified().isBefore(retentionThreshold))
                        .map(S3Object::key)
                        .toList();

                if (!candidatesToCheck.isEmpty()) {
                    Set<String> existingFiles = noteRepository.findExistingPaths(candidatesToCheck);

                    List<String> orphans = candidatesToCheck.stream()
                            .filter(key -> !existingFiles.contains(key))
                            .toList();

                    for (String orphanKey : orphans) {
                        deleteFromS3(orphanKey);
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
