package com.faculdade.sail.dtos;

import com.faculdade.sail.model.Usuario;

public record UsuarioResposta(Long id, String nome, String email) {

    public static UsuarioResposta de(Usuario usuario) {
        return usuario == null ? null : new UsuarioResposta(usuario.getId(), usuario.getNome(), usuario.getEmail());
    }
}
