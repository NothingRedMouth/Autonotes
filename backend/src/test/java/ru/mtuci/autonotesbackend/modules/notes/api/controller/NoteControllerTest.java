package ru.mtuci.autonotesbackend.modules.notes.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;
import ru.mtuci.autonotesbackend.BaseIntegrationTest;
import ru.mtuci.autonotesbackend.modules.notes.api.dto.NoteDto;
import ru.mtuci.autonotesbackend.modules.notes.api.dto.NoteImageDto;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.LectureNote;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.NoteStatus;
import ru.mtuci.autonotesbackend.modules.notes.impl.repository.LectureNoteRepository;
import ru.mtuci.autonotesbackend.modules.user.api.dto.AuthRequestDto;
import ru.mtuci.autonotesbackend.modules.user.api.dto.AuthResponseDto;
import ru.mtuci.autonotesbackend.modules.user.impl.domain.User;
import ru.mtuci.autonotesbackend.modules.user.impl.repository.UserRepository;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;

class NoteControllerTest extends BaseIntegrationTest {

    private final List<String> uploadedFilePaths = new ArrayList<>();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LectureNoteRepository noteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String testBucketName;

    @AfterEach
    void cleanup() {
        uploadedFilePaths.forEach(path -> {
            try {
                s3Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(testBucketName)
                        .key(path)
                        .build());
            } catch (Exception ignored) {
            }
        });
        uploadedFilePaths.clear();
        noteRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void uploadNote_shouldCreateNoteWithMultipleFiles() throws Exception {
        User user = createUserInDb("multi-user", "multi@test.com");
        String token = loginAndGetToken("multi-user");

        byte[] content = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
        MockMultipartFile file1 = new MockMultipartFile("files", "page1.jpg", MediaType.IMAGE_JPEG_VALUE, content);
        MockMultipartFile file2 = new MockMultipartFile("files", "page2.jpg", MediaType.IMAGE_JPEG_VALUE, content);
        MockPart titlePart = new MockPart("title", "Multi Page Lecture".getBytes());

        MvcResult result = mockMvc.perform(multipart("/api/v1/notes")
                        .file(file1)
                        .file(file2)
                        .part(titlePart)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Multi Page Lecture"))
                .andExpect(jsonPath("$.images", hasSize(2)))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        NoteDto responseDto = objectMapper.readValue(responseBody, NoteDto.class);

        LectureNote savedNote = noteRepository.findById(responseDto.getId()).orElseThrow();
        assertThat(savedNote.getImages()).hasSize(2);

        for (NoteImageDto img : responseDto.getImages()) {
            LectureNote note = noteRepository.findById(responseDto.getId()).orElseThrow();
            String path = note.getImages().stream()
                    .filter(i -> i.getId().equals(img.getId()))
                    .findFirst()
                    .orElseThrow()
                    .getFileStoragePath();
            uploadedFilePaths.add(path);
        }

        String firstFilePath = savedNote.getImages().getFirst().getFileStoragePath();
        s3Client.headObject(HeadObjectRequest.builder()
                .bucket(testBucketName)
                .key(firstFilePath)
                .build());
    }

    @Test
    void getAllNotes_shouldReturnUserNotes() throws Exception {
        User user = createUserInDb("list-user", "list@test.com");
        String token = loginAndGetToken("list-user");

        createNoteInDb(user, "Note 1");
        createNoteInDb(user, "Note 2");

        mockMvc.perform(get("/api/v1/notes").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title").exists());
    }

    @Test
    void getNoteById_shouldReturnDetails() throws Exception {
        User user = createUserInDb("detail-user", "detail@test.com");
        String token = loginAndGetToken("detail-user");
        LectureNote note = createNoteInDb(user, "My Note");

        mockMvc.perform(get("/api/v1/notes/" + note.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(note.getId()))
                .andExpect(jsonPath("$.title").value("My Note"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void getNoteById_whenNotFound_shouldReturn404() throws Exception {
        createUserInDb("404-user", "404@test.com");
        String token = loginAndGetToken("404-user");

        mockMvc.perform(get("/api/v1/notes/999999").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteNote_shouldRemoveNote() throws Exception {
        User user = createUserInDb("del-user", "del@test.com");
        String token = loginAndGetToken("del-user");
        LectureNote note = createNoteInDb(user, "To Delete");

        mockMvc.perform(delete("/api/v1/notes/" + note.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        entityManager.flush();
        entityManager.clear();

        assertThat(noteRepository.findById(note.getId())).isEmpty();

        java.sql.Timestamp deletedAt = jdbcTemplate.queryForObject(
                "SELECT deleted_at FROM lecture_notes WHERE id = ?", java.sql.Timestamp.class, note.getId());

        assertThat(deletedAt).isNotNull();
    }

    private User createUserInDb(String username, String email) {
        return userRepository.save(User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode("password123"))
                .build());
    }

    private LectureNote createNoteInDb(User user, String title) {
        return noteRepository.save(LectureNote.builder()
                .user(user)
                .title(title)
                .status(NoteStatus.COMPLETED)
                .build());
    }

    private String loginAndGetToken(String username) throws Exception {
        AuthRequestDto authRequest = new AuthRequestDto(username, "password123");
        MvcResult result = mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(authRequest)))
                .andReturn();
        return objectMapper
                .readValue(result.getResponse().getContentAsString(), AuthResponseDto.class)
                .getToken();
    }
}
