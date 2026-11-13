package com.faculdade.sail.controller;

import com.faculdade.sail.exception.VendaInvalidaException;
import com.faculdade.sail.exception.ApiExceptionHandler;
import com.faculdade.sail.exception.ApiException;
import com.faculdade.sail.model.Produto;
import com.faculdade.sail.service.ProdutoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProdutoControllerTests {

    private ProdutoService service;
    private MockMvc mvc;

    @BeforeEach
    void prepararController() {
        service = mock(ProdutoService.class);
        ProdutoController controller = new ProdutoController(service);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .defaultRequest(get("/").principal(() -> "3")).build();
    }

    @Test
    void preservaContratoDaVendaValida() throws Exception {
        when(service.venderProduto(1L, 2, 3L, null, null)).thenReturn(produto());

        mvc.perform(post("/produtos/1/vender").param("quantidade", "2").header("usuarioId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("Produto de teste"))
                .andExpect(jsonPath("$.quantidade").value(8))
                .andExpect(jsonPath("$.preco").value(12.5));
    }

    @Test
    void retornaMensagemParaRejeicaoDeNegocio() throws Exception {
        when(service.venderProduto(1L, 0, 3L, null, null))
                .thenThrow(new VendaInvalidaException("Informe uma quantidade inteira maior que zero."));

        mvc.perform(post("/produtos/1/vender").param("quantidade", "0").header("usuarioId", "3"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Informe uma quantidade inteira maior que zero."));
    }

    @Test
    void encaminhaVersaoEChaveDaVendaRevisada() throws Exception {
        when(service.venderProduto(1L, 2, 3L, "venda-revisada", "\"versao-1\"")).thenReturn(produto());
        mvc.perform(post("/produtos/1/vender").param("quantidade", "2")
                        .header("If-Match", "\"versao-1\"").header("Idempotency-Key", "venda-revisada"))
                .andExpect(status().isOk()).andExpect(header().exists("ETag"));
        verify(service).venderProduto(1L, 2, 3L, "venda-revisada", "\"versao-1\"");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.5", "abc", "2147483648"})
    void rejeitaParametroQueNaoRepresentaInteger(String quantidade) throws Exception {
        mvc.perform(post("/produtos/1/vender").param("quantidade", quantidade).header("usuarioId", "3"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void rejeitaQuantidadeAusente() throws Exception {
        mvc.perform(post("/produtos/1/vender").header("usuarioId", "3"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void identificaResponsavelPelaSessaoSemHeaderLegado() throws Exception {
        when(service.venderProduto(1L, 1, 3L, null, null)).thenReturn(produto());
        mvc.perform(post("/produtos/1/vender").param("quantidade", "1"))
                .andExpect(status().isOk());
        verify(service).venderProduto(1L, 1, 3L, null, null);
    }

    @Test
    void rejeitaResponsavelDiferenteDaSessao() throws Exception {
        mvc.perform(post("/produtos/1/vender").param("quantidade", "1").header("usuarioId", "9"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACESSO_NEGADO"));
        verifyNoInteractions(service);
    }

    @Test
    void trataRestricaoDePersistenciaSemExporDetalhes() throws Exception {
        when(service.venderProduto(1L, 2, 3L, null, null))
                .thenThrow(new DataIntegrityViolationException("Detalhe interno simulado"));
        mvc.perform(post("/produtos/1/vender").param("quantidade", "2").header("usuarioId", "3"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_DE_DADOS"))
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Detalhe interno"))));
    }

    @Test
    void trataFalhaInesperadaSemExporDetalhes() throws Exception {
        when(service.venderProduto(1L, 2, 3L, null, null)).thenThrow(new IllegalStateException("Detalhe interno simulado"));
        mvc.perform(post("/produtos/1/vender").param("quantidade", "2").header("usuarioId", "3"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.codigo").value("ERRO_INTERNO"))
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Detalhe interno"))));
    }

    @Test
    void preservaContratoDoCadastro() throws Exception {
        when(service.salvarProduto(any(Produto.class), eq(3L), isNull())).thenReturn(produto());

        mvc.perform(post("/produtos").header("usuarioId", "3")
                .contentType("application/json")
                .content("{\"nome\":\"Produto de teste\",\"quantidade\":8,\"preco\":12.5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.quantidade").value(8));

        verify(service, times(1)).salvarProduto(any(Produto.class), eq(3L), isNull());
    }

    @Test
    void trataBancoIndisponivelSemExporDetalhes() throws Exception {
        when(service.listarProdutos()).thenThrow(new DataAccessResourceFailureException("Detalhe interno simulado"));
        mvc.perform(get("/produtos"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.codigo").value("SERVICO_INDISPONIVEL"))
                .andExpect(jsonPath("$.mensagem").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Detalhe interno"))));
    }

    @Test
    void rejeitaJsonMalformadoSemGravar() throws Exception {
        mvc.perform(post("/produtos").contentType("application/json").content("{\"nome\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));
        verifyNoInteractions(service);
    }

    @Test
    void rejeitaMetodoNaoSuportado() throws Exception {
        mvc.perform(patch("/produtos/1").contentType("application/json").content("{}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", org.hamcrest.Matchers.containsString("PUT")))
                .andExpect(jsonPath("$.codigo").value("METODO_NAO_PERMITIDO"));
        verifyNoInteractions(service);
    }

    @Test
    void rejeitaConteudoQueNaoEJson() throws Exception {
        mvc.perform(post("/produtos").contentType("text/plain").content("produto"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.codigo").value("FORMATO_NAO_SUPORTADO"));
        verifyNoInteractions(service);
    }

    @Test
    void informaProdutoInexistente() throws Exception {
        when(service.obterProduto(9L)).thenThrow(ApiException.naoEncontrado("Produto não encontrado no estoque."));
        mvc.perform(get("/produtos/9"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("RECURSO_NAO_ENCONTRADO"));
    }

    @Test
    void preservaContratoDaListagem() throws Exception {
        when(service.listarProdutos()).thenReturn(List.of(produto()));

        mvc.perform(get("/produtos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void preservaContratoDaEdicao() throws Exception {
        when(service.atualizarProduto(eq(1L), any(Produto.class), eq(3L), isNull())).thenReturn(produto());

        mvc.perform(put("/produtos/1").header("usuarioId", "3")
                .contentType("application/json")
                .content("{\"nome\":\"Produto de teste\",\"quantidade\":8,\"preco\":12.5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidade").value(8));
    }

    @Test
    void preservaContratoDaExclusao() throws Exception {
        mvc.perform(delete("/produtos/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Produto removido do estoque com sucesso!"));
        verify(service).deletarProduto(1L, 3L, null);
    }

    private Produto produto() {
        Produto produto = new Produto();
        produto.setId(1L);
        produto.setNome("Produto de teste");
        produto.setQuantidade(8);
        produto.setPreco(12.5);
        return produto;
    }
}
