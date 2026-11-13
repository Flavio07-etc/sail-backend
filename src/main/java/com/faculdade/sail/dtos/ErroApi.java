package com.faculdade.sail.dtos;

import java.util.Map;

public record ErroApi(int status, String codigo, String mensagem, Map<String, String> campos) {

    public ErroApi(int status, String codigo, String mensagem) {
        this(status, codigo, mensagem, Map.of());
    }
}
