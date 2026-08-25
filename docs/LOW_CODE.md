# Automação low-code com n8n

1. Importe `low_code/n8n_flowguard.json` no n8n.
2. Configure a credencial GitHub no node de criação de Issue e selecione o repositório do projeto. O token fica apenas no n8n.
3. Ative o fluxo, copie a URL do Webhook e use-a em `LOW_CODE_WEBHOOK_URL`.
4. Execute o cenário de baixo/médio risco. O FlowGuard envia evento `delivery.routed`; o n8n cria uma Issue observável.

Risco alto não chama o webhook: o backend mantém a decisão em `PENDING_HUMAN_APPROVAL`.

## Evidência de execução real

Execução realizada em 2026-08-25 com o workflow publicado no n8n e credencial GitHub configurada no node `Criar Issue no GitHub`.

- Endpoint n8n: Production URL configurada em ambiente local como `LOW_CODE_WEBHOOK_URL`; a URL completa não foi versionada para evitar exposição de um webhook ativo.
- Entrada: cenário de baixo risco com CI verde, mudança documental e módulos `docs` e `low_code`.
- Resultado do FlowGuard: `status=ROUTED`, `risk=LOW`, `requiresHumanApproval=false`.
- `correlationId`: `1786ab2b-e2a8-4379-9be9-b3d2c75f90e3`.
- Evidência observável criada pelo n8n: Issue #18 no repositório `gvmertens/flowguard`, título `FlowGuard 1786ab2b-e2a8-4379-9be9-b3d2c75f90e3`.
