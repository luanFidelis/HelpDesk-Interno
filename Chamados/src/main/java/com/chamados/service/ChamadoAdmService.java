package com.chamados.service;

import com.chamados.exception.ChamadoNotFoundException;
import com.chamados.model.Chamado;
import com.chamados.model.Status;
import com.chamados.repository.ChamadoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** Operações do lado do atendimento — quem trata a fila de chamados. */
@Service
public class ChamadoAdmService {

    private final ChamadoRepository chamadoRepository;

    public ChamadoAdmService(ChamadoRepository chamadoRepository) {
        this.chamadoRepository = chamadoRepository;
    }

    /**
     * Todas as versões de todos os chamados, em ordem cronológica.
     *
     * <p>A API entrega o registro cru e a tela dobra as versões em um chamado por
     * linha. Assim a mesma resposta serve para a fila e para a linha do tempo.
     */
    @Transactional(readOnly = true)
    public List<Chamado> listarTudo() {
        return chamadoRepository.findAllByOrderByDataAberturaAsc();
    }

    @Transactional(readOnly = true)
    public List<Chamado> listarPorStatus(Status status) {
        return chamadoRepository.findByStatus(status);
    }

    /**
     * Grava uma nova versão com o status escolhido pelo atendente, partindo sempre
     * da versão mais recente do chamado.
     */
    @Transactional
    public Chamado mudarStatus(String numeroChamado, Status novoStatus) {
        Chamado atual = versaoAtual(numeroChamado);
        Chamado atualizado = atual.proximaVersao(novoStatus, null, LocalDateTime.now());
        return chamadoRepository.save(atualizado);
    }

    /**
     * Reabre o chamado a partir da sua versão mais recente. Como {@code REABERTO}
     * não encerra o atendimento, a data de fechamento volta a ficar vazia.
     */
    @Transactional
    public Chamado reabrir(String numeroChamado) {
        Chamado atual = versaoAtual(numeroChamado);
        Chamado reaberto = atual.proximaVersao(Status.REABERTO, null, LocalDateTime.now());
        return chamadoRepository.save(reaberto);
    }

    private Chamado versaoAtual(String numeroChamado) {
        return chamadoRepository
                .findFirstByNumeroChamadoOrderByDataAberturaDesc(numeroChamado)
                .orElseThrow(() -> new ChamadoNotFoundException(numeroChamado));
    }
}
