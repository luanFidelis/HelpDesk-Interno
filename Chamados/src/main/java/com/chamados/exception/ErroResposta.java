package com.chamados.exception;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Formato único de erro da API, para o cliente não precisar adivinhar se a falha
 * voltou como texto puro, como ProblemDetail ou como stack trace.
 *
 * @param campos erro por campo, presente apenas quando a falha é de validação
 */
public record ErroResposta(
        int status,
        String mensagem,
        Map<String, String> campos,
        LocalDateTime momento
) {

    public static ErroResposta de(int status, String mensagem) {
        return new ErroResposta(status, mensagem, null, LocalDateTime.now());
    }

    public static ErroResposta de(int status, String mensagem, Map<String, String> campos) {
        return new ErroResposta(status, mensagem, campos, LocalDateTime.now());
    }
}
