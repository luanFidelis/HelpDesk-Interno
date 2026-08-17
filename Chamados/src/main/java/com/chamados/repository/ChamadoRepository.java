package com.chamados.repository;

import com.chamados.model.Chamado;
import com.chamados.model.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChamadoRepository extends JpaRepository<Chamado, UUID> {

    /** Todas as versões gravadas, em ordem cronológica: a linha do tempo geral. */
    List<Chamado> findAllByOrderByDataAberturaAsc();

    /** Linha do tempo de um chamado específico. */
    List<Chamado> findAllByNumeroChamadoOrderByDataAberturaAsc(String numeroChamado);

    /** Versão mais recente de um chamado, ou seja, o seu estado atual. */
    Optional<Chamado> findFirstByNumeroChamadoOrderByDataAberturaDesc(String numeroChamado);

    List<Chamado> findByStatus(Status status);

    boolean existsByNumeroChamado(String numeroChamado);

    /** Quantos chamados distintos já foram abertos no ano, para numerar o próximo. */
    @Query("select count(distinct c.numeroChamado) from Chamado c "
            + "where c.numeroChamado like concat(:prefixo, '%')")
    long contarChamadosComPrefixo(@Param("prefixo") String prefixo);
}
