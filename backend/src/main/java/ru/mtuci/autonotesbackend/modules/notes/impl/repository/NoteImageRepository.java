package ru.mtuci.autonotesbackend.modules.notes.impl.repository;

import java.util.Collection;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.NoteImage;

@Repository
public interface NoteImageRepository extends JpaRepository<NoteImage, Long> {

    @Query("SELECT CASE WHEN COUNT(ni) > 0 THEN true ELSE false END FROM NoteImage ni WHERE ni.fileStoragePath = :path")
    boolean existsByFileStoragePath(@Param("path") String fileStoragePath);

    @Query("SELECT ni.fileStoragePath FROM NoteImage ni WHERE ni.fileStoragePath IN :paths")
    Set<String> findExistingPaths(@Param("paths") Collection<String> paths);
}
