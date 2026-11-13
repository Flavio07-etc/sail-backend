package com.faculdade.sail.service;

import com.faculdade.sail.model.Produto;
import com.faculdade.sail.model.Registro;
import com.faculdade.sail.model.Usuario;
import com.faculdade.sail.repository.ProdutoRepository;
import com.faculdade.sail.repository.RegistroRepository;
import com.faculdade.sail.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
class ProdutoTransacaoTests {

    @Autowired
    private ProdutoService service;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @MockitoSpyBean
    private RegistroRepository registroRepository;

    private Usuario usuario;

    @BeforeEach
    void prepararUsuario() {
        usuario = new Usuario();
        usuario.setNome("Usuário de teste");
        usuario.setEmail(UUID.randomUUID() + "@example.invalid");
        usuario.setSenha(UUID.randomUUID().toString());
        usuario = usuarioRepository.save(usuario);
    }

    @Test
    void confirmaCadastroComUmRegistro() {
        Produto salvo = service.salvarProduto(produto(), usuario.getId(), null);

        assertThat(produtoRepository.findById(salvo.getId())).isPresent();
        assertThat(registroRepository.findAll().stream()
                .filter(registro -> registro.getProduto().getId().equals(salvo.getId())))
                .singleElement().satisfies(registro -> {
                    assertThat(registro.getAcao()).isEqualTo("CADASTRO");
                    assertThat(registro.getQuantidade()).isEqualTo(10);
                    assertThat(registro.getValorTotal()).isZero();
                });
    }

    @Test
    void confirmaVendaComSaldoERegistroCorrespondentes() {
        Produto salvo = service.salvarProduto(produto(), usuario.getId(), null);

        service.venderProduto(salvo.getId(), 2, usuario.getId(), null);

        assertThat(produtoRepository.findById(salvo.getId()).orElseThrow().getQuantidade()).isEqualTo(8);
        assertThat(registroRepository.findAll().stream()
                .filter(registro -> registro.getProduto().getId().equals(salvo.getId()) && "VENDA".equals(registro.getAcao())))
                .singleElement().satisfies(registro -> {
                    assertThat(registro.getQuantidade()).isEqualTo(2);
                    assertThat(registro.getValorTotal()).isEqualByComparingTo("25.00");
                });
    }

    @Test
    void reverteCadastroQuandoRegistroFalha() {
        Produto novo = produto();
        falharRegistro();

        assertThatThrownBy(() -> service.salvarProduto(novo, usuario.getId(), null))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(produtoRepository.findAll()).noneMatch(produto -> produto.getNome().equals(novo.getNome()));
        assertThat(registroRepository.findAll()).noneMatch(registro -> registro.getProduto().getId().equals(novo.getId()));
    }

    @Test
    void reverteSaldoQuandoRegistroDaVendaFalha() {
        Produto salvo = service.salvarProduto(produto(), usuario.getId(), null);
        falharRegistro();

        assertThatThrownBy(() -> service.venderProduto(salvo.getId(), 2, usuario.getId(), null))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(produtoRepository.findById(salvo.getId()).orElseThrow().getQuantidade()).isEqualTo(10);
        assertThat(registroRepository.findAll()).noneMatch(registro ->
                registro.getProduto().getId().equals(salvo.getId()) && "VENDA".equals(registro.getAcao()));
    }

    private void falharRegistro() {
        doThrow(new DataIntegrityViolationException("Falha de persistência simulada"))
                .when(registroRepository).save(any(Registro.class));
    }

    private Produto produto() {
        Produto produto = new Produto();
        produto.setNome("Produto de teste " + UUID.randomUUID());
        produto.setQuantidade(10);
        produto.setPreco(12.5);
        return produto;
    }
}
