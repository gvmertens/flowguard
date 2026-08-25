# Arquitetura e requisitos do grafo

| Node | Responsabilidade | Tipo |
| --- | --- | --- |
| `validate_and_guard` | Valida payload e bloqueia prompt injection | Regra determinística |
| `retrieve_context` | Recupera ADRs e incidentes relevantes | Contexto/RAG simples |
| `query_github` | Consulta PR na GitHub API, com timeout/fallback | Tool/API |
| `assess_risk` | Classifica risco em `RiskLevel` e justifica | Modelo ou fallback |
| `enforce_policy` | Exige aprovação humana para risco alto | Regra determinística |

`retrieve_context` e `query_github` iniciam após `start_parallel` e convergem em `assess_risk`. O estado serializável permite que LangGraph4j clone e recupere o processo em cada node.

