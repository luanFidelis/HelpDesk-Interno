package com.chamados.controller;

import com.chamados.dto.AbrirChamadoRequest;
import com.chamados.dto.AlterarDescricaoRequest;
import com.chamados.dto.ChamadoResponse;
import com.chamados.dto.MudarStatusRequest;
import com.chamados.model.Chamado;
import com.chamados.service.ChamadoAdmService;
import com.chamados.service.ChamadoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chamados")
public class ChamadoController {

    private final ChamadoService chamadoService;
    private final ChamadoAdmService chamadoAdmService;

    public ChamadoController(ChamadoService chamadoService, ChamadoAdmService chamadoAdmService) {
        this.chamadoService = chamadoService;
        this.chamadoAdmService = chamadoAdmService;
    }

    /** Fila do atendimento: todas as versões gravadas, em ordem cronológica. */
    @GetMapping
    public List<ChamadoResponse> listar() {
        return ChamadoResponse.de(chamadoAdmService.listarTudo());
    }

    /** Linha do tempo de um chamado. Responde 404 quando o protocolo não existe. */
    @GetMapping("/{numeroChamado}")
    public List<ChamadoResponse> historico(@PathVariable String numeroChamado) {
        return ChamadoResponse.de(chamadoService.historico(numeroChamado));
    }

    /** Abre um chamado e devolve 201 com o protocolo gerado no corpo e no Location. */
    @PostMapping
    public ResponseEntity<ChamadoResponse> abrir(@Valid @RequestBody AbrirChamadoRequest request,
                                                 UriComponentsBuilder uriBuilder) {
        Chamado aberto = chamadoService.abrir(request);

        var uri = uriBuilder
                .path("/api/v1/chamados/{numeroChamado}")
                .buildAndExpand(aberto.getNumeroChamado())
                .toUri();

        return ResponseEntity.created(uri).body(ChamadoResponse.de(aberto));
    }

    @PatchMapping("/{numeroChamado}/status")
    public ChamadoResponse mudarStatus(@PathVariable String numeroChamado,
                                       @Valid @RequestBody MudarStatusRequest request) {
        return ChamadoResponse.de(chamadoAdmService.mudarStatus(numeroChamado, request.status()));
    }

    @PostMapping("/{numeroChamado}/reaberturas")
    public ChamadoResponse reabrir(@PathVariable String numeroChamado) {
        return ChamadoResponse.de(chamadoAdmService.reabrir(numeroChamado));
    }

    @PatchMapping("/{numeroChamado}/descricao")
    public ChamadoResponse alterarDescricao(@PathVariable String numeroChamado,
                                            @Valid @RequestBody AlterarDescricaoRequest request) {
        return ChamadoResponse.de(
                chamadoService.alterarDescricao(numeroChamado, request.descricao()));
    }
}
