package com.faculdade.sail.service;

import com.faculdade.sail.exception.ApiException;
import com.faculdade.sail.dtos.PaginaResposta;
import com.faculdade.sail.dtos.ProdutoResposta;
import com.faculdade.sail.dtos.ResumoEstoque;
import org.springframework.data.domain.PageRequest;
import com.faculdade.sail.exception.VendaInvalidaException;
import com.faculdade.sail.model.Produto;
import com.faculdade.sail.model.Registro;
import com.faculdade.sail.model.Usuario;
import com.faculdade.sail.repository.ProdutoRepository;
import com.faculdade.sail.repository.RegistroRepository;
import com.faculdade.sail.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
public class ProdutoService {

    private final RegistroRepository registroRepository;
    private final ProdutoRepository repository;
    private final UsuarioRepository usuarioRepository;

    public ProdutoService(RegistroRepository registroRepository, ProdutoRepository repository, UsuarioRepository usuarioRepository) {
        this.registroRepository = registroRepository;
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public Produto salvarProduto(Produto produto, Long usuarioId, String chave) {
        validarProduto(produto);
        if (produto.getId() != null) {
            throw ApiException.invalida("Não informe um ID ao cadastrar um produto.");
        }
        validarChave(chave);
        Usuario usuario = obterResponsavel(usuarioId);
        String assinatura = VersaoProduto.assinatura("CADASTRO:" + produto.getNome().length() + ":" + produto.getNome()
        + ":" + produto.getQuantidade() + ":" + produto.getPreco()
        + ":" + (produto.getImagem() == null ? 0 : produto.getImagem().hashCode()));
        Produto repetido = obterOperacaoAnterior(usuarioId, chave, assinatura);
        if (repetido != null) {
            return repetido;
        }
        produto.setCriadoPor(usuario);
        Produto salvo = repository.save(produto);
        registrar(salvo, salvo.getQuantidade(), BigDecimal.ZERO, usuario, "CADASTRO", chave, assinatura);
        return salvo;
    }

    @Transactional(readOnly = true)
    public List<Produto> listarProdutos() {
        return repository.findAllByAtivoTrueOrderByIdDesc();
    }

    @Transactional(readOnly = true)
    public PaginaResposta<ProdutoResposta> listarPagina(int tamanho, Long antesDe) {
        Paginacao.validarTamanho(tamanho);
        if (antesDe != null && antesDe <= 0) throw ApiException.invalida("A página de produtos é inválida. Atualize a lista.");
        var limite = PageRequest.of(0, tamanho + 1);
        List<Produto> encontrados = antesDe == null ? repository.findAllByAtivoTrueOrderByIdDesc(limite)
                : repository.findAllByAtivoTrueAndIdLessThanOrderByIdDesc(antesDe, limite);
        List<Produto> pagina = encontrados.subList(0, Math.min(tamanho, encontrados.size()));
        String proximo = encontrados.size() > tamanho ? pagina.getLast().getId().toString() : null;
        return new PaginaResposta<>(pagina.stream().map(ProdutoResposta::de).toList(), proximo);
    }

    @Transactional(readOnly = true)
    public ResumoEstoque resumirEstoque() {
        return repository.resumirEstoque();
    }

    @Transactional(readOnly = true)
    public Produto obterProduto(Long id) {
        return repository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> ApiException.naoEncontrado("Produto não encontrado no estoque."));
    }

    @Transactional
    public Produto atualizarProduto(Long id, Produto dados, Long usuarioId, String versao) {
        validarProduto(dados);
        if (dados.getId() != null && !id.equals(dados.getId())) {
            throw ApiException.invalida("O ID do produto não corresponde à operação solicitada.");
        }
        Usuario usuario = obterResponsavel(usuarioId);
        Produto produto = obterParaAtualizacao(id);
        validarVersao(produto, versao);
        long variacao = (long) dados.getQuantidade() - produto.getQuantidade();
        if (variacao > Integer.MAX_VALUE || variacao < Integer.MIN_VALUE) {
            throw ApiException.conflito("O saldo atual exige um ajuste específico antes desta alteração.");
        }
        int diferenca = (int) variacao;
            boolean imagemNova = dados.getImagem() != null && !dados.getImagem().equals(produto.getImagem());
        boolean alterado = diferenca != 0 || !Objects.equals(produto.getNome(), dados.getNome())
                || !Objects.equals(produto.getPreco(), dados.getPreco()) || imagemNova;
        if (!alterado) {
            return produto;
        }
        produto.setNome(dados.getNome());
        produto.setQuantidade(dados.getQuantidade());
        produto.setPreco(dados.getPreco());
        if (imagemNova) {
            produto.setImagem(dados.getImagem());
        }
        produto.setAtualizadoPor(usuario);
        Produto salvo = repository.save(produto);
        registrar(salvo, diferenca, BigDecimal.ZERO, usuario, diferenca == 0 ? "EDICAO" : "AJUSTE", null, null);
        return salvo;
    }

    @Transactional
    public void deletarProduto(Long id, Long usuarioId, String versao) {
        Usuario usuario = obterResponsavel(usuarioId);
        Produto produto = repository.buscarParaAtualizacao(id)
                .orElseThrow(() -> ApiException.naoEncontrado("Produto não encontrado no estoque."));
        if (!produto.isAtivo()) {
            return;
        }
        validarVersao(produto, versao);
        produto.setAtivo(false);
        produto.setAtualizadoPor(usuario);
        repository.save(produto);
        registrar(produto, 0, BigDecimal.ZERO, usuario, "ARQUIVAMENTO", null, null);
    }

    @Transactional
    public Produto venderProduto(Long id, Integer quantidade, Long usuarioId, String chave) {
        return venderProduto(id, quantidade, usuarioId, chave, null);
    }

    @Transactional
    public Produto venderProduto(Long id, Integer quantidade, Long usuarioId, String chave, String versao) {
        if (quantidade == null || quantidade <= 0) {
            throw new VendaInvalidaException("Informe uma quantidade inteira maior que zero.");
        }
        validarChave(chave);
        Usuario usuario = obterResponsavel(usuarioId);
        String assinatura = VersaoProduto.assinatura("VENDA:" + id + ":" + quantidade);
        Produto repetido = obterOperacaoAnterior(usuarioId, chave, assinatura);
        if (repetido != null) {
            return repetido;
        }
        Produto produto = obterParaAtualizacao(id);
        validarVersao(produto, versao);
        if (produto.getQuantidade() == null || produto.getQuantidade() < 0) {
            throw ApiException.conflito("Revise o saldo deste produto antes de registrar uma venda.");
        }
        if (!precoValido(produto.getPreco())) {
            throw ApiException.conflito("Revise o preço deste produto antes de registrar uma venda.");
        }
        if (produto.getQuantidade() < quantidade) {
            throw new VendaInvalidaException("Estoque insuficiente para essa venda.");
        }
        produto.setQuantidade(produto.getQuantidade() - quantidade);
        Produto salvo = repository.save(produto);
        BigDecimal total = BigDecimal.valueOf(produto.getPreco()).multiply(BigDecimal.valueOf(quantidade));
        registrar(salvo, quantidade, total, usuario, "VENDA", chave, assinatura);
        return salvo;
    }

    private void validarProduto(Produto produto) {
        if (produto == null || produto.getNome() == null || produto.getNome().isBlank()
                || produto.getNome().strip().length() > 255) {
            throw ApiException.invalida("Informe um nome de produto válido com até 255 caracteres.");
        }
        if (produto.getQuantidade() == null || produto.getQuantidade() < 0) {
            throw ApiException.invalida("Informe uma quantidade inteira de estoque maior ou igual a zero.");
        }
        Double preco = produto.getPreco();
        if (!precoValido(preco)) {
            throw ApiException.invalida("Informe um preço válido, não negativo e com até duas casas decimais.");
        }
        String imagem = produto.getImagem();
        if (imagem != null && (!imagem.startsWith("data:image/") || imagem.length() > 1_500_000)) {
            throw ApiException.invalida("A foto do produto é inválida ou grande demais.");
        }
        produto.setNome(produto.getNome().strip());
    }

    private boolean precoValido(Double preco) {
        return preco != null && Double.isFinite(preco) && preco >= 0 && preco < 1000000000000.0
                && BigDecimal.valueOf(preco).stripTrailingZeros().scale() <= 2;
    }

    private Usuario obterResponsavel(Long id) {
        return usuarioRepository.buscarParaOperacao(id)
                .orElseThrow(() -> ApiException.naoEncontrado("Usuário não encontrado!"));
    }

    private Produto obterParaAtualizacao(Long id) {
        Produto produto = repository.buscarParaAtualizacao(id)
                .orElseThrow(() -> ApiException.naoEncontrado("Produto não encontrado!"));
        if (!produto.isAtivo()) {
            throw ApiException.naoEncontrado("Produto não encontrado no estoque.");
        }
        return produto;
    }

    private void validarVersao(Produto produto, String versao) {
        if (versao != null && Arrays.stream(versao.split(",")).map(String::strip)
                .noneMatch(valor -> valor.equals("*") || valor.equals(VersaoProduto.calcular(produto)))) {
            throw new ApiException(HttpStatus.PRECONDITION_FAILED, "PRODUTO_ALTERADO", "Este produto foi alterado por outra operação. Recarregue os dados antes de salvar.");
        }
    }

    private void validarChave(String chave) {
        if (chave != null && !chave.matches("[A-Za-z0-9:_-]{1,100}")) {
            throw ApiException.invalida("A identificação da operação é inválida.");
        }
    }

    private Produto obterOperacaoAnterior(Long usuarioId, String chave, String assinatura) {
        if (chave == null) {
            return null;
        }
        return registroRepository.findByUsuarioIdAndChaveOperacao(usuarioId, chave).map(registro -> {
            if (!assinatura.equals(registro.getAssinaturaOperacao())) {
                throw ApiException.conflito("Esta identificação já foi usada em outra operação. Atualize os dados e tente novamente.");
            }
            if (!registro.getProduto().isAtivo()) {
                throw ApiException.conflito("Esta operação já foi concluída e o produto foi removido do estoque.");
            }
            return registro.getProduto();
        }).orElse(null);
    }

    private void registrar(Produto produto, int quantidade, BigDecimal total, Usuario usuario, String acao, String chave, String assinatura) {
        Registro registro = new Registro(produto, quantidade, total, usuario, acao);
        registro.setChaveOperacao(chave);
        registro.setAssinaturaOperacao(assinatura);
        registroRepository.save(registro);
    }
}
