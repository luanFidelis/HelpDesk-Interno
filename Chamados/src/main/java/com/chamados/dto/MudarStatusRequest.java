package com.chamados.dto;

import com.chamados.model.Status;
import jakarta.validation.constraints.NotNull;

/**
 * Corpo do PATCH de status.
 *
 * <p>Um objeto, e não o enum cru no corpo: assim a requisição é um JSON legítimo
 * ({@code {"status":"EM_ANDAMENTO"}}) e cabe adicionar campos no futuro — uma
 * justificativa do atendente, por exemplo — sem quebrar quem já consome a API.
 */
public record MudarStatusRequest(

        @NotNull(message = "Informe o novo status.")
        Status status
) {
}
