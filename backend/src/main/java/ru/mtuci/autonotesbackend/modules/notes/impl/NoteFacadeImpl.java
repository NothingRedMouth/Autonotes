package ru.mtuci.autonotesbackend.modules.notes.impl;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import ru.mtuci.autonotesbackend.modules.notes.api.NoteFacade;
import ru.mtuci.autonotesbackend.modules.notes.api.dto.NoteDetailDto;
import ru.mtuci.autonotesbackend.modules.notes.api.dto.NoteDto;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.LectureNote;
import ru.mtuci.autonotesbackend.modules.notes.impl.mapper.NoteMapper;
import ru.mtuci.autonotesbackend.modules.notes.impl.service.NoteService;

@Component
@RequiredArgsConstructor
public class NoteFacadeImpl implements NoteFacade {

    private final NoteService noteService;
    private final NoteMapper noteMapper;

    @Override
    public NoteDto createNote(String title, List<MultipartFile> files, Long userId) {
        LectureNote newNote = noteService.createNote(title, files, userId);
        return noteMapper.toDto(newNote);
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
