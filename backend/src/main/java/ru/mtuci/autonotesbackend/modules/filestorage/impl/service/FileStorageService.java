package ru.mtuci.autonotesbackend.modules.filestorage.impl.service;

import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.mtuci.autonotesbackend.modules.filestorage.api.exception.FileStorageException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    public String save(MultipartFile file, Long userId) {
        if (file.isEmpty()) {
            throw new FileStorageException("Cannot save an empty file.");
        }

        try {
            String extension = FilenameUtils.getExtension(file.getOriginalFilename());
            String uniqueFileName = UUID.randomUUID() + "." + extension;
            String filePath = userId + "/" + uniqueFileName;

            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filePath)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            log.info("File saved to MinIO. Path: {}", filePath);
            return filePath;

        } catch (IOException | SdkException e) {
            log.error("Failed to save file for user {}", userId, e);
            throw new FileStorageException("Failed to save file: " + file.getOriginalFilename(), e);
        }
    }

    public void delete(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            log.warn("Attempted to delete a file with a null or empty path.");
            return;
        }
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filePath)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("File deleted from MinIO. Path: {}", filePath);

        } catch (SdkException e) {
            log.error("Failed to delete file from path {}", filePath, e);
            throw new FileStorageException("Failed to delete file: " + filePath, e);
        }
    }
}
