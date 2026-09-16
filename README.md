<div align="center">

# 💰 Controle Financeiro

### Conheça a **Lumi**, sua assistente de gastos por voz

**Fale quanto gastou. A Lumi registra no banco, vigia o seu orçamento e responde falando com você.**

[![CI](https://github.com/BrunoBergamin/desafio-dio-spring-ai-budgeting/actions/workflows/ci.yml/badge.svg)](https://github.com/BrunoBergamin/desafio-dio-spring-ai-budgeting/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-25-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/25/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-2.0-6DB33F?style=for-the-badge&logo=spring&logoColor=white)](https://docs.spring.io/spring-ai/reference/)
[![Spring Security](https://img.shields.io/badge/Spring%20Security-7%20%2B%20JWT-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)](https://spring.io/projects/spring-security)
[![React](https://img.shields.io/badge/React-19%20%2B%20Vite-20232A?style=for-the-badge&logo=react&logoColor=61DAFB)](https://react.dev/)

[![Maven](https://img.shields.io/badge/Maven-3.9-C71A36?style=flat-square&logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![Flyway](https://img.shields.io/badge/Flyway-migrations-CC0200?style=flat-square&logo=flyway&logoColor=white)](https://flywaydb.org/)
[![Docker](https://img.shields.io/badge/Docker-um%20comando-2496ED?style=flat-square&logo=docker&logoColor=white)](#-rodando-com-um-comando-docker)
[![Groq](https://img.shields.io/badge/Groq-plano%20gratuito-F55036?style=flat-square&logo=groq&logoColor=white)](https://console.groq.com/)
[![OpenAI](https://img.shields.io/badge/OpenAI-opcional-412991?style=flat-square&logo=openai&logoColor=white)](https://platform.openai.com/docs/models)
[![MySQL](https://img.shields.io/badge/MySQL-9-4479A1?style=flat-square&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![H2](https://img.shields.io/badge/H2-em%20mem%C3%B3ria-0000BB?style=flat-square&logo=h2database&logoColor=white)](https://www.h2database.com/)
[![Swagger](https://img.shields.io/badge/Swagger-UI-85EA2D?style=flat-square&logo=swagger&logoColor=black)](https://springdoc.org/)
[![Testes](https://img.shields.io/badge/testes-106%20unit%C3%A1rios%20%2B%209%20com%20IA%20real-success?style=flat-square&logo=junit5&logoColor=white)](#-testes-automatizados)
[![DIO](https://img.shields.io/badge/DIO-Desafio%20de%20Projeto-30A3DC?style=flat-square)](https://www.dio.me/)
[![Licença MIT](https://img.shields.io/badge/licen%C3%A7a-MIT-yellow?style=flat-square)](LICENSE)

[Rodar com Docker](#-rodando-com-um-comando-docker) · [WhatsApp](#-falando-com-a-lumi-pelo-whatsapp) · [Prints](#-a-aplicação-rodando) · [Fluxo](#-fluxo-principal) · [Arquitetura](#️-arquitetura) · [Decisões](#-decisões-de-arquitetura) · [Melhorias](#-o-que-evoluí-sobre-o-projeto-base) · [Rodar sem Docker](#️-rodando-sem-docker) · [Endpoints](#endpoints) · [Testes](#-testes-automatizados) · [O que aprendi](#-o-que-aprendi)

</div>

---

**Controle Financeiro** é um aplicativo de gastos pessoais em que você **fala** o que gastou ("gastei 80 reais no mercado") e a **Lumi**, a assistente de IA, entende, registra no banco, confere o seu orçamento do mês e responde em linguagem natural (em áudio, no perfil OpenAI). Também dá para perguntar ("quanto gastei este mês?", "e o de ontem?") e definir limites ("meu limite de mercado é 800").

Nasceu como entrega do **Desafio de Projeto DIO + Itaú**, evoluindo o projeto final do módulo [05-spring-ai](https://github.com/digitalinnovationone/dio-spring-boot-learning-track/tree/main/05-spring-ai) do expert Poiani, e cresceu até virar um projeto completo: **API com JWT e multiusuário, memória de conversa, orçamento com alertas, frontend React que grava a voz pelo navegador, atendimento pelo WhatsApp, migrations, CI e Docker**.

```
🎙️  você: "Gastei 85 reais no mercado hoje"
     ↓  transcrição (Whisper)
🤖  Lumi escolhe a ferramenta registrar_transacao  →  💾 banco  →  🎯 confere o orçamento
     ↓
🔊  Lumi: "Gasto registrado: oitenta e cinco reais no mercado hoje. Atenção, você já usou
           oitenta e cinco por cento do limite de mercado neste mês, restam quinze reais."
```

> **Tudo aqui foi rodado de verdade e sem pagar nada**, na camada gratuita da [Groq](https://console.groq.com). Os prints e as respostas deste README são reais, não exemplos inventados.

---

## ✅ O que a entrega cobre

| O que o desafio pede | Resposta curta | Onde ver |
|---|---|---|
| **O que o projeto faz** | Recebe comando de voz ou texto; a Lumi entende a intenção, executa uma função real (Tool Calling), grava ou consulta no banco e responde. | [Fluxo](#-fluxo-principal) |
| **Como executar** | `docker compose up --build` (um comando) ou `./mvnw spring-boot:run`. Sem instalar nada além do Docker. | [Rodar](#-rodando-com-um-comando-docker) |
| **Qual melhoria implementei** | 22 evoluções sobre o projeto base, de validação no caminho da IA até login com JWT e frontend. | [Melhorias](#-o-que-evoluí-sobre-o-projeto-base) |
| **Tecnologias** | Java 25, Spring Boot 4.1, Spring AI 2.0, Spring Security 7, JPA + Flyway, React 19, Docker, GitHub Actions. | [Tecnologias](#️-tecnologias) |
| **Como testar o fluxo principal** | Pelo navegador (botão do microfone), pelo Swagger ou por `curl`, com áudios de exemplo no repositório. | [Como testar](#-como-testar-o-fluxo-principal) |
| **O que aprendi** | Treze lições, incluindo dois bugs de biblioteca que precisei contornar. | [O que aprendi](#-o-que-aprendi) |

---

## 🐳 Rodando com um comando (Docker)

Precisa só do [Docker](https://www.docker.com/products/docker-desktop/). Não precisa de Java, Maven nem Node: tudo é compilado dentro da imagem.

```bash
git clone https://github.com/BrunoBergamin/desafio-dio-spring-ai-budgeting.git
cd desafio-dio-spring-ai-budgeting
cp .env.example .env        # preencha GROQ_API_KEY (grátis) e APP_JWT_SECRET
docker compose up --build
```

Abra **http://localhost:8080**, crie uma conta e fale com a Lumi. O Swagger fica em `/swagger-ui.html` e o health check em `/actuator/health`.

- A chave da Groq é gratuita: crie em https://console.groq.com/keys (sem cartão).
- Quer MySQL em vez do H2 em memória? `SPRING_PROFILES_ACTIVE=groq,mysql` no `.env` e `docker compose --profile mysql up --build`.
- A imagem final roda como usuário sem privilégio (`lumi`), tem `HEALTHCHECK` e pesa ~800 MB (JRE 25 + Ubuntu; um `alpine` reduziria, mas priorizei previsibilidade).

---

## 💬 Falando com a Lumi pelo WhatsApp

A mesma Lumi atende pelo WhatsApp: você manda um áudio ou um texto e ela registra o gasto, avisa do orçamento e responde por lá. A integração usa a [Evolution API](https://github.com/EvolutionAPI/evolution-api) (perfil `whatsapp`), que roda **junto no `docker compose`**: a Evolution chama a aplicação pela rede interna, então **não precisa de ngrok nem de servidor público**.

```bash
# .env: SPRING_PROFILES_ACTIVE=groq,whatsapp  +  EVOLUTION_API_KEY  +  WHATSAPP_WEBHOOK_SECRET
docker compose --profile whatsapp up --build
```

1. Abra a página **WhatsApp** do site, clique em **Gerar QR code** e escaneie com o celular (WhatsApp → Aparelhos conectados). Use um chip que não seja o seu pessoal.
2. Ainda na página, **vincule o seu número** à conta. Número desconhecido recebe só um convite para se cadastrar: a Lumi nunca registra gasto de quem ela não conhece.
3. Mande "gastei 30 reais na farmácia" (texto ou áudio) para o número conectado.

Como funciona por dentro: `POST /api/whatsapp/webhook/{segredo}` recebe o evento `messages.upsert`, responde `202` na hora e processa em segundo plano (`@Async`); o áudio chega em base64 (`ogg/opus`, aceito direto pelo Whisper); o número vira o usuário pela tabela `users.phone`; a resposta volta por `POST /message/sendText` (e em áudio, no perfil OpenAI). A conversa do WhatsApp tem memória própria, separada da do site. O provedor fica atrás da interface `WhatsAppGateway`: trocar a Evolution pela **API oficial da Meta** é escrever outra implementação, e mais nada.

> **Escolha consciente:** a Evolution não é a API oficial (usa o WhatsApp Web por baixo) e a Meta pode bloquear o número. Para demonstração e portfólio ela é imbatível: grátis, local, sem cadastro de empresa. Para produção, a implementação oficial entra no lugar sem mexer no resto.

---

## 📸 A aplicação rodando

**Frontend React**: cadastro, conversa com a Lumi (voz ou texto), gráfico do mês e alertas de orçamento em tempo real.

![Tela de conversa com a Lumi: gasto registrado, alerta de orçamento na resposta, gráfico por categoria e barra do orçamento](docs/images/ui-conversa.png)

<details>
<summary><b>Gravando pela voz no navegador (clique para ver)</b></summary>

O botão do microfone usa `MediaRecorder` e grava em `webm/opus`, formato aceito pelo Whisper. Abaixo, a transcrição da minha voz e a resposta com dados reais do banco:

![Áudio gravado no navegador, transcrito e respondido pela Lumi](docs/images/ui-voz.png)

</details>

<details>
<summary><b>Login, gastos e orçamentos (clique para ver)</b></summary>

![Tela de login](docs/images/ui-login.png)
![Lista de gastos com filtro por categoria](docs/images/ui-gastos.png)
![Orçamentos do mês com barra de progresso e status](docs/images/ui-orcamentos.png)

</details>

<details>
<summary><b>Swagger e os primeiros testes com a minha voz (clique para ver)</b></summary>

![Swagger UI com todos os endpoints](docs/images/swagger-overview.png)

Os dois áudios estão em [`docs/audio`](docs/audio): me apresentando (ela entendeu que não havia gasto e **não inventou nada**) e perguntando o total do mês (ela buscou no banco em vez de chutar).

![Áudio de apresentação](docs/images/voz-apresentacao.png)
![Pergunta sobre o total do mês](docs/images/voz-consulta.png)

</details>

---

## 🔄 Fluxo principal

```mermaid
sequenceDiagram
    participant U as Navegador (React)
    participant AC as AssistantController
    participant AS as AssistantService
    participant W as Whisper (speech-to-text)
    participant L as Lumi (ChatClient + memória)
    participant T as TransactionTools / BudgetTools
    participant S as Services
    participant DB as Banco (JPA + Flyway)

    U->>AC: POST /api/assistant/voice/text (áudio + JWT)
    AC->>AS: voiceToText(userId, conversa, áudio)
    AS->>W: transcribe(áudio)
    W-->>AS: "gastei 85 reais no mercado"
    AS->>L: answer(userId, chave da conversa, texto)
    Note over L: system prompt + data de hoje<br/>toolContext = {userId}<br/>histórico da conversa
    L->>T: tool call registrar_transacao(85.00, GROCERIES) + ToolContext
    T->>S: ExpenseService.register(userId, ...)
    S->>DB: salva a transação
    S->>DB: soma os gastos da categoria no mês
    S-->>L: transação + status do orçamento (WARNING, 85%)
    L-->>AS: "Registrei... atenção, você já usou 85% do limite"
    AS-->>U: transcrição + resposta (+ MP3 no perfil OpenAI)
```

1. O navegador grava o áudio e envia com o token JWT.
2. O áudio vira texto (`TranscriptionModel`).
3. A Lumi (`ChatClient` com `MessageChatMemoryAdvisor`) entende a intenção e escolhe uma **ferramenta** (Tool Calling).
4. A ferramenta recebe o usuário pelo **`ToolContext`** (o modelo nunca vê nem escolhe o usuário) e delega ao service.
5. O service valida, grava e confere o orçamento da categoria; o alerta volta no resultado da própria ferramenta.
6. A Lumi responde em uma frase curta; no perfil OpenAI, a frase vira MP3 (`TextToSpeechModel`).

---

## 🏗️ Arquitetura

```
src/main/java/dio/budgeting
├── controller/   AuthController, TransactionController, BudgetController, AssistantController  (prefixo /api)
├── service/      AuthService, TransactionService, BudgetService, ExpenseService, AssistantService, LumiChat
├── tool/         TransactionTools, BudgetTools, ToolUser   (@Tool: adaptadores entre a IA e os services)
├── security/     JwtService, CurrentUserProvider, AppUserDetailsService, ProblemDetailResponses
├── repository/   UserRepository, TransactionRepository, BudgetRepository  (toda consulta filtra por usuário)
├── entity/       User, Transaction, Budget, Category, BudgetStatus
├── dto/          request/ e response/ (a entity nunca sai da API)
├── mapper/       Entity ⇄ DTO
├── exception/    exceções de negócio + GlobalExceptionHandler (ProblemDetail / RFC 9457)
├── config/       SecurityConfig, JwtConfig, ChatClientConfig, OpenApiConfig, WebMvcConfig
└── web/          SpaForwardController (entrega o React nas rotas da SPA)

src/main/resources/db/migration   V1 transações · V2 usuários · V3 orçamentos  (Flyway)
frontend/                          React 19 + Vite + TypeScript (gravação de voz, gráfico, orçamentos)
.github/workflows/ci.yml           backend (mvnw verify) · frontend (tsc + build) · imagem Docker
```

**A regra que sustenta tudo:** o controller não conhece o repository, a IA nunca toca no banco, e **REST e Lumi passam pelo mesmo service com as mesmas regras**. O `userId` é o primeiro parâmetro de todo método de service, sem sobrecarga sem ele: esquecer o usuário não compila.

---

## 🧠 Decisões de arquitetura

As três que mais valem uma conversa de entrevista:

**1. O usuário chega à ferramenta pelo `ToolContext`, não pelo `SecurityContext`.**
O Spring AI injeta um parâmetro `ToolContext` no método `@Tool` e **o exclui do JSON Schema** enviado ao modelo. Logo a IA não vê, não descreve e não consegue preencher o `userId`. Há um teste que manda um `userId` falso nos argumentos da ferramenta e prova que ele é ignorado (`TransactionToolsTest.should_ignoreUserIdSentByTheModel`). O `SecurityContextHolder` até funciona hoje, porque o `ToolCallingAdvisor.adviseCall` é síncrono, mas trocar `.call()` por `.stream()` moveria a execução para outra thread e o `ThreadLocal` sumiria em silêncio. Defesa em quatro camadas: schema, assinatura do service, filtro no repositório e `ToolUser.require`, que explode se o contexto vier vazio.

**2. A memória de conversa fica fora do loop de ferramentas.**
O `MessageChatMemoryAdvisor` (ordem `MIN+200`) roda antes do `ToolCallingAdvisor` (`MIN+300`), então o histórico guarda só o par pergunta/resposta, e não as idas e vindas das ferramentas. Isso segura o custo em tokens. A chave da memória é sempre `userId:conversa`, derivada do JWT: mandar o mesmo `conversationId` de outra pessoa não lê nada dela. Memória em RAM com janela de 10 mensagens, escolha consciente: some no restart e não funciona com várias instâncias, e está documentado.

**3. O alerta de orçamento é determinístico, não depende do modelo lembrar.**
Em vez de esperar que a IA chame uma segunda ferramenta, a própria `registrar_transacao` devolve a transação **e** o status do orçamento. O modelo recebe o alerta no resultado e comenta naturalmente, sem round-trip extra. Os limiares (80% / 100%) são calculados com os valores exatos, não com o percentual arredondado: `399,99 de 500` ainda é OK. Um teste de fronteira pegou esse bug antes de virar produto.

Outras decisões: `VARCHAR(36)` e `TIMESTAMP(6)` nas migrations para o **mesmo SQL** servir H2 e MySQL (`DATETIME` não existe no H2 2.x; UUID nativo vira `BINARY(16)` no MySQL); JWT com o suporte nativo do Spring Security (`NimbusJwtEncoder`, HS256) em vez de biblioteca extra; 401/403 escritos como `ProblemDetail` por um `AuthenticationEntryPoint` próprio, porque exceções de segurança acontecem antes do `@RestControllerAdvice`; frontend empacotado dentro do jar para **uma porta, sem CORS, um container**; token no `localStorage` com o trade-off (XSS) documentado, cookie `HttpOnly` seria o próximo passo.

---

## 🚀 O que evoluí sobre o projeto base

| # | Melhoria | Onde |
|---|----------|------|
| 1 | **Reorganização em camadas clássicas** (controller, service, repository, entity, dto, mapper, exception, config, tool, security, web). | todo o projeto |
| 2 | **Validações antes de salvar** (valor, descrição, categoria, data futura), reaplicadas no service porque a ferramenta da IA não passa pelo `@Valid` do controller. | `TransactionService`, `BudgetService` |
| 3 | **Bug de centavos corrigido**: o base mostrava 80 reais como `8000.0`. Valores em `BigDecimal`. | `Transaction` |
| 4 | **Login e multiusuário com JWT** (Spring Security 7): cada pessoa só vê os próprios gastos; transação alheia responde 404, não 403. | `SecurityConfig`, `AuthService`, `JwtService` |
| 5 | **Escopo do usuário no Tool Calling via `ToolContext`**, com teste adversarial. | `ToolUser`, `TransactionTools` |
| 6 | **Memória de conversa** (`MessageWindowChatMemory`): "e o de ontem?" funciona; chave por usuário; `DELETE /api/assistant/conversation`. | `LumiChat`, `ConversationKey` |
| 7 | **Orçamento mensal por categoria com alertas** (OK / atenção / estourado) e 4 ferramentas novas para a Lumi. | `BudgetService`, `BudgetTools` |
| 8 | **Alerta entregue junto do registro do gasto**, sem round-trip extra ao modelo. | `ExpenseService` |
| 9 | **Migrations com Flyway** (3 versões, SQL portátil H2/MySQL) e `ddl-auto=validate`; os testes de repositório rodam sobre as migrations. | `db/migration`, `@JpaTest` |
| 10 | **Frontend React 19 + Vite + TypeScript**: gravação pelo navegador (`MediaRecorder`), chat, gráfico por categoria, gastos e orçamentos; F5 em qualquer rota funciona. | `frontend/`, `SpaForwardController` |
| 11 | **Docker em 3 estágios** (Node → Maven → JRE), usuário não-root, `HEALTHCHECK`, `compose` com MySQL opcional. | `Dockerfile`, `compose.yml` |
| 12 | **CI no GitHub Actions**: backend, frontend e imagem Docker, verde sem nenhum segredo. | `.github/workflows/ci.yml` |
| 13 | **Actuator** (`health`, `info`, `metrics`) e prefixo `/api` em todos os endpoints. | `WebMvcConfig`, `application.properties` |
| 14 | **Perfil gratuito (`groq`)**: mesma aplicação, só configuração; text-to-speech opcional (503 explicado). | `application-groq.properties` |
| 15 | **Contorno de bug aberto do Spring AI** (`reasoning_content` reenviado à Groq, [issue #6968](https://github.com/spring-projects/spring-ai/issues/6968)). | `application-groq.properties` |
| 16 | **Conflito de versão do Swagger** (Spring AI × springdoc) resolvido no `pom.xml`, com o porquê. | `pom.xml` |
| 17 | **Áudio em formato inesperado vira 422 explicado** (o Gravador do Windows salva AAC cru como `.m4a`). Descoberto testando com a minha voz. | `AssistantService` |
| 18 | **Erros padronizados** com `ProblemDetail` em toda a API, inclusive 401/403 da camada de segurança. | `GlobalExceptionHandler`, `ProblemDetailResponses` |
| 19 | **System prompt** com data de hoje, proibição de inventar valores, memória e orçamento; persona "Lumi". | `prompts/system-message.st` |
| 21 | **WhatsApp via Evolution API** (perfil `whatsapp`): webhook protegido por segredo, vínculo número → conta, áudio e texto, resposta em segundo plano, provedor atrás de interface. | `whatsapp/`, `WhatsAppController` |
| 22 | **16 categorias** (mercado, restaurante, saúde, moradia, transporte, carro, assinaturas, roupas, beleza, lazer, educação, pets, viagem, presentes, impostos, outros) com um guia no schema da ferramenta para o modelo classificar melhor. Sem migration: a coluna já era texto. | `Category` |
| 20 | **106 testes** (unitários, `@WebMvcTest` com segurança real, `@DataJpaTest` com Flyway, ponta a ponta com IA). | `src/test` |

---

## 🛠️ Tecnologias

- **Java 25** · **Spring Boot 4.1** (Web, Validation, Data JPA, Actuator, Security, OAuth2 Resource Server)
- **Spring AI 2.0**: `ChatClient`, Tool Calling (`@Tool` + `ToolContext`), `MessageChatMemoryAdvisor`, `TranscriptionModel`, `TextToSpeechModel`
  - **Groq** (perfil `groq`, gratuito): `openai/gpt-oss-120b` + `whisper-large-v3-turbo`
  - **OpenAI** (perfil padrão): `gpt-4o-mini` + `whisper-1` + `gpt-4o-mini-tts`
- **Flyway 12** · **H2** (padrão) e **MySQL 9** · **Lombok** · **springdoc-openapi**
- **React 19**, **Vite 8**, **TypeScript**, `react-router`, `axios`, `recharts`, CSS puro
- **JUnit 5, Mockito, MockMvc, Spring Security Test, AssertJ**
- **Maven** (wrapper) · **Docker** · **GitHub Actions**

---

## ▶️ Rodando sem Docker

Pré-requisitos: JDK 25 e (só para o frontend em modo dev) Node 22.

```bash
cp .env.example .env            # GROQ_API_KEY + APP_JWT_SECRET
./mvnw spring-boot:run -Dspring-boot.run.profiles=groq     # API em :8080 (Windows: .\mvnw.cmd ...)
```

Frontend com hot reload (o Vite faz proxy de `/api` para a API, sem CORS):

```bash
cd frontend && npm install && npm run dev                  # http://localhost:5173
```

Para servir o React pela própria API (como no Docker): `npm run build`, copie `frontend/dist` para `src/main/resources/static` e empacote com `./mvnw package`.

| O quê | URL |
|------|-----|
| Aplicação | http://localhost:8080 (ou :5173 em dev) |
| Swagger UI | http://localhost:8080/swagger-ui.html — botão **Authorize** com o token do `/api/auth/login` |
| Health | http://localhost:8080/actuator/health |
| Console H2 | http://localhost:8080/h2-console (`jdbc:h2:mem:budgeting`, usuário `sa`) |

Perfil OpenAI (com áudio de resposta em MP3): defina `OPENAI_API_KEY` e rode sem `-Dspring-boot.run.profiles`. Perfil MySQL local: `-Dspring-boot.run.profiles=groq,mysql` (o Spring sobe o `compose.yml` do banco sozinho).

---

## 🧪 Como testar o fluxo principal

**Pelo navegador (mais fácil):** crie uma conta, clique no microfone, fale *"gastei 45 reais na farmácia"* e veja a transação aparecer no gráfico. Depois pergunte *"quanto gastei este mês?"*.

**Pelo Swagger:** `POST /api/auth/register` → copie o `token` → botão **Authorize** → `POST /api/assistant/voice/text` com um dos áudios de `src/test/resources/audio`.

**Por `curl`** (o arquivo [`requests.http`](requests.http) tem tudo pronto para o IntelliJ/VS Code):

```bash
# 1. conta + token
TOKEN=$(curl -s -X POST localhost:8080/api/auth/register -H "Content-Type: application/json" \
  -d '{"name":"Bruno","email":"bruno@email.com","password":"senha-forte-123"}' | jq -r .token)

# 2. limite do mês
curl -s -X POST localhost:8080/api/budgets -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"category":"GROCERIES","monthlyLimit":100}'

# 3. voz -> texto (resposta real abaixo)
curl -s -X POST localhost:8080/api/assistant/voice/text -H "Authorization: Bearer $TOKEN" \
  -F "file=@src/test/resources/audio/recording-1.m4a"

# 4. voz -> MP3 (perfil OpenAI)
curl -s -X POST localhost:8080/api/assistant/voice -H "Authorization: Bearer $TOKEN" \
  -F "file=@src/test/resources/audio/recording-1.m4a" --output resposta.mp3
```

Resposta real do passo 3 (o texto varia conforme o modelo):
```json
{
  "transcription": "Passei na farmácia rapidinho e deixei 80 reais em 3 itens.",
  "answer": "Gasto de oitenta reais registrado na farmácia para hoje.",
  "conversationId": "default"
}
```

> **Dica:** se gravar com o Gravador de Voz do Windows, o `.m4a` pode vir como AAC cru e a API responde `422` explicando. Converta com `ffmpeg -i gravacao.m4a gravacao.mp3`. Pelo navegador isso não acontece: o `MediaRecorder` grava em `webm`, aceito direto.

### Endpoints

| Método | Rota | Descrição |
|-------|------|-----------|
| POST | `/api/auth/register` · `/api/auth/login` | Conta e token JWT (público) |
| GET | `/api/auth/me` | Usuário autenticado |
| POST/GET | `/api/transactions` | Registra · lista (`?category=&start=&end=`) |
| GET | `/api/transactions/summary` | Total e percentual por categoria (padrão: mês atual) |
| GET/PUT/DELETE | `/api/transactions/{id}` | Busca · atualiza · remove (só do próprio usuário) |
| POST/GET | `/api/budgets` | Define · lista limites do mês com quanto já foi gasto |
| GET | `/api/budgets/alerts` | Só as categorias em atenção ou estouradas |
| PUT/DELETE | `/api/budgets/{id}` | Altera limite · remove |
| POST | `/api/assistant/chat` | Comando em texto (`conversationId` opcional) |
| POST | `/api/assistant/voice/text` | Áudio → transcrição + resposta JSON |
| POST | `/api/assistant/voice` | Áudio → resposta MP3 (503 no perfil groq) |
| DELETE | `/api/assistant/conversation` | Apaga o histórico da conversa |
| PUT | `/api/auth/me/phone` | Vincula o número do WhatsApp à conta |
| GET/POST | `/api/whatsapp/status` · `/api/whatsapp/connect` | Estado da conexão · QR code para parear (perfil `whatsapp`) |
| POST | `/api/whatsapp/webhook/{segredo}` | Chamado pela Evolution a cada mensagem (público, protegido pelo segredo) |

Categorias: `GROCERIES`, `RESTAURANT`, `PHARMA`, `HOUSING`, `TRANSPORT`, `AUTO`, `SUBSCRIPTIONS`, `CLOTHING`, `PERSONAL_CARE`, `LEISURE`, `EDUCATION`, `PETS`, `TRAVEL`, `GIFTS`, `TAXES`, `OTHER`. Ferramentas que a Lumi conhece: `registrar_transacao`, `listar_transacoes`, `ultimas_transacoes`, `resumo_de_gastos`, `definir_orcamento`, `consultar_orcamentos`, `status_do_orcamento`, `alertas_de_orcamento`.

---

## ✅ Testes automatizados

```bash
./mvnw test      # 106 testes sem custo (unitários, WebMvc com segurança real, JPA sobre as migrations)
./mvnw verify    # + 9 de ponta a ponta com a IA (só rodam se GROQ_API_KEY ou OPENAI_API_KEY existir)
```

| Classe | Tipo | O que garante |
|-------|------|---------------|
| `TransactionServiceTest`, `BudgetServiceTest`, `ExpenseServiceTest` | Unitário | validações, 404 para dado alheio, total/percentual, **fronteiras 80%/100% do orçamento**, alerta no registro |
| `AuthServiceTest`, `JwtServiceTest` | Unitário | cadastro, e-mail duplicado, credencial inválida → 401, token com `sub` = id, assinatura com outra chave falha |
| `AssistantServiceTest`, `LumiChatTest`, `ConversationKeyTest` | Unitário | conversa presa ao usuário, `userId` no `ToolContext`, formatos de áudio, TTS desligado |
| `TransactionToolsTest`, `BudgetToolsTest` | Unitário | ferramentas expostas, **`userId` fora do schema**, `userId` falso do modelo ignorado, fail-fast sem contexto |
| `AuthControllerTest`, `TransactionControllerTest`, `BudgetControllerTest`, `AssistantControllerTest` | `@WebMvcTest` + `SecurityConfig` real | 401 com `ProblemDetail`, 201/400/404/422/503, validação por campo |
| `WhatsAppServiceTest`, `WhatsAppControllerTest` | Unitário + `@WebMvcTest` | ignora mensagens próprias/grupos, extrai número (inclusive com LID), número não vinculado só recebe convite, áudio em base64 vai para o Whisper, segredo errado → 404 |
| `TransactionRepositoryTest`, `UserAndBudgetRepositoryTest` | `@DataJpaTest` + Flyway | isolamento por usuário nas queries, agregações, `UNIQUE` de e-mail e de orçamento |
| `AssistantFlowGroqIT` (6) · `AssistantFlowIT` (3) | Ponta a ponta com IA real | grava na categoria certa, transcreve áudio, **usuário B não vê o total de A**, lembra a mensagem anterior, avisa do orçamento, MP3 |

Resultado local: **106 passando** sem chave; **115 passando** com a chave da Groq (`BUILD SUCCESS` no `./mvnw verify`). No CI os testes de IA são pulados por condição, não por erro.

---

## 📚 O que aprendi

- **A IA não é o centro do projeto.** Ela é só mais uma porta de entrada, como um endpoint REST. Quem manda é o service. Foi isso que deixou o mesmo código servir para o REST, para a voz e para o frontend.

- **A IA pula o `@Valid`.** Quando o modelo chama uma ferramenta, o controller não roda. Sem validar também no service, a IA conseguiria salvar um gasto negativo.

- **Nunca deixe o modelo escolher o usuário.** Se `userId` fosse parâmetro da ferramenta, bastava a IA "inventar" um id para ler dados de outra pessoa. O `ToolContext` resolve isso, e eu escrevi um teste que tenta exatamente esse ataque.

- **Segurança acontece antes do controller.** Um 401 é decidido na cadeia de filtros, então meu `@RestControllerAdvice` nunca via a exceção e o erro voltava vazio. Precisei de um `AuthenticationEntryPoint` próprio.

- **`@WebMvcTest` não carrega a sua `SecurityConfig`.** Ele carrega a cadeia padrão do Boot e todos os testes viram 401. Uma anotação composta que importa a configuração real resolveu de uma vez.

- **Memória de conversa custa tokens.** Deixá-la fora do loop de ferramentas (ordem dos advisors) evita reenviar as chamadas de tool a cada turno.

- **Erro bom é erro explicado.** Se a ferramenta lança uma mensagem clara, o Spring AI passa para o modelo, e ele explica para a pessoa em português.

- **O prompt muda tudo.** Data de hoje, "nunca invente valores" e "avise se o orçamento vier WARNING" fizeram a Lumi parar de chutar e começar a avisar.

- **Dinheiro é `BigDecimal`**, e **percentual arredondado não serve para decidir status**: `399,99 de 500` virava 80,0% e caía em "atenção". Um teste de fronteira achou isso.

- **SQL portátil tem limite.** `VARCHAR(36)` e `TIMESTAMP(6)` rodam em H2 e MySQL; `DATETIME` e "tornar coluna `NOT NULL`" não. Descobri testando as migrations no próprio build.

- **Biblioteca nova tem bug, e faz parte.** O Tool Calling na Groq quebrava por um bug aberto do Spring AI ([#6968](https://github.com/spring-projects/spring-ai/issues/6968)); o Swagger quebrava por um conflito de versões que o Maven resolve diferente do Gradle. Ler o log até o fim resolveu os dois.

- **WhatsApp é só mais uma porta.** Como a Lumi vive no service, atender pelo WhatsApp foi um webhook, um cliente HTTP e uma tabela de números: o núcleo não mudou. E o formato do WhatsApp (`ogg/opus`) é aceito pelo Whisper sem conversão, ao contrário do Gravador do Windows.

- **Testar com a minha própria voz achou bug.** O Gravador do Windows salva um `.m4a` que não é m4a; a API dava 500. Virou uma mensagem explicando o que fazer, e o frontend gravando em `webm` eliminou o problema de vez.

---

## 🔗 Referências

- [Trilha Spring Boot DIO](https://github.com/digitalinnovationone/dio-spring-boot-learning-track)
- [Spring AI Reference](https://docs.spring.io/spring-ai/reference/index.html) · [ChatClient](https://docs.spring.io/spring-ai/reference/api/chatclient.html) · [Tools](https://docs.spring.io/spring-ai/reference/api/tools.html) · [Chat Memory](https://docs.spring.io/spring-ai/reference/api/chat-memory.html) · [Transcription](https://docs.spring.io/spring-ai/reference/api/audio/transcriptions.html) · [Speech](https://docs.spring.io/spring-ai/reference/api/audio/speech.html)
- [Spring Security — OAuth2 Resource Server (JWT)](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)
- [Flyway](https://documentation.red-gate.com/flyway) · [RFC 9457 – Problem Details](https://www.rfc-editor.org/rfc/rfc9457)
- [MediaRecorder API](https://developer.mozilla.org/en-US/docs/Web/API/MediaRecorder)
