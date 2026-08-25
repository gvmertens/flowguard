package br.com.flowguard.application;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

import br.com.flowguard.api.DeliveryAssessment;
import br.com.flowguard.api.DeliverySignal;
import br.com.flowguard.api.RiskLevel;
import br.com.flowguard.infrastructure.GitHubPullRequestClient;
import br.com.flowguard.infrastructure.LowCodeNotifier;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.springframework.stereotype.Service;

@Service
public class DeliveryRiskService {
    private final PromptInjectionGuard injectionGuard;
    private final RepositoryContextService contextService;
    private final GitHubPullRequestClient gitHubClient;
    private final RiskClassifier classifier;
    private final AuditService auditService;
    private final LowCodeNotifier notifier;

    public DeliveryRiskService(
            PromptInjectionGuard injectionGuard,
            RepositoryContextService contextService,
            GitHubPullRequestClient gitHubClient,
            RiskClassifier classifier,
            AuditService auditService,
            LowCodeNotifier notifier) {
        this.injectionGuard = injectionGuard;
        this.contextService = contextService;
        this.gitHubClient = gitHubClient;
        this.classifier = classifier;
        this.auditService = auditService;
        this.notifier = notifier;
    }

    public DeliveryAssessment assess(DeliverySignal signal) {
        Instant startedAt = Instant.now();
        String correlationId = UUID.randomUUID().toString();
        FlowGuardState finalState = invokeGraph(signal, correlationId);
        long latencyMs = Duration.between(startedAt, Instant.now()).toMillis();
        DeliveryAssessment assessment = new DeliveryAssessment(
                correlationId,
                finalState.status(),
                finalState.risk(),
                finalState.reasons(),
                finalState.context(),
                finalState.action(),
                finalState.requiresApproval(),
                latencyMs,
                finalState.auditEventId());
        if (!assessment.requiresHumanApproval() && "ROUTED".equals(assessment.status())) {
            notifier.notifyRoute(assessment);
        }
        return assessment;
    }

    private FlowGuardState invokeGraph(DeliverySignal signal, String correlationId) {
        try {
            var graph = new StateGraph<>(FlowGuardState.SCHEMA, FlowGuardState::new)
                    .addNode("validate_and_guard", node_async(this::validateAndGuard))
                    .addNode("start_parallel", node_async(state -> Map.of()))
                    .addNode("retrieve_context", node_async(this::retrieveContext))
                    .addNode("query_github", node_async(this::queryGitHub))
                    .addNode("assess_risk", node_async(this::assessRisk))
                    .addNode("enforce_policy", node_async(this::enforcePolicy))
                    .addEdge(START, "validate_and_guard")
                    .addConditionalEdges(
                            "validate_and_guard",
                            edge_async(state -> "BLOCKED".equals(state.status()) ? "blocked" : "trusted"),
                            Map.of("blocked", END, "trusted", "start_parallel"))
                    .addEdge("start_parallel", "retrieve_context")
                    .addEdge("start_parallel", "query_github")
                    .addEdge("retrieve_context", "assess_risk")
                    .addEdge("query_github", "assess_risk")
                    .addEdge("assess_risk", "enforce_policy")
                    .addEdge("enforce_policy", END)
                    .compile();
            return graph.invoke(Map.of(FlowGuardState.SIGNAL, signal, FlowGuardState.CORRELATION_ID, correlationId))
                    .orElseThrow(() -> new IllegalStateException("grafo terminou sem estado final"));
        } catch (GraphStateException exception) {
            throw new IllegalStateException("grafo FlowGuard inválido", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("falha ao executar grafo FlowGuard", exception);
        }
    }

    private Map<String, Object> validateAndGuard(FlowGuardState state) {
        DeliverySignal signal = state.signal();
        boolean untrusted = injectionGuard.isUntrusted(signal.diffSummary())
                || signal.ciLogs().stream().anyMatch(injectionGuard::isUntrusted);
        if (untrusted) {
            String auditId = auditService.record(
                    state.correlationId(), "security.input_blocked", Map.of("repository", signal.repository()));
            return Map.of(
                    FlowGuardState.STATUS, "BLOCKED",
                    FlowGuardState.ACTION, "Encaminhar o conteúdo para revisão manual sem chamar tools.",
                    FlowGuardState.REASONS, List.of("Entrada não confiável detectada."),
                    FlowGuardState.REQUIRES_APPROVAL, false,
                    FlowGuardState.AUDIT_EVENT_ID, auditId);
        }
        auditService.record(state.correlationId(), "delivery.validated", Map.of("repository", signal.repository()));
        return Map.of();
    }

    private Map<String, Object> retrieveContext(FlowGuardState state) {
        return Map.of(FlowGuardState.CONTEXT, contextService.retrieve(state.signal()));
    }

    private Map<String, Object> queryGitHub(FlowGuardState state) {
        GitHubPullRequestClient.PullRequestContext context = gitHubClient.fetch(state.signal());
        return Map.of(FlowGuardState.CI_FINDINGS, context.evidence());
    }

    private Map<String, Object> assessRisk(FlowGuardState state) {
        List<String> findings = new ArrayList<>(state.ciFindings());
        state.signal().ciLogs().forEach(log -> {
            String normalized = log.toLowerCase();
            if (normalized.contains("failed") || normalized.contains("error")) {
                findings.add("FALHA de CI identificada: " + log.substring(0, Math.min(120, log.length())));
            }
            if (normalized.contains("coverage") && normalized.matches(".*([0-5][0-9])%.*")) {
                findings.add("Cobertura abaixo de 60% identificada no CI.");
            }
        });
        RiskClassifier.Classification classification = classifier.classify(state.signal(), state.context(), findings);
        return Map.of(
                FlowGuardState.RISK, classification.risk(),
                FlowGuardState.REASONS, classification.reasons(),
                FlowGuardState.ACTION, classification.recommendedAction());
    }

    private Map<String, Object> enforcePolicy(FlowGuardState state) {
        boolean needsApproval = state.risk() == RiskLevel.HIGH;
        String status = needsApproval ? "PENDING_HUMAN_APPROVAL" : "ROUTED";
        String auditId = auditService.record(
                state.correlationId(),
                "policy.decision",
                Map.of("risk", state.risk(), "status", status, "requiresApproval", needsApproval));
        return Map.of(
                FlowGuardState.STATUS, status,
                FlowGuardState.REQUIRES_APPROVAL, needsApproval,
                FlowGuardState.AUDIT_EVENT_ID, auditId);
    }
}
