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
    void findExistingPaths_shouldReturnOnlyPathsPresentInDb() {
        // Arrange
        User user = userRepository.save(User.builder()
                .username("batch_check")
                .email("batch@test.com")
                .password("pass")
                .build());

        lectureNoteRepository.save(LectureNote.builder()
                .user(user)
                .title("Note 1")
                .originalFileName("1.jpg")
                .fileStoragePath("path/exist_1.jpg")
                .status(NoteStatus.COMPLETED)
                .build());

        lectureNoteRepository.save(LectureNote.builder()
                .user(user)
                .title("Note 2")
                .originalFileName("2.jpg")
                .fileStoragePath("path/exist_2.jpg")
                .status(NoteStatus.COMPLETED)
                .build());

        List<String> pathsToCheck =
                List.of("path/exist_1.jpg", "path/exist_2.jpg", "path/phantom_file.jpg", "path/another_fake.jpg");

        // Act
        var result = lectureNoteRepository.findExistingPaths(pathsToCheck);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder("path/exist_1.jpg", "path/exist_2.jpg");
        assertThat(result).doesNotContain("path/phantom_file.jpg");
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
