package ru.mtuci.autonotesbackend.modules.notes.impl.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.mtuci.autonotesbackend.BaseIntegrationTest;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.LectureNote;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.NoteImage;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.NoteStatus;
import ru.mtuci.autonotesbackend.modules.user.impl.domain.User;
import ru.mtuci.autonotesbackend.modules.user.impl.repository.UserRepository;

class NoteImageRepositoryTest extends BaseIntegrationTest {

    @Autowired
    private NoteImageRepository noteImageRepository;

    @Autowired
    private LectureNoteRepository lectureNoteRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void findExistingPaths_shouldReturnOnlyPathsPresentInDb() {
        // Arrange
        User user = userRepository.save(User.builder()
                .username("repo_test")
                .email("repo@test.com")
                .password("pass")
                .build());

        LectureNote note = LectureNote.builder()
                .user(user)
                .title("Note 1")
                .status(NoteStatus.COMPLETED)
                .build();

        note.addImage(NoteImage.builder()
                .originalFileName("1.jpg")
                .fileStoragePath("path/exist_1.jpg")
                .orderIndex(0)
                .build());

        note.addImage(NoteImage.builder()
                .originalFileName("2.jpg")
                .fileStoragePath("path/exist_2.jpg")
                .orderIndex(1)
                .build());

        lectureNoteRepository.save(note);

        List<String> pathsToCheck = List.of("path/exist_1.jpg", "path/exist_2.jpg", "path/phantom_file.jpg");

        // Act
        var result = noteImageRepository.findExistingPaths(pathsToCheck);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyInAnyOrder("path/exist_1.jpg", "path/exist_2.jpg");
    }
}
