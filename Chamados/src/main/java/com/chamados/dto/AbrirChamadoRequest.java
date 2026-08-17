package com.chamados.dto;

import com.chamados.model.Prioridade;
import com.chamados.model.TipoChamado;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Dados que o solicitante envia para abrir um chamado.
 *
 * <p>Enum usa {@code @NotNull}, e não {@code @NotBlank}: só existe validador de
 * {@code @NotBlank} para {@code CharSequence}, então em um enum ele estouraria
 * {@code UnexpectedTypeException} (HTTP 500) em vez de recusar o campo vazio.
 */
public record AbrirChamadoRequest(

        @NotBlank(message = "Informe o seu nome.")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
        String nome,

        @NotBlank(message = "Informe o seu e-mail.")
        @Email(message = "Informe um e-mail válido.")
        @Size(max = 180, message = "O e-mail deve ter no máximo 180 caracteres.")
        String email,

        @NotNull(message = "Escolha o tipo do chamado.")
        TipoChamado tipoChamado,

        @NotNull(message = "Escolha a prioridade.")
        Prioridade prioridade,

        @NotBlank(message = "Descreva o problema.")
        @Size(min = 10, max = 2000, message = "A descrição deve ter de 10 a 2000 caracteres.")
        String descricao
) {
}
