# Code review com apoio de IA

Prompt utilizado para revisar a alteração de governança:

```text
Revise este diff Java como mudança de segurança. Procure caminho em que uma entrada de PR
atinja API, webhook ou modelo sem validação. Priorize por impacto e proponha teste reproduzível.
```

Achado: o estado do LangGraph4j é clonado entre nodes; o DTO de entrada não serializável interrompia a execução. A correção foi implementar `Serializable` em `DeliverySignal` e adicionar testes de serviço e de aceitação HTTP. O teste de injection é prioritário porque uma regressão poderia acionar integração externa a partir de texto hostil.

## Estratégia de testes priorizada por risco

| Prioridade | Cenário | Tipo | Evidência |
| --- | --- | --- |
| P0 | Prompt injection não chama tool e termina bloqueado | Segurança/E2E | `GovernancePolicyTest` e `DeliveryRiskControllerAcceptanceTest` |
| P0 | `payment`/`auth` com CI falho exige humano | Aceitação HTTP | `DeliveryRiskControllerAcceptanceTest` |
| P1 | Tool GitHub aceita contrato e ativa fallback | Integração | `GitHubPullRequestClientIntegrationTest` |
| P1 | Recuperação de ADR/incidente preserva fonte | Unidade | `RepositoryContextServiceTest` |
| P2 | Payload inválido é recusado no endpoint | Contrato HTTP | `DeliveryRiskControllerAcceptanceTest` |

Os casos P0 protegem efeitos externos e autorização; por isso executam em toda CI antes do package.

Durante a criação do teste de aceitação, o carregamento do contexto revelou duas falhas: os adaptadores pediam `ObjectMapper`, mas a configuração não o expunha como bean; e o fallback `RiskClassifier` se autoexcluía quando a IA estava desligada. `JsonConfiguration` torna a primeira dependência explícita, enquanto `DeterministicRiskClassifier` agora é condicionado diretamente a `flowguard.ai.enabled=false`.
