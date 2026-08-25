# DevOps inteligente e anomalias

O pipeline executa Checkstyle, testes e package. Use este prompt para explicar logs de duas etapas do CI:

```text
Explique o log de lint e o de testes. Identifique causa provável, impacto no merge,
menor correção segura e não invente evidências ausentes.
```

Anomalia demonstrável: enviar logs contendo `FAILED` e cobertura abaixo de 60% em módulos `payment` ou `auth`. O FlowGuard classifica risco alto e exige aprovação humana. A estimativa simples de risco é baseada nessa regra reproduzível, não uma previsão estatística: falha de CI + módulo crítico = alto; apenas um sinal = médio; ausência = baixo.

## Leitura de duas etapas da CI

| Etapa | Sinal observado | Diagnóstico e menor correção segura |
| --- | --- | --- |
| Checkstyle | `0 Checkstyle violations` | A convenção de código está atendida; nenhuma correção necessária. |
| Testes de aceitação HTTP | Primeiro carregamento falhou por `ObjectMapper` ausente e fallback de `RiskClassifier` autoexcluído. | Adicionar `JsonConfiguration` e condicionar o classificador determinístico diretamente a `flowguard.ai.enabled=false`; repetir a suíte. |
| Package | JAR criado somente após lint e testes. | Impede publicar um artefato quando uma etapa anterior falha. |

O workflow limita a execução a cinco minutos e publica `target/surefire-reports` mesmo quando a job falha. Assim, a análise deixa de depender do texto efêmero do log da Actions.

## Tendência de risco (dados simulados)

O arquivo [simulated-delivery-signals.csv](simulated-delivery-signals.csv) contém cinco entregas sintéticas, para demonstração reproduzível. Há três builds com sinal de falha de CI (60%) e dois classificados como `HIGH` (40%). Os dois riscos altos combinam módulo crítico (`auth`) com CI `FAILED`; os riscos médios mantêm apenas um dos sinais. A amostra não é previsão estatística de produção: ela serve para demonstrar como o squad pode priorizar investigação e aprovação humana.

## Evidência de observabilidade

`AuditServiceTest#persistsCorrelatedStructuredAuditEvent` registra um evento JSON e confirma que `eventId`, `correlationId`, evento e latência são preservados no arquivo de auditoria. Assim, é possível correlacionar o log estruturado do console, a linha de `runtime/audit.jsonl` e a resposta da API.
