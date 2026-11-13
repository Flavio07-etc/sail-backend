package com.faculdade.sail;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.UUID;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"logging.level.org.hibernate.SQL=WARN", "logging.level.org.springframework=INFO"})
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PerformanceTests {

    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private MockMvc mvc;
    @Autowired
    private JwtEncoder encoder;
    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private final JsonMapper json = JsonMapper.builder().build();
    private String authorization;

    @BeforeAll
    void prepararDadosExclusivosDeTeste() {
        assertThat(jdbc.queryForObject("SELECT DATABASE()", String.class)).isEqualTo("sail_test_round1");
        long existentes = jdbc.queryForObject("SELECT COUNT(*) FROM produtos WHERE nome LIKE 'SAIL-PERF-F4-%'", Long.class);
        if (existentes == 0) {
            jdbc.update("INSERT INTO usuarios (nome, email, senha, senha_codificada) VALUES (?, ?, ?, false)",
                    "Usuário de desempenho", "performance-f4@example.invalid", "Senha exclusiva de teste");
            Long usuarioId = jdbc.queryForObject("SELECT id FROM usuarios WHERE email = ?", Long.class, "performance-f4@example.invalid");
            List<Object[]> produtos = new ArrayList<>();
            for (int i = 0; i < 1200; i++) produtos.add(new Object[]{"SAIL-PERF-F4-" + i, 10, 12.5, usuarioId});
            jdbc.batchUpdate("INSERT INTO produtos (nome, quantidade, preco, ativo, created_by) VALUES (?, ?, ?, true, ?)", produtos);
            List<Long> ids = jdbc.queryForList("SELECT id FROM produtos WHERE nome LIKE 'SAIL-PERF-F4-%' ORDER BY id", Long.class);
            List<Object[]> registros = new ArrayList<>();
            for (Long id : ids) {
                registros.add(new Object[]{id, usuarioId});
                registros.add(new Object[]{id, usuarioId});
            }
            jdbc.batchUpdate("INSERT INTO registros (produto_id, quantidade, valor_total, data_venda, usuario_id, acao) VALUES (?, 1, 12.5, '1888-06-15 12:00:00', ?, 'VENDA')", registros);
        } else {
            assertThat(existentes).isEqualTo(1200);
        }
        Long usuarioId = jdbc.queryForObject("SELECT id FROM usuarios WHERE email = ?", Long.class, "performance-f4@example.invalid");
        Instant agora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("sail").audience(List.of("sail-app"))
                .subject(usuarioId.toString()).issuedAt(agora).notBefore(agora).expiresAt(agora.plusSeconds(1800)).build();
        authorization = "Bearer " + encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    @Test
    void medeListagensCompletas() throws Exception {
        Medicao produtos = medir(get("/produtos"));
        Medicao historico = medir(get("/historico").param("mes", "6").param("ano", "1888"));
        assertThat(produtos.dados().size()).isGreaterThanOrEqualTo(1200);
        assertThat(historico.dados().get("lancamentos").size()).isGreaterThanOrEqualTo(2400);
        imprimir("COMPLETO_PRODUTOS", produtos, produtos.dados().size());
        imprimir("COMPLETO_HISTORICO", historico, historico.dados().get("lancamentos").size());
    }

    @Test
    void paginasEResumoLimitamTransferenciaEHidratacaoSemPerderValores() throws Exception {
        Medicao produtos = medir(get("/produtos"));
        Medicao historico = medir(get("/historico").param("mes", "6").param("ano", "1888"));
        Medicao paginaProdutos = medir(get("/produtos/pagina"));
        Medicao paginaHistorico = medir(get("/historico/pagina").param("mes", "6").param("ano", "1888"));
        Medicao resumo = medir(get("/produtos/resumo"));
        assertThat(paginaProdutos.dados().get("itens").size()).isEqualTo(50);
        assertThat(paginaHistorico.dados().get("itens").size()).isEqualTo(50);
        assertThat(paginaProdutos.bytes()).isLessThan(produtos.bytes() / 10);
        assertThat(paginaHistorico.bytes()).isLessThan(historico.bytes() / 10);
        assertThat(paginaProdutos.entidades()).isLessThanOrEqualTo(153);
        assertThat(paginaHistorico.entidades()).isLessThanOrEqualTo(204);
        assertThat(paginaProdutos.consultas()).isEqualTo(1);
        assertThat(paginaHistorico.consultas()).isEqualTo(2);
        assertThat(paginaHistorico.dados().get("totalDoMes").decimalValue()).isEqualByComparingTo(historico.dados().get("totalDoMes").decimalValue());
        assertThat(resumo.entidades()).isZero();
        assertThat(resumo.consultas()).isEqualTo(1);
        assertThat(resumo.bytes()).isLessThan(200);
        assertThat(resumo.dados().get("quantidadeProdutos").asLong()).isEqualTo(produtos.dados().size());
        double valor = StreamSupport.stream(produtos.dados().spliterator(), false)
                .mapToDouble(item -> item.get("preco").asDouble() * item.get("quantidade").asInt()).sum();
        assertThat(resumo.dados().get("valorEstoque").asDouble()).isCloseTo(valor, org.assertj.core.data.Offset.offset(0.00001));
        imprimir("PAGINA_PRODUTOS", paginaProdutos, 50);
        imprimir("PAGINA_HISTORICO", paginaHistorico, 50);
        imprimir("RESUMO_ESTOQUE", resumo, 1);
    }

    @Test
    void percorreProdutosSemDuplicarOuOmitirItens() throws Exception {
        List<Long> esperados = ids(medir(get("/produtos")).dados());
        List<Long> encontrados = new ArrayList<>();
        String cursor = null;
        int paginas = 0;
        do {
            var pedido = get("/produtos/pagina").param("tamanho", "100");
            if (cursor != null) pedido.param("antesDe", cursor);
            JsonNode pagina = medir(pedido).dados();
            List<Long> atuais = ids(pagina.get("itens"));
            assertThat(atuais).hasSizeLessThanOrEqualTo(100);
            encontrados.addAll(atuais);
            cursor = pagina.get("proximoCursor").isNull() ? null : pagina.get("proximoCursor").asString();
            assertThat(++paginas).isLessThan(100);
        } while (cursor != null);
        assertThat(encontrados).containsExactlyElementsOf(esperados);
        assertThat(new HashSet<>(encontrados)).hasSameSizeAs(encontrados);
        assertThat(medir(get("/produtos/pagina").param("antesDe", "1")).dados().get("itens").size()).isZero();
    }

    @Test
    void percorreHistoricoComDatasIguaisSemDuplicarOuOmitirItens() throws Exception {
        JsonNode completo = medir(get("/historico").param("mes", "6").param("ano", "1888")).dados();
        List<Long> esperados = ids(completo.get("lancamentos"));
        List<Long> encontrados = new ArrayList<>();
        String cursor = null;
        int paginas = 0;
        do {
            var pedido = get("/historico/pagina").param("mes", "6").param("ano", "1888").param("tamanho", "100");
            if (cursor != null) pedido.param("cursor", cursor);
            JsonNode pagina = medir(pedido).dados();
            encontrados.addAll(ids(pagina.get("itens")));
            assertThat(pagina.get("totalDoMes").decimalValue()).isEqualByComparingTo(completo.get("totalDoMes").decimalValue());
            cursor = pagina.get("proximoCursor").isNull() ? null : pagina.get("proximoCursor").asString();
            assertThat(++paginas).isLessThan(100);
        } while (cursor != null);
        assertThat(encontrados).containsExactlyElementsOf(esperados);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 101, Integer.MAX_VALUE})
    void rejeitaTamanhoInvalido(int tamanho) throws Exception {
        for (String rota : List.of("/produtos/pagina", "/historico/pagina")) {
            mvc.perform(get(rota).param("tamanho", Integer.toString(tamanho)).header("Authorization", authorization))
                    .andExpect(status().isBadRequest());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"/produtos/pagina", "/historico/pagina", "/produtos/resumo"})
    void novosRecursosExigemAutenticacao(String rota) throws Exception {
        mvc.perform(get(rota)).andExpect(status().isUnauthorized());
    }

    @Test
    void rejeitaCursoresInvalidosOuDeOutroPeriodo() throws Exception {
        mvc.perform(get("/produtos/pagina").param("antesDe", "-1").header("Authorization", authorization)).andExpect(status().isBadRequest());
        for (String cursor : List.of("", "!!!", "a".repeat(257))) {
            mvc.perform(get("/historico/pagina").param("cursor", cursor).header("Authorization", authorization)).andExpect(status().isBadRequest());
        }
        String cursor = medir(get("/historico/pagina").param("mes", "6").param("ano", "1888")).dados().get("proximoCursor").asString();
        mvc.perform(get("/historico/pagina").param("mes", "7").param("ano", "1888").param("cursor", cursor)
                        .header("Authorization", authorization)).andExpect(status().isBadRequest());
        JsonNode vazio = medir(get("/historico/pagina").param("mes", "1").param("ano", "1")).dados();
        assertThat(vazio.get("itens").size()).isZero();
        assertThat(vazio.get("proximoCursor").isNull()).isTrue();
        assertThat(vazio.get("totalDoMes").decimalValue()).isZero();
    }

    @Test
    void resumoRefleteCadastroVendaEdicaoEArquivamentoSemCache() throws Exception {
        JsonNode antes = medir(get("/produtos/resumo")).dados();
        String dados = json.writeValueAsString(java.util.Map.of("nome", "Resumo " + UUID.randomUUID(), "quantidade", 10, "preco", 12.5));
        MvcResult cadastro = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/produtos")
                        .header("Authorization", authorization).contentType("application/json").content(dados)).andExpect(status().isOk()).andReturn();
        long id = json.readTree(cadastro.getResponse().getContentAsString()).get("id").asLong();
        JsonNode cadastrado = medir(get("/produtos/resumo")).dados();
        assertThat(cadastrado.get("quantidadeProdutos").asLong()).isEqualTo(antes.get("quantidadeProdutos").asLong() + 1);
        assertThat(cadastrado.get("valorEstoque").asDouble() - antes.get("valorEstoque").asDouble()).isCloseTo(125.0, org.assertj.core.data.Offset.offset(0.00001));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/produtos/" + id + "/vender")
                        .header("Authorization", authorization).param("quantidade", "2")).andExpect(status().isOk());
        JsonNode vendido = medir(get("/produtos/resumo")).dados();
        assertThat(cadastrado.get("valorEstoque").asDouble() - vendido.get("valorEstoque").asDouble()).isCloseTo(25.0, org.assertj.core.data.Offset.offset(0.00001));
        String edicao = json.writeValueAsString(java.util.Map.of("nome", "Resumo editado", "quantidade", 6, "preco", 20));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/produtos/" + id)
                        .header("Authorization", authorization).contentType("application/json").content(edicao)).andExpect(status().isOk());
        JsonNode editado = medir(get("/produtos/resumo")).dados();
        assertThat(editado.get("valorEstoque").asDouble() - antes.get("valorEstoque").asDouble()).isCloseTo(120.0, org.assertj.core.data.Offset.offset(0.00001));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/produtos/" + id)
                        .header("Authorization", authorization)).andExpect(status().isOk());
        JsonNode removido = medir(get("/produtos/resumo")).dados();
        assertThat(removido).isEqualTo(antes);
    }

    private List<Long> ids(JsonNode itens) {
        return StreamSupport.stream(itens.spliterator(), false).map(item -> item.get("id").asLong()).toList();
    }

    @Test
    void totalPaginadoPreservaComparacaoExataDaAcaoLegada() throws Exception {
        Long produtoId = jdbc.queryForObject("SELECT MIN(id) FROM produtos WHERE nome LIKE 'SAIL-PERF-F4-%'", Long.class);
        Long usuarioId = jdbc.queryForObject("SELECT id FROM usuarios WHERE email = ?", Long.class, "performance-f4@example.invalid");
        for (String acao : List.of("VENDA", "venda", "VENDA ", "CADASTRO")) {
            jdbc.update("INSERT INTO registros (produto_id, quantidade, valor_total, data_venda, usuario_id, acao) VALUES (?, 1, 7.5, '1889-01-15 12:00:00', ?, ?)", produtoId, usuarioId, acao);
        }
        JsonNode completo = medir(get("/historico").param("mes", "1").param("ano", "1889")).dados();
        JsonNode pagina = medir(get("/historico/pagina").param("mes", "1").param("ano", "1889")).dados();
        assertThat(pagina.get("totalDoMes").decimalValue()).isEqualByComparingTo(completo.get("totalDoMes").decimalValue());
    }

    private Medicao medir(MockHttpServletRequestBuilder pedido) throws Exception {
        var estatisticas = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        boolean habilitadas = estatisticas.isStatisticsEnabled();
        estatisticas.setStatisticsEnabled(true);
        estatisticas.clear();
        try {
            long inicio = System.nanoTime();
            MvcResult resultado = mvc.perform(pedido.header("Authorization", authorization)).andExpect(status().isOk()).andReturn();
            long nanos = System.nanoTime() - inicio;
            return new Medicao(json.readTree(resultado.getResponse().getContentAsString()), resultado.getResponse().getContentAsByteArray().length,
                    estatisticas.getEntityLoadCount(), estatisticas.getPrepareStatementCount(), nanos / 1_000_000.0);
        } finally {
            estatisticas.clear();
            estatisticas.setStatisticsEnabled(habilitadas);
        }
    }

    private void imprimir(String nome, Medicao medicao, int itens) {
        System.out.printf("PERF_%s itens=%d bytes=%d entidades=%d consultas=%d tempoMs=%.2f%n",
                nome, itens, medicao.bytes(), medicao.entidades(), medicao.consultas(), medicao.tempoMs());
    }

    private record Medicao(JsonNode dados, int bytes, long entidades, long consultas, double tempoMs) {}
}
