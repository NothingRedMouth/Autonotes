package ru.mtuci.autonotesbackend.modules.notes.impl.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@Entity
@Table(name = "note_images")
public class NoteImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "note_id", nullable = false)
    private LectureNote note;

    @Column(name = "file_storage_path", nullable = false)
    private String fileStoragePath;

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
