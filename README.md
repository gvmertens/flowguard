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

## Classificação da solução

O FlowGuard é um sistema híbrido. A parte agêntica organiza o fluxo, recupera contexto, consulta tools e produz uma avaliação estruturada de risco. A parte determinística da aplicação, implementada em Java, valida entradas, bloqueia prompt injection, controla side effects e decide os limites de autonomia.

Essa separação é intencional: o modelo pode classificar e explicar, mas não aprova merge, não ignora regras de segurança e não executa ações externas sem a política permitir.

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

Depois de iniciar, acesse `http://localhost:8080` para usar a interface web simples. Ela permite carregar cenários prontos, chamar a API real e visualizar status, risco, aprovação humana, latência e JSON completo da resposta.

Sem chave de IA, mantenha `FLOWGUARD_AI_ENABLED=false`; o classificador determinístico permite reproduzir todos os cenários. Para ativar modelo, defina `OPENAI_API_KEY`, `FLOWGUARD_MODEL` e `FLOWGUARD_AI_ENABLED=true`. Nenhuma credencial deve entrar no Git.

Exemplo de requisição:

```bash
curl -X POST http://localhost:8080/api/v1/delivery-risk/assess -H "Content-Type: application/json" -d "{\"repository\":\"acme/billing-api\",\"pullRequestId\":\"43\",\"diffSummary\":\"Altera autorização da cobrança\",\"ciLogs\":[\"Tests FAILED: authorization scenario\",\"Coverage 55%\"],\"changedModules\":[\"payment\",\"auth\"]}"
```

## Cenários demonstráveis

1. **Fluxo principal:** mudança não crítica e CI aprovado resulta em `ROUTED`, risco `LOW` e webhook low-code opcional.
2. **Risco alto:** alteração em `payment`/`auth` com falha no CI resulta em `PENDING_HUMAN_APPROVAL`; nenhum webhook é chamado.
3. **Entrada adversarial:** texto que tenta ignorar instruções resulta em `BLOCKED` antes da consulta GitHub ou do modelo.

Para uma demonstração com rastreabilidade completa, use os cenários baseados em branches e PRs reais descritos em [docs/DEMO_BRANCHES.md](docs/DEMO_BRANCHES.md).

## Segurança, memória e observabilidade

- Segredos são configurados apenas por ambiente.
- `PromptInjectionGuard` bloqueia padrões hostis antes de qualquer tool.
- O modelo recebe contexto confiável, mas a política determinística decide autonomia.
- Limites de autonomia, segredos e demonstração adversarial estão em [docs/GOVERNANCE.md](docs/GOVERNANCE.md).
- `RepositoryContextService` recupera ADRs e incidentes curados pelo módulo alterado. Cada item é um chunk versionado com fonte e tags; veja [docs/RAG_CONTEXT.md](docs/RAG_CONTEXT.md).
- Cada execução produz log JSON e linha em `runtime/audit.jsonl`, correlacionados por `correlationId`; a resposta informa a latência.
- A GitHub API tem timeout e fallback de fixture somente em modo de demonstração.

## Qualidade, CI e evidências

```bash
mvn checkstyle:check
mvn test
mvn -DskipTests package
```

Os testes E2E cobrem rota normal, risco alto com aprovação humana e prompt injection. Veja [docs/qa/AI_CODE_REVIEW.md](docs/qa/AI_CODE_REVIEW.md), [docs/evidencias/DEVOPS_ANALYSIS.md](docs/evidencias/DEVOPS_ANALYSIS.md) e [docs/LOW_CODE.md](docs/LOW_CODE.md).

## Análise crítica e limitações

Durante o desenvolvimento, a criação dos testes de aceitação revelou dois ajustes importantes: o DTO de entrada precisava ser serializável para circular pelo estado do LangGraph4j, e o classificador determinístico precisava ser ativado explicitamente quando `FLOWGUARD_AI_ENABLED=false`. Esses achados foram incorporados ao código e documentados em [docs/qa/AI_CODE_REVIEW.md](docs/qa/AI_CODE_REVIEW.md) e [docs/prompts/system-prompt.md](docs/prompts/system-prompt.md).

As principais limitações atuais são o RAG local com corpus pequeno, a ausência de persistência histórica em banco vetorial e o uso de uma política simples de risco. Evoluções naturais seriam persistir avaliações por squad, ampliar o corpus com ADRs reais, adicionar métricas Prometheus e transformar a aprovação humana em um fluxo explícito de ChatOps.

## Entrega

Links centrais para avaliação e rastreabilidade:

| Artefato | Link |
| --- | --- |
| Repositório | [gvmertens/flowguard](https://github.com/gvmertens/flowguard) |
| GitHub Project | [FlowGuard — Projeto Avaliativo SCTEC](https://github.com/users/gvmertens/projects/5) |
| GitHub Actions | [Workflows do projeto](https://github.com/gvmertens/flowguard/actions) |
| Pull Requests | [Histórico de PRs](https://github.com/gvmertens/flowguard/pulls?q=is%3Apr+is%3Aclosed) |
| Issues/Kanban | [Backlog rastreável](https://github.com/gvmertens/flowguard/issues?q=is%3Aissue) |
| Professor colaborador | [wangsouza](https://github.com/wangsouza) com permissão `read` |
| Roteiro de vídeo | [docs/VIDEO_SCRIPT.md](docs/VIDEO_SCRIPT.md) |
| Guia de branches para demo | [docs/DEMO_BRANCHES.md](docs/DEMO_BRANCHES.md) |
| Vídeo | [Apresentação do FlowGuard](https://youtu.be/fqjtfDGNpTM) |

Evidências principais:

| Requisito | Evidência |
| --- | --- |
| Escopo e arquitetura | [docs/PROJECT_CHARTER.md](docs/PROJECT_CHARTER.md), [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) |
| Frontend de demonstração | [index.html](src/main/resources/static/index.html), [FrontendResourceTest.java](src/test/java/br/com/flowguard/api/FrontendResourceTest.java) |
| Cenários com branches reais | [docs/DEMO_BRANCHES.md](docs/DEMO_BRANCHES.md), [PR #26](https://github.com/gvmertens/flowguard/pull/26), [PR #16](https://github.com/gvmertens/flowguard/pull/16), [PR #14](https://github.com/gvmertens/flowguard/pull/14) |
| Grafo LangGraph4j | [DeliveryRiskService.java](src/main/java/br/com/flowguard/application/DeliveryRiskService.java), [FlowGuardState.java](src/main/java/br/com/flowguard/application/FlowGuardState.java) |
| Tool/API GitHub | [GitHubPullRequestClient.java](src/main/java/br/com/flowguard/infrastructure/GitHubPullRequestClient.java), [PR #12](https://github.com/gvmertens/flowguard/pull/12) |
| Memória/RAG | [docs/RAG_CONTEXT.md](docs/RAG_CONTEXT.md), [RepositoryContextService.java](src/main/java/br/com/flowguard/application/RepositoryContextService.java) |
| Governança e segurança | [docs/GOVERNANCE.md](docs/GOVERNANCE.md), [PR #14](https://github.com/gvmertens/flowguard/pull/14) |
| Observabilidade | [AuditService.java](src/main/java/br/com/flowguard/application/AuditService.java), [PR #11](https://github.com/gvmertens/flowguard/pull/11) |
| QA com IA e testes | [docs/qa/AI_CODE_REVIEW.md](docs/qa/AI_CODE_REVIEW.md), [PR #15](https://github.com/gvmertens/flowguard/pull/15) |
| CI e análise de anomalias | [docs/evidencias/DEVOPS_ANALYSIS.md](docs/evidencias/DEVOPS_ANALYSIS.md), [PR #16](https://github.com/gvmertens/flowguard/pull/16) |
| Automação low-code n8n | [docs/LOW_CODE.md](docs/LOW_CODE.md), [PR #19](https://github.com/gvmertens/flowguard/pull/19), [Issue #18](https://github.com/gvmertens/flowguard/issues/18) e [Issue #29](https://github.com/gvmertens/flowguard/issues/29) criadas pelo n8n |
