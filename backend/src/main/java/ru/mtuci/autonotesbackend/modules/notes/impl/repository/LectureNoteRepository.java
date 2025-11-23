package ru.mtuci.autonotesbackend.modules.notes.impl.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.LectureNote;

@Repository
public interface LectureNoteRepository extends JpaRepository<LectureNote, Long> {

    List<LectureNote> findByUserId(Long userId);

    Optional<LectureNote> findByIdAndUserId(Long id, Long userId);
}
