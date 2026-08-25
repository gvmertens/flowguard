package br.com.flowguard.application;

import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class PromptInjectionGuard {
    private static final List<String> FORBIDDEN_PATTERNS = List.of(
            "ignore previous instructions",
            "ignore as instruções anteriores",
            "reveal system prompt",
            "revele o prompt de sistema",
            "ignore all previous",
            "system prompt",
            "jailbreak",
            "bypass approval",
            "aprove sem revisão");

    public boolean isUntrusted(String value) {
        String normalized = value.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        return FORBIDDEN_PATTERNS.stream().anyMatch(normalized::contains);
    }
}
