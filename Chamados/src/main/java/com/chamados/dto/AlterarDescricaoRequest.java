package com.chamados.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Corpo do PATCH de descrição, usado quando o solicitante complementa o relato. */
public record AlterarDescricaoRequest(

        @NotBlank(message = "Descreva o problema.")
        @Size(min = 10, max = 2000, message = "A descrição deve ter de 10 a 2000 caracteres.")
        String descricao
) {
}
