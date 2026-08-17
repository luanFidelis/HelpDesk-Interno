package com.chamados.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(ChamadoNotFoundException.class)
    public ResponseEntity<ErroResposta> chamadoNaoEncontrado(ChamadoNotFoundException e) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErroResposta.de(HttpStatus.NOT_FOUND.value(), e.getMessage()));
    }

    /** Dispara quando o {@code @Valid} de um corpo de requisição recusa algum campo. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResposta> validacao(MethodArgumentNotValidException e) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError erro : e.getBindingResult().getFieldErrors()) {
            campos.putIfAbsent(erro.getField(), erro.getDefaultMessage());
        }
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErroResposta.de(
                        HttpStatus.BAD_REQUEST.value(),
                        "Há campos inválidos na solicitação.",
                        campos));
    }

    /**
     * Corpo que o Jackson não conseguiu ler — tipicamente um valor fora do enum,
     * como {@code "prioridade":"URGENTE"}. Sem este tratamento a resposta sairia
     * como erro genérico, sem dizer ao cliente o que recusar.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResposta> corpoInvalido(HttpMessageNotReadableException e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErroResposta.de(
                        HttpStatus.BAD_REQUEST.value(),
                        "Corpo da requisição inválido: verifique os valores enviados."));
    }
}
