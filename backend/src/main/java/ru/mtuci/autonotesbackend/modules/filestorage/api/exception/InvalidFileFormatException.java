package ru.mtuci.autonotesbackend.modules.filestorage.api.exception;

public class InvalidFileFormatException extends RuntimeException {
    public InvalidFileFormatException(String message) {
        super(message);
    }
}
