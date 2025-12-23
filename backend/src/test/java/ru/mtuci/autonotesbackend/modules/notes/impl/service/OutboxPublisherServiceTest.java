package ru.mtuci.autonotesbackend.modules.notes.impl.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import ru.mtuci.autonotesbackend.modules.notes.impl.config.RabbitMqConfig;
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
    void publishEvents_whenEventsExist_shouldPublishAndBatchDelete() throws Exception {
        // Arrange
        String jsonPayload = "{\"noteId\":1}";
        OutboxEvent event = OutboxEvent.builder()
                .id(10L)
                .eventType("NOTE_CREATED")
                .payload(jsonPayload)
                .build();

        NoteProcessingEvent mappedEvent = new NoteProcessingEvent(1L, "bucket", List.of("path"));

        when(outboxEventRepository.findBatchToProcess(anyInt())).thenReturn(List.of(event));

        when(objectMapper.readValue(jsonPayload, NoteProcessingEvent.class)).thenReturn(mappedEvent);

        // Act
        publisherService.publishEvents();

        // Assert
        // 1. Проверяем отправку в RabbitMQ
        verify(rabbitTemplate)
                .convertAndSend(
                        eq(RabbitMqConfig.EXCHANGE_NOTES), eq(RabbitMqConfig.ROUTING_KEY_PROCESS), eq(mappedEvent));

        verify(outboxEventRepository).deleteAllInBatch(List.of(event));
    }

    @Test
    void publishEvents_whenNoEvents_shouldDoNothing() {
        // Arrange
        when(outboxEventRepository.findBatchToProcess(anyInt())).thenReturn(Collections.emptyList());

        // Act
        publisherService.publishEvents();

        // Assert
        verify(rabbitTemplate, never()).convertAndSend(any(String.class), any(String.class), any(Object.class));
        verify(outboxEventRepository, never()).deleteAllInBatch(any());
        verify(outboxEventRepository, never()).delete(any());
    }

    @Test
    void publishEvents_whenRabbitFails_shouldStopProcessingAndNotDeleteFailedEvent() throws Exception {
        // Arrange
        OutboxEvent event = OutboxEvent.builder()
                .id(1L)
                .eventType("NOTE_CREATED")
                .payload("{}")
                .build();

        when(outboxEventRepository.findBatchToProcess(anyInt())).thenReturn(List.of(event));

        when(objectMapper.readValue(any(String.class), eq(NoteProcessingEvent.class)))
                .thenReturn(new NoteProcessingEvent(1L, "b", List.of("p")));

        doThrow(new AmqpException("Rabbit is down"))
                .when(rabbitTemplate)
                .convertAndSend(any(String.class), any(String.class), any(Object.class));

        // Act
        publisherService.publishEvents();

        // Assert
        verify(outboxEventRepository, never()).deleteAllInBatch(any());
        verify(outboxEventRepository, never()).delete(any());
    }

    @Test
    void publishEvents_whenJsonCorrupted_shouldDeleteToPreventBlocking() throws Exception {
        // Arrange
        String badJson = "{invalid";
        OutboxEvent fatalEvent = OutboxEvent.builder()
                .id(666L)
                .eventType("NOTE_CREATED")
                .payload(badJson)
                .build();

        when(outboxEventRepository.findBatchToProcess(anyInt())).thenReturn(List.of(fatalEvent));

        when(objectMapper.readValue(badJson, NoteProcessingEvent.class))
                .thenThrow(new JsonProcessingException("Error") {});

        // Act
        publisherService.publishEvents();

        // Assert
        verify(rabbitTemplate, never()).convertAndSend(any(String.class), any(String.class), any(Object.class));
        verify(outboxEventRepository).deleteAllInBatch(List.of(fatalEvent));
    }
}
