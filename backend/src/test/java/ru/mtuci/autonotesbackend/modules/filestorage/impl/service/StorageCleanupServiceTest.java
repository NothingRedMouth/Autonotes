package ru.mtuci.autonotesbackend.modules.filestorage.impl.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.util.ReflectionTestUtils;
import ru.mtuci.autonotesbackend.BaseIntegrationTest;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.LectureNote;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.NoteStatus;
import ru.mtuci.autonotesbackend.modules.notes.impl.repository.LectureNoteRepository;
import ru.mtuci.autonotesbackend.modules.user.impl.domain.User;
import ru.mtuci.autonotesbackend.modules.user.impl.repository.UserRepository;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

class StorageCleanupServiceTest extends BaseIntegrationTest {

    @Autowired
    private StorageCleanupService cleanupService;

    @Autowired
    private S3Client s3Client;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LectureNoteRepository noteRepository;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(cleanupService, "retentionHours", 24);
    }

    @Test
    void shouldDeleteOrphanedFile_WhenRetentionTimePassed() {
        String orphanKey = "orphan-to-delete.txt";
        createFileInS3(orphanKey);

        ReflectionTestUtils.setField(cleanupService, "retentionHours", 0);

        cleanupService.cleanupOrphanedFiles();

        assertThat(isFileExistsInS3(orphanKey)).isFalse();
    }

    @Test
    void shouldKeepOrphanedFile_WhenFileIsFresh() {
        String freshOrphanKey = "fresh-orphan-keep.txt";
        createFileInS3(freshOrphanKey);

        cleanupService.cleanupOrphanedFiles();

        assertThat(isFileExistsInS3(freshOrphanKey)).isTrue();
    }

    @Test
    void shouldKeepLinkedFile_EvenIfOld() {
        User user = userRepository.save(User.builder()
                .username("storage_user")
                .email("storage@test.com")
                .password("pass")
                .build());

        String linkedKey = "linked-file.txt";
        createFileInS3(linkedKey);

        noteRepository.save(LectureNote.builder()
                .user(user)
                .title("Linked Note")
                .originalFileName("orig.txt")
                .fileStoragePath(linkedKey)
                .status(NoteStatus.COMPLETED)
                .build());

        ReflectionTestUtils.setField(cleanupService, "retentionHours", 0);

        cleanupService.cleanupOrphanedFiles();

        assertThat(isFileExistsInS3(linkedKey)).isTrue();
    }

    private void createFileInS3(String key) {
        s3Client.putObject(
                PutObjectRequest.builder().bucket(bucketName).key(key).build(),
                RequestBody.fromString("dummy content"));
    }

    private boolean isFileExistsInS3(String key) {
        try {
            s3Client.headObject(
                    HeadObjectRequest.builder().bucket(bucketName).key(key).build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }
}
