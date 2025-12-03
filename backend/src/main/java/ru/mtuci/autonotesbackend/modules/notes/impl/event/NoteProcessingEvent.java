package ru.mtuci.autonotesbackend.modules.notes.impl.event;

import java.io.Serializable;

public record NoteProcessingEvent(
    Long noteId,
    String bucketName,
    String fileStoragePath
) implements Serializable {}
