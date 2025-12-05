package ru.mtuci.autonotesbackend.modules.notes.impl;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import ru.mtuci.autonotesbackend.modules.filestorage.api.FileStorageFacade;
import ru.mtuci.autonotesbackend.modules.notes.api.NoteFacade;
import ru.mtuci.autonotesbackend.modules.notes.api.dto.NoteDetailDto;
import ru.mtuci.autonotesbackend.modules.notes.api.dto.NoteDto;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.LectureNote;
import ru.mtuci.autonotesbackend.modules.notes.impl.mapper.NoteMapper;
import ru.mtuci.autonotesbackend.modules.notes.impl.service.NoteService;

@Slf4j
@Component
@RequiredArgsConstructor
public class NoteFacadeImpl implements NoteFacade {

    private final NoteService noteService;
    private final FileStorageFacade fileStorageFacade;
    private final NoteMapper noteMapper;

    @Override
    public NoteDto createNote(String title, MultipartFile file, Long userId) {
        String filePath = fileStorageFacade.save(file, userId);

        try {
            LectureNote newNote = noteService.createNote(title, file, filePath, userId);
            return noteMapper.toDto(newNote);
        } catch (Exception e) {
            log.error("Business logic failed. Attempting to rollback file: {}", filePath, e);
            try {
                fileStorageFacade.delete(filePath);
            } catch (Exception deleteEx) {
                log.error("Failed to delete file during rollback: {}", filePath, deleteEx);
            }
            throw e;
        }
    }

    @Override
    public List<NoteDto> findAllUserNotes(Long userId) {
        return noteService.findAllDtosByUserId(userId);
    }

    @Override
    public NoteDetailDto getNoteById(Long noteId, Long userId) {
        LectureNote note = noteService.findByIdAndUserId(noteId, userId);
        return noteMapper.toDetailDto(note);
    }

    @Override
    public void deleteNote(Long noteId, Long userId) {
        noteService.deleteByIdAndUserId(noteId, userId);
    }
}
