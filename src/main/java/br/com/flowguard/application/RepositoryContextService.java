package br.com.flowguard.application;

import br.com.flowguard.api.DeliverySignal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RepositoryContextService {
    public List<String> retrieve(DeliverySignal signal) {
        boolean touchesCritical = signal.changedModules().stream()
                .anyMatch(module -> module.equalsIgnoreCase("payment") || module.equalsIgnoreCase("auth"));
        if (touchesCritical) {
            return List.of(
                    "ADR-012: mudanças em payment e auth exigem testes de integração e aprovação humana.",
                    "INC-024: quedas de cobertura nesse módulo precederam incidentes de produção.");
        }
        return List.of("Padrão de revisão: todo PR deve apresentar resultado de testes e escopo da mudança.");
    }
}

