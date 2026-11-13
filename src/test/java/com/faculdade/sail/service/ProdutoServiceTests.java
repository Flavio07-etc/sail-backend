package com.faculdade.sail.service;

import com.faculdade.sail.model.Produto;
import com.faculdade.sail.model.Registro;
import com.faculdade.sail.model.Usuario;
import com.faculdade.sail.exception.ApiException;
import com.faculdade.sail.repository.ProdutoRepository;
import com.faculdade.sail.repository.RegistroRepository;
import com.faculdade.sail.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProdutoServiceTests {

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private RegistroRepository registroRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private ProdutoService service;

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    void rejeitaQuantidadeInvalidaSemAcessarPersistencia(Integer quantidade) {
        assertThatThrownBy(() -> service.venderProduto(1L, quantidade, 1L, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Informe uma quantidade inteira maior que zero.");

        verifyNoInteractions(produtoRepository, registroRepository, usuarioRepository);
    }

    @Test
    void rejeitaVendaAcimaDoSaldoSemAlterarProduto() {
        Produto produto = produto(10);
        prepararVenda(produto);

        assertThatThrownBy(() -> service.venderProduto(1L, 11, 1L, null))
                .hasMessage("Estoque insuficiente para essa venda.");

        assertThat(produto.getQuantidade()).isEqualTo(10);
        verify(produtoRepository, never()).save(any());
        verifyNoInteractions(registroRepository);
    }

    @Test
    void vendeQuantidadeValidaERegistraValorCorrespondente() {
        Produto produto = produto(10);
        prepararVenda(produto);
        when(produtoRepository.save(produto)).thenReturn(produto);

        Produto resultado = service.venderProduto(1L, 2, 1L, null);

        assertThat(resultado.getQuantidade()).isEqualTo(8);
        ArgumentCaptor<Registro> captor = ArgumentCaptor.forClass(Registro.class);
        verify(registroRepository).save(captor.capture());
        assertThat(captor.getValue().getQuantidade()).isEqualTo(2);
        assertThat(captor.getValue().getValorTotal()).isEqualByComparingTo("25.00");
        assertThat(captor.getValue().getAcao()).isEqualTo("VENDA");
        assertThat(captor.getValue().getProduto()).isSameAs(produto);
        assertThat(captor.getValue().getUsuario().getId()).isEqualTo(1L);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(doubles = {-0.01, Double.NaN, Double.POSITIVE_INFINITY})
    void rejeitaVendaComPrecoLegadoInvalidoSemGravar(Double preco) {
        Produto produto = produto(10);
        produto.setPreco(preco);
        prepararVenda(produto);

        assertThatThrownBy(() -> service.venderProduto(1L, 1, 1L, null))
                .isInstanceOf(ApiException.class)
                .hasMessage("Revise o preço deste produto antes de registrar uma venda.");

        assertThat(produto.getQuantidade()).isEqualTo(10);
        verify(produtoRepository, never()).save(any());
        verifyNoInteractions(registroRepository);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {-1})
    void rejeitaVendaComSaldoLegadoInvalidoSemGravar(Integer quantidade) {
        Produto produto = produto(10);
        produto.setQuantidade(quantidade);
        prepararVenda(produto);

        assertThatThrownBy(() -> service.venderProduto(1L, 1, 1L, null))
                .isInstanceOf(ApiException.class)
                .hasMessage("Revise o saldo deste produto antes de registrar uma venda.");

        assertThat(produto.getQuantidade()).isEqualTo(quantidade);
        verify(produtoRepository, never()).save(any());
        verifyNoInteractions(registroRepository);
    }

    @Test
    void permiteVenderTodoOEstoque() {
        Produto produto = produto(10);
        prepararVenda(produto);
        when(produtoRepository.save(produto)).thenReturn(produto);

        assertThat(service.venderProduto(1L, 10, 1L, null).getQuantidade()).isZero();
        verify(registroRepository).save(any(Registro.class));
    }

    @Test
    void rejeitaUsuarioInexistenteSemGravar() {
        when(usuarioRepository.buscarParaOperacao(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.venderProduto(1L, 1, 1L, null))
                .hasMessage("Usuário não encontrado!");

        verifyNoInteractions(produtoRepository, registroRepository);
    }

    @Test
    void rejeitaProdutoInexistenteSemGravar() {
        when(usuarioRepository.buscarParaOperacao(1L)).thenReturn(Optional.of(usuario()));
        when(produtoRepository.buscarParaAtualizacao(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.venderProduto(1L, 1, 1L, null))
                .hasMessage("Produto não encontrado!");

        verify(produtoRepository, never()).save(any());
        verifyNoInteractions(registroRepository);
    }

    @Test
    void cadastraProdutoERegistroNaMesmaOperacao() {
        Usuario usuario = usuario();
        Produto produto = produto(10);
        produto.setId(null);
        when(usuarioRepository.buscarParaOperacao(1L)).thenReturn(Optional.of(usuario));
        when(produtoRepository.save(produto)).thenReturn(produto);

        assertThat(service.salvarProduto(produto, 1L, null)).isSameAs(produto);

        ArgumentCaptor<Registro> captor = ArgumentCaptor.forClass(Registro.class);
        verify(registroRepository).save(captor.capture());
        assertThat(captor.getValue().getAcao()).isEqualTo("CADASTRO");
        assertThat(captor.getValue().getQuantidade()).isEqualTo(10);
        assertThat(captor.getValue().getValorTotal()).isZero();
        assertThat(captor.getValue().getUsuario()).isSameAs(usuario);
        assertThat(captor.getValue().getProduto()).isSameAs(produto);
    }

    @Test
    void propagaFalhaDePersistenciaDoRegistro() {
        Produto produto = produto(10);
        prepararVenda(produto);
        when(produtoRepository.save(produto)).thenReturn(produto);
        DataIntegrityViolationException falha = new DataIntegrityViolationException("Falha de teste");
        when(registroRepository.save(any(Registro.class))).thenThrow(falha);

        assertThatThrownBy(() -> service.venderProduto(1L, 1, 1L, null)).isSameAs(falha);
    }

    private void prepararVenda(Produto produto) {
        when(usuarioRepository.buscarParaOperacao(1L)).thenReturn(Optional.of(usuario()));
        when(produtoRepository.buscarParaAtualizacao(1L)).thenReturn(Optional.of(produto));
    }

    private Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        return usuario;
    }

    private Produto produto(int quantidade) {
        Produto produto = new Produto();
        produto.setId(1L);
        produto.setNome("Produto de teste");
        produto.setQuantidade(quantidade);
        produto.setPreco(12.5);
        return produto;
    }
}
