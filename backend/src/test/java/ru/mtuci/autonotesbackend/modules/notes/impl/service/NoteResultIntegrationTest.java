package ru.mtuci.autonotesbackend.modules.notes.impl.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.mtuci.autonotesbackend.BaseIntegrationTest;
import ru.mtuci.autonotesbackend.modules.notes.impl.config.RabbitMqConfig;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.LectureNote;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.NoteStatus;
import ru.mtuci.autonotesbackend.modules.notes.impl.dto.NoteResultDto;
import ru.mtuci.autonotesbackend.modules.notes.impl.repository.LectureNoteRepository;
import ru.mtuci.autonotesbackend.modules.user.impl.domain.User;
import ru.mtuci.autonotesbackend.modules.user.impl.repository.UserRepository;

@Transactional(propagation = Propagation.NOT_SUPPORTED)
class NoteResultIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private LectureNoteRepository noteRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @AfterEach
    void tearDown() {
        noteRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldUpdateNoteStatus_WhenMlResultReceived() {
        User user = userRepository.save(User.builder()
            .username("async_user")
            .email("async@test.com")
            .password("pass")
            .build());

        LectureNote note = noteRepository.save(LectureNote.builder()
            .user(user)
            .title("Async Note")
            .status(NoteStatus.PROCESSING)
            .build());

        Long noteId = note.getId();

        NoteResultDto resultDto = NoteResultDto.builder()
            .noteId(noteId)
            .status("COMPLETED")
            .recognizedText("# Success\nText recognized.")
            .summaryText("Short summary.")
            .build();

        rabbitTemplate.convertAndSend(
            RabbitMqConfig.EXCHANGE_NOTES,
            RabbitMqConfig.ROUTING_KEY_RESULTS,
            resultDto
        );

        await()
            .atMost(10, TimeUnit.SECONDS)
            .pollInterval(Duration.ofMillis(500))
            .untilAsserted(() -> {
                LectureNote updatedNote = noteRepository.findById(noteId).orElseThrow();

                assertThat(updatedNote.getStatus()).isEqualTo(NoteStatus.COMPLETED);
                assertThat(updatedNote.getRecognizedText()).isEqualTo("# Success\nText recognized.");
            });
    }
}
