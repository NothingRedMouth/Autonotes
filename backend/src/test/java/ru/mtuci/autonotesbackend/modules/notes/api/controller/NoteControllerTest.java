package ru.mtuci.autonotesbackend.modules.notes.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
import ru.mtuci.autonotesbackend.modules.notes.api.dto.NoteImageDto;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.LectureNote;
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
    }

    @Test
    void uploadNote_shouldCreateNoteWithMultipleFiles() throws Exception {
        // Arrange
        createUserInDb();
        String token = loginAndGetToken();

        byte[] content = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
        MockMultipartFile file1 = new MockMultipartFile("files", "page1.jpg", MediaType.IMAGE_JPEG_VALUE, content);
        MockMultipartFile file2 = new MockMultipartFile("files", "page2.jpg", MediaType.IMAGE_JPEG_VALUE, content);
        MockPart titlePart = new MockPart("title", "Multi Page Lecture".getBytes());

        // Act
        MvcResult result = mockMvc.perform(multipart("/api/v1/notes")
                        .file(file1)
                        .file(file2)
                        .part(titlePart)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Multi Page Lecture"))
                .andExpect(jsonPath("$.images", hasSize(2)))
                .andExpect(jsonPath("$.images[0].originalFileName").value("page1.jpg"))
                .andExpect(jsonPath("$.images[1].originalFileName").value("page2.jpg"))
                .andReturn();

        // Assert - DB Check
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

        // Assert - MinIO Check
        String firstFilePath = savedNote.getImages().getFirst().getFileStoragePath();
        s3Client.headObject(HeadObjectRequest.builder()
                .bucket(testBucketName)
                .key(firstFilePath)
                .build());
    }

    private void createUserInDb() {
        if (userRepository.findByUsername("multi-user").isPresent()) return;
        userRepository.save(User.builder()
                .username("multi-user")
                .email("multi@test.com")
                .password(passwordEncoder.encode("password123"))
                .build());
    }

    private String loginAndGetToken() throws Exception {
        AuthRequestDto authRequest = new AuthRequestDto("multi-user", "password123");
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
