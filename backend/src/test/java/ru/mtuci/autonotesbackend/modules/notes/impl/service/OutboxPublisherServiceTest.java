package ru.mtuci.autonotesbackend.modules.notes.impl.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import ru.mtuci.autonotesbackend.config.RabbitMqConfig;
import ru.mtuci.autonotesbackend.modules.notes.impl.domain.OutboxEvent;
import ru.mtuci.autonotesbackend.modules.notes.impl.event.NoteProcessingEvent;
import ru.mtuci.autonotesbackend.modules.notes.impl.repository.OutboxEventRepository;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OutboxPublisherService publisherService;

    @Test
    void publishEvents_whenEventsExist_shouldPublishAndDelete() throws Exception {
        // Arrange
        String jsonPayload = "{\"noteId\":1}";
        OutboxEvent event = OutboxEvent.builder()
            .id(10L)
            .eventType("NOTE_CREATED")
            .payload(jsonPayload)
            .build();

        NoteProcessingEvent mappedEvent = new NoteProcessingEvent(1L, "bucket", "path");

        when(outboxEventRepository.findAll(any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(event)));

        when(objectMapper.readValue(jsonPayload, NoteProcessingEvent.class))
            .thenReturn(mappedEvent);

        // Act
        publisherService.publishEvents();

        // Assert
        verify(rabbitTemplate).convertAndSend(
            eq(RabbitMqConfig.EXCHANGE_NOTES),
            eq(RabbitMqConfig.ROUTING_KEY_PROCESS),
            eq(mappedEvent)
        );

        verify(outboxEventRepository).delete(event);
    }

    @Test
    void publishEvents_whenNoEvents_shouldDoNothing() {
        // Arrange
        when(outboxEventRepository.findAll(any(Pageable.class)))
            .thenReturn(new PageImpl<>(Collections.emptyList()));

        // Act
        publisherService.publishEvents();

        // Assert
        verify(rabbitTemplate, never()).convertAndSend(any(String.class), any(String.class), any(Object.class));
        verify(outboxEventRepository, never()).delete(any());
    }

    @Test
    void publishEvents_whenRabbitFails_shouldNotDeleteEvent() throws Exception {
        // Arrange
        OutboxEvent event = OutboxEvent.builder().id(1L).eventType("NOTE_CREATED").payload("{}").build();

        when(outboxEventRepository.findAll(any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(event)));

        when(objectMapper.readValue(any(String.class), eq(NoteProcessingEvent.class)))
            .thenReturn(new NoteProcessingEvent(1L, "b", "p"));

        doThrow(new RuntimeException("Rabbit is down"))
            .when(rabbitTemplate)
            .convertAndSend(any(String.class), any(String.class), any(Object.class));

        // Act
        publisherService.publishEvents();

        // Assert
        verify(outboxEventRepository, never()).delete(event);
    }
}
