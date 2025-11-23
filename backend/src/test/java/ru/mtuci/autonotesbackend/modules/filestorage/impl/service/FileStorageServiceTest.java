package ru.mtuci.autonotesbackend.modules.filestorage.impl.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import ru.mtuci.autonotesbackend.modules.filestorage.api.exception.FileStorageException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    @Mock
    private S3Client s3Client;

    @InjectMocks
    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(fileStorageService, "bucketName", "test-bucket");
    }

    @Test
    void delete_whenValidPath_shouldCallS3ClientDeleteObject() {
        // Arrange
        String filePath = "1/some-uuid.jpg";

        // Act
        fileStorageService.delete(filePath);

        // Assert
        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client, times(1)).deleteObject(captor.capture());

        DeleteObjectRequest capturedRequest = captor.getValue();
        assertThat(capturedRequest.bucket()).isEqualTo("test-bucket");
        assertThat(capturedRequest.key()).isEqualTo(filePath);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void delete_whenPathIsBlank_shouldDoNothingAndNotThrowException(String blankPath) {
        // Act
        fileStorageService.delete(blankPath);

        // Assert
        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void delete_whenPathIsNull_shouldDoNothingAndNotThrowException() {
        // Act
        fileStorageService.delete(null);

        // Assert
        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void delete_whenS3ClientFails_shouldThrowFileStorageException() {
        // Arrange
        String filePath = "1/some-uuid.jpg";
        doThrow(SdkException.builder().message("Connection refused").build())
                .when(s3Client)
                .deleteObject(any(DeleteObjectRequest.class));

        // Act & Assert
        FileStorageException exception =
                assertThrows(FileStorageException.class, () -> fileStorageService.delete(filePath));

        assertThat(exception.getMessage()).isEqualTo("Failed to delete file: " + filePath);
        assertThat(exception.getCause()).isInstanceOf(SdkException.class);
    }
}
