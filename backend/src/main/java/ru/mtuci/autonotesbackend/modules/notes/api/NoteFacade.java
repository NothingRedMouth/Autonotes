package ru.mtuci.autonotesbackend.modules.notes.api;

import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import ru.mtuci.autonotesbackend.modules.notes.api.dto.NoteDetailDto;
import ru.mtuci.autonotesbackend.modules.notes.api.dto.NoteDto;

public interface NoteFacade {
    NoteDto createNote(String title, List<MultipartFile> files, Long userId);

    List<NoteDto> findAllUserNotes(Long userId);

    NoteDetailDto getNoteById(Long noteId, Long userId);

    void deleteNote(Long noteId, Long userId);
}
