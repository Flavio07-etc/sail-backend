package com.faculdade.sail.dtos;

import java.time.Instant;

public record LoginResposta(Long id, String nome, String email, String token, Instant expiraEm) {

    @Override
    public String toString() { return "LoginResposta[id=" + id + ", token protegido]"; }
}
