package com.faculdade.sail.config;

import com.faculdade.sail.dtos.ErroApi;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.json.JsonMapper;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Configuration
public class SegurancaConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new DelegatingPasswordEncoder("pbkdf2", Map.of("pbkdf2", Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8()));
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public SecretKey chaveJwt(@Value("${sail.auth.secret:}") String segredo) {
        byte[] bytes;
        if (segredo.isBlank()) {
            bytes = new byte[32];
            new SecureRandom().nextBytes(bytes);
        } else {
            bytes = segredo.getBytes(StandardCharsets.UTF_8);
            if (bytes.length < 32) {
                throw new IllegalStateException("A chave de autenticação configurada deve ter pelo menos 32 bytes.");
            }
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey chaveJwt) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chaveJwt));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey chaveJwt, Clock clock) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(chaveJwt).macAlgorithm(MacAlgorithm.HS256).build();
        JwtTimestampValidator validade = new JwtTimestampValidator(Duration.ZERO);
        validade.setClock(clock);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validade,
                new JwtIssuerValidator("sail"),
                new JwtClaimValidator<List<String>>("aud", audiencias -> audiencias != null && audiencias.contains("sail-app"))));
        return decoder;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sessao -> sessao.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(acesso -> acesso
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/usuarios/login", "/usuarios/cadastrar", "/error").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults())
                        .authenticationEntryPoint((request, response, erro) -> escreverErro(response, 401, "SESSAO_INVALIDA", "Sua sessão expirou ou é inválida. Entre novamente."))
                        .accessDeniedHandler((request, response, erro) -> escreverErro(response, 403, "ACESSO_NEGADO", "Você não tem permissão para realizar esta operação.")))
                .exceptionHandling(erros -> erros
                        .authenticationEntryPoint((request, response, erro) -> escreverErro(response, 401, "SESSAO_INVALIDA", "Entre na sua conta para continuar."))
                        .accessDeniedHandler((request, response, erro) -> escreverErro(response, 403, "ACESSO_NEGADO", "Você não tem permissão para realizar esta operação.")))
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "usuarioId", "Idempotency-Key", "If-Match"));
        config.setExposedHeaders(List.of("ETag"));
        config.setAllowCredentials(false);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private void escreverErro(HttpServletResponse response, int status, String codigo, String mensagem) throws IOException {
        response.setStatus(status);
        if (status == 401) response.setHeader("WWW-Authenticate", "Bearer");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        JsonMapper.builder().build().writeValue(response.getWriter(), new ErroApi(status, codigo, mensagem));
    }
}
