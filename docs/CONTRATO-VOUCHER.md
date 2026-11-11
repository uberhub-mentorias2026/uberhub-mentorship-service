# Contrato da API de Vouchers (mentorship-service)

**Base URL:** `http://localhost:8084/api/v1/vouchers`
**Autenticação:** `Authorization: Bearer <JWT>` em todos os endpoints.

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
{ "quantity": 5 }
```
`quantity`: obrigatório, de 1 a 100.

Response `201 Created`:
```json
[
  { "id": 1, "code": "K7M2-9QXA", "status": "AVAILABLE", "createdAt": "2026-11-11T20:00:00Z", "redeemedAt": null }
]
```
Erros: `400` (quantity inválida), `401` (sem token), `403` (sem role ADMIN).

## POST /redeem  (mentorado resgata)

Role exigida: `MENTEE`. O id do mentorado vem do **token (claim `sub`)**, nunca do body.

Request:
```json
{ "code": "K7M2-9QXA" }
```
O código não diferencia maiúsculas/minúsculas e ignora espaços nas pontas.

Response `200 OK`:
```json
{ "id": 1, "code": "K7M2-9QXA", "status": "REDEEMED", "createdAt": "2026-11-11T20:00:00Z", "redeemedAt": "2026-11-11T20:05:00Z" }
```
Erros: `400` (code vazio), `401`, `403` (não é MENTEE), `404` (código não existe), `409` (já resgatado).

## GET /validate/{code}

Qualquer usuário autenticado.

Response `200 OK`:
```json
{ "code": "K7M2-9QXA", "valid": true, "status": "AVAILABLE" }
```
`valid` é `true` somente se o status for `AVAILABLE`.
Erros: `401`, `404` (código não existe).

## Pontos a alinhar com outras equipes

- Nome do claim e valores de role no JWT (`ADMIN`, `MENTEE`, `MENTOR`) - Equipe B.
- O admin-service repassa o JWT do admin ao chamar `/generate`, ou usa um token de serviço? - Equipe C.
