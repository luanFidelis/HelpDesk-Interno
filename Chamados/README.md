# Chamados HQ

API e interface para abertura e acompanhamento de chamados de suporte interno — o
fluxo de um help desk: o funcionário abre o chamado e recebe um protocolo, acompanha
o andamento por esse número, e o atendimento trata a fila, muda o status e reabre o
que voltou a dar problema.

Feito em Spring Boot com banco H2 em memória, então **sobe com um comando e sem
instalar banco nenhum**.

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-brightgreen)
![H2](https://img.shields.io/badge/H2-em%20mem%C3%B3ria-blue)
![Testes](https://img.shields.io/badge/testes-14-success)

---

## Como rodar

Precisa de **JDK 21 ou superior**. Nada mais — o Maven vem embutido no wrapper.

```bash
./mvnw spring-boot:run
```

No Windows, `mvnw.cmd spring-boot:run`.

Depois abra <http://localhost:8081>. A aplicação já sobe com alguns chamados de
demonstração, em estados diferentes, para a tela não abrir vazia.

Se a porta 8081 estiver ocupada:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8090
```

O console do banco fica em <http://localhost:8081/h2-console> (JDBC URL
`jdbc:h2:mem:chamados`, usuário `sa`, senha vazia).

> Como o banco é em memória, tudo que você criar desaparece quando a aplicação para.
> É de propósito: quem clonar o repositório sempre começa do mesmo estado.

## Rodar os testes

```bash
./mvnw test
```

---

## As três telas

A interface é servida pela própria aplicação, em `src/main/resources/static`.

| Tela | O que faz |
| --- | --- |
| **Abrir chamado** | Formulário de abertura. Ao enviar, mostra o recibo com o protocolo gerado. |
| **Meus chamados** | Consulta pelo protocolo e mostra a linha do tempo do atendimento. |
| **Painel do atendimento** | Fila completa, com contadores por status, filtros por status e tipo, troca de status e reabertura. |

---

## A decisão principal: histórico em vez de update

Cada linha da tabela é uma **versão** de um chamado, não o chamado inteiro. Abrir,
mudar status, reabrir ou complementar a descrição sempre **gravam uma linha nova**
com o mesmo `numeroChamado` — nada é sobrescrito.

```
NC-2026-0004 · ABERTO        10/08 13:30
NC-2026-0004 · RESOLVIDO     11/08 16:45   ← fechamento estampado
NC-2026-0004 · REABERTO      16/08 11:08   ← fechamento volta a ficar vazio
```

O estado atual é a versão mais recente, e o conjunto de linhas é a linha do tempo.

**Por que assim:** em chamado, o histórico é a informação. Saber que um problema foi
resolvido e voltou três dias depois vale mais do que saber apenas que ele está
reaberto agora. Com `UPDATE` essa trilha se perde; com append-only ela é o próprio
armazenamento, e nenhum update malfeito apaga o passado.

**O preço:** a listagem devolve todas as versões, e quem consome precisa dobrar por
`numeroChamado` para ter um chamado por linha — é o que a tela faz antes de montar a
fila. Em troca, a mesma resposta serve para a lista e para a linha do tempo.

Por causa disso, `dataAbertura` significa "quando esta versão foi registrada": na
primeira linha é a abertura de fato, nas seguintes é o instante da mudança.

---

## API

Base: `/api/v1/chamados`. O chamado é identificado pelo **protocolo**
(`NC-ANO-SEQUENCIA`), que é o número que o solicitante tem em mãos.

| Método | Rota | O que faz |
| --- | --- | --- |
| `GET` | `/api/v1/chamados` | Todas as versões gravadas, em ordem cronológica |
| `POST` | `/api/v1/chamados` | Abre um chamado → `201` com o protocolo no corpo e no `Location` |
| `GET` | `/api/v1/chamados/{protocolo}` | Linha do tempo de um chamado |
| `PATCH` | `/api/v1/chamados/{protocolo}/status` | Muda o status |
| `PATCH` | `/api/v1/chamados/{protocolo}/descricao` | Complementa a descrição |
| `POST` | `/api/v1/chamados/{protocolo}/reaberturas` | Reabre um chamado encerrado |

### Abrir um chamado

```http
POST /api/v1/chamados
Content-Type: application/json

{
  "nome": "Ana Souza",
  "email": "ana.souza@exemplo.com.br",
  "tipoChamado": "EMAIL",
  "prioridade": "MEDIA",
  "descricao": "Nao consigo enviar e-mails com anexo maior que 5 MB."
}
```

```http
HTTP/1.1 201 Created
Location: /api/v1/chamados/NC-2026-0006

{
  "id": "38e6be8e-3b1b-47bf-a818-26464ed4210d",
  "numeroChamado": "NC-2026-0006",
  "nome": "Ana Souza",
  "email": "ana.souza@exemplo.com.br",
  "descricao": "Nao consigo enviar e-mails com anexo maior que 5 MB.",
  "tipoChamado": "EMAIL",
  "prioridade": "MEDIA",
  "status": "ABERTO",
  "dataAbertura": "2026-08-17T13:59:12.482",
  "dataFechamento": null
}
```

O protocolo vem **na resposta**, não em uma consulta seguinte: é o único jeito de o
cliente saber com certeza qual chamado acabou de criar.

### Mudar o status

```http
PATCH /api/v1/chamados/NC-2026-0006/status
Content-Type: application/json

{ "status": "EM_ANDAMENTO" }
```

Um objeto, e não o enum solto no corpo, para a requisição ser um JSON legítimo e
caber um campo novo depois — uma justificativa do atendente, por exemplo — sem
quebrar quem já consome a API.

### Valores aceitos

| Campo | Valores |
| --- | --- |
| `tipoChamado` | `PERMISSOES` · `EMAIL` · `HARDWARE` · `IMPRESSORA` · `SISTEMA` · `OUTROS` |
| `prioridade` | `BAIXA` · `MEDIA` · `ALTA` · `CRITICA` |
| `status` | `ABERTO` · `EM_ANDAMENTO` · `RESOLVIDO` · `FECHADO` · `REABERTO` |

`RESOLVIDO` e `FECHADO` encerram o atendimento e estampam a `dataFechamento`;
qualquer outro status a deixa vazia. Isso fica no próprio enum, em `Status.encerra()`,
para a regra não se espalhar pelos services.

---

## Erros

Toda falha responde no mesmo formato, então o cliente nunca precisa adivinhar se veio
texto puro, JSON ou stack trace.

```json
{
  "status": 400,
  "mensagem": "Há campos inválidos na solicitação.",
  "campos": {
    "email": "Informe um e-mail válido.",
    "descricao": "A descrição deve ter de 10 a 2000 caracteres."
  },
  "momento": "2026-08-17T13:55:22.962"
}
```

| Situação | Resposta |
| --- | --- |
| Campo inválido ou ausente | `400` com o erro de cada campo em `campos` |
| Valor fora do enum | `400` |
| Protocolo inexistente | `404` com o protocolo na mensagem |

A tela mostra essas mensagens direto ao usuário, em vez de um código de status solto.

---

## Testes

14 testes, com `MockMvc` sobre a aplicação real e H2. Cobrem o caminho feliz e,
principalmente, os erros que este projeto já cometeu:

- abertura devolve `201`, protocolo no padrão `NC-ANO-SEQUENCIA` e `Location`
- corpo vazio responde `400` com todos os campos apontados — **e não `500`**
- e-mail malformado e valor fora do enum respondem `400`
- protocolo inexistente responde `404` em consulta, status, descrição e reabertura
- mudar status grava uma versão nova **preservando tipo, prioridade e solicitante**
- resolver estampa a data de fechamento; reabrir a limpa
- a listagem volta em ordem cronológica

Dois deles são testes de regressão de bugs reais:

**O tipo do chamado desaparecia.** A entidade tinha dois campos de tipo, e as versões
de histórico eram montadas a partir do que nunca era preenchido — na primeira mudança
de status o chamado perdia a categoria. A cópia de versão agora é um único método na
entidade (`Chamado.proximaVersao`), então não existe mais lugar onde esquecer um
campo, e o teste falha se algum deixar de ser copiado.

**Campo vazio virava erro 500.** O DTO usava `@NotBlank` em cima de enum. Só existe
validador de `@NotBlank` para texto, então em enum ele estoura `UnexpectedTypeException`
em vez de recusar o campo — e o cliente recebia `500` no lugar de `400`. Em enum o
certo é `@NotNull`.

---

## Stack

- **Java 21**, **Spring Boot 4.0.6**
- **Spring Web** · **Spring Data JPA** · **Bean Validation**
- **H2** em memória, populado por `data.sql`
- Front em HTML, CSS e JavaScript sem framework, servido pela própria aplicação

```
src/main/java/com/chamados/
├── controller/   ChamadoController          rotas e códigos HTTP
├── service/      ChamadoService             abertura, consulta, descrição
│                 ChamadoAdmService          fila, status, reabertura
├── repository/   ChamadoRepository          consultas do Spring Data
├── model/        Chamado                    entidade + cópia de versão
│                 Status, Prioridade, TipoChamado
├── dto/          AbrirChamadoRequest        entrada validada
│                 MudarStatusRequest, AlterarDescricaoRequest
│                 ChamadoResponse            saída da API
└── exception/    RestExceptionHandler       traduz exceção em resposta
                  ErroResposta, ChamadoNotFoundException

src/main/resources/
├── static/       index.html, app.js, styles.css
├── data.sql      chamados de demonstração
└── application.properties
```

A entidade não é serializada direto: a API responde `ChamadoResponse`, então o
contrato do JSON não muda sozinho quando o modelo muda.

---

## Limitações conhecidas

Coisas que ficaram de fora de propósito, para o projeto continuar simples:

- **Sem autenticação.** Qualquer um lê a fila e muda status. Num sistema real, abrir
  chamado e atender chamado são papéis diferentes, com login e autorização por perfil.
- **Numeração não é segura sob concorrência.** A sequência vem de uma contagem, e duas
  aberturas ao mesmo tempo poderiam disputar o mesmo número — o código cobre a colisão
  com uma nova tentativa, mas o certo seria tirar o número de uma *sequence* do banco,
  que resolve a disputa no próprio SGBD.
- **Listagem sem paginação.** Devolver todas as versões só se sustenta em volume baixo.
  Com histórico crescendo, o caminho é paginar e ter um endpoint que já entregue o
  estado atual de cada chamado, em vez de a tela dobrar as versões.
- **Sem transição de status validada.** Hoje qualquer status pode virar qualquer outro.
  Um fluxo real recusaria pular de `ABERTO` direto para `FECHADO`.
- **Banco em memória.** Trocar para Postgres é mudar a URL e a dependência; o resto do
  código não sabe qual banco está embaixo.
