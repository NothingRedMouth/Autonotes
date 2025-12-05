package ru.mtuci.autonotesbackend.app.web.notes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;
import ru.mtuci.autonotesbackend.BaseIntegrationTest;
import ru.mtuci.autonotesbackend.modules.notes.api.dto.NoteDto;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.LectureNote;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.NoteStatus;
import ru.mtuci.autonotesbackend.modules.notes.impl.repository.LectureNoteRepository;
import ru.mtuci.autonotesbackend.modules.user.api.dto.AuthRequestDto;
import ru.mtuci.autonotesbackend.modules.user.api.dto.AuthResponseDto;
import ru.mtuci.autonotesbackend.modules.user.impl.domain.User;
import ru.mtuci.autonotesbackend.modules.user.impl.repository.UserRepository;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

class NoteControllerTest extends BaseIntegrationTest {

    private final List<String> uploadedFilePaths = new ArrayList<>();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LectureNoteRepository noteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String testBucketName;

    @AfterEach
    void cleanup() {
        uploadedFilePaths.forEach(path -> {
            try {
                DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                        .bucket(testBucketName)
                        .key(path)
                        .build();
                s3Client.deleteObject(deleteRequest);
            } catch (Exception ignored) {
            }
        });
        uploadedFilePaths.clear();
    }

    @Test
    void uploadNote_whenAuthenticated_shouldCreateNoteAndUploadFile() throws Exception {
        // Arrange
        User user = createUserInDb("note-user", "note@test.com");
        String token = loginAndGetToken();

        byte[] jpegContent = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x01, 0x02};

        MockMultipartFile file = new MockMultipartFile("file", "hello.jpg", MediaType.IMAGE_JPEG_VALUE, jpegContent);

        MockPart titlePart = new MockPart("title", "My First Lecture".getBytes());

        // Act
        MvcResult result = mockMvc.perform(multipart("/api/v1/notes")
                        .file(file)
                        .part(titlePart)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("My First Lecture"))
                .andExpect(jsonPath("$.originalFileName").value("hello.jpg"))
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andReturn();

        // Assert - DB Check
        String responseBody = result.getResponse().getContentAsString();
        NoteDto responseDto = objectMapper.readValue(responseBody, NoteDto.class);
        uploadedFilePaths.add(getFilePathFromDto(responseDto.getId()));

        LectureNote savedNote = noteRepository.findById(responseDto.getId()).orElseThrow();
        assertThat(savedNote.getUser().getId()).isEqualTo(user.getId());
        assertThat(savedNote.getTitle()).isEqualTo("My First Lecture");

        // Assert - MinIO Check
        HeadObjectRequest headRequest = HeadObjectRequest.builder()
                .bucket(testBucketName)
                .key(savedNote.getFileStoragePath())
                .build();
        s3Client.headObject(headRequest);
    }

    @Test
    void uploadNote_whenUnauthenticated_shouldReturnUnauthorized() throws Exception {
        MockMultipartFile file =
                new MockMultipartFile("file", "test.jpg", MediaType.IMAGE_JPEG_VALUE, "content".getBytes());
        MockPart titlePart = new MockPart("title", "some title".getBytes());

        mockMvc.perform(multipart("/api/v1/notes").file(file).part(titlePart)).andExpect(status().isUnauthorized());
    }

    @Test
    void uploadNote_whenFilePartIsMissing_shouldReturnBadRequest() throws Exception {
        String token = loginAndGetToken("test-user", true);
        MockPart titlePart = new MockPart("title", "some title".getBytes());

        mockMvc.perform(multipart("/api/v1/notes").part(titlePart).header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadNote_withEmptyFile_shouldReturnBadRequest() throws Exception {
        String token = loginAndGetToken("test-user", true);
        MockMultipartFile emptyFile =
                new MockMultipartFile("file", "empty.txt", MediaType.TEXT_PLAIN_VALUE, new byte[0]);
        MockPart titlePart = new MockPart("title", "Empty File Test".getBytes());

        mockMvc.perform(multipart("/api/v1/notes")
                        .file(emptyFile)
                        .part(titlePart)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot save an empty file."));
    }

    @Test
    void uploadNote_whenMinioFailsOnSave_shouldReturnServiceUnavailable() throws Exception {
        // Arrange
        String token = loginAndGetToken("fail-user", true);

        byte[] jpegContent = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x01, 0x02};

        MockMultipartFile file = new MockMultipartFile("file", "fail.jpg", MediaType.IMAGE_JPEG_VALUE, jpegContent);

        MockPart titlePart = new MockPart("title", "Failing Upload".getBytes());

        doThrow(SdkException.builder().message("MinIO is down").build())
                .when(s3Client)
                .putObject(any(PutObjectRequest.class), any(RequestBody.class));

        // Act & Assert
        mockMvc.perform(multipart("/api/v1/notes")
                        .file(file)
                        .part(titlePart)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("File storage service is currently unavailable."));
    }

    @Test
    void getAllNotes_whenAuthenticatedAndHasNotes_shouldReturnNoteList() throws Exception {
        // Arrange
        User user = createUserInDb("list-user", "list@test.com");
        createNoteInDb("Math Lecture", "1/math.jpg", user);
        createNoteInDb("History Lecture", "1/history.jpg", user);
        String token = loginAndGetToken("list-user");

        // Act & Assert
        mockMvc.perform(get("/api/v1/notes").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].title", containsInAnyOrder("Math Lecture", "History Lecture")))
                .andExpect(jsonPath("$[0].userId").value(user.getId()));
    }

    @Test
    void getAllNotes_whenAuthenticatedAndHasNoNotes_shouldReturnEmptyList() throws Exception {
        // Arrange
        createUserInDb("empty-user", "empty@test.com");
        String token = loginAndGetToken("empty-user");

        // Act & Assert
        mockMvc.perform(get("/api/v1/notes").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getAllNotes_whenAuthenticated_shouldNotReturnOtherUsersNotes() throws Exception {
        // Arrange
        User userA = createUserInDb("userA", "a@test.com");
        User userB = createUserInDb("userB", "b@test.com");
        createNoteInDb("Note from User B", "b/secret.jpg", userB);
        String tokenForUserA = loginAndGetToken("userA");

        // Act & Assert
        mockMvc.perform(get("/api/v1/notes").header("Authorization", "Bearer " + tokenForUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getAllNotes_whenUnauthenticated_shouldReturnUnauthorized() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/notes")).andExpect(status().isUnauthorized());
    }

    @Test
    void deleteNote_whenOwner_shouldSoftDeleteNoteAndKeepFile() throws Exception {
        // Arrange
        User user = createUserInDb("delete-user", "delete@test.com");
        String filePath = "delete-user/to-be-deleted.jpg";
        LectureNote note = createNoteInDb("Note to Delete", filePath, user);
        String token = loginAndGetToken("delete-user");

        // Act
        mockMvc.perform(delete("/api/v1/notes/" + note.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Assert - DB Check (Logically Deleted)
        assertThat(noteRepository.findById(note.getId())).isEmpty();

        // Assert - DB Check (Physically Exists)
        assertThat(noteRepository.existsByFileStoragePath(filePath)).isTrue();

        // Assert - MinIO Check
        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void deleteNote_whenNotOwner_shouldReturnNotFound() throws Exception {
        // Arrange
        User owner = createUserInDb("owner", "owner@test.com");
        User attacker = createUserInDb("attacker", "attacker@test.com");
        LectureNote note = createNoteInDb("Secret Note", "owner/secret.jpg", owner);
        String attackerToken = loginAndGetToken("attacker");

        // Act & Assert
        mockMvc.perform(delete("/api/v1/notes/" + note.getId()).header("Authorization", "Bearer " + attackerToken))
                .andExpect(status().isNotFound());

        // Assert - DB and MinIO state unchanged
        assertThat(noteRepository.findById(note.getId())).isPresent();
        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void deleteNote_whenNoteNotFound_shouldReturnNotFound() throws Exception {
        // Arrange
        String token = loginAndGetToken("any-user", true);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/notes/99999").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteNote_whenUnauthenticated_shouldReturnUnauthorized() throws Exception {
        // Arrange
        User user = createUserInDb("some-user", "some@test.com");
        LectureNote note = createNoteInDb("Some Note", "some-user/file.jpg", user);

        // Act & Assert
        mockMvc.perform(delete("/api/v1/notes/" + note.getId())).andExpect(status().isUnauthorized());
    }

    private User createUserInDb(String username, String email) {
        return userRepository.save(User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode("password123"))
                .build());
    }

    private String loginAndGetToken() throws Exception {
        return loginAndGetToken("note-user", false);
    }

    private String loginAndGetToken(String username, boolean createUserIfNeeded) throws Exception {
        if (createUserIfNeeded && userRepository.findByUsername(username).isEmpty()) {
            createUserInDb(username, username + "@test.com");
        }

        AuthRequestDto authRequest = new AuthRequestDto(username, "password123");
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andReturn();
        String responseBody = result.getResponse().getContentAsString();
        return objectMapper.readValue(responseBody, AuthResponseDto.class).getToken();
    }

    private String getFilePathFromDto(Long noteId) {
        return noteRepository.findById(noteId).orElseThrow().getFileStoragePath();
    }

    private LectureNote createNoteInDb(String title, String filePath, User user) {
        return noteRepository.save(LectureNote.builder()
                .title(title)
                .user(user)
                .originalFileName("test.jpg")
                .fileStoragePath(filePath)
                .status(NoteStatus.COMPLETED)
                .build());
    }

    private String loginAndGetToken(String username) throws Exception {
        AuthRequestDto authRequest = new AuthRequestDto(username, "password123");
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andReturn();
        String responseBody = result.getResponse().getContentAsString();
        return objectMapper.readValue(responseBody, AuthResponseDto.class).getToken();
    }
}
