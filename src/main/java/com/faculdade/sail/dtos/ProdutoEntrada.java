package com.faculdade.sail.dtos;

import com.faculdade.sail.model.Produto;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProdutoEntrada(
        Long id,
        @NotBlank(message = "Informe o nome do produto.")
        @Size(max = 255, message = "O nome deve ter no máximo 255 caracteres.") String nome,
        @NotNull(message = "Informe a quantidade em estoque.")
        @DecimalMin(value = "0", message = "O estoque não pode ser negativo.")
        @DecimalMax(value = "2147483647", message = "A quantidade está acima do limite permitido.")
        @Digits(integer = 10, fraction = 0, message = "A quantidade deve ser um número inteiro.") BigDecimal quantidade,
        @NotNull(message = "Informe o preço do produto.")
        @DecimalMin(value = "0", message = "O preço não pode ser negativo.")
        @Digits(integer = 12, fraction = 2, message = "Informe um preço válido com até duas casas decimais.") BigDecimal preco,
        String imagem) {

    public Produto paraProduto() {
        Produto produto = new Produto();
        produto.setId(id);
        produto.setNome(nome.strip());
        produto.setQuantidade(quantidade.intValueExact());
        produto.setPreco(preco.doubleValue());
        return produto;
    }
}
