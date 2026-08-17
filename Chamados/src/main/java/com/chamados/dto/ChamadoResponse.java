package com.chamados.dto;

import com.chamados.model.Chamado;
import com.chamados.model.Prioridade;
import com.chamados.model.Status;
import com.chamados.model.TipoChamado;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Versão de um chamado como a API devolve.
 *
 * <p>Existe para a entidade não sair serializada direto: o contrato do JSON deixa
 * de mudar sozinho quando o modelo muda, e nada que seja interno vaza sem querer.
 */
public record ChamadoResponse(
        UUID id,
        String numeroChamado,
        String nome,
        String email,
        String descricao,
        TipoChamado tipoChamado,
        Prioridade prioridade,
        Status status,
        LocalDateTime dataAbertura,
        LocalDateTime dataFechamento
) {

    public static ChamadoResponse de(Chamado chamado) {
        return new ChamadoResponse(
                chamado.getId(),
                chamado.getNumeroChamado(),
                chamado.getNome(),
                chamado.getEmail(),
                chamado.getDescricao(),
                chamado.getTipoChamado(),
                chamado.getPrioridade(),
                chamado.getStatus(),
                chamado.getDataAbertura(),
                chamado.getDataFechamento());
    }

    public static List<ChamadoResponse> de(List<Chamado> chamados) {
        return chamados.stream().map(ChamadoResponse::de).toList();
    }
}
