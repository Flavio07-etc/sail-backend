package com.faculdade.sail.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginEntrada(
        @NotBlank(message = "Informe seu e-mail.")
        @Email(message = "Informe um e-mail válido.")
        @Size(max = 254, message = "Informe um e-mail válido.") String email,
        @NotBlank(message = "Informe sua senha.")
        @Size(max = 255, message = "A senha informada está acima do limite permitido.") String senha) {

    @Override
    public String toString() { return "LoginEntrada[credenciais protegidas]"; }
}
