# Automação low-code com n8n

1. Importe `low_code/n8n_flowguard.json` no n8n.
2. Configure a credencial GitHub no node de criação de Issue e selecione o repositório do projeto. O token fica apenas no n8n.
3. Ative o fluxo, copie a URL do Webhook e use-a em `LOW_CODE_WEBHOOK_URL`.
4. Execute o cenário de baixo/médio risco. O FlowGuard envia evento `delivery.routed`; o n8n cria uma Issue observável.

Risco alto não chama o webhook: o backend mantém a decisão em `PENDING_HUMAN_APPROVAL`.

