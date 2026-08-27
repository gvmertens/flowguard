# Arquitetura e requisitos do grafo

| Node | Responsabilidade | Tipo |
| --- | --- | --- |
| `validate_and_guard` | Valida payload e bloqueia prompt injection | Regra determinística |
| `retrieve_context` | Recupera ADRs e incidentes relevantes | Contexto/RAG simples |
| `query_github` | Consulta PR na GitHub API, com timeout/fallback | Tool/API |
| `assess_risk` | Classifica risco em `RiskLevel` e justifica | Modelo ou fallback |
| `enforce_policy` | Exige aprovação humana para risco alto | Regra determinística |

`retrieve_context` e `query_github` iniciam após `start_parallel` e convergem em `assess_risk`. O estado serializável permite que LangGraph4j clone e recupere o processo em cada node.

## Contrato da tool GitHub

`GitHubPullRequestClient` chama `GET /repos/{owner}/{repository}/pulls/{pullRequestId}`. Antes da chamada, o identificador do repositório precisa respeitar o esquema `owner/repository`; o número da PR é fornecido pelo DTO validado da API. A tool aceita somente JSON que tenha `number` e `state`, usa `Accept: application/vnd.github+json`, tem timeout de três segundos e nunca registra o token.

Sem `GITHUB_TOKEN`, a aplicação usa uma fixture local para demonstração. Em timeout, falha de conexão, payload inválido ou resposta HTTP de erro, retorna `fixture_fallback` com evidência explícita. O teste de integração local reproduz o contrato HTTP, um `503` e a rejeição de um repositório fora do esquema, sem chamar a API pública.
