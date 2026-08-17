package com.chamados.service;

import com.chamados.dto.AbrirChamadoRequest;
import com.chamados.exception.ChamadoNotFoundException;
import com.chamados.model.Chamado;
import com.chamados.model.Status;
import com.chamados.repository.ChamadoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Operações do lado de quem abre o chamado. */
@Service
public class ChamadoService {

    private final ChamadoRepository chamadoRepository;

    public ChamadoService(ChamadoRepository chamadoRepository) {
        this.chamadoRepository = chamadoRepository;
    }

    /**
     * Abre um chamado e devolve a versão gravada — é ela que carrega o
     * {@code numeroChamado} gerado, o protocolo que o solicitante usa para consultar.
     */
    @Transactional
    public Chamado abrir(AbrirChamadoRequest request) {
        Chamado chamado = new Chamado(
                gerarNumeroChamado(),
                request.nome(),
                request.email(),
                request.descricao(),
                request.tipoChamado(),
                request.prioridade(),
                Status.ABERTO,
                LocalDateTime.now());

        return chamadoRepository.save(chamado);
    }

    /** Linha do tempo completa de um chamado, da abertura à última mudança. */
    @Transactional(readOnly = true)
    public List<Chamado> historico(String numeroChamado) {
        List<Chamado> versoes = chamadoRepository
                .findAllByNumeroChamadoOrderByDataAberturaAsc(numeroChamado);

        if (versoes.isEmpty()) {
            throw new ChamadoNotFoundException(numeroChamado);
        }
        return versoes;
    }

    /**
     * Registra uma nova versão com a descrição complementada, mantendo o status atual.
     */
    @Transactional
    public Chamado alterarDescricao(String numeroChamado, String novaDescricao) {
        Chamado atual = versaoAtual(numeroChamado);
        Chamado complementado = atual.proximaVersao(
                atual.getStatus(), novaDescricao, LocalDateTime.now());

        return chamadoRepository.save(complementado);
    }

    /** Estado atual de um chamado, isto é, a versão mais recente. */
    @Transactional(readOnly = true)
    public Chamado versaoAtual(String numeroChamado) {
        return chamadoRepository
                .findFirstByNumeroChamadoOrderByDataAberturaDesc(numeroChamado)
                .orElseThrow(() -> new ChamadoNotFoundException(numeroChamado));
    }

    /**
     * Protocolo no formato {@code NC-ANO-SEQUENCIA}, curto o bastante para o
     * solicitante digitar na consulta.
     *
     * <p>A sequência vem de uma contagem, então duas aberturas simultâneas poderiam
     * disputar o mesmo número; o laço abaixo cobre a colisão. Em produção o número
     * sairia de uma sequence do banco, que resolve a disputa no próprio SGBD.
     */
    private String gerarNumeroChamado() {
        String prefixo = "NC-" + LocalDate.now().getYear() + "-";
        long sequencia = chamadoRepository.contarChamadosComPrefixo(prefixo) + 1;

        String numero = prefixo + String.format("%04d", sequencia);
        while (chamadoRepository.existsByNumeroChamado(numero)) {
            sequencia++;
            numero = prefixo + String.format("%04d", sequencia);
        }
        return numero;
    }
}
