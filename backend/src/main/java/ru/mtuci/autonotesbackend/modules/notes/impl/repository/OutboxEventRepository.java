package ru.mtuci.autonotesbackend.modules.notes.impl.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.OutboxEvent;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Query(
            value =
                    """
        SELECT * FROM outbox_events
        ORDER BY created_at ASC
        LIMIT :limit
        FOR UPDATE SKIP LOCKED
        """,
            nativeQuery = true)
    List<OutboxEvent> findBatchToProcess(@Param("limit") int limit);
}
