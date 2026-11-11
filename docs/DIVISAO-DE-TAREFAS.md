# Divisão de tarefas - mentorship-service

| Pessoa | Frente | Entregáveis | Marco |
|---|---|---|---|
| **Gabriel Mota** | Vouchers + base do projeto + DER | `POST /vouchers/generate`, `POST /vouchers/redeem`, `GET /vouchers/validate/{code}`; DER; revisão de PRs; depois, concessão de voucher ao mentor | 1 (até 14/11), 3 |
| **Gabriel Manzi** | Núcleo da mentoria | `POST /mentorships` (usa MatchmakingClient), `GET /mentorships/mentee`, `GET /mentorships/mentor`, DTOs e mapper | 2 (até 28/11) |
| **Maxsuel Silva** | Agendamento e status | `PUT /{id}/schedule`, `POST /{id}/schedule/confirm`, `PUT /{id}/status`, validação das transições de status | 2-3 |
| **Matheus Rezende** | Feedback e integrações | `POST /{id}/feedback/mentor`, `POST /{id}/feedback/mentee`, `POST /{id}/request-change`, `AdminClient` (notificação e analytics), `ProfileClient` | 3 (até 05/12) |

## Para evitar conflitos de merge

Cada pessoa cria os **seus próprios arquivos**, em vez de editar o mesmo:

- Manzi: `MentorshipController`, `MentorshipService`
- Maxsuel: `MentorshipScheduleController`, `MentorshipScheduleService`
- Matheus: `MentorshipFeedbackController`, `MentorshipFeedbackService`, pacote `client/`

Vários controllers podem usar o mesmo `@RequestMapping("/api/v1/mentorships")`, desde que os caminhos finais sejam diferentes.

Arquivos compartilhados (`pom.xml`, `SecurityConfig`, `application.properties`, `GlobalExceptionHandler`): avisar o grupo antes de alterar.

## Fluxo

`git checkout main && git pull` -> `git checkout -b feature/<tarefa>` -> commits pequenos -> `mvn clean verify` -> push -> Pull Request com 1 revisor.
