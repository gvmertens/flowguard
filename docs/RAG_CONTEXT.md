# Memória e recuperação de contexto

O FlowGuard usa um RAG simples, local e auditável para demonstrar recuperação de conhecimento sem enviar documentos internos para um serviço externo.

## Corpus e versionamento

O corpus é versionado em `src/main/resources/context/` no mesmo repositório:

| Fonte | Tags indexadas | Uso |
| --- | --- | --- |
| `ADR-012.md` | `payment`, `auth`, `security` | Exige teste de integração e aprovação humana. |
| `INC-024.md` | `payment`, `auth`, `coverage` | Relaciona queda de cobertura a incidentes anteriores. |
| `REVIEW-STANDARD.md` | `default`, `review` | Fallback para PRs sem contexto específico. |

Cada documento é um chunk curto, identificado pelo nome do arquivo e mantido no histórico Git. Para um corpus maior, a evolução prevista é quebrar os arquivos por seção e persistir os mesmos metadados (`source`, tags e conteúdo) em pgvector.

## Indexação e recuperação

Na inicialização, `RepositoryContextService` carrega os chunks do classpath e monta um índice invertido `tag -> chunks`. A consulta é formada pelos módulos alterados, tokens do resumo do diff e o sinal de cobertura do CI. Os chunks recuperados chegam ao node `retrieve_context` com a origem explícita, por exemplo `[ADR-012.md] ...`.

Se nenhum termo recuperar contexto, o agente devolve `REVIEW-STANDARD.md`. O teste `RepositoryContextServiceTest` reproduz a recuperação de ADR/incidente para `payment`/`auth` e o fallback para `catalog`.
