package ru.mtuci.autonotesbackend.modules.notes.impl.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NoteResultDto implements Serializable {

    private Long noteId;
    private String status;
    private String recognizedText;
    private String summaryText;
    private String errorMessage;
}
