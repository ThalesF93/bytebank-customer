# ByteBank Customer Service

Microsserviço responsável pelo gerenciamento de clientes do ecossistema ByteBank. Centraliza o cadastro, consulta e atualização de clientes, orquestrando a abertura de conta bancária de forma assíncrona via RabbitMQ.

---

## Funcionalidades

**Cadastro de clientes**
Registra um novo cliente com validações de CPF (único), e-mail, idade mínima e endereço. Ao concluir, publica um `CustomerCreatedEvent` via RabbitMQ para o `bytebank-accounts` criar a conta automaticamente. A resposta informa que a conta está sendo processada de forma assíncrona.

**Consulta paginada**
Retorna a lista de clientes com suporte a paginação via parâmetros `page` e `size`.

**Busca por ID**
Retorna os dados resumidos de um cliente pelo ID, com cache no Redis (`@Cacheable`) para evitar consultas desnecessárias ao banco.

**Atualização de dados**
Permite atualizar nome, e-mail e endereço. Invalida o cache automaticamente após a atualização (`@CacheEvict`).

**Idempotência com Redis**
O cadastro recebe um `Idempotency-Key` no header. O resultado é cacheado com TTL de 24h — requisições duplicadas retornam o resultado original sem reprocessar nem publicar evento novamente.

**Recebimento de eventos de conta**
Consome eventos `AccountOpenedEvent` e `AccountFailedEvent` via RabbitMQ para atualizar o `AccountStatus` do cliente conforme o resultado da abertura de conta no `bytebank-accounts`.

**Versão v1 e v2**
O serviço mantém compatibilidade com a versão v1 (endpoint legado direto no repositório) e a versão v2 (com DTOs, validações, idempotência e eventos).

---

## Stack

| Camada | Tecnologia |
|--------|------------|
| Framework | Spring Boot 3.x |
| Mensageria | RabbitMQ (com DLQ configurada) |
| Cache | Redis (`@Cacheable`, `@CacheEvict`, `RedisTemplate`) |
| Banco de dados | PostgreSQL |
| Observabilidade | Prometheus, Zipkin, Spring Boot Actuator |
| Testes | JUnit 5, Mockito |
| Documentação | Swagger / OpenAPI 3 |

---

## Arquitetura

```
src/main/java/br/com/bytebank/customers/
├── api/
│   ├── controller/         # CustomerControllerV1, CustomerControllerV2
│   ├── dtos/               # Requests, Responses, PagedResponse
│   └── openapi/            # Interface com anotações Swagger
├── application/
│   ├── service/            # Interface CustomerService
│   └── impl/               # CustomerServiceImpl
├── domain/
│   ├── entity/             # Customer, PendingAccountOpening
│   ├── enums/              # CustomerStatus, AccountStatus
│   └── exception/          # Exceções customizadas + GlobalExceptionHandler
└── infrastructure/
    ├── config/             # Redis, RabbitMQ, OpenAPI
    ├── messaging/          # CustomerEventPublisher, AccountEventListener, eventos
    └── repositories/       # CustomerRepository, PendingAccountRepository
```

---

## Fluxo de Cadastro

```
POST /api/v2/customers
        ↓
Valida CPF único + campos obrigatórios
        ↓
Verifica idempotência no Redis
        ↓
Persiste Customer (status: PENDING)
        ↓
Publica CustomerCreatedEvent → RabbitMQ → bytebank-accounts
        ↓
Retorna 201 (conta sendo criada de forma assíncrona)

[bytebank-accounts processa e publica AccountOpenedEvent]
        ↓
AccountEventListener consome → atualiza Customer (status: CREATED)
```

---

## Endpoints

> Documentação completa disponível no **[Swagger UI](https://bytebank.thalesf.dev/swagger-ui.html)**

**v2 (atual)**
```
POST   /api/v2/customers              → Cadastrar cliente
GET    /api/v2/customers?page=0&size=10 → Listar clientes (paginado)
GET    /api/v2/customers/{id}         → Buscar cliente por ID
PUT    /api/v2/customers/update/{id}  → Atualizar cliente
```

**v1 (legado)**
```
POST   /api/v1/customers              → Cadastrar cliente (sem validações)
GET    /api/v1/customers              → Listar todos
GET    /api/v1/customers/{id}         → Buscar por ID
PUT    /api/v1/customers/{id}         → Atualizar
DELETE /api/v1/customers/{id}         → Remover
```

Operações de escrita na v2 exigem o header:
```
Idempotency-Key: <UUID>
```

---

## Estados do Cliente

```
AccountStatus:
  PENDING  → conta sendo criada (após cadastro)
  CREATED  → conta aberta com sucesso
  FAILED   → falha na abertura de conta

CustomerStatus:
  ACTIVE   → cliente ativo
  INACTIVE → cliente inativo
```

---

## Eventos RabbitMQ

**Publicados**
- `CustomerCreatedEvent` → exchange `customer.exchange`, routing key `customer.created`, consumido pelo `bytebank-accounts` para abrir a conta. Publicado via `@TransactionalEventListener(AFTER_COMMIT)` — só dispara após commit da transação.

**Consumidos**
- `AccountOpenedEvent` → fila `account.opened`, atualiza `AccountStatus` do cliente para `CREATED`
- `AccountFailedEvent` → fila `account.failed`, atualiza `AccountStatus` do cliente para `FAILED`

A fila `customer.created` tem DLQ configurada (`customer.created.dlq`) para mensagens não processadas.

---

## Como Executar

### Pré-requisitos

- Docker e Docker Compose instalados
- Rede Docker `bytebank-net` criada

### Variáveis de Ambiente

```env
DB_URL=jdbc:postgresql://customer-db:5432/customer_db
DB_USERNAME=bytebank
DB_PASSWORD=bytebank
SPRING_RABBITMQ_HOST=rabbitmq
REDIS_HOST=redis
EUREKA_DEFAULT_ZONE=http://eureka-server:8761/eureka/
ZIPKIN_ENDPOINT=http://zipkin:9411/api/v2/spans
```

### Subindo o serviço

```bash
docker compose -p bytebank-customer up -d --build
```

---

## Testes

Testes unitários cobrindo os principais cenários do `CustomerServiceImpl`:

- Cadastro bem-sucedido com publicação de evento
- Retorno do cache quando a idempotency key já existe (sem reprocessar)
- Lançamento de `DuplicateCustomerException` para CPF duplicado
- Listagem paginada de clientes
- Listagem vazia
- Atualização de cliente bem-sucedida
- `CustomerNotFoundException` ao atualizar cliente inexistente
- Busca por ID bem-sucedida
- `CustomerNotFoundException` ao buscar cliente inexistente

```bash
./gradlew test
```

---

## Autor

**Thales Fernandes**

[![GitHub](https://img.shields.io/badge/GitHub-ThalesF93-181717?style=flat&logo=github)](https://github.com/ThalesF93)
[![LinkedIn](https://img.shields.io/badge/LinkedIn-Thales_Fernandes-0A66C2?style=flat&logo=linkedin)](https://www.linkedin.com/in/thales-fernandes-24418126a/)
