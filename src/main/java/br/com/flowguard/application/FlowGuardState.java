package br.com.flowguard.application;

import br.com.flowguard.api.DeliverySignal;
import br.com.flowguard.api.RiskLevel;
import java.util.List;
import java.util.Map;
import org.bsc.langgraph4j.state.AgentState;

public class FlowGuardState extends AgentState {
    public static final String SIGNAL = "signal";
    public static final String CORRELATION_ID = "correlationId";
    public static final String STATUS = "status";
    public static final String CONTEXT = "context";
    public static final String CI_FINDINGS = "ciFindings";
    public static final String RISK = "risk";
    public static final String REASONS = "reasons";
    public static final String ACTION = "action";
    public static final String REQUIRES_APPROVAL = "requiresApproval";
    public static final String AUDIT_EVENT_ID = "auditEventId";

    public static final Map<String, org.bsc.langgraph4j.state.Channel<?>> SCHEMA = Map.of();

    public FlowGuardState(Map<String, Object> initData) {
        super(initData);
    }

    public DeliverySignal signal() {
        return value(SIGNAL).map(DeliverySignal.class::cast).orElseThrow();
    }

    public String correlationId() {
        return value(CORRELATION_ID).map(String.class::cast).orElseThrow();
    }

    public String status() {
        return value(STATUS).map(String.class::cast).orElse("PROCESSING");
    }

    public List<String> context() {
        return value(CONTEXT).map(value -> (List<String>) value).orElse(List.of());
    }

    public List<String> ciFindings() {
        return value(CI_FINDINGS).map(value -> (List<String>) value).orElse(List.of());
    }

    public RiskLevel risk() {
        return value(RISK).map(RiskLevel.class::cast).orElse(null);
    }

    public List<String> reasons() {
        return value(REASONS).map(value -> (List<String>) value).orElse(List.of());
    }

    public String action() {
        return value(ACTION).map(String.class::cast).orElse("");
    }

    public boolean requiresApproval() {
        return value(REQUIRES_APPROVAL).map(Boolean.class::cast).orElse(false);
    }

    public String auditEventId() {
        return value(AUDIT_EVENT_ID).map(String.class::cast).orElse("");
    }
}
