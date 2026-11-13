package com.faculdade.sail.exception;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String codigo;

    public ApiException(HttpStatus status, String codigo, String mensagem) {
        super(mensagem);
        this.status = status;
        this.codigo = codigo;
    }

    public HttpStatus getStatus() { return status; }
    public String getCodigo() { return codigo; }

    public static ApiException invalida(String mensagem) {
        return new ApiException(HttpStatus.BAD_REQUEST, "OPERACAO_INVALIDA", mensagem);
    }

    public static ApiException naoEncontrado(String mensagem) {
        return new ApiException(HttpStatus.NOT_FOUND, "RECURSO_NAO_ENCONTRADO", mensagem);
    }

    public static ApiException conflito(String mensagem) {
        return new ApiException(HttpStatus.CONFLICT, "CONFLITO", mensagem);
    }
}
