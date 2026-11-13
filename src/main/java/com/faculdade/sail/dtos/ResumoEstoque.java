package com.faculdade.sail.dtos;

public record ResumoEstoque(Long quantidadeProdutos, Double valorEstoque, Long quantidadeSemEstoque) {
    public ResumoEstoque(Long quantidadeProdutos, Double valorEstoque) {
        this(quantidadeProdutos, valorEstoque, null);
    }
}
