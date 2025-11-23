package ru.mtuci.autonotesbackend.modules.filestorage.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import ru.mtuci.autonotesbackend.modules.filestorage.api.FileStorageFacade;
import ru.mtuci.autonotesbackend.modules.filestorage.impl.service.FileStorageService;

@Component
@Profile("minio-storage")
@RequiredArgsConstructor
public class FileStorageFacadeImpl implements FileStorageFacade {

    private final FileStorageService fileStorageService;

    @Override
    public String save(MultipartFile file, Long userId) {
        return fileStorageService.save(file, userId);
    }

    @Override
    public void delete(String filePath) {
        fileStorageService.delete(filePath);
    }
}
