package ru.mtuci.autonotesbackend.modules.notes.api.controller;

import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.mtuci.autonotesbackend.modules.notes.api.NoteFacade;
import ru.mtuci.autonotesbackend.modules.notes.api.dto.NoteDetailDto;
import ru.mtuci.autonotesbackend.modules.notes.api.dto.NoteDto;
import ru.mtuci.autonotesbackend.security.SecurityUser;

@RestController
@RequestMapping("/api/v1/notes")
@RequiredArgsConstructor
public class NoteController implements NoteResource {

    private final NoteFacade noteFacade;

    @Override
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<NoteDto> uploadNote(
            @RequestPart("title") String title,
            @RequestPart("file") MultipartFile file,
            @Parameter(hidden = true) @AuthenticationPrincipal SecurityUser securityUser) {

        NoteDto createdNote = noteFacade.createNote(title, file, securityUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdNote);
    }

    @Override
    @GetMapping
    public ResponseEntity<List<NoteDto>> getAllNotes(
            @Parameter(hidden = true) @AuthenticationPrincipal SecurityUser securityUser) {

        List<NoteDto> notes = noteFacade.findAllUserNotes(securityUser.getId());
        return ResponseEntity.ok(notes);
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<NoteDetailDto> getNoteById(
            @PathVariable Long id, @Parameter(hidden = true) @AuthenticationPrincipal SecurityUser securityUser) {

        NoteDetailDto noteDetail = noteFacade.getNoteById(id, securityUser.getId());
        return ResponseEntity.ok(noteDetail);
    }

    @Override
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNote(
            @PathVariable Long id, @Parameter(hidden = true) @AuthenticationPrincipal SecurityUser securityUser) {

        noteFacade.deleteNote(id, securityUser.getId());
        return ResponseEntity.noContent().build();
    }
}
