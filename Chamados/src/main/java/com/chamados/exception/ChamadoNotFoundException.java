package com.chamados.exception;

import java.util.UUID;

public class ChamadoNotFoundException extends RuntimeException {

    public ChamadoNotFoundException(String numeroChamado) {
        super("Chamado " + numeroChamado + " não encontrado.");
    }

    public ChamadoNotFoundException(UUID id) {
        super("Chamado de id " + id + " não encontrado.");
    }
}
