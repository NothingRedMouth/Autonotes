package ru.mtuci.autonotesbackend.modules.notes.impl.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.mtuci.autonotesbackend.BaseIntegrationTest;
import ru.mtuci.autonotesbackend.modules.filestorage.api.FileStorageFacade;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.LectureNote;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.NoteImage;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.NoteStatus;
import ru.mtuci.autonotesbackend.modules.notes.impl.repository.LectureNoteRepository;
import ru.mtuci.autonotesbackend.modules.user.impl.domain.User;
import ru.mtuci.autonotesbackend.modules.user.impl.repository.UserRepository;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@TestPropertySource(
        properties = {
            "app.notes.soft-delete-retention-days=30",
            "app.scheduling.soft-delete-cleanup-cron=-",
            "app.scheduling.cleanup-cron=-",
            "app.scheduling.s3-cleanup-cron=-",
            "app.scheduling.outbox-interval-ms=99999999"
        })
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SoftDeleteCleanupServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private SoftDeleteCleanupService cleanupService;

    @Autowired
    private LectureNoteRepository noteRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private S3Client s3Client;

    @MockitoSpyBean
    private FileStorageFacade fileStorageFacade;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    private final List<Long> createdUserIds = new ArrayList<>();
    private final List<String> createdFileKeys = new ArrayList<>();

    @AfterEach
    void tearDown() {
        createdFileKeys.forEach(key -> {
            try {
                s3Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build());
            } catch (Exception ignored) {
            }
        });
        createdFileKeys.clear();

        if (!createdUserIds.isEmpty()) {
            userRepository.deleteAllById(createdUserIds);
            createdUserIds.clear();
        }
    }

    @Test
    void shouldPermanentlyDeleteOldNotesAndFiles() {
        // Arrange
        User user = createUser("clean_user", "clean@test.com");

        // 1. Старая заметка (31 день назад) - должна удалиться полностью
        String oldFilePath = "clean_user/old_note.jpg";
        createFileInS3(oldFilePath);
        LectureNote oldNote = createSoftDeletedNote(user, "Old Note", oldFilePath, 31);

        // 2. Свежая заметка (5 дней назад) - должна остаться в БД (soft deleted) и в S3
        String freshFilePath = "clean_user/fresh_note.jpg";
        createFileInS3(freshFilePath);
        LectureNote freshNote = createSoftDeletedNote(user, "Fresh Note", freshFilePath, 5);

        // Act
        cleanupService.permanentlyDeleteOldNotes();

        // Assert - Проверка БД
        assertThat(checkRecordExistsInDb(oldNote.getId())).isFalse();
        assertThat(checkRecordExistsInDb(freshNote.getId())).isTrue();

        // Assert - Проверка S3
        assertThat(isFileExistsInS3(oldFilePath)).isFalse();
        assertThat(isFileExistsInS3(freshFilePath)).isTrue();
    }

    @Test
    void shouldDeleteFromDb_EvenIfS3Fails() {
        // Arrange
        User user = createUser("error_user", "error@test.com");
        String path = "error_user/fail.jpg";
        createFileInS3(path);
        LectureNote note = createSoftDeletedNote(user, "Error Note", path, 40);

        doThrow(new RuntimeException("S3 Unavailable")).when(fileStorageFacade).delete(anyString());

        // Act
        cleanupService.permanentlyDeleteOldNotes();

        // Assert
        assertThat(checkRecordExistsInDb(note.getId())).isFalse();

        verify(fileStorageFacade, times(1)).delete(path);
    }

    private boolean checkRecordExistsInDb(Long id) {
        Integer count =
                jdbcTemplate.queryForObject("SELECT count(*) FROM lecture_notes WHERE id = ?", Integer.class, id);
        return count != null && count > 0;
    }

    private User createUser(String username, String email) {
        User user = userRepository.save(
                User.builder().username(username).email(email).password("pass").build());
        createdUserIds.add(user.getId());
        return user;
    }

    private LectureNote createSoftDeletedNote(User user, String title, String path, int daysAgo) {
        LectureNote note = LectureNote.builder()
                .user(user)
                .title(title)
                .status(NoteStatus.COMPLETED)
                .build();

        note.addImage(NoteImage.builder()
                .originalFileName("test.jpg")
                .fileStoragePath(path)
                .orderIndex(0)
                .build());

        noteRepository.save(note);

        OffsetDateTime pastDate = OffsetDateTime.now().minusDays(daysAgo);
        jdbcTemplate.update("UPDATE lecture_notes SET deleted_at = ? WHERE id = ?", pastDate, note.getId());

        return note;
    }

    private void createFileInS3(String key) {
        s3Client.putObject(
                PutObjectRequest.builder().bucket(bucketName).key(key).build(),
                RequestBody.fromString("dummy content"));
        createdFileKeys.add(key);
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
