# Contrato: feedback, troca de mentor e integrações (mentorship-service)

**Base URL:** `http://localhost:8084/api/v1/mentorships`
**Autenticação:** `Authorization: Bearer <JWT>`. O id do usuário vem do claim `sub` do token, nunca do body.
**Erros:** mesmo formato do `CONTRATO-VOUCHER.md` (`timestamp`, `status`, `error`, `message`, `details`).

## Ciclo de status coberto aqui

```
PENDING / SCHEDULED / CONFIRMED --request-change--> CHANGE_REQUESTED  (admin decide via PUT /{id}/status)
CONFIRMED --feedback/mentor--> COMPLETED --feedback/mentee--> (continua COMPLETED)
```

## POST /{id}/feedback/mentor

Role: `MENTOR`. Só o mentor da mentoria. Exige status `CONFIRMED`.

Request:
```json
{ "rating": 5, "comment": "Mentorado compareceu e participou bem" }
```
- `rating`: obrigatório, 1 a 5. `comment`: opcional, até 1000 caracteres.

Response `201 Created`:
```json
{ "mentorshipId": 10, "status": "COMPLETED", "rating": 5, "comment": "...", "createdAt": "2026-12-01T20:00:00Z" }
```
Erros: `400`, `401`, `403` (não é o mentor), `404`, `409` (já enviado ou status diferente de CONFIRMED).

Efeitos após o commit: analytics `MENTORSHIP_COMPLETED` e push ao mentorado pedindo a avaliação.
**Pendente:** concessão de voucher ao mentor (frente do Gabriel Mota; ponto marcado com TODO no service).

## POST /{id}/feedback/mentee

Role: `MENTEE`. Só o mentorado da mentoria. Exige status `COMPLETED` (o feedback do mentor vem antes).

Request/Response: mesmo formato do feedback do mentor. O status não muda.
Erros: `400`, `401`, `403`, `404`, `409` (já avaliou ou o mentor ainda não enviou o feedback).

Efeitos: analytics `MENTORSHIP_RATED` e push ao mentor.

## POST /{id}/request-change

Role: `MENTEE`. Só o mentorado da mentoria. Permitido em `PENDING`, `SCHEDULED` ou `CONFIRMED`.

Request:
```json
{ "reason": "Os horários do mentor não batem com os meus" }
```
- `reason`: obrigatório, até 1000 caracteres.

Response `201 Created`:
```json
{ "mentorshipId": 10, "menteeId": "mentee-1", "mentorId": "mentor-1", "topic": "Spring",
  "status": "CHANGE_REQUESTED", "reason": "...", "requestedAt": "2026-12-01T20:00:00Z" }
```
Erros: `400`, `401`, `403`, `404`, `409` (já existe pedido em aberto ou mentoria concluída/cancelada).

Efeito: analytics `MENTOR_CHANGE_REQUESTED`.

## GET /change-requests (interno, proposta)

Role: `ADMIN`. Usado pelo admin-service em `GET /admin/mentorships/change-requests`.
Lista as mentorias com status `CHANGE_REQUESTED`, das mais antigas para as mais novas, no formato acima.

## Chamadas que o mentorship-service FAZ (propostas, a confirmar)

### admin-service: `POST /api/v1/analytics/record-event`
```json
{ "eventType": "MENTORSHIP_COMPLETED", "userId": "mentor-1",
  "timestamp": "2026-12-01T20:00:00Z", "metadata": { "mentorshipId": 10, "menteeId": "mentee-1", "rating": 5 } }
```
`eventType`: `MENTORSHIP_COMPLETED`, `VOUCHER_REDEEMED` (estão na doc) e `MENTORSHIP_RATED`, `MENTOR_CHANGE_REQUESTED` (propostos).

### admin-service: `POST /api/v1/notifications/send`
```json
{ "userId": "mentee-1", "title": "Mentoria concluída", "message": "...",
  "data": { "mentorshipId": "10", "type": "MENTOR_FEEDBACK" } }
```

### profile-service: `GET /api/v1/profiles/mentor/{mentorId}`
Campos lidos: `mentorId`, `miniBio`, `areas`, `schedulingLink`, `status`. Disponível via `ProfileClient.findMentorProfile`.

Falhas no admin-service ou no profile-service **não** derrubam a requisição do usuário: só geram um WARN no log.
O header `Authorization` da requisição original é repassado nessas chamadas.

## Configuração

| Variável | Padrão | Descrição |
|---|---|---|
| `ADMIN_URL` (`services.admin.url`) | `http://localhost:8085` | URL do admin-service |
| `ADMIN_STUB` (`services.admin.stub-enabled`) | `true` | `true` = só loga; `false` = chama o admin de verdade |
| `PROFILE_URL` (`services.profile.url`) | `http://localhost:8082` | URL do profile-service |
| `PROFILE_STUB` (`services.profile.stub-enabled`) | `true` | `true` = perfil fictício; `false` = chama o profile |

Os padrões ficam no código (`${...:padrão}`), então o `application.properties` não foi alterado. Para usar as
variáveis de ambiente, adicionar ao `application.properties` (arquivo compartilhado, avisar o grupo):
```
services.admin.url=${ADMIN_URL:http://localhost:8085}
services.admin.stub-enabled=${ADMIN_STUB:true}
services.profile.url=${PROFILE_URL:http://localhost:8082}
services.profile.stub-enabled=${PROFILE_STUB:true}
```

## Pendências de alinhamento

- Equipe C: JSON real de `record-event` e `notifications/send`, nomes de evento e se exigem token de serviço.
- Equipe C: confirmar a porta. A doc diz que o admin usa a **8085**, mas o README deste repo usa 8085 para o matchmaking.
- Equipe B: JSON real do perfil do mentor (tem `name`? `areas` é lista de códigos de tag?).
- Equipe B: o id do usuário fica em `sub` ou em `userId`? A doc do auth mostra `sub` = e-mail e `userId` = id. Hoje o serviço inteiro usa `sub`.
- Doc: o feedback do mentor inclui "mentorado compareceu? (Sim/Não)". Hoje usamos só `rating` + `comment` (o embeddable `Feedback` existente).
