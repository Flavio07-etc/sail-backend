package com.faculdade.sail.controller;

import com.faculdade.sail.model.Usuario;
import com.faculdade.sail.service.UsuarioService;
import com.faculdade.sail.service.TokenService;
import com.faculdade.sail.dtos.CadastroUsuarioEntrada;
import com.faculdade.sail.dtos.LoginEntrada;
import com.faculdade.sail.dtos.LoginResposta;
import com.faculdade.sail.dtos.UsuarioResposta;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService service;
    private final TokenService tokenService;

    public UsuarioController(UsuarioService service, TokenService tokenService) {
        this.service = service;
        this.tokenService = tokenService;
    }

    
    @PostMapping("/cadastrar")
    public ResponseEntity<UsuarioResposta> cadastrar(@Valid @RequestBody CadastroUsuarioEntrada entrada) {
        Usuario usuario = new Usuario();
        usuario.setNome(entrada.nome());
        usuario.setEmail(entrada.email());
        usuario.setSenha(entrada.senha());
        return ResponseEntity.ok(UsuarioResposta.de(service.cadastrar(usuario)));
    }

    
    @PostMapping("/login")
    public ResponseEntity<LoginResposta> login(@Valid @RequestBody LoginEntrada entrada) {
        return ResponseEntity.ok(tokenService.criarSessao(service.fazerLogin(entrada.email(), entrada.senha())));
    }
}
