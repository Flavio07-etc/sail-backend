package com.faculdade.sail.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;

public record CadastroUsuarioEntrada(
        @Null(message = "Não informe um ID ao cadastrar uma conta.") Long id,
        @NotBlank(message = "Informe seu nome.")
        @Size(max = 255, message = "O nome deve ter no máximo 255 caracteres.") String nome,
        @NotBlank(message = "Informe seu e-mail.")
        @Email(message = "Informe um e-mail válido.")
        @Size(max = 254, message = "O e-mail deve ter no máximo 254 caracteres.") String email,
        @NotBlank(message = "Informe sua senha.")
        @Size(min = 8, max = 128, message = "A senha deve ter entre 8 e 128 caracteres.") String senha) {

    @Override
    public String toString() { return "CadastroUsuarioEntrada[credenciais protegidas]"; }
}
