package ru.mtuci.autonotesbackend.modules.filestorage.api;

import org.springframework.web.multipart.MultipartFile;
import ru.mtuci.autonotesbackend.modules.filestorage.api.exception.FileStorageException;

public interface FileStorageFacade {

    String save(MultipartFile file, Long userId) throws FileStorageException;

    void delete(String filePath) throws FileStorageException;
}
