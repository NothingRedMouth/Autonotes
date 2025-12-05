package ru.mtuci.autonotesbackend.modules.notes.impl.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mtuci.autonotesbackend.config.RabbitMqConfig;
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

    @Scheduled(fixedDelayString = "${app.scheduling.outbox-interval-ms:2000}")
    @Transactional
    public void publishEvents() {
        List<OutboxEvent> events = outboxEventRepository
                .findAll(PageRequest.of(0, 50, Sort.by(Sort.Direction.ASC, "createdAt")))
                .getContent();

        if (events.isEmpty()) {
            return;
        }

        log.debug("Found {} outbox events to publish", events.size());

        for (OutboxEvent event : events) {
            try {
                processEvent(event);
                outboxEventRepository.delete(event);
            } catch (Exception e) {
                log.error("Failed to publish event id: {}. Will retry next cycle.", event.getId(), e);
                break;
            }
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
