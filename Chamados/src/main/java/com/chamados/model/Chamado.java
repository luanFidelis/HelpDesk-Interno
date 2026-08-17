package com.chamados.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Uma linha desta tabela é uma <b>versão</b> de um chamado, não o chamado inteiro.
 *
 * <p>O registro é append-only: abrir, mudar status, reabrir ou editar a descrição
 * sempre gravam uma linha nova com o mesmo {@code numeroChamado}. O estado atual é
 * a versão mais recente, e o conjunto de linhas é a linha do tempo do atendimento —
 * nada é sobrescrito, então o histórico não pode ser perdido por um update.
 *
 * <p>Por isso {@code dataAbertura} significa "quando esta versão foi registrada":
 * na primeira linha ela é a abertura de fato; nas seguintes, o instante da mudança.
 */
@Entity
@Table(name = "chamado")
public class Chamado {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 30)
    private String numeroChamado;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, length = 180)
    private String email;

    @Column(nullable = false, length = 2000)
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoChamado tipoChamado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Prioridade prioridade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(nullable = false)
    private LocalDateTime dataAbertura;

    private LocalDateTime dataFechamento;

    /** Exigido pelo JPA. */
    protected Chamado() {
    }

    public Chamado(String numeroChamado,
                   String nome,
                   String email,
                   String descricao,
                   TipoChamado tipoChamado,
                   Prioridade prioridade,
                   Status status,
                   LocalDateTime dataAbertura) {
        this.numeroChamado = numeroChamado;
        this.nome = nome;
        this.email = email;
        this.descricao = descricao;
        this.tipoChamado = tipoChamado;
        this.prioridade = prioridade;
        this.status = status;
        this.dataAbertura = dataAbertura;
        this.dataFechamento = status != null && status.encerra() ? dataAbertura : null;
    }

    /**
     * Copia este chamado para uma nova versão, preservando os dados de identificação
     * (número, solicitante, tipo e prioridade) e trocando apenas o que mudou.
     *
     * <p>Centralizar a cópia aqui evita o erro de esquecer um campo em cada ponto que
     * grava histórico — era exatamente assim que o tipo do chamado se perdia.
     *
     * @param novoStatus    status da nova versão
     * @param novaDescricao descrição nova, ou {@code null} para manter a atual
     * @param quando        instante do registro
     */
    public Chamado proximaVersao(Status novoStatus, String novaDescricao, LocalDateTime quando) {
        return new Chamado(
                numeroChamado,
                nome,
                email,
                novaDescricao != null ? novaDescricao : descricao,
                tipoChamado,
                prioridade,
                novoStatus,
                quando);
    }

    public UUID getId() {
        return id;
    }

    public String getNumeroChamado() {
        return numeroChamado;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getDescricao() {
        return descricao;
    }

    public TipoChamado getTipoChamado() {
        return tipoChamado;
    }

    public Prioridade getPrioridade() {
        return prioridade;
    }

    public Status getStatus() {
        return status;
    }

    public LocalDateTime getDataAbertura() {
        return dataAbertura;
    }

    public LocalDateTime getDataFechamento() {
        return dataFechamento;
    }
}
