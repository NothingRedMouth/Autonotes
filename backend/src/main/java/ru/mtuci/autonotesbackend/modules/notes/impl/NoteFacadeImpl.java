package ru.mtuci.autonotesbackend.modules.notes.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.mtuci.autonotesbackend.modules.filestorage.api.FileStorageFacade;
import ru.mtuci.autonotesbackend.modules.notes.api.NoteFacade;
import ru.mtuci.autonotesbackend.modules.notes.api.dto.NoteDetailDto;
import ru.mtuci.autonotesbackend.modules.notes.api.dto.NoteDto;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.LectureNote;
import ru.mtuci.autonotesbackend.modules.notes.impl.mapper.NoteMapper;
import ru.mtuci.autonotesbackend.modules.notes.impl.service.NoteService;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class NoteFacadeImpl implements NoteFacade {

    private final NoteService noteService;
    private final FileStorageFacade fileStorageFacade;
    private final NoteMapper noteMapper;

    @Override
    @Transactional
    public NoteDto createNote(String title, MultipartFile file, Long userId) {
        String filePath = fileStorageFacade.save(file, userId);

        try {
            LectureNote newNote = noteService.createNote(title, file, filePath, userId);

            return noteMapper.toDto(newNote);
        } catch (Exception e) {
            log.error("Business logic failed. Rolling back file upload: {}", filePath, e);
            fileStorageFacade.delete(filePath);
            throw e;
        }
    }

    @Override
    public List<NoteDto> findAllUserNotes(Long userId) {
        List<LectureNote> notes = noteService.findAllByUserId(userId);
        return noteMapper.toDtoList(notes);
    }

    @Override
    public NoteDetailDto getNoteById(Long noteId, Long userId) {
        LectureNote note = noteService.findByIdAndUserId(noteId, userId);
        return noteMapper.toDetailDto(note);
    }

    @Override
    @Transactional
    public void deleteNote(Long noteId, Long userId) {
        String filePath = noteService.deleteByIdAndUserId(noteId, userId);
        fileStorageFacade.delete(filePath);
    }
}
