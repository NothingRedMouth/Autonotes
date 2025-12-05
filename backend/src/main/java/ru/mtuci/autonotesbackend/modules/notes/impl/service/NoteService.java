package ru.mtuci.autonotesbackend.modules.notes.impl.service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.mtuci.autonotesbackend.config.RabbitMqConfig;
import ru.mtuci.autonotesbackend.exception.ResourceNotFoundException;
import ru.mtuci.autonotesbackend.modules.notes.api.dto.NoteDto;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.LectureNote;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.NoteStatus;
import ru.mtuci.autonotesbackend.modules.notes.impl.event.NoteProcessingEvent;
import ru.mtuci.autonotesbackend.modules.notes.impl.repository.LectureNoteRepository;
import ru.mtuci.autonotesbackend.modules.user.impl.domain.User;
import ru.mtuci.autonotesbackend.modules.user.impl.repository.UserRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoteService {

    private final LectureNoteRepository noteRepository;
    private final UserRepository userRepository;
    private final RabbitTemplate rabbitTemplate;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;

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
        NoteProcessingEvent event = new NoteProcessingEvent(note.getId(), bucketName, note.getFileStoragePath());

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                rabbitTemplate.convertAndSend(RabbitMqConfig.EXCHANGE_NOTES, RabbitMqConfig.ROUTING_KEY_PROCESS, event);
                log.info("Event sent to RabbitMQ for noteId: {}", note.getId());
                return;
            } catch (AmqpException e) {
                log.warn("Attempt {}/{} failed to send event to RabbitMQ for noteId: {}. Error: {}",
                    attempt, MAX_RETRIES, note.getId(), e.getMessage());

                if (attempt == MAX_RETRIES) {
                    log.error("All attempts failed. Note {} will be handled by CleanupService.", note.getId(), e);
                } else {
                    try {
                        TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } catch (Exception e) {
                log.error("Fatal error sending event for noteId: {}", note.getId(), e);
                break;
            }
        }
    }

    @Transactional(readOnly = true)
    public List<NoteDto> findAllDtosByUserId(Long userId) {
        return noteRepository.findAllProjectedByUserId(userId).stream()
            .map(this::mapProjectionToDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<LectureNote> findAllByUserId(Long userId) {
        return noteRepository.findByUserId(userId);
    }

    private NoteDto mapProjectionToDto(LectureNoteRepository.NoteProjection view) {
        NoteDto dto = new NoteDto();
        dto.setId(view.getId());
        dto.setUserId(view.getUserId());
        dto.setTitle(view.getTitle());
        dto.setOriginalFileName(view.getOriginalFileName());
        dto.setStatus(view.getStatus());
        dto.setCreatedAt(view.getCreatedAt());
        return dto;
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
