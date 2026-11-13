package com.faculdade.sail.controller;

import com.faculdade.sail.dtos.HistoricoDTO;
import com.faculdade.sail.dtos.PaginaHistoricoResposta;
import com.faculdade.sail.service.HistoricoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/historico")
public class HistoricoController {

    private final HistoricoService service;

    public HistoricoController(HistoricoService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<HistoricoDTO> obterHistorico(
            @RequestParam(required = false, defaultValue = "0") int mes,
            @RequestParam(required = false, defaultValue = "0") int ano) {
        return ResponseEntity.ok(service.obterHistorico(mes, ano));
    }

    @GetMapping("/pagina")
    public ResponseEntity<PaginaHistoricoResposta> obterPagina(@RequestParam(defaultValue = "0") int mes,
            @RequestParam(defaultValue = "0") int ano, @RequestParam(defaultValue = "50") int tamanho,
            @RequestParam(required = false) String cursor) {
        return ResponseEntity.ok(service.obterPagina(mes, ano, tamanho, cursor));
    }
}
