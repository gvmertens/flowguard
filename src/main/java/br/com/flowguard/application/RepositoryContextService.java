package br.com.flowguard.application;

import br.com.flowguard.api.DeliverySignal;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class RepositoryContextService {
    private static final String DEFAULT_DOCUMENT = "REVIEW-STANDARD.md";
    private final List<ContextChunk> chunks;
    private final Map<String, List<ContextChunk>> invertedIndex;

    public RepositoryContextService() {
        chunks = List.of(
                chunk("ADR-012.md", Set.of("payment", "auth", "security")),
                chunk("INC-024.md", Set.of("payment", "auth", "coverage")),
                chunk(DEFAULT_DOCUMENT, Set.of("default", "review")));
        invertedIndex = chunks.stream()
                .flatMap(chunk -> chunk.tags().stream().map(tag -> Map.entry(tag, chunk)))
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toUnmodifiableList())));
    }

    public List<String> retrieve(DeliverySignal signal) {
        Set<String> queryTerms = queryTerms(signal);
        List<ContextChunk> matches = queryTerms.stream()
                .flatMap(term -> invertedIndex.getOrDefault(term, List.of()).stream())
                .distinct()
                .sorted(Comparator.comparing(ContextChunk::source))
                .toList();
        if (matches.isEmpty()) {
            matches = chunks.stream().filter(chunk -> DEFAULT_DOCUMENT.equals(chunk.source())).toList();
        }
        return matches.stream().map(ContextChunk::display).toList();
    }

    private ContextChunk chunk(String source, Set<String> tags) {
        try {
            String content = new ClassPathResource("context/" + source).getContentAsString(StandardCharsets.UTF_8).trim();
            return new ContextChunk(source, tags, content);
        } catch (IOException exception) {
            throw new IllegalStateException("Corpus de contexto indisponível: " + source, exception);
        }
    }

    private Set<String> queryTerms(DeliverySignal signal) {
        Set<String> terms = new LinkedHashSet<>();
        signal.changedModules().forEach(module -> terms.add(module.toLowerCase()));
        signal.diffSummary().toLowerCase().replaceAll("[^a-záéíóúãõç ]", " ").lines()
                .flatMap(line -> List.of(line.split("\\s+")).stream())
                .filter(token -> token.length() >= 3)
                .forEach(terms::add);
        signal.ciLogs().stream().map(String::toLowerCase).filter(log -> log.contains("coverage"))
                .forEach(log -> terms.add("coverage"));
        return terms;
    }

    private record ContextChunk(String source, Set<String> tags, String content) {
        private String display() {
            return "[%s] %s".formatted(source, content);
        }
    }
}
