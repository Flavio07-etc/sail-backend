package com.faculdade.sail.service;

import com.faculdade.sail.model.Produto;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class VersaoProduto {

    private VersaoProduto() {
    }

    public static String calcular(Produto produto) {
        String dados = produto.getId() + ":" + produto.getNome().length() + ":" + produto.getNome()
                + ":" + produto.getQuantidade() + ":" + produto.getPreco() + ":" + produto.isAtivo()
                + ":" + (produto.getAtualizadoPor() == null ? "" : produto.getAtualizadoPor().getId());
        return "\"" + assinatura(dados) + "\"";
    }

    public static String assinatura(String dados) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(dados.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException erro) {
            throw new IllegalStateException("Algoritmo obrigatório indisponível.", erro);
        }
    }
}
