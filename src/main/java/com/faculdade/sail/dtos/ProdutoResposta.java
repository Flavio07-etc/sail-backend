package com.faculdade.sail.dtos;

import com.faculdade.sail.model.Produto;

public record ProdutoResposta(Long id, String nome, Integer quantidade, Double preco, String imagem,
        UsuarioResposta criadoPor, UsuarioResposta atualizadoPor) {

    public static ProdutoResposta de(Produto produto) {
        return new ProdutoResposta(produto.getId(), produto.getNome(), produto.getQuantidade(), produto.getPreco(),
                produto.getImagem(),
                UsuarioResposta.de(produto.getCriadoPor()), UsuarioResposta.de(produto.getAtualizadoPor()));
    }
}