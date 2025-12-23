package ru.mtuci.autonotesbackend.modules.notes.impl.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mtuci.autonotesbackend.modules.notes.impl.config.RabbitMqConfig;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.OutboxEvent;
import ru.mtuci.autonotesbackend.modules.notes.impl.event.NoteProcessingEvent;
import ru.mtuci.autonotesbackend.modules.notes.impl.repository.OutboxEventRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxPublisherService {

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    private static final int BATCH_SIZE = 50;

    @Scheduled(fixedDelayString = "${app.scheduling.outbox-interval-ms:2000}")
    @Transactional
    public void publishEvents() {
        List<OutboxEvent> events = outboxEventRepository.findBatchToProcess(BATCH_SIZE);

        if (events.isEmpty()) {
            return;
        }

        List<OutboxEvent> processedEvents = new ArrayList<>();

        for (OutboxEvent event : events) {
            try {
                processEvent(event);
                processedEvents.add(event);

            } catch (JsonProcessingException e) {
                log.error(
                        "Fatal error: Corrupted payload in event ID {}. Deleting to prevent blocking.",
                        event.getId(),
                        e);
                processedEvents.add(event);

            } catch (AmqpException e) {
                log.error(
                        "Transient error: Could not connect to RabbitMQ for event ID {}. Will retry later.",
                        event.getId(),
                        e);
                break;

            } catch (Exception e) {
                log.error("Unknown error processing event ID {}. Stopping batch.", event.getId(), e);
                break;
            }
        }

        if (!processedEvents.isEmpty()) {
            outboxEventRepository.deleteAllInBatch(processedEvents);
            log.debug("Published and deleted {} outbox events.", processedEvents.size());
        }
    }

    private void processEvent(OutboxEvent event) throws JsonProcessingException {
        if ("NOTE_CREATED".equals(event.getEventType())) {
            NoteProcessingEvent payload = objectMapper.readValue(event.getPayload(), NoteProcessingEvent.class);

            rabbitTemplate.convertAndSend(RabbitMqConfig.EXCHANGE_NOTES, RabbitMqConfig.ROUTING_KEY_PROCESS, payload);
        } else {
            log.warn("Unknown event type: {}", event.getEventType());
        }
    }
}
