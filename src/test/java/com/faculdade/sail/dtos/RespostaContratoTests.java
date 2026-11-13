package com.faculdade.sail.dtos;

import com.faculdade.sail.model.Produto;
import com.faculdade.sail.model.Registro;
import com.faculdade.sail.model.Usuario;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class RespostaContratoTests {

    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void respostaDeUsuarioPreservaJsonSemCredenciais() {
        Usuario usuario = usuario();
        verificarMesmoJson(UsuarioResposta.de(usuario), usuario);
        assertThat(json.writeValueAsString(UsuarioResposta.de(usuario))).doesNotContain("senha", "credencial-de-teste");
        assertThat(UsuarioResposta.de(null)).isNull();
    }

    @Test
    void respostaDeProdutoPreservaCamposRelacionamentosENulos() {
        Produto produto = produto();
        verificarMesmoJson(ProdutoResposta.de(produto), produto);
        produto.setAtualizadoPor(null);
        produto.setCriadoPor(null);
        verificarMesmoJson(ProdutoResposta.de(produto), produto);
    }

    @Test
    void respostaDeRegistroPreservaValoresDataHoraEUsuarioOpcional() {
        Registro registro = new Registro(produto(), 2, new BigDecimal("25.00"), usuario());
        registro.setId(7L);
        registro.setDataVenda(LocalDateTime.of(2026, 10, 29, 12, 30));
        registro.setChaveOperacao("chave-de-teste");
        registro.setAssinaturaOperacao("assinatura-de-teste");
        verificarMesmoJson(RegistroResposta.de(registro), registro);
        registro.setUsuario(null);
        verificarMesmoJson(RegistroResposta.de(registro), registro);
        assertThat(json.writeValueAsString(RegistroResposta.de(registro)))
                .doesNotContain("senha", "chaveOperacao", "assinaturaOperacao");
    }

    private void verificarMesmoJson(Object resposta, Object entidade) {
        JsonNode original = json.valueToTree(entidade);
        JsonNode atual = json.valueToTree(resposta);
        assertThat(atual).isEqualTo(original);
    }

    private Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNome("Usuário de teste");
        usuario.setEmail("teste@example.invalid");
        usuario.setSenha("credencial-de-teste");
        usuario.setSenhaCodificada(true);
        return usuario;
    }

    private Produto produto() {
        Produto produto = new Produto();
        produto.setId(2L);
        produto.setNome("Produto de teste");
        produto.setQuantidade(10);
        produto.setPreco(12.5);
        produto.setCriadoPor(usuario());
        produto.setAtualizadoPor(usuario());
        return produto;
    }
}
