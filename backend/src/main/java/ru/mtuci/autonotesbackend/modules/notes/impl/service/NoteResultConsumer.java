package ru.mtuci.autonotesbackend.modules.notes.impl.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import ru.mtuci.autonotesbackend.modules.notes.impl.config.RabbitMqConfig;
import ru.mtuci.autonotesbackend.modules.notes.impl.dto.NoteResultDto;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoteResultConsumer {

    private final NoteService noteService;

    @RabbitListener(queues = RabbitMqConfig.QUEUE_NOTES_RESULTS)
    public void consume(NoteResultDto result) {
        log.debug("Received message from queue {}: {}", RabbitMqConfig.QUEUE_NOTES_RESULTS, result);
        try {
            noteService.processCompletion(result);
        } catch (Exception e) {
            log.error("Unexpected error processing ML result for noteId: {}", result.getNoteId(), e);
            throw e;
        }
    }
}
