package ru.mtuci.autonotesbackend.modules.notes.impl.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.mtuci.autonotesbackend.config.RabbitMqConfig;
import ru.mtuci.autonotesbackend.exception.ResourceNotFoundException;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.LectureNote;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.NoteStatus;
import ru.mtuci.autonotesbackend.modules.notes.impl.event.NoteProcessingEvent;
import ru.mtuci.autonotesbackend.modules.notes.impl.repository.LectureNoteRepository;
import ru.mtuci.autonotesbackend.modules.user.impl.domain.User;
import ru.mtuci.autonotesbackend.modules.user.impl.repository.UserRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoteService {

    private final LectureNoteRepository noteRepository;
    private final UserRepository userRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Transactional
    public LectureNote createNote(String title, MultipartFile file, String filePath, Long userId) {
        User user = userRepository.getReferenceById(userId);

        LectureNote note = LectureNote.builder()
            .title(title)
            .user(user)
            .originalFileName(file.getOriginalFilename())
            .fileStoragePath(filePath)
            .status(NoteStatus.PROCESSING)
            .build();

        LectureNote savedNote = noteRepository.save(note);

        sendProcessingEvent(savedNote);

        return savedNote;
    }

    private void sendProcessingEvent(LectureNote note) {
        try {
            NoteProcessingEvent event = new NoteProcessingEvent(
                note.getId(),
                bucketName,
                note.getFileStoragePath()
            );

            rabbitTemplate.convertAndSend(
                RabbitMqConfig.EXCHANGE_NOTES,
                RabbitMqConfig.ROUTING_KEY_PROCESS,
                event
            );
            log.info("Event sent to RabbitMQ for noteId: {}", note.getId());
        } catch (Exception e) {
            log.error("Failed to send event to RabbitMQ for noteId: {}. Fallback to scheduler.", note.getId(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<LectureNote> findAllByUserId(Long userId) {
        return noteRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public LectureNote findByIdAndUserId(Long noteId, Long userId) {
        return noteRepository
            .findByIdAndUserId(noteId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Note not found with id: " + noteId));
    }

    @Transactional
    public String deleteByIdAndUserId(Long noteId, Long userId) {
        LectureNote noteToDelete = findByIdAndUserId(noteId, userId);
        String filePath = noteToDelete.getFileStoragePath();
        noteRepository.delete(noteToDelete);
        return filePath;
    }
}
