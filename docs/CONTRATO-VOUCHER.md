# Contrato da API de Vouchers (mentorship-service)

**Base URL:** `http://localhost:8084/api/v1/vouchers`
**Autenticação:** `Authorization: Bearer <JWT>` em todos os endpoints.

**Formato do código:** `XXXX-XXXX-XXXX` (letras e números, 14 caracteres com os hifens).

**Fluxo:** o mentor solicita ao admin -> o admin gera/libera o voucher para o mentor (`ownerId`)
-> o mentor compartilha o código -> o mentorado resgata. Depois do resgate o voucher não é usado
em nenhuma outra regra.

Formato de erro (todos os endpoints):

```json
{
  "timestamp": "2026-11-11T20:00:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "Voucher já foi utilizado",
  "details": []
}
```

## POST /generate  (interno, chamado pelo admin-service)

Role exigida: `ADMIN`.

Request:
```json
{ "quantity": 3, "ownerId": "id-do-mentor" }
```
- `quantity`: obrigatório, de 1 a 100.
- `ownerId`: opcional, até 64 caracteres. Id do mentor que recebe os vouchers. Se omitido, o voucher fica sem dono.

Response `201 Created`:
```json
[
  { "id": 1, "code": "K7M2-9QXA-4HTB", "status": "AVAILABLE", "createdAt": "2026-11-11T20:00:00Z", "redeemedAt": null }
]
```
Erros: `400` (dados inválidos), `401` (sem token), `403` (sem role ADMIN).

## GET /mentor  (proposta, a confirmar com o grupo)

Role exigida: `MENTOR`. Lista os vouchers cujo `ownerId` é o `sub` do token, do mais recente ao mais antigo.

Response `200 OK`: lista no mesmo formato do `/generate` (inclui os já resgatados, com `status: "REDEEMED"`).
Erros: `401`, `403`.

## POST /redeem  (mentorado resgata)

Role exigida: `MENTEE` (a confirmar com a Equipe B). O id do mentorado vem do **token (claim `sub`)**.

Request:
```json
{ "code": "K7M2-9QXA-4HTB" }
```
O código deve ter o formato `XXXX-XXXX-XXXX`. Não diferencia maiúsculas de minúsculas.

Response `200 OK`:
```json
{ "id": 1, "code": "K7M2-9QXA-4HTB", "status": "REDEEMED", "createdAt": "2026-11-11T20:00:00Z", "redeemedAt": "2026-11-11T20:05:00Z" }
```
Erros: `400` (formato inválido), `401`, `403`, `404` (código não existe), `409` (já resgatado).

## GET /validate/{code}

Qualquer usuário autenticado.

Response `200 OK`:
```json
{ "code": "K7M2-9QXA-4HTB", "valid": true, "status": "AVAILABLE" }
```
`valid` é `true` somente se o status for `AVAILABLE`. Erros: `401`, `404`.

## Pendências

- Como o mentor solicita o voucher ao admin (Equipe C).
- Dados que o mentorship-service envia ao admin-service após o resgate (contrato do admin).
- Role do usuário antes do resgate e nome do claim no JWT (Equipe B).
