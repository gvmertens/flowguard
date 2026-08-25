package br.com.flowguard.application;

import br.com.flowguard.api.DeliverySignal;
import br.com.flowguard.api.RiskLevel;
import java.util.List;

public interface RiskClassifier {
    Classification classify(DeliverySignal signal, List<String> context, List<String> ciFindings);

    record Classification(RiskLevel risk, List<String> reasons, String recommendedAction) {
    }
}

