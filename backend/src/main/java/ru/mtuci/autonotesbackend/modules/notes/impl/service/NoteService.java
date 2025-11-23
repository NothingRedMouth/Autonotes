package ru.mtuci.autonotesbackend.modules.notes.impl.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.mtuci.autonotesbackend.exception.ResourceNotFoundException;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.LectureNote;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.NoteStatus;
import ru.mtuci.autonotesbackend.modules.notes.impl.repository.LectureNoteRepository;
import ru.mtuci.autonotesbackend.modules.user.impl.domain.User;
import ru.mtuci.autonotesbackend.modules.user.impl.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class NoteService {

    private final LectureNoteRepository noteRepository;
    private final UserRepository userRepository;

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

        return noteRepository.save(note);
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
