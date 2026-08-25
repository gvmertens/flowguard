# DevOps inteligente e anomalias

O pipeline executa Checkstyle, testes e package. Use este prompt para explicar logs de duas etapas do CI:

```text
Explique o log de lint e o de testes. Identifique causa provável, impacto no merge,
menor correção segura e não invente evidências ausentes.
```

Anomalia demonstrável: enviar logs contendo `FAILED` e cobertura abaixo de 60% em módulos `payment` ou `auth`. O FlowGuard classifica risco alto e exige aprovação humana. A estimativa simples de risco é baseada nessa regra reproduzível, não uma previsão estatística: falha de CI + módulo crítico = alto; apenas um sinal = médio; ausência = baixo.

## Evidência de observabilidade

`AuditServiceTest#persistsCorrelatedStructuredAuditEvent` registra um evento JSON e confirma que `eventId`, `correlationId`, evento e latência são preservados no arquivo de auditoria. Assim, é possível correlacionar o log estruturado do console, a linha de `runtime/audit.jsonl` e a resposta da API.
