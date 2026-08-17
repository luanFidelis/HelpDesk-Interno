package com.chamados.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ChamadoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String CHAMADO_VALIDO = """
            {
              "nome": "Ana Souza",
              "email": "ana.souza@exemplo.com.br",
              "tipoChamado": "HARDWARE",
              "prioridade": "ALTA",
              "descricao": "O notebook do financeiro nao liga depois da queda de energia."
            }
            """;

    /** Abre um chamado e devolve o protocolo gerado, para o teste seguir dele. */
    private String abrirChamado() throws Exception {
        String corpo = mockMvc.perform(post("/api/v1/chamados")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CHAMADO_VALIDO))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(corpo).get("numeroChamado").asText();
    }

    private JsonNode historico(String numeroChamado) throws Exception {
        String corpo = mockMvc.perform(get("/api/v1/chamados/{numero}", numeroChamado))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(corpo);
    }

    @Test
    @DisplayName("abrir chamado responde 201 com o protocolo no corpo e no Location")
    void abrirChamadoDeveResponder201ComProtocolo() throws Exception {
        mockMvc.perform(post("/api/v1/chamados")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CHAMADO_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.numeroChamado").value(
                        org.hamcrest.Matchers.matchesPattern("NC-\\d{4}-\\d{4}")))
                .andExpect(jsonPath("$.status").value("ABERTO"))
                .andExpect(jsonPath("$.tipoChamado").value("HARDWARE"))
                .andExpect(jsonPath("$.prioridade").value("ALTA"))
                .andExpect(jsonPath("$.dataAbertura").exists())
                .andExpect(jsonPath("$.dataFechamento").doesNotExist());
    }

    /**
     * Regressão do bug de validação: com {@code @NotBlank} em cima de um enum, o
     * Bean Validation estoura {@code UnexpectedTypeException} e a resposta vira 500.
     * Este teste falha se a anotação errada voltar ao DTO.
     */
    @Test
    @DisplayName("chamado sem campos obrigatorios responde 400 e nao 500")
    void chamadoIncompletoDeveResponder400() throws Exception {
        mockMvc.perform(post("/api/v1/chamados")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.campos.nome").exists())
                .andExpect(jsonPath("$.campos.email").exists())
                .andExpect(jsonPath("$.campos.tipoChamado").exists())
                .andExpect(jsonPath("$.campos.prioridade").exists())
                .andExpect(jsonPath("$.campos.descricao").exists());
    }

    @Test
    @DisplayName("e-mail invalido responde 400 apontando o campo")
    void emailInvalidoDeveResponder400() throws Exception {
        String corpo = CHAMADO_VALIDO.replace("ana.souza@exemplo.com.br", "ana.souza");

        mockMvc.perform(post("/api/v1/chamados")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.email").exists());
    }

    @Test
    @DisplayName("valor fora do enum responde 400 em vez de erro generico")
    void prioridadeForaDoEnumDeveResponder400() throws Exception {
        String corpo = CHAMADO_VALIDO.replace("\"ALTA\"", "\"URGENTE\"");

        mockMvc.perform(post("/api/v1/chamados")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").exists());
    }

    @Test
    @DisplayName("consultar protocolo inexistente responde 404")
    void protocoloInexistenteDeveResponder404() throws Exception {
        mockMvc.perform(get("/api/v1/chamados/{numero}", "NC-2026-9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensagem").value(
                        org.hamcrest.Matchers.containsString("NC-2026-9999")));
    }

    /**
     * Regressão do bug do campo duplicado: a entidade tinha {@code tipo} e
     * {@code tipoChamado}, e as versões de histórico eram montadas a partir do campo
     * que nunca era preenchido — o chamado perdia a categoria na primeira mudança
     * de status. Este teste falha se a cópia da versão deixar de levar algum campo.
     */
    @Test
    @DisplayName("mudar status grava nova versao preservando tipo, prioridade e solicitante")
    void mudarStatusDevePreservarOsDadosDoChamado() throws Exception {
        String protocolo = abrirChamado();

        mockMvc.perform(patch("/api/v1/chamados/{numero}/status", protocolo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"EM_ANDAMENTO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_ANDAMENTO"))
                .andExpect(jsonPath("$.tipoChamado").value("HARDWARE"))
                .andExpect(jsonPath("$.prioridade").value("ALTA"));

        JsonNode versoes = historico(protocolo);
        assertThat(versoes).hasSize(2);
        assertThat(versoes.get(0).get("status").asText()).isEqualTo("ABERTO");
        assertThat(versoes.get(1).get("status").asText()).isEqualTo("EM_ANDAMENTO");

        for (JsonNode versao : versoes) {
            assertThat(versao.get("numeroChamado").asText()).isEqualTo(protocolo);
            assertThat(versao.get("tipoChamado").asText()).isEqualTo("HARDWARE");
            assertThat(versao.get("prioridade").asText()).isEqualTo("ALTA");
            assertThat(versao.get("nome").asText()).isEqualTo("Ana Souza");
            assertThat(versao.get("email").asText()).isEqualTo("ana.souza@exemplo.com.br");
        }
    }

    @Test
    @DisplayName("resolver estampa a data de fechamento e reabrir a limpa")
    void fechamentoDeveSeguirOStatus() throws Exception {
        String protocolo = abrirChamado();

        mockMvc.perform(patch("/api/v1/chamados/{numero}/status", protocolo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVIDO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataFechamento").exists());

        mockMvc.perform(post("/api/v1/chamados/{numero}/reaberturas", protocolo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REABERTO"))
                .andExpect(jsonPath("$.dataFechamento").doesNotExist());

        assertThat(historico(protocolo)).hasSize(3);
    }

    @Test
    @DisplayName("alterar descricao grava nova versao e mantem o status atual")
    void alterarDescricaoDeveManterOStatus() throws Exception {
        String protocolo = abrirChamado();
        String novaDescricao = "O notebook nao liga e agora tambem nao carrega a bateria.";

        mockMvc.perform(patch("/api/v1/chamados/{numero}/descricao", protocolo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("descricao", novaDescricao))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao").value(novaDescricao))
                .andExpect(jsonPath("$.status").value("ABERTO"));

        assertThat(historico(protocolo)).hasSize(2);
    }

    /**
     * Antes, alterar a descrição de um protocolo inexistente respondia 202 ACCEPTED:
     * o service devolvia {@code Optional.empty()} sem lançar e o controller ignorava.
     */
    @Test
    @DisplayName("alterar descricao de protocolo inexistente responde 404")
    void alterarDescricaoDeProtocoloInexistenteDeveResponder404() throws Exception {
        mockMvc.perform(patch("/api/v1/chamados/{numero}/descricao", "NC-2026-9999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descricao\":\"Descricao com tamanho suficiente.\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("reabrir protocolo inexistente responde 404 e nao 500")
    void reabrirProtocoloInexistenteDeveResponder404() throws Exception {
        mockMvc.perform(post("/api/v1/chamados/{numero}/reaberturas", "NC-2026-9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("mudar status de protocolo inexistente responde 404")
    void mudarStatusDeProtocoloInexistenteDeveResponder404() throws Exception {
        mockMvc.perform(patch("/api/v1/chamados/{numero}/status", "NC-2026-9999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"EM_ANDAMENTO\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("status ausente no corpo responde 400")
    void statusAusenteDeveResponder400() throws Exception {
        String protocolo = abrirChamado();

        mockMvc.perform(patch("/api/v1/chamados/{numero}/status", protocolo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.status").exists());
    }

    @Test
    @DisplayName("a listagem devolve todas as versoes em ordem cronologica")
    void listagemDeveVirEmOrdemCronologica() throws Exception {
        String protocolo = abrirChamado();

        mockMvc.perform(patch("/api/v1/chamados/{numero}/status", protocolo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"EM_ANDAMENTO\"}"))
                .andExpect(status().isOk());

        String corpo = mockMvc.perform(get("/api/v1/chamados"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode linhas = objectMapper.readTree(corpo);
        assertThat(linhas).hasSize(2);
        assertThat(linhas.get(0).get("dataAbertura").asText())
                .isLessThanOrEqualTo(linhas.get(1).get("dataAbertura").asText());
    }
}
