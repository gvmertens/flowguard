package br.com.flowguard.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuditServiceTest {
    private static final Path AUDIT_FILE = Path.of("runtime", "audit.jsonl");

    @Test
    void persistsCorrelatedStructuredAuditEvent() throws Exception {
        String correlationId = "test-" + UUID.randomUUID();
        String eventId = new AuditService().record(
                correlationId,
                "test.observability",
                Map.of("latencyMs", 12, "status", "ROUTED"));

        String latestLine = Files.readAllLines(AUDIT_FILE).getLast();
        JsonNode event = new ObjectMapper().readTree(latestLine);

        assertThat(event.path("eventId").asText()).isEqualTo(eventId);
        assertThat(event.path("correlationId").asText()).isEqualTo(correlationId);
        assertThat(event.path("event").asText()).isEqualTo("test.observability");
        assertThat(event.path("fields").path("latencyMs").asInt()).isEqualTo(12);
    }
}
