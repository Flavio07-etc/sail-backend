package com.faculdade.sail.service;

import com.faculdade.sail.dtos.LoginResposta;
import com.faculdade.sail.model.Usuario;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final Clock clock;

    public TokenService(JwtEncoder encoder, Clock clock) {
        this.encoder = encoder;
        this.clock = clock;
    }

    public LoginResposta criarSessao(Usuario usuario) {
        Instant agora = clock.instant();
        Instant validade = agora.plus(Duration.ofMinutes(30));
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer("sail").subject(usuario.getId().toString())
                .audience(List.of("sail-app")).issuedAt(agora).notBefore(agora).expiresAt(validade)
                .id(UUID.randomUUID().toString()).build();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new LoginResposta(usuario.getId(), usuario.getNome(), usuario.getEmail(), token, validade);
    }
}
