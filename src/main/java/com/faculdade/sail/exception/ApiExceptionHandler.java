package com.faculdade.sail.exception;

import com.faculdade.sail.dtos.ErroApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErroApi> tratarNegocio(ApiException erro) {
        return ResponseEntity.status(erro.getStatus())
                .body(new ErroApi(erro.getStatus().value(), erro.getCodigo(), erro.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroApi> tratarValidacao(MethodArgumentNotValidException erro) {
        Map<String, String> campos = new LinkedHashMap<>();
        erro.getBindingResult().getFieldErrors().forEach(campo ->
                campos.putIfAbsent(campo.getField(), campo.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ErroApi(400, "DADOS_INVALIDOS", "Revise os campos informados.", campos));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, MissingRequestHeaderException.class})
    public ResponseEntity<ErroApi> tratarFormato(Exception erro) {
        return resposta(HttpStatus.BAD_REQUEST, "REQUISICAO_INVALIDA", "Confira os dados e parâmetros enviados.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroApi> tratarIntegridade(DataIntegrityViolationException erro) {
        logger.warn("Operação rejeitada por restrição de integridade: {}", erro.getClass().getSimpleName());
        return resposta(HttpStatus.CONFLICT, "CONFLITO_DE_DADOS", "Não foi possível concluir a operação porque os dados estão em conflito. Atualize as informações e tente novamente.");
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<ErroApi> tratarConcorrencia(PessimisticLockingFailureException erro) {
        return resposta(HttpStatus.CONFLICT, "OPERACAO_CONCORRENTE", "Outra operação está alterando este produto. Atualize o estoque e tente novamente.");
    }

    @ExceptionHandler({DataAccessResourceFailureException.class, QueryTimeoutException.class})
    public ResponseEntity<ErroApi> tratarIndisponibilidade(DataAccessException erro) {
        logger.error("Persistência temporariamente indisponível: {}", erro.getClass().getSimpleName());
        return resposta(HttpStatus.SERVICE_UNAVAILABLE, "SERVICO_INDISPONIVEL", "O serviço está temporariamente indisponível. Tente novamente em instantes.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroApi> tratarAcesso(AccessDeniedException erro) {
        return resposta(HttpStatus.FORBIDDEN, "ACESSO_NEGADO", "Você não tem permissão para realizar esta operação.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErroApi> tratarRota(NoResourceFoundException erro) {
        return resposta(HttpStatus.NOT_FOUND, "RECURSO_NAO_ENCONTRADO", "O recurso solicitado não foi encontrado.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErroApi> tratarMetodo(HttpRequestMethodNotSupportedException erro) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .allow(erro.getSupportedHttpMethods().toArray(org.springframework.http.HttpMethod[]::new))
                .body(new ErroApi(405, "METODO_NAO_PERMITIDO", "Esta operação não está disponível para o recurso solicitado."));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErroApi> tratarConteudo(HttpMediaTypeNotSupportedException erro) {
        return resposta(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "FORMATO_NAO_SUPORTADO", "Envie os dados no formato JSON.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroApi> tratarInesperado(Exception erro) {
        logger.error("Falha inesperada ao processar operação", erro);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, "ERRO_INTERNO", "Não foi possível concluir a operação. Tente novamente em instantes.");
    }

    private ResponseEntity<ErroApi> resposta(HttpStatus status, String codigo, String mensagem) {
        return ResponseEntity.status(status).body(new ErroApi(status.value(), codigo, mensagem));
    }
}
