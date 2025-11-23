package ru.mtuci.autonotesbackend.modules.notes.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import ru.mtuci.autonotesbackend.modules.filestorage.api.FileStorageFacade;
import ru.mtuci.autonotesbackend.modules.notes.impl.service.NoteService;

@ExtendWith(MockitoExtension.class)
class NoteFacadeImplTest {

    @Mock
    private NoteService noteService;

    @Mock
    private FileStorageFacade fileStorageFacade;

    @InjectMocks
    private NoteFacadeImpl noteFacade;

    @Test
    void createNote_whenDbSaveFails_shouldCallFileDelete() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "content".getBytes());
        String title = "Test Title";
        Long userId = 1L;
        String filePath = "1/some-uuid.txt";

        when(fileStorageFacade.save(file, userId)).thenReturn(filePath);

        when(noteService.createNote(title, file, filePath, userId))
                .thenThrow(new RuntimeException("Simulated DB Error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> noteFacade.createNote(title, file, userId));

        verify(fileStorageFacade, times(1)).delete(filePath);
    }
}
