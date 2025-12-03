package ru.mtuci.autonotesbackend.modules.filestorage.impl.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.MimeTypeUtils;
import ru.mtuci.autonotesbackend.modules.filestorage.api.exception.FileStorageException;
import ru.mtuci.autonotesbackend.modules.filestorage.api.exception.InvalidFileFormatException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    @Mock
    private S3Client s3Client;

    @InjectMocks
    private FileStorageService fileStorageService;

    private final String bucketName = "test-bucket";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(fileStorageService, "bucketName", bucketName);
    }

    @Test
    void save_whenValidImage_shouldUploadToS3() {
        // Arrange
        byte[] jpegBytes = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x01};
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", MimeTypeUtils.IMAGE_JPEG_VALUE, jpegBytes);
        Long userId = 100L;

        // Act
        String resultPath = fileStorageService.save(file, userId);

        // Assert
        assertThat(resultPath).startsWith("100/").endsWith(".jpg");

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));

        PutObjectRequest request = captor.getValue();
        assertThat(request.bucket()).isEqualTo(bucketName);
        assertThat(request.key()).isEqualTo(resultPath);
        assertThat(request.contentType()).isEqualTo(MimeTypeUtils.IMAGE_JPEG_VALUE);
    }

    @Test
    void save_whenFakeImage_shouldThrowInvalidFileFormatException() {
        // Arrange
        byte[] textBytes = "This is not an image".getBytes();
        MockMultipartFile fakeFile = new MockMultipartFile("file", "virus.exe.jpg", "image/jpeg", textBytes);
        Long userId = 100L;

        // Act & Assert
        assertThrows(InvalidFileFormatException.class, () -> fileStorageService.save(fakeFile, userId));

        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void save_whenS3Fails_shouldThrowFileStorageException() {
        // Arrange
        byte[] jpegBytes = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", jpegBytes);

        doThrow(SdkException.builder().message("S3 Down").build())
                .when(s3Client)
                .putObject(any(PutObjectRequest.class), any(RequestBody.class));

        // Act & Assert
        assertThrows(FileStorageException.class, () -> fileStorageService.save(file, 1L));
    }

    @Test
    void delete_whenValidPath_shouldCallS3ClientDeleteObject() {
        String filePath = "1/some-uuid.jpg";
        fileStorageService.delete(filePath);

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());
        assertThat(captor.getValue().key()).isEqualTo(filePath);
    }

    @Test
    void delete_whenPathIsBlank_shouldDoNothing() {
        fileStorageService.delete("");
        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }
}
