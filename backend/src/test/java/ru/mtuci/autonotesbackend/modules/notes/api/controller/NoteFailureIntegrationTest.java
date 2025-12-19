package ru.mtuci.autonotesbackend.modules.notes.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.mtuci.autonotesbackend.BaseIntegrationTest;
import ru.mtuci.autonotesbackend.modules.notes.impl.service.NoteService;
import ru.mtuci.autonotesbackend.modules.user.api.dto.AuthRequestDto;
import ru.mtuci.autonotesbackend.modules.user.api.dto.AuthResponseDto;
import ru.mtuci.autonotesbackend.modules.user.impl.domain.User;
import ru.mtuci.autonotesbackend.modules.user.impl.repository.UserRepository;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;

class NoteFailureIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private S3Client s3Client;

    @MockitoBean
    private NoteService noteService;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Test
    void uploadNote_whenDbTransactionFails_shouldDeleteFileFromS3() throws Exception {
        // 1. Arrange
        String username = "fail_test_user";
        if (userRepository.findByUsername(username).isEmpty()) {
            userRepository.save(User.builder()
                    .username(username)
                    .email("fail@test.com")
                    .password(passwordEncoder.encode("password"))
                    .build());
        }
        User user = userRepository.findByUsername(username).orElseThrow();

        String token = login(username);

        byte[] validJpegBytes = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x01};

        MockMultipartFile file =
                new MockMultipartFile("files", "rollback_check.jpg", MediaType.IMAGE_JPEG_VALUE, validJpegBytes);
        MockPart title = new MockPart("title", "Fail Note".getBytes());

        doThrow(new RuntimeException("Database Connection Error"))
                .when(noteService)
                .createNote(anyString(), any(), eq(user.getId()));

        int initialCount = countFilesInBucket();

        // 2. Act
        mockMvc.perform(multipart("/api/v1/notes").file(file).part(title).header("Authorization", "Bearer " + token))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred. Please contact support."));

        // 3. Assert
        int finalCount = countFilesInBucket();

        assertThat(finalCount)
                .as("File count in S3 should not increase due to rollback")
                .isEqualTo(initialCount);
    }

    private String login(String username) throws Exception {
        AuthRequestDto loginRequest = new AuthRequestDto(username, "password");
        String response = mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readValue(response, AuthResponseDto.class).getToken();
    }

    private int countFilesInBucket() {
        var response = s3Client.listObjectsV2(
                ListObjectsV2Request.builder().bucket(bucketName).build());
        return response.contents().size();
    }
}
