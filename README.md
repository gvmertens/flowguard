# FlowGuard

Agente Java para ajudar squads ágeis a avaliar risco de entrega antes do merge. Ele recebe sinais de um Pull Request e do CI, recupera contexto do repositório, consolida evidências e gera uma decisão estruturada. O agente não aprova merge, publica comentário ou cria Issue sem que a política permita; risco alto exige aprovação humana.

## Escopo

- **Público:** desenvolvedores, Tech Leads, QA e SREs.
- **Entrada:** repositório, número do PR, resumo do diff, logs de CI e módulos alterados.
- **Saída:** JSON tipado com risco, evidências, recomendação, decisão de autonomia, `correlationId`, latência e evento de auditoria.
- **Fora de escopo:** substituir revisão de código ou alterar repositórios remotamente.

## Arquitetura

```text
POST /api/v1/delivery-risk/assess
  -> validate_and_guard
  -> start_parallel
  -> [retrieve_context] + [query_github]
  -> assess_risk (Spring AI ou fallback determinístico)
  -> enforce_policy
  -> ROUTED | PENDING_HUMAN_APPROVAL | BLOCKED
```

O grafo é implementado com LangGraph4j, compartilhando `FlowGuardState` entre nodes e usando `START`, `END`, edges explícitas, ramificação condicional e duas consultas paralelas. A execução termina sempre em uma decisão; não há ciclo de retry sem limite.

## Tecnologias

- Java 21, Spring Boot 4.1 e Maven
- LangGraph4j para orquestração agêntica
- Spring AI/OpenAI opcional para classificar risco em saída estruturada
- GitHub REST API como tool, com timeout, validação de repositório e fallback de fixture
- n8n como automação low-code por webhook

## Executar

```bash
copy .env.example .env
mvn test
mvn spring-boot:run
```

Sem chave de IA, mantenha `FLOWGUARD_AI_ENABLED=false`; o classificador determinístico permite reproduzir todos os cenários. Para ativar modelo, defina `OPENAI_API_KEY`, `FLOWGUARD_MODEL` e `FLOWGUARD_AI_ENABLED=true`. Nenhuma credencial deve entrar no Git.

Exemplo de requisição:

```bash
curl -X POST http://localhost:8080/api/v1/delivery-risk/assess -H "Content-Type: application/json" -d "{\"repository\":\"acme/billing-api\",\"pullRequestId\":\"43\",\"diffSummary\":\"Altera autorização da cobrança\",\"ciLogs\":[\"Tests FAILED: authorization scenario\",\"Coverage 55%\"],\"changedModules\":[\"payment\",\"auth\"]}"
```

## Cenários demonstráveis

1. **Fluxo principal:** mudança não crítica e CI aprovado resulta em `ROUTED`, risco `LOW` e webhook low-code opcional.
2. **Risco alto:** alteração em `payment`/`auth` com falha no CI resulta em `PENDING_HUMAN_APPROVAL`; nenhum webhook é chamado.
3. **Entrada adversarial:** texto que tenta ignorar instruções resulta em `BLOCKED` antes da consulta GitHub ou do modelo.

## Segurança, memória e observabilidade

- Segredos são configurados apenas por ambiente.
- `PromptInjectionGuard` bloqueia padrões hostis antes de qualquer tool.
- O modelo recebe contexto confiável, mas a política determinística decide autonomia.
- `RepositoryContextService` recupera ADRs e incidentes curados pelo módulo alterado. Cada item é um chunk versionado; a evolução prevista é indexá-los em pgvector sem alterar o contrato do grafo.
- Cada execução produz log JSON e linha em `runtime/audit.jsonl`, correlacionados por `correlationId`; a resposta informa a latência.
- A GitHub API tem timeout e fallback de fixture somente em modo de demonstração.

## Qualidade, CI e evidências

```bash
mvn checkstyle:check
mvn test
mvn -DskipTests package
```

Os testes E2E cobrem rota normal, risco alto com aprovação humana e prompt injection. Veja [docs/qa/AI_CODE_REVIEW.md](docs/qa/AI_CODE_REVIEW.md), [docs/evidencias/DEVOPS_ANALYSIS.md](docs/evidencias/DEVOPS_ANALYSIS.md) e [docs/LOW_CODE.md](docs/LOW_CODE.md).

## Entrega

Inclua aqui, antes da submissão, os links do repositório, GitHub Project e vídeo não listado no YouTube.

| Artefato | Link |
| --- | --- |
| Repositório | A preencher |
| GitHub Project | A preencher |
| Vídeo | A preencher |

