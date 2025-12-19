package ru.mtuci.autonotesbackend.modules.notes.impl.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import ru.mtuci.autonotesbackend.BaseIntegrationTest;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.LectureNote;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.NoteStatus;
import ru.mtuci.autonotesbackend.modules.notes.impl.repository.LectureNoteRepository;
import ru.mtuci.autonotesbackend.modules.user.impl.domain.User;
import ru.mtuci.autonotesbackend.modules.user.impl.repository.UserRepository;

@TestPropertySource(properties = {"app.notes.processing-timeout-minutes=10", "app.scheduling.cleanup-cron=-"})
class NoteCleanupServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private NoteCleanupService noteCleanupService;

    @Autowired
    private LectureNoteRepository noteRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldMarkOldProcessingNotesAsFailed() {
        User user = userRepository.save(User.builder()
                .username("time_traveler")
                .email("mcfly@example.com")
                .password("doc123")
                .build());

        LectureNote freshNote = noteRepository.save(LectureNote.builder()
                .user(user)
                .title("Fresh Note")
                .originalFileName("fresh.jpg")
                .fileStoragePath("1/fresh.jpg")
                .status(NoteStatus.PROCESSING)
                .build());

        LectureNote oldNote = noteRepository.save(LectureNote.builder()
                .user(user)
                .title("Stuck Note")
                .originalFileName("stuck.jpg")
                .fileStoragePath("1/stuck.jpg")
                .status(NoteStatus.PROCESSING)
                .build());

        jdbcTemplate.execute("ALTER TABLE lecture_notes DISABLE TRIGGER update_lecture_notes_updated_at");

        jdbcTemplate.update(
                "UPDATE lecture_notes SET updated_at = ? WHERE id = ?",
                OffsetDateTime.now().minusMinutes(20),
                oldNote.getId());

        jdbcTemplate.execute("ALTER TABLE lecture_notes ENABLE TRIGGER update_lecture_notes_updated_at");
        noteCleanupService.markStuckNotesAsFailed();

        LectureNote freshFromDb = noteRepository.findById(freshNote.getId()).orElseThrow();
        assertThat(freshFromDb.getStatus()).isEqualTo(NoteStatus.PROCESSING);

        LectureNote oldFromDb = noteRepository.findById(oldNote.getId()).orElseThrow();
        assertThat(oldFromDb.getStatus()).isEqualTo(NoteStatus.FAILED);
        assertThat(oldFromDb.getSummaryText()).contains("timed out");
    }
}
