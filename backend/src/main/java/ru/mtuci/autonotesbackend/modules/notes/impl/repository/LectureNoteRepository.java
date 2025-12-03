package ru.mtuci.autonotesbackend.modules.notes.impl.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.LectureNote;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.NoteStatus;

@Repository
public interface LectureNoteRepository extends JpaRepository<LectureNote, Long> {

    List<LectureNote> findByUserId(Long userId);

    Optional<LectureNote> findByIdAndUserId(Long id, Long userId);

    List<LectureNote> findAllByStatusAndUpdatedAtBefore(NoteStatus status, OffsetDateTime updatedAt);
}
