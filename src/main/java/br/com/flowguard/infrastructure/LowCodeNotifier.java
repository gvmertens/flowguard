package br.com.flowguard.infrastructure;

import br.com.flowguard.api.DeliveryAssessment;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

@Service
public class LowCodeNotifier {
    private final String webhookUrl;
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();

    public LowCodeNotifier(@Value("${flowguard.low-code.webhook-url:}") String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public boolean notifyRoute(DeliveryAssessment assessment) {
        if (webhookUrl.isBlank() || assessment.requiresHumanApproval()) {
            return false;
        }
        try {
            String body = "{\"event\":\"delivery.routed\",\"correlationId\":\"%s\",\"risk\":\"%s\"}"
                    .formatted(assessment.correlationId(), assessment.risk());
            HttpRequest request = HttpRequest.newBuilder(URI.create(webhookUrl))
                    .timeout(Duration.ofSeconds(3))
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            return client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode() < 300;
        } catch (Exception ignored) {
            return false;
        }
    }
}

