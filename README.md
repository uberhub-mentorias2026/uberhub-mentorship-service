# mentorship-service

Serviço de **Mentorias e Vouchers** do app de mentorias UberHub (Equipe A - Jornada).

- **Porta:** 8084
- **Base URL:** `http://localhost:8084/api/v1`
- **Stack:** Java 17, Spring Boot 3.3, MySQL (Spring Data JPA), Spring Security (JWT), OpenFeign

## Como rodar

```bash
docker compose up -d        # sobe o MySQL local (porta 3307)
mvn spring-boot:run
```

Verificação: `GET http://localhost:8084/actuator/health`

## Variáveis de ambiente

| Variável | Padrão (dev) | Descrição |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3307/mentorship?serverTimezone=UTC` | URL JDBC do MySQL |
| `DB_USER` | `root` | Usuário do banco |
| `DB_PASSWORD` | `root` | Senha do banco (apenas dev) |
| `JWT_SECRET` | valor de desenvolvimento | Segredo do JWT (alinhar com o auth-service) |
| `JWT_ROLES_CLAIM` | `role` | Nome do claim com o papel do usuário |
| `MATCHMAKING_URL` | `http://localhost:8085` | URL do matchmaking-service |
| `MATCHMAKING_STUB` | `true` | Usa um matchmaking falso enquanto o real não existe |

## Estrutura

```
br.com.uberhub.mentorship
├── config/       Segurança (JWT), JPA
├── controller/   Endpoints REST (sem regra de negócio)
├── service/      Regras de negócio e transições de status
├── repository/   Acesso ao banco (Spring Data JPA)
├── model/        Entidades JPA (Mentorship, Voucher) e enums
├── dto/          request/ e response/
├── client/       Chamadas a outros serviços (Matchmaking, Admin, Profile)
├── mapper/       Entity <-> DTO
└── exception/    Exceções de negócio e handler global
```

## Pendências de alinhamento

- [ ] Contrato do matchmaking-service (endpoint, request, response)
- [ ] Formato do JWT do auth-service (algoritmo, claim de roles, claim do userId)
- [ ] Contrato do endpoint de geração de voucher (admin-service)
- [ ] Contratos do admin-service (notificação e analytics)

## Padrão de commits

`feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `chore:`

## Atenção (Windows)

Clone o projeto em um caminho **sem acentos nem espaços** (ex.: `C:\dev\...`).
Caminhos como `Microsserviços` podem causar `ClassNotFoundException` no `mvn spring-boot:run`.
