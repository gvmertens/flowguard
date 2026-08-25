package br.com.flowguard.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuditService.class);
    private static final Path AUDIT_FILE = Path.of("runtime", "audit.jsonl");
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String record(String correlationId, String event, Map<String, Object> fields) {
        String eventId = UUID.randomUUID().toString();
        Map<String, Object> record = Map.of(
                "timestamp", Instant.now().toString(),
                "eventId", eventId,
                "correlationId", correlationId,
                "event", event,
                "fields", fields);
        try {
            String json = objectMapper.writeValueAsString(record);
            LOGGER.info("{}", json);
            Files.createDirectories(AUDIT_FILE.getParent());
            Files.writeString(AUDIT_FILE, json + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (JsonProcessingException exception) {
            LOGGER.error("Não foi possível serializar evento de auditoria", exception);
        } catch (IOException exception) {
            LOGGER.error("Não foi possível persistir evento de auditoria", exception);
        }
        return eventId;
    }
}
