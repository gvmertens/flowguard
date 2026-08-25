# Code review com apoio de IA

Prompt utilizado para revisar a alteração de governança:

```text
Revise este diff Java como mudança de segurança. Procure caminho em que uma entrada de PR
atinja API, webhook ou modelo sem validação. Priorize por impacto e proponha teste reproduzível.
```

Achado: o estado do LangGraph4j é clonado entre nodes; o DTO de entrada não serializável interrompia a execução. A correção foi implementar `Serializable` em `DeliverySignal` e adicionar três testes E2E. O teste de injection é prioritário porque uma regressão poderia acionar integração externa a partir de texto hostil.

