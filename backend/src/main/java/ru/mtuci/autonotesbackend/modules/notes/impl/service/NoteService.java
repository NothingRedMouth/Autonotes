package ru.mtuci.autonotesbackend.modules.notes.impl.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.mtuci.autonotesbackend.exception.ResourceNotFoundException;
import ru.mtuci.autonotesbackend.modules.notes.api.dto.NoteDto;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.LectureNote;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.NoteStatus;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.OutboxEvent;
import ru.mtuci.autonotesbackend.modules.notes.impl.event.NoteProcessingEvent;
import ru.mtuci.autonotesbackend.modules.notes.impl.repository.LectureNoteRepository;
import ru.mtuci.autonotesbackend.modules.notes.impl.repository.OutboxEventRepository;
import ru.mtuci.autonotesbackend.modules.user.impl.domain.User;
import ru.mtuci.autonotesbackend.modules.user.impl.repository.UserRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoteService {

    private final LectureNoteRepository noteRepository;
    private final UserRepository userRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

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

        saveOutboxEvent(savedNote);

        return savedNote;
    }

    private void saveOutboxEvent(LectureNote note) {
        try {
            NoteProcessingEvent eventPayload =
                    new NoteProcessingEvent(note.getId(), bucketName, note.getFileStoragePath());

            String jsonPayload = objectMapper.writeValueAsString(eventPayload);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateId(note.getId())
                    .eventType("NOTE_CREATED")
                    .payload(jsonPayload)
                    .build();

            outboxEventRepository.save(outboxEvent);
            log.info("Outbox event saved for noteId: {}", note.getId());

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event payload", e);
        }
    }

    @Transactional(readOnly = true)
    public List<NoteDto> findAllDtosByUserId(Long userId) {
        return noteRepository.findAllProjectedByUserId(userId).stream()
                .map(this::mapProjectionToDto)
                .toList();
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
    public void deleteByIdAndUserId(Long noteId, Long userId) {
        LectureNote noteToDelete = findByIdAndUserId(noteId, userId);
        noteRepository.delete(noteToDelete);
    }
}
