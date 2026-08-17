# HelpDesk-Interno
# Chamados HQ

Sistema de abertura e acompanhamento de chamados internos, desenvolvido em **Java 21** com **Spring Boot**.  
O projeto simula o fluxo de um help desk: o funcionário abre um chamado, recebe um protocolo e acompanha o andamento; o atendimento visualiza a fila, altera status, complementa descrições e reabre chamados quando necessário.

## Tecnologias

- Java 21
- Spring Boot 4
- Spring Web
- Spring Data JPA
- Bean Validation
- H2 Database em memória
- HTML, CSS e JavaScript
- Maven Wrapper

## Funcionalidades

- Abertura de chamados com geração automática de protocolo
- Consulta de chamado pelo número de protocolo
- Painel administrativo para acompanhamento da fila
- Alteração de status do chamado
- Reabertura de chamados encerrados
- Complemento de descrição durante o atendimento
- Validação de dados de entrada
- Tratamento padronizado de erros
- Interface web servida pela própria aplicação
- Banco H2 em memória com dados de demonstração

## Regra principal do projeto

O sistema trabalha com histórico por versões.

Cada alteração em um chamado gera uma nova linha no banco, preservando o histórico completo do atendimento.  
Isso evita que mudanças de status ou descrição apaguem informações anteriores.

Exemplo:

```text
NC-2026-0001 - ABERTO
NC-2026-0001 - EM_ANDAMENTO
NC-2026-0001 - RESOLVIDO
NC-2026-0001 - REABERTO
```

Dessa forma, o sistema consegue mostrar tanto o estado atual quanto a linha do tempo completa do chamado.

## Como executar

É necessário ter o **JDK 21** instalado.

No Windows:

```bash
mvnw.cmd spring-boot:run
```

No Linux ou macOS:

```bash
./mvnw spring-boot:run
```

Depois acesse:

```text
http://localhost:8081
```

Console do banco H2:

```text
http://localhost:8081/h2-console
```

Configuração do H2:

```text
JDBC URL: jdbc:h2:mem:chamados
Usuário: sa
Senha: deixe em branco
```

## Endpoints da API

Base da API:

```text
/api/v1/chamados
```

| Método | Rota | Descrição |
| --- | --- | --- |
| GET | `/api/v1/chamados` | Lista todas as versões dos chamados |
| POST | `/api/v1/chamados` | Abre um novo chamado |
| GET | `/api/v1/chamados/{numeroChamado}` | Consulta o histórico de um chamado |
| PATCH | `/api/v1/chamados/{numeroChamado}/status` | Altera o status |
| PATCH | `/api/v1/chamados/{numeroChamado}/descricao` | Complementa a descrição |
| POST | `/api/v1/chamados/{numeroChamado}/reaberturas` | Reabre um chamado encerrado |

## Exemplo de abertura de chamado

```json
{
  "nome": "Ana Souza",
  "email": "ana.souza@empresa.com.br",
  "tipoChamado": "EMAIL",
  "prioridade": "MEDIA",
  "descricao": "Não consigo enviar e-mails com anexo."
}
```

Resposta esperada:

```json
{
  "numeroChamado": "NC-2026-0001",
  "nome": "Ana Souza",
  "email": "ana.souza@empresa.com.br",
  "tipoChamado": "EMAIL",
  "prioridade": "MEDIA",
  "status": "ABERTO"
}
```

## Valores aceitos

### Tipo de chamado

- PERMISSOES
- EMAIL
- HARDWARE
- IMPRESSORA
- SISTEMA
- OUTROS

### Prioridade

- BAIXA
- MEDIA
- ALTA
- CRITICA

### Status

- ABERTO
- EM_ANDAMENTO
- RESOLVIDO
- FECHADO
- REABERTO

## Estrutura do projeto

```text
src/main/java/com/chamados
├── controller
├── service
├── repository
├── model
├── dto
└── exception

src/main/resources
├── static
├── data.sql
└── application.properties
```

## Testes

Para executar os testes:

```bash
mvnw.cmd test
```

Os testes cobrem abertura de chamados, validações, respostas de erro, alteração de status, reabertura e preservação do histórico.

## Observações

Este projeto foi criado com foco em estudo e prática de desenvolvimento backend com Spring Boot, organização em camadas, API REST, validação de dados, persistência com JPA e construção de uma interface simples integrada ao backend.
