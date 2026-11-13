package com.faculdade.sail.service;

import com.faculdade.sail.exception.ApiException;

import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.util.Base64;

record CursorHistorico(LocalDateTime data, Long id) {

    String codificar(int mes, int ano) {
        String valor = mes + ";" + ano + ";" + data + ";" + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(valor.getBytes(StandardCharsets.UTF_8));
    }

    static CursorHistorico decodificar(String valor, int mes, int ano) {
        if (valor == null) return null;
        try {
            if (valor.isBlank() || valor.length() > 256) throw new IllegalArgumentException();
            String[] partes = new String(Base64.getUrlDecoder().decode(valor), StandardCharsets.UTF_8).split(";", -1);
            if (partes.length != 4 || Integer.parseInt(partes[0]) != mes || Integer.parseInt(partes[1]) != ano) {
                throw new IllegalArgumentException();
            }
            LocalDateTime data = LocalDateTime.parse(partes[2]);
            Long id = Long.valueOf(partes[3]);
            if (id <= 0 || data.getYear() < 1 || data.getYear() > 9999) throw new IllegalArgumentException();
            return new CursorHistorico(data, id);
        } catch (IllegalArgumentException | DateTimeException erro) {
            throw ApiException.invalida("A página do histórico é inválida. Atualize a lista e tente novamente.");
        }
    }
}
