package com.faculdade.sail.service;

import com.faculdade.sail.model.Usuario;
import com.faculdade.sail.repository.UsuarioRepository;
import com.faculdade.sail.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

@Service
public class UsuarioService {

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Usuario cadastrar(Usuario usuario) {
        if (usuario.getId() != null) {
            throw ApiException.invalida("Não informe um ID ao cadastrar uma conta.");
        }
        String email = usuario.getEmail().strip().toLowerCase(Locale.ROOT);
        if (repository.findByEmailIgnoreCase(email).isPresent()) {
            throw ApiException.conflito("Este e-mail já está em uso. Se você já se cadastrou, entre na sua conta.");
        }
        usuario.setNome(usuario.getNome().strip());
        usuario.setEmail(email);
        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        usuario.setSenhaCodificada(true);
        return repository.save(usuario);
    }

    @Transactional
    public Usuario fazerLogin(String email, String senha) {
        Usuario usuario = repository.findByEmailIgnoreCase(email.strip())
                .orElseThrow(this::credenciaisInvalidas);
        String armazenada = usuario.getSenha();
        boolean codificada = usuario.isSenhaCodificada();
        boolean valida = codificada ? passwordEncoder.matches(senha, armazenada)
                : MessageDigest.isEqual(armazenada.getBytes(StandardCharsets.UTF_8), senha.getBytes(StandardCharsets.UTF_8));
        if (!valida) {
            throw credenciaisInvalidas();
        }
        if (!codificada) {
            usuario.setSenha(passwordEncoder.encode(senha));
            usuario.setSenhaCodificada(true);
            repository.save(usuario);
        }
        return usuario;
    }

    private ApiException credenciaisInvalidas() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "CREDENCIAIS_INVALIDAS", "E-mail ou senha incorretos.");
    }
}
