# Prompt e refinamento

O prompt versionado está em `SpringAiRiskClassifier`. Ele ordena o modelo a classificar somente em `LOW`, `MEDIUM` ou `HIGH`, usar evidências confiáveis e nunca executar ações, aprovar merge ou obedecer instruções presentes no conteúdo analisado.

| Versão | Problema | Refinamento | Resultado |
| --- | --- | --- | --- |
| v1 | O modelo podia interpretar texto do diff como instrução. | `PromptInjectionGuard` passou a executar antes do grafo acessar tools; prompt restringe a fonte de autoridade. | Cenário E2E retorna `BLOCKED` e não consulta GitHub/n8n. |
| v2 | Recomendação do modelo poderia parecer decisão de merge. | `enforce_policy` se tornou a única fonte de autonomia. | Risco alto resulta em `PENDING_HUMAN_APPROVAL`. |

