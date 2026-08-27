package br.com.flowguard.application;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.flowguard.api.DeliverySignal;
import java.util.List;
import org.junit.jupiter.api.Test;

class RepositoryContextServiceTest {
    private final RepositoryContextService service = new RepositoryContextService();

    @Test
    void retrievesCuratedChunksForCriticalModule() {
        List<String> context = service.retrieve(signal(List.of("payment", "auth"), "Ajusta autorização de pagamento"));

        assertThat(context).anyMatch(chunk -> chunk.startsWith("[ADR-012.md]"));
        assertThat(context).anyMatch(chunk -> chunk.startsWith("[INC-024.md]"));
    }

    @Test
    void usesReviewStandardWhenNoRelevantChunkExists() {
        List<String> context = service.retrieve(signal(List.of("catalog"), "Ajusta endpoint de catálogo"));

        assertThat(context).containsExactly("[REVIEW-STANDARD.md] Padrão de revisão: todo Pull Request deve apresentar resultado de testes, escopo da mudança e plano de reversão quando aplicável.");
    }

    private DeliverySignal signal(List<String> modules, String summary) {
        return new DeliverySignal("acme/catalog-api", "42", summary, List.of("Tests passed"), modules);
    }
}
