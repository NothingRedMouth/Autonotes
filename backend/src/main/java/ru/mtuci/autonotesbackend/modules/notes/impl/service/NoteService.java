package ru.mtuci.autonotesbackend.modules.notes.impl.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import ru.mtuci.autonotesbackend.exception.ResourceNotFoundException;
import ru.mtuci.autonotesbackend.modules.filestorage.api.FileStorageFacade;
import ru.mtuci.autonotesbackend.modules.filestorage.api.exception.InvalidFileFormatException;
import ru.mtuci.autonotesbackend.modules.notes.api.dto.NoteDto;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.LectureNote;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.NoteImage;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.NoteStatus;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.OutboxEvent;
import ru.mtuci.autonotesbackend.modules.notes.impl.dto.NoteResultDto;
import ru.mtuci.autonotesbackend.modules.notes.impl.event.NoteProcessingEvent;
import ru.mtuci.autonotesbackend.modules.notes.impl.mapper.NoteMapper;
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
    private final FileStorageFacade fileStorageFacade;
    private final TransactionTemplate transactionTemplate;
    private final NoteMapper noteMapper;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    public LectureNote createNote(String title, List<MultipartFile> files, Long userId) {
        if (files == null || files.isEmpty()) {
            throw new InvalidFileFormatException("At least one image file is required.");
        }

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                throw new InvalidFileFormatException("File '" + file.getOriginalFilename() + "' is empty.");
            }
        }

        List<String> uploadedPaths = new ArrayList<>();

        try {
            for (MultipartFile file : files) {
                String path = fileStorageFacade.save(file, userId);
                uploadedPaths.add(path);
            }
        } catch (Exception e) {
            log.error("Error during file upload sequence. Rolling back {} uploaded files.", uploadedPaths.size(), e);
            rollbackS3Uploads(uploadedPaths);
            throw e;
        }

        try {
            return transactionTemplate.execute(ignored -> {
                User user = userRepository.getReferenceById(userId);

                LectureNote note = LectureNote.builder()
                        .title(title)
                        .user(user)
                        .status(NoteStatus.PROCESSING)
                        .build();

                for (int i = 0; i < files.size(); i++) {
                    NoteImage image = NoteImage.builder()
                            .fileStoragePath(uploadedPaths.get(i))
                            .originalFileName(files.get(i).getOriginalFilename())
                            .orderIndex(i)
                            .build();
                    note.addImage(image);
                }

                LectureNote savedNote = noteRepository.save(note);

                saveOutboxEvent(savedNote, uploadedPaths);

                return savedNote;
            });

        } catch (Exception e) {
            log.error("Database transaction failed. Rolling back S3 uploads.", e);
            rollbackS3Uploads(uploadedPaths);
            throw e;
        }
    }

    @Transactional
    public void processCompletion(NoteResultDto result) {
        log.info("Processing ML result for noteId: {}", result.getNoteId());

        noteRepository.findById(result.getNoteId()).ifPresentOrElse(note -> {
            if (note.getStatus() != NoteStatus.PROCESSING) {
                log.warn("Note {} is already in status {}. Ignoring duplicate result.", note.getId(), note.getStatus());
                return;
            }

            if ("COMPLETED".equalsIgnoreCase(result.getStatus())) {
                note.setStatus(NoteStatus.COMPLETED);
                note.setRecognizedText(result.getRecognizedText());
                note.setSummaryText(result.getSummaryText());
                log.info("Note {} successfully updated with ML data.", note.getId());
            } else {
                note.setStatus(NoteStatus.FAILED);
                String errorMsg = result.getErrorMessage() != null ? result.getErrorMessage() : "Unknown ML Error";
                note.setSummaryText("Processing failed: " + errorMsg);
                log.error("Note {} failed processing. Reason: {}", note.getId(), errorMsg);
            }

            noteRepository.save(note);

        }, () -> log.warn("Received result for non-existent (or deleted) noteId: {}. Skipping.", result.getNoteId()));
    }

    private void rollbackS3Uploads(List<String> paths) {
        for (String path : paths) {
            try {
                fileStorageFacade.delete(path);
            } catch (Exception ex) {
                log.error("Failed to delete file during rollback: {}", path, ex);
            }
        }
    }

    private void saveOutboxEvent(LectureNote note, List<String> filePaths) {
        try {
            NoteProcessingEvent eventPayload = new NoteProcessingEvent(note.getId(), bucketName, filePaths);

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
        List<LectureNote> notes = noteRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return noteMapper.toDtoList(notes);
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
