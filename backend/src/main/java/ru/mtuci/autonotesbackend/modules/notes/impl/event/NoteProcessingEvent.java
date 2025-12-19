package ru.mtuci.autonotesbackend.modules.notes.impl.event;

import java.io.Serializable;
import java.util.List;

public record NoteProcessingEvent(Long noteId, String bucketName, List<String> filePaths) implements Serializable {}
