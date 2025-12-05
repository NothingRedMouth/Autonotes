package ru.mtuci.autonotesbackend.modules.notes.impl.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.LectureNote;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.NoteStatus;

@Repository
public interface LectureNoteRepository extends JpaRepository<LectureNote, Long> {

    List<LectureNote> findByUserId(Long userId);

    Optional<LectureNote> findByIdAndUserId(Long id, Long userId);

    List<LectureNote> findAllByStatusAndUpdatedAtBefore(NoteStatus status, OffsetDateTime updatedAt, Pageable pageable);

    @Query("SELECT ln.id as id, ln.user.id as userId, ln.title as title, "
            + "ln.originalFileName as originalFileName, ln.status as status, "
            + "ln.createdAt as createdAt "
            + "FROM LectureNote ln WHERE ln.user.id = :userId "
            + "ORDER BY ln.createdAt DESC")
    List<NoteProjection> findAllProjectedByUserId(@Param("userId") Long userId);

    @Query(value = "SELECT count(*) > 0 FROM lecture_notes WHERE file_storage_path = :path", nativeQuery = true)
    boolean existsByFileStoragePath(@Param("path") String fileStoragePath);

    @Query(value = "SELECT * FROM lecture_notes WHERE deleted_at < :threshold", nativeQuery = true)
    List<LectureNote> findAllSoftDeletedBefore(@Param("threshold") OffsetDateTime threshold, Pageable pageable);

    @Modifying
    @Query(value = "DELETE FROM lecture_notes WHERE id = :id", nativeQuery = true)
    void hardDeleteById(@Param("id") Long id);

    interface NoteProjection {
        Long getId();

        Long getUserId();

        String getTitle();

        String getOriginalFileName();

        NoteStatus getStatus();

        OffsetDateTime getCreatedAt();
    }
}
