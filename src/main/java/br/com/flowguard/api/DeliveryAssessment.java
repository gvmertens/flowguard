package br.com.flowguard.api;

import java.util.List;

public record DeliveryAssessment(
        String correlationId,
        String status,
        RiskLevel risk,
        List<String> reasons,
        List<String> evidence,
        String recommendedAction,
        boolean requiresHumanApproval,
        long latencyMs,
        String auditEventId) {
}

