package com.faculdade.sail.service;

import com.faculdade.sail.exception.ApiException;

final class Paginacao {

    private Paginacao() {}

    static void validarTamanho(int tamanho) {
        if (tamanho < 1 || tamanho > 100) {
            throw ApiException.invalida("Solicite entre 1 e 100 itens por página.");
        }
    }
}
