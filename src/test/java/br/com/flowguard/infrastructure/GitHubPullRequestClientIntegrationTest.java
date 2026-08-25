package br.com.flowguard.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.flowguard.api.DeliverySignal;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GitHubPullRequestClientIntegrationTest {
    private HttpServer server;
    private final AtomicReference<String> authorization = new AtomicReference<>();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void readsValidatedPullRequestFromGitHubApiContract() {
        server.createContext("/repos/acme/catalog-api/pulls/42", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            writeJson(exchange, 200, "{\"number\":42,\"state\":\"open\"}");
        });
        server.start();

        GitHubPullRequestClient.PullRequestContext result = client().fetch(signal("acme/catalog-api", "42"));

        assertThat(result.source()).isEqualTo("github_api");
        assertThat(result.evidence()).containsExactly("PR #42 em estado open.");
        assertThat(authorization.get()).isEqualTo("Bearer test-token");
    }

    @Test
    void fallsBackWhenGitHubReturnsAnError() {
        server.createContext("/repos/acme/catalog-api/pulls/42", exchange -> writeJson(exchange, 503, "{}"));
        server.start();

        GitHubPullRequestClient.PullRequestContext result = client().fetch(signal("acme/catalog-api", "42"));

        assertThat(result.source()).isEqualTo("fixture_fallback");
        assertThat(result.evidence()).contains("GitHub indisponível; fallback documentado ativado.");
    }

    @Test
    void rejectsRepositoryOutsideExpectedSchema() {
        assertThatThrownBy(() -> client().fetch(signal("acme/catalog-api/extra", "42")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("owner/repository");
    }

    private GitHubPullRequestClient client() {
        String baseUrl = "http://localhost:" + server.getAddress().getPort();
        return new GitHubPullRequestClient(baseUrl, "test-token", new ObjectMapper());
    }

    private DeliverySignal signal(String repository, String pullRequestId) {
        return new DeliverySignal(repository, pullRequestId, "Resumo confiável", List.of("Tests passed"), List.of("catalog"));
    }

    private void writeJson(com.sun.net.httpserver.HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
