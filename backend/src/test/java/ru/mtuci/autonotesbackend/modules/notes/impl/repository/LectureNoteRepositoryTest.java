package ru.mtuci.autonotesbackend.modules.notes.impl.repository;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.mtuci.autonotesbackend.BaseIntegrationTest;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.LectureNote;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.NoteStatus;
import ru.mtuci.autonotesbackend.modules.user.impl.domain.User;
import ru.mtuci.autonotesbackend.modules.user.impl.repository.UserRepository;

class LectureNoteRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private LectureNoteRepository lectureNoteRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findAllProjectedByUserId_shouldReturnCorrectProjection() {
        // Arrange
        User user = userRepository.save(User.builder()
                .username("proj_user")
                .email("proj@test.com")
                .password("pass")
                .build());

        lectureNoteRepository.save(LectureNote.builder()
                .user(user)
                .title("Projection Title")
                .originalFileName("orig.jpg")
                .fileStoragePath("path/1")
                .status(NoteStatus.PROCESSING)
                .build());

        // Act
        List<LectureNoteRepository.NoteProjection> result =
                lectureNoteRepository.findAllProjectedByUserId(user.getId());

        // Assert
        assertThat(result).hasSize(1);
        LectureNoteRepository.NoteProjection projection = result.getFirst();

        assertThat(projection.getTitle()).isEqualTo("Projection Title");
        assertThat(projection.getStatus()).isEqualTo(NoteStatus.PROCESSING);
        assertThat(projection.getOriginalFileName()).isEqualTo("orig.jpg");
        assertThat(projection.getUserId()).isEqualTo(user.getId());
        assertThat(projection.getCreatedAt()).isNotNull();
    }

    @Test
    void hardDeleteById_shouldPhysicallyDeleteRecord() {
        // Arrange
        User user = userRepository.save(User.builder()
                .username("del_user")
                .email("del@test.com")
                .password("pass")
                .build());

        LectureNote note = lectureNoteRepository.save(LectureNote.builder()
                .user(user)
                .title("To Delete")
                .originalFileName("file.jpg")
                .fileStoragePath("path/del")
                .status(NoteStatus.COMPLETED)
                .build());

        Long noteId = note.getId();

        // Act
        lectureNoteRepository.hardDeleteById(noteId);

        entityManager.clear();

        // Assert
        assertThat(lectureNoteRepository.findById(noteId)).isEmpty();

        Integer count =
                jdbcTemplate.queryForObject("SELECT count(*) FROM lecture_notes WHERE id = ?", Integer.class, noteId);
        assertThat(count).isEqualTo(0);
    }
}
