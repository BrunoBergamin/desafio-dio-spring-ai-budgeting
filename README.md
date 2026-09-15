# 💰 Assistente de Orçamento com Spring AI

API REST de controle de gastos em que você **fala** o que gastou ("gastei 80 reais no mercado") e a IA registra a transação no banco e responde **em áudio**. Também dá para perguntar ("quanto gastei este mês?") e receber um resumo por categoria.

Projeto desenvolvido no **Desafio de Projeto DIO + Itaú**, evoluindo o projeto final do módulo [05-spring-ai](https://github.com/digitalinnovationone/dio-spring-boot-learning-track/tree/main/05-spring-ai) do expert Poiani.

---

## 🔄 Fluxo principal

```mermaid
sequenceDiagram
    participant C as Cliente
    participant AC as AssistantController
    participant AS as AssistantService
    participant W as Whisper (speech-to-text)
    participant LLM as ChatClient (gpt-4o-mini)
    participant T as TransactionTools
    participant TS as TransactionService
    participant DB as Banco (JPA)
    participant TTS as TextToSpeech

    C->>AC: POST /assistant/voice (áudio)
    AC->>AS: voiceToVoice(file)
    AS->>W: transcribe(áudio)
    W-->>AS: "gastei 80 reais no mercado"
    AS->>LLM: prompt(system + texto)
    LLM->>T: tool call registrar_transacao(80.00, GROCERIES)
    T->>TS: create(request)
    TS->>TS: valida regras de negócio
    TS->>DB: save(transaction)
    DB-->>LLM: transação salva
    LLM-->>AS: "Registrei oitenta reais em mercado."
    AS->>TTS: call(texto)
    TTS-->>C: resposta.mp3
```

1. O cliente envia um arquivo de áudio.
2. O áudio vira texto (`TranscriptionModel` / Whisper).
3. O `ChatClient` entende a intenção e escolhe uma **ferramenta** (Tool Calling).
4. A ferramenta chama o `TransactionService`, que valida e persiste/consulta pelo `TransactionRepository`.
5. A IA gera a resposta final em texto.
6. O texto vira áudio MP3 (`TextToSpeechModel`).

---

## 🏗️ Arquitetura em camadas

```
src/main/java/dio/budgeting
├── controller/     → recebe HTTP, valida entrada (@Valid) e devolve DTOs
│   ├── TransactionController   (CRUD + consultas)
│   └── AssistantController     (texto e voz)
├── service/        → regras de negócio e orquestração
│   ├── TransactionService      (validação, cálculo do resumo)
│   └── AssistantService        (áudio → IA → áudio)
├── tool/           → adaptador entre a IA e o service (@Tool)
│   └── TransactionTools
├── repository/     → acesso a dados (Spring Data JPA)
│   ├── TransactionRepository
│   └── CategoryTotal           (projeção da consulta agregada)
├── entity/         → modelo persistido
│   ├── Transaction
│   └── Category (enum)
├── dto/
│   ├── request/    → TransactionRequest, ChatRequest
│   └── response/   → TransactionResponse, SpendingSummaryResponse, CategorySummaryResponse, AssistantResponse
├── mapper/         → converte Entity ⇄ DTO
├── exception/      → exceções de negócio + GlobalExceptionHandler (ProblemDetail)
└── config/         → ChatClient (tools registradas) e OpenAPI/Swagger
```

**Regra que segui:** o controller não conhece o repository, a entity não sai da API (sempre DTO), e a IA nunca toca no banco diretamente: ela só chama `TransactionTools`, que delega para o mesmo `TransactionService` usado pelos endpoints REST. Assim, **REST e IA passam pelas mesmas regras de negócio**.

---

## 🚀 Melhorias que implementei

| # | Melhoria | Onde |
|---|----------|------|
| 1 | **Reorganização em camadas clássicas** (controller, service, repository, entity, dto, mapper, exception, config, tool). O `ChatClient` saiu do construtor do controller e foi para `ChatClientConfig`; o fluxo de IA saiu do controller e foi para `AssistantService`. | todo o projeto |
| 2 | **Validações antes de salvar**: valor > 0 e ≤ 100.000, no máximo 2 casas decimais, descrição obrigatória (até 120 caracteres), categoria obrigatória, data não pode ser futura. As regras ficam nas anotações do DTO e o **service reaplica o mesmo `Validator`**, porque quando a IA chama a ferramenta a requisição não passa pelo `@Valid` do controller. | `TransactionRequest`, `TransactionService#validate` |
| 3 | **Correção de valor monetário**: o projeto base guardava centavos em `long`, mas a saída não dividia por 100 (80 reais apareciam como 8000.0). Troquei por `BigDecimal` em reais com 2 casas. | `Transaction`, `TransactionRequest` |
| 4 | **Novas consultas financeiras**: listagem com filtro opcional de categoria e período, e **resumo de gastos** com total, quantidade e percentual por categoria (padrão: mês atual). | `GET /transactions`, `GET /transactions/summary` |
| 5 | **Novas ferramentas para Tool Calling**: `registrar_transacao` (agora com data), `listar_transacoes`, `ultimas_transacoes` e `resumo_de_gastos`. | `TransactionTools` |
| 6 | **Respostas da IA melhores**: o system prompt recebe a **data de hoje** (para entender "ontem", "este mês"), proíbe inventar valores, pede o valor quando ele não foi dito e gera frases curtas e sem markdown, próprias para virar áudio. Mais categorias: restaurante, transporte, moradia, lazer, educação, outros. | `prompts/system-message.st`, `Category` |
| 7 | **Endpoints REST completos**: CRUD (`GET/PUT/DELETE /transactions/{id}`), `201 Created` com header `Location`, e erros padronizados com **ProblemDetail (RFC 9457)**: 400 com erros por campo, 404, 422 para regra de negócio, 413 para áudio grande. | `TransactionController`, `GlobalExceptionHandler` |
| 8 | **Endpoints de IA para testar sem ouvir áudio**: `POST /assistant/chat` (texto → texto) e `POST /assistant/voice/text` (áudio → transcrição + resposta em JSON), além do fluxo original áudio → MP3. Validação do arquivo enviado (vazio / não é áudio). | `AssistantController`, `AssistantService` |
| 9 | **Auditoria por log**: cada chamada de ferramenta, transcrição, pergunta e resposta da IA é registrada no log, e a entidade guarda `createdAt`/`updatedAt`. | `TransactionTools`, `AssistantService`, `Transaction` |
| 10 | **Testes automatizados** dos principais fluxos (22 testes sem custo + 3 de ponta a ponta com OpenAI). | `src/test` |
| 11 | **Roda sem Docker**: H2 em memória por padrão; MySQL continua disponível pelo perfil `mysql`. Documentação interativa com **Swagger UI**. | `application*.properties` |

---

## 🛠️ Tecnologias

- **Java 25** (LTS mais recente)
- **Spring Boot 4.1** (Web, Validation, Data JPA)
- **Spring AI 2.0** com OpenAI: `ChatClient`, Tool Calling (`@Tool`), `TranscriptionModel` (whisper-1), `TextToSpeechModel` (gpt-4o-mini-tts)
- **H2** (padrão) e **MySQL 9** via Docker Compose (perfil `mysql`)
- **Lombok**
- **springdoc-openapi** (Swagger UI)
- **JUnit 5, Mockito, MockMvc, @DataJpaTest, AssertJ**
- **Maven** (Maven Wrapper incluso, não precisa instalar)

---

## ▶️ Como executar

### Pré-requisitos
- JDK 25 (com `JAVA_HOME` apontando para ele)
- Uma chave da OpenAI (só para os endpoints `/assistant/**`; o CRUD funciona sem ela)

### 1. Defina a chave da OpenAI

Linux/macOS/Git Bash:
```bash
export OPENAI_API_KEY="sk-..."
```
PowerShell:
```powershell
$env:OPENAI_API_KEY="sk-..."
```

### 2. Suba a aplicação

```bash
./mvnw spring-boot:run          # Windows: .\mvnw.cmd spring-boot:run
```

Com MySQL (precisa do Docker rodando, o Spring sobe o `compose.yml` sozinho):
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=mysql
```

### 3. Acesse

| O quê | URL |
|------|-----|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Console H2 | http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:budgeting`, usuário `sa`) |

---

## 🧪 Como testar o fluxo principal

O arquivo [`requests.http`](requests.http) tem todas as chamadas prontas para o IntelliJ ou VS Code. Com `curl`:

### Voz → voz (fluxo principal)
```bash
curl -X POST http://localhost:8080/assistant/voice \
  -F "file=@src/test/resources/audio/recording-1.m4a" \
  --output resposta.mp3
```
Abra o `resposta.mp3` para ouvir a confirmação.

### Voz → texto (para ver o que a IA entendeu)
```bash
curl -X POST http://localhost:8080/assistant/voice/text \
  -F "file=@src/test/resources/audio/recording-1.m4a"
```
Formato da resposta (o texto exato varia conforme o modelo):
```json
{
  "transcription": "Gastei 80 reais no mercado.",
  "answer": "Registrei oitenta reais em mercado."
}
```

### Texto → texto
```bash
curl -X POST http://localhost:8080/assistant/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Quanto eu gastei este mês e em qual categoria gastei mais?"}'
```

### REST direto (sem IA)
```bash
# criar
curl -i -X POST http://localhost:8080/transactions \
  -H "Content-Type: application/json" \
  -d '{"description":"Compras no mercado","amount":80.50,"category":"GROCERIES"}'

# resumo do mês
curl http://localhost:8080/transactions/summary
```

Exemplos de respostas reais obtidas rodando a aplicação:

```http
HTTP/1.1 201
Location: http://localhost:8080/transactions/c0905fed-9902-4793-9fb7-d8536e8964c7

{"id":"c0905fed-...","description":"Compras no mercado","amount":80.50,
 "category":"GROCERIES","categoryLabel":"Mercado","date":"2026-09-15"}
```

```json
// POST /transactions com {"description":"","amount":-5,"date":"2099-01-01"} → 400
{
  "title": "Dados inválidos",
  "status": 400,
  "detail": "Um ou mais campos estão inválidos",
  "errors": {
    "date": "a data não pode estar no futuro",
    "amount": "o valor deve ser maior que zero",
    "category": "a categoria é obrigatória",
    "description": "a descrição é obrigatória"
  }
}
```

```json
// GET /transactions/summary
{"start":"2026-09-01","end":"2026-09-15","total":80.50,"quantity":1,
 "categories":[{"category":"GROCERIES","categoryLabel":"Mercado","total":80.50,"quantity":1,"percentage":100.0}]}
```

### Endpoints

| Método | Rota | Descrição |
|-------|------|-----------|
| POST | `/transactions` | Registra um gasto |
| GET | `/transactions?category=&start=&end=` | Lista com filtros opcionais |
| GET | `/transactions/summary?start=&end=` | Total e percentual por categoria |
| GET | `/transactions/{id}` | Busca por id |
| PUT | `/transactions/{id}` | Atualiza |
| DELETE | `/transactions/{id}` | Remove |
| POST | `/assistant/chat` | Comando em texto |
| POST | `/assistant/voice/text` | Áudio → transcrição + resposta JSON |
| POST | `/assistant/voice` | Áudio → resposta MP3 |

Categorias: `GROCERIES`, `PHARMA`, `AUTO`, `RESTAURANT`, `TRANSPORT`, `HOUSING`, `LEISURE`, `EDUCATION`, `OTHER`.

---

## ✅ Testes automatizados

```bash
./mvnw test      # testes unitários, WebMvc e JPA (sem custo)
./mvnw verify    # também roda os *IT com a OpenAI (precisa da OPENAI_API_KEY)
```

| Classe | Tipo | O que garante |
|-------|------|---------------|
| `TransactionServiceTest` | Unitário (Mockito) | validações (valor negativo, data futura, campos vazios), 404, período invertido, cálculo de total e percentual |
| `TransactionControllerTest` | `@WebMvcTest` | 201 + Location, 400 com erros por campo, 404, 422, categoria inválida |
| `TransactionRepositoryTest` | `@DataJpaTest` (H2) | consulta agregada por categoria, filtros e ordenação |
| `TransactionToolsTest` | Unitário | as 4 ferramentas estão expostas ao modelo, conversão de datas, erro legível para a IA |
| `BudgetingApplicationTests` | `@SpringBootTest` | contexto sobe |
| `AssistantFlowIT` | Ponta a ponta (OpenAI real) | texto cria transação na categoria certa; sem valor **não** salva; áudio → banco → MP3 |

Resultado local: **22 testes passando**; os 3 do `AssistantFlowIT` rodam no `./mvnw verify` e só executam quando `OPENAI_API_KEY` está definida (evita custo e falha no CI).

---

## 📚 O que aprendi

- **Spring AI não substitui arquitetura.** A IA é só mais uma "porta de entrada", igual ao controller. Colocando as regras no service, o mesmo código atende REST e Tool Calling.
- **Tool Calling pula o `@Valid`.** Quando o modelo chama um `@Tool`, nenhum controller é executado, então a validação precisa estar também no service. Reaproveitar o `Validator` com as anotações do DTO evitou duplicar regra.
- **Erros viram contexto para o modelo.** Se a ferramenta lança exceção com mensagem clara ("o valor deve ser maior que zero"), o Spring AI devolve essa mensagem ao LLM e ele explica o problema à pessoa em linguagem natural.
- **Prompt é configuração.** Passar a data de hoje, proibir valores inventados e pedir frases curtas (porque vão virar áudio) mudou muito a qualidade das respostas.
- **Dinheiro é `BigDecimal`.** O bug de centavos do projeto base mostrou por que não usar `double`/`long` sem cuidado.
- **Testar IA tem camadas:** a maior parte (service, controller, repository, tools) é testável sem chamar a OpenAI; só o fluxo de ponta a ponta precisa da chave.

---

## 🔗 Referências

- [Trilha Spring Boot DIO](https://github.com/digitalinnovationone/dio-spring-boot-learning-track)
- [Spring AI Reference](https://docs.spring.io/spring-ai/reference/index.html) · [ChatClient](https://docs.spring.io/spring-ai/reference/api/chatclient.html) · [Tools](https://docs.spring.io/spring-ai/reference/api/tools.html) · [Transcription](https://docs.spring.io/spring-ai/reference/api/audio/transcriptions.html) · [Speech](https://docs.spring.io/spring-ai/reference/api/audio/speech.html)
- [RFC 9457 – Problem Details](https://www.rfc-editor.org/rfc/rfc9457)
