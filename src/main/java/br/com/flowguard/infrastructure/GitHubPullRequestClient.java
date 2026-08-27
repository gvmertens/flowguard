package br.com.flowguard.infrastructure;

import br.com.flowguard.api.DeliverySignal;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GitHubPullRequestClient {
    private static final Pattern REPOSITORY = Pattern.compile("^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$");
    private final String apiBaseUrl;
    private final String token;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public GitHubPullRequestClient(
            @Value("${flowguard.github.api-base-url}") String apiBaseUrl,
            @Value("${flowguard.github.token:}") String token,
            ObjectMapper objectMapper) {
        this.apiBaseUrl = apiBaseUrl;
        this.token = token;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    }

    public PullRequestContext fetch(DeliverySignal signal) {
        String repository = normalizeRepository(signal.repository());
        if (!REPOSITORY.matcher(repository).matches()) {
            throw new IllegalArgumentException("repository deve seguir o formato owner/repository");
        }
        if (token.isBlank()) {
            return new PullRequestContext("fixture", List.of("PR consultado em fixture local sem credencial."));
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(
                            "%s/repos/%s/pulls/%s".formatted(apiBaseUrl, repository, signal.pullRequestId())))
                    .header("Accept", "application/vnd.github+json")
                    .header("Authorization", "Bearer " + token)
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new IOException("GitHub respondeu " + response.statusCode());
            }
            JsonNode payload = objectMapper.readTree(response.body());
            if (!payload.hasNonNull("number") || !payload.hasNonNull("state")) {
                throw new IOException("payload GitHub inválido");
            }
            return new PullRequestContext(
                    "github_api",
                    List.of("PR #%s em estado %s.".formatted(payload.get("number").asText(), payload.get("state").asText())));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return fallback();
        } catch (IOException exception) {
            return fallback();
        }
    }

    private PullRequestContext fallback() {
        return new PullRequestContext("fixture_fallback", List.of("GitHub indisponível; fallback documentado ativado."));
    }

    private String normalizeRepository(String repository) {
        String normalized = repository.trim()
                .replaceFirst("^https?://github\\.com/", "")
                .replaceFirst("\\.git$", "");
        int queryIndex = normalized.indexOf('?');
        if (queryIndex >= 0) {
            normalized = normalized.substring(0, queryIndex);
        }
        int fragmentIndex = normalized.indexOf('#');
        if (fragmentIndex >= 0) {
            normalized = normalized.substring(0, fragmentIndex);
        }
        return normalized.replaceFirst("/$", "");
    }

    public record PullRequestContext(String source, List<String> evidence) {
    }
}
