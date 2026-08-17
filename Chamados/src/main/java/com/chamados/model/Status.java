package com.chamados.model;

public enum Status {
    ABERTO,
    EM_ANDAMENTO,
    RESOLVIDO,
    FECHADO,
    REABERTO;

    /** Status que encerram o atendimento e, portanto, estampam a data de fechamento. */
    public boolean encerra() {
        return this == RESOLVIDO || this == FECHADO;
    }
}
