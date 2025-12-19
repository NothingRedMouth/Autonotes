package ru.mtuci.autonotesbackend.modules.notes.impl.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.mtuci.autonotesbackend.modules.notes.api.dto.NoteDetailDto;
import ru.mtuci.autonotesbackend.modules.notes.api.dto.NoteDto;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.LectureNote;

@Mapper(componentModel = "spring")
public interface NoteMapper {
    @Mapping(source = "user.id", target = "userId")
    NoteDto toDto(LectureNote lectureNote);

    List<NoteDto> toDtoList(List<LectureNote> notes);

    @Mapping(source = "user.id", target = "userId")
    NoteDetailDto toDetailDto(LectureNote lectureNote);
}
