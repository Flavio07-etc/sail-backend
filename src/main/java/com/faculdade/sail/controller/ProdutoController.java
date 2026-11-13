package com.faculdade.sail.controller;

import com.faculdade.sail.dtos.ProdutoEntrada;
import com.faculdade.sail.dtos.ProdutoResposta;
import com.faculdade.sail.dtos.PaginaResposta;
import com.faculdade.sail.dtos.ResumoEstoque;
import com.faculdade.sail.exception.ApiException;
import com.faculdade.sail.model.Produto;
import com.faculdade.sail.service.ProdutoService;
import com.faculdade.sail.service.VersaoProduto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/produtos")
public class ProdutoController {

    private final ProdutoService service;

    public ProdutoController(ProdutoService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ProdutoResposta> adicionar(@Valid @RequestBody ProdutoEntrada entrada, Principal principal,
            @RequestHeader(value = "usuarioId", required = false) Long usuarioId,
            @RequestHeader(value = "Idempotency-Key", required = false) String chave) {
        Produto produto = service.salvarProduto(entrada.paraProduto(), responsavel(principal, usuarioId), chave);
        return resposta(produto);
    }

    @PostMapping("/{id}/vender")
    public ResponseEntity<ProdutoResposta> vender(@PathVariable Long id, @RequestParam Integer quantidade, Principal principal,
            @RequestHeader(value = "usuarioId", required = false) Long usuarioId,
            @RequestHeader(value = "Idempotency-Key", required = false) String chave,
            @RequestHeader(value = "If-Match", required = false) String versao) {
        return resposta(service.venderProduto(id, quantidade, responsavel(principal, usuarioId), chave, versao));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProdutoResposta> atualizar(@PathVariable Long id, @Valid @RequestBody ProdutoEntrada entrada, Principal principal,
            @RequestHeader(value = "usuarioId", required = false) Long usuarioId,
            @RequestHeader(value = "If-Match", required = false) String versao) {
        return resposta(service.atualizarProduto(id, entrada.paraProduto(), responsavel(principal, usuarioId), versao));
    }

    @GetMapping
    public List<ProdutoResposta> listar() {
        return service.listarProdutos().stream().map(ProdutoResposta::de).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProdutoResposta> obter(@PathVariable Long id) {
        return resposta(service.obterProduto(id));
    }

    @GetMapping("/pagina")
    public PaginaResposta<ProdutoResposta> listarPagina(@RequestParam(defaultValue = "50") int tamanho,
            @RequestParam(required = false) Long antesDe) {
        return service.listarPagina(tamanho, antesDe);
    }

    @GetMapping("/resumo")
    public ResumoEstoque resumo() {
        return service.resumirEstoque();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deletar(@PathVariable Long id, Principal principal,
            @RequestHeader(value = "usuarioId", required = false) Long usuarioId,
            @RequestHeader(value = "If-Match", required = false) String versao) {
        service.deletarProduto(id, responsavel(principal, usuarioId), versao);
        return ResponseEntity.ok("Produto removido do estoque com sucesso!");
    }

    private ResponseEntity<ProdutoResposta> resposta(Produto produto) {
        return ResponseEntity.ok().eTag(VersaoProduto.calcular(produto)).body(ProdutoResposta.de(produto));
    }

    private Long responsavel(Principal principal, Long informado) {
        if (principal == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "SESSAO_INVALIDA", "Entre na sua conta para continuar.");
        }
        Long autenticado;
        try {
            autenticado = Long.valueOf(principal.getName());
        } catch (NumberFormatException erro) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "SESSAO_INVALIDA", "Entre novamente na sua conta.");
        }
        if (informado != null && !autenticado.equals(informado)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACESSO_NEGADO", "O usuário informado não corresponde à sessão atual.");
        }
        return autenticado;
    }
}
