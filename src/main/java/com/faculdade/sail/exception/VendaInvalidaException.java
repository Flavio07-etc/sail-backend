package com.faculdade.sail.exception;

import org.springframework.http.HttpStatus;

public class VendaInvalidaException extends ApiException {

    public VendaInvalidaException(String mensagem) {
        super(HttpStatus.BAD_REQUEST, "VENDA_INVALIDA", mensagem);
    }
}
