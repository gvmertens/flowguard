package br.com.flowguard.application;

import br.com.flowguard.api.DeliverySignal;
import br.com.flowguard.api.RiskLevel;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "flowguard.ai", name = "enabled", havingValue = "false", matchIfMissing = true)
public class DeterministicRiskClassifier implements RiskClassifier {
    @Override
    public Classification classify(DeliverySignal signal, List<String> context, List<String> ciFindings) {
        List<String> reasons = new ArrayList<>(ciFindings);
        boolean criticalModule = signal.changedModules().stream()
                .map(String::toLowerCase)
                .anyMatch(module -> module.contains("payment") || module.contains("auth") || module.contains("security"));
        boolean ciFailure = ciFindings.stream().anyMatch(finding -> finding.contains("FALHA"));
        if (criticalModule) {
            reasons.add("Módulo crítico foi alterado.");
        }
        if (ciFailure && criticalModule) {
            return new Classification(
                    RiskLevel.HIGH,
                    reasons,
                    "Solicitar revisão humana e corrigir o CI antes de liberar o merge.");
        }
        if (ciFailure || criticalModule) {
            return new Classification(
                    RiskLevel.MEDIUM,
                    reasons.isEmpty() ? List.of("Mudança exige revisão do squad.") : reasons,
                    "Encaminhar para revisão técnica com evidências do CI.");
        }
        return new Classification(
                RiskLevel.LOW,
                List.of("Não foram encontrados sinais críticos na mudança e no CI."),
                "Permitir merge após a revisão normal do Pull Request.");
    }
}
